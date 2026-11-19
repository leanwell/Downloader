package com.leanwell.downloader.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.annotation.VisibleForTesting
import com.leanwell.downloader.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed cryptographic vault for user cookies and authentication tokens.
 *
 * Security Features:
 * - Hardware-backed Android Keystore master key (AES-256).
 * - Authenticated encryption via AES/GCM/NoPadding (12-byte IV, 128-bit authentication tag).
 * - GCM AAD binds file header (magic, version, timestamps) and cookie type to the ciphertext,
 *   preventing timestamp tampering and cross-type file-swap attacks (v2 format).
 * - Enforces automatic expiration (default 30 days) with automated cryptographic shredding upon expiry.
 * - Stored exclusively in context.noBackupFilesDir to prevent cloud or device backup exfiltration.
 * - Ephemeral file decryption for yt-dlp execution with owner-only permissions, in-place zero-fill
 *   byte shredding, and immediate deletion.
 * - Full session purging (Keystore encrypted files, WebView cookies, and WebStorage).
 * - Automatic purge of corrupted files (AEADBadTagException) and invalidated Keystore keys.
 */
object SecureCookieVault {

    private const val TAG = "SecureCookieVault"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "DownloaderCookieMasterKey"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    // 4-byte magic identifier "SECV" + 1-byte version
    private val MAGIC_HEADER = byteArrayOf(0x53, 0x45, 0x43, 0x56)
    private const val FORMAT_VERSION: Byte = 2
    private const val LEGACY_FORMAT_VERSION: Byte = 1

    const val DEFAULT_EXPIRATION_DAYS = 30
    const val NOTICE_AUTO_DELETE = "Sessions automatically expire and are securely wiped after 30 days."

    /** Tracks active ephemeral file usage to prevent races with purge/cleanup operations. */
    private val activeEphemeralCount = AtomicInteger(0)

    enum class CookieType(val encryptedFileName: String, val legacyFileName: String, val displayName: String) {
        YOUTUBE("youtube_cookies.enc", "youtube_cookies.txt", "YouTube"),
        FACEBOOK("facebook_cookies.enc", "facebook_cookies.txt", "Facebook"),
        INSTAGRAM("instagram_cookies.enc", "instagram_cookies.txt", "Instagram")
    }

    data class SessionMeta(
        val type: CookieType,
        val createdAt: Long,
        val expiresAt: Long,
        val isExpired: Boolean,
        val remainingDays: Int,
        val formattedExpiration: String
    )

    @VisibleForTesting
    internal var masterKeyProvider: () -> SecretKey = ::getOrCreateMasterKey

    @VisibleForTesting
    internal var encryptedFileDirProvider: (Context) -> File = { it.noBackupFilesDir }

    @VisibleForTesting
    internal var ephemeralDirProvider: (Context) -> File = { File(it.cacheDir, ".vault_ephemeral") }

    @VisibleForTesting
    internal var legacyFilesDirProvider: (Context) -> File = { it.filesDir }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
            if (key != null) return key
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Deletes the master key alias from AndroidKeyStore when permanently invalidated.
     */
    @Synchronized
    fun resetMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Deleted invalidated master key alias '$MASTER_KEY_ALIAS' from KeyStore.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete invalidated KeyStore entry", e)
        }
    }

    private fun getEncryptedFile(context: Context, type: CookieType): File {
        return File(encryptedFileDirProvider(context), type.encryptedFileName)
    }

    /**
     * Saves raw cookie data into the encrypted vault with an expiration date.
     * Overwrites any prior session and securely isolates it in noBackupFilesDir.
     *
     * The file header (magic, version, timestamps) and cookie type name are authenticated
     * as GCM AAD, binding them cryptographically to the ciphertext.
     *
     * @throws IllegalArgumentException if [expirationDays] is not in 1..365.
     */
    @Synchronized
    fun saveCookies(
        context: Context,
        type: CookieType,
        content: String,
        expirationDays: Int = DEFAULT_EXPIRATION_DAYS
    ): Boolean {
        if (content.isBlank()) return false
        require(expirationDays in 1..365) {
            "Expiration must be between 1 and 365 days, was $expirationDays"
        }
        return try {
            val masterKey = masterKeyProvider()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv

            val now = System.currentTimeMillis()
            val expiresAt = now + (expirationDays.toLong() * 24 * 60 * 60 * 1000L)

            // Build the 21-byte file header: MAGIC(4) + VERSION(1) + createdAt(8) + expiresAt(8)
            val headerBuf = ByteBuffer.allocate(4 + 1 + 16)
            headerBuf.put(MAGIC_HEADER)
            headerBuf.put(FORMAT_VERSION)
            headerBuf.putLong(now)
            headerBuf.putLong(expiresAt)
            val headerBytes = headerBuf.array()

            // Authenticate header + cookie type name as GCM AAD
            val typeNameBytes = type.name.toByteArray(Charsets.UTF_8)
            cipher.updateAAD(headerBytes)
            cipher.updateAAD(typeNameBytes)

            val plainBytes = content.toByteArray(Charsets.UTF_8)
            val cipherText = cipher.doFinal(plainBytes)

            val targetFile = getEncryptedFile(context, type)
            FileOutputStream(targetFile).use { fos ->
                fos.write(headerBytes)
                // Write 12-byte IV
                fos.write(iv)
                // Write ciphertext + authentication tag
                fos.write(cipherText)
                fos.flush()
            }

            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Encrypted and saved ${type.displayName} session. Expires in $expirationDays days.")
            }
            true
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Keystore key permanently invalidated for ${type.displayName}. Resetting key and purging.", e)
            resetMasterKey()
            shredAndPurge(context, type)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save cookies for ${type.displayName}", e)
            false
        }
    }

    /**
     * Reads session metadata (creation time, expiration, remaining days) without decrypting the payload.
     * If the session has expired, automatically purges the file.
     * Accepts both v1 (legacy) and v2 (current) format files.
     */
    @Synchronized
    fun getSessionMeta(context: Context, type: CookieType): SessionMeta? {
        val file = getEncryptedFile(context, type)
        if (!file.exists() || file.length() < 37) return null // 4 + 1 + 16 + 12 + 4 min

        var isExpired = false
        var expiredTime: Long = 0
        var meta: SessionMeta? = null

        try {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                if (fis.read(magic) != 4 || !magic.contentEquals(MAGIC_HEADER)) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Corrupt or unrecognized magic header for ${type.displayName}")
                    }
                    return null
                }

                val version = fis.read()
                if (version != FORMAT_VERSION.toInt() && version != LEGACY_FORMAT_VERSION.toInt()) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Unsupported format version $version for ${type.displayName}")
                    }
                    return null
                }

                val timeBytes = ByteArray(16)
                if (fis.read(timeBytes) != 16) return null
                val timeBuf = ByteBuffer.wrap(timeBytes)
                val createdAt = timeBuf.long
                val expiresAt = timeBuf.long

                val now = System.currentTimeMillis()
                isExpired = now >= expiresAt
                expiredTime = expiresAt
                val remainingMs = (expiresAt - now).coerceAtLeast(0)
                val remainingDays = (remainingMs / (24 * 60 * 60 * 1000L)).toInt()

                if (!isExpired) {
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    meta = SessionMeta(
                        type = type,
                        createdAt = createdAt,
                        expiresAt = expiresAt,
                        isExpired = false,
                        remainingDays = remainingDays,
                        formattedExpiration = dateFormat.format(Date(expiresAt))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting session metadata for ${type.displayName}", e)
            return null
        }

        if (isExpired) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "${type.displayName} session has expired on ${Date(expiredTime)}. Securely shredding...")
            }
            shredAndPurge(context, type)
            return null
        }

        return meta
    }

    /**
     * Checks if active, unexpired cookies exist for the specified type.
     * Auto-deletes expired data if found.
     */
    fun hasValidCookies(context: Context, type: CookieType): Boolean {
        return getSessionMeta(context, type) != null
    }

    /**
     * Decrypts cookies and returns the raw plaintext bytes.
     * Handles both v1 (no AAD) and v2 (authenticated header + type AAD) formats.
     * Automatically purges the encrypted file on authentication failure or Keystore invalidation.
     *
     * Callers are responsible for zeroing the returned [ByteArray] after use.
     */
    @Synchronized
    private fun decryptCookiesBytes(context: Context, type: CookieType): ByteArray? {
        getSessionMeta(context, type) ?: return null
        val file = getEncryptedFile(context, type)

        return try {
            val bytes = file.readBytes()
            val headerOffset = 4 + 1 + 16 // 21 bytes
            val version = bytes[4].toInt()
            val iv = bytes.copyOfRange(headerOffset, headerOffset + GCM_IV_LENGTH_BYTES)
            val cipherText = bytes.copyOfRange(headerOffset + GCM_IV_LENGTH_BYTES, bytes.size)

            val masterKey = masterKeyProvider()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

            // v2: authenticate header + cookie type name as AAD
            if (version == FORMAT_VERSION.toInt()) {
                val headerBytes = bytes.copyOfRange(0, headerOffset)
                val typeNameBytes = type.name.toByteArray(Charsets.UTF_8)
                cipher.updateAAD(headerBytes)
                cipher.updateAAD(typeNameBytes)
            }
            // v1 (legacy): decrypt without AAD

            cipher.doFinal(cipherText)
        } catch (e: AEADBadTagException) {
            Log.e(TAG, "Authentication tag mismatch for ${type.displayName} — file tampered or type swapped. Purging.", e)
            shredAndPurge(context, type)
            null
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Keystore key permanently invalidated for ${type.displayName}. Resetting key and purging.", e)
            resetMasterKey()
            shredAndPurge(context, type)
            null
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Security exception decrypting ${type.displayName}. Purging.", e)
            shredAndPurge(context, type)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt cookies for ${type.displayName}", e)
            null
        }
    }

    /**
     * Decrypts cookies directly into memory. Returns null if unconfigured or expired.
     * The decrypted byte buffer is zeroed immediately after conversion to String.
     */
    @Synchronized
    fun decryptCookies(context: Context, type: CookieType): String? {
        val plainBytes = decryptCookiesBytes(context, type) ?: return null
        return try {
            String(plainBytes, Charsets.UTF_8)
        } finally {
            Arrays.fill(plainBytes, 0.toByte())
        }
    }

    /**
     * Decrypts cookies into an ephemeral, temporary file inside the app cache solely for yt-dlp execution.
     * The file is created with owner-only read permissions. In a finally block, the decrypted byte buffer
     * is zeroed and the file is overwritten in-place with zeros and deleted immediately.
     *
     * Note: The in-place zero-fill is best-effort on flash storage; see [shredFile] for details.
     */
    fun <T> withEphemeralCookieFile(
        context: Context,
        type: CookieType,
        block: (File?) -> T
    ): T {
        val plainBytes = decryptCookiesBytes(context, type)
        if (plainBytes == null || plainBytes.isEmpty()) {
            return block(null)
        }

        activeEphemeralCount.incrementAndGet()
        try {
            val tempDir = ephemeralDirProvider(context).apply { mkdirs() }
            val tempFile = File(tempDir, "ephem_${type.name.lowercase()}_${UUID.randomUUID().toString().take(8)}.txt")

            try {
                tempFile.createNewFile()
                tempFile.setReadable(false, false)
                tempFile.setReadable(true, true)
                tempFile.setWritable(false, false)
                tempFile.setWritable(true, true)
                tempFile.writeBytes(plainBytes)
                return block(tempFile)
            } finally {
                shredFile(tempFile)
            }
        } finally {
            Arrays.fill(plainBytes, 0.toByte())
            activeEphemeralCount.decrementAndGet()
        }
    }

    /**
     * Overwrites a file's content in-place with zeros before deletion.
     *
     * Uses [RandomAccessFile] with mode `"rws"` to seek to position 0 and overwrite existing
     * data blocks, with every write synchronized to the underlying storage device.
     *
     * **Best-effort on flash storage**: On eMMC/UFS media with ext4/F2FS, the Flash Translation
     * Layer (FTL) may remap writes to new physical NAND pages via wear-leveling. Original data
     * can persist in deallocated physical pages until the FTL's garbage collector reclaims them.
     * This overwrite narrows the exposure window and defeats logical-level recovery, but cannot
     * guarantee forensic-level erasure on flash media.
     */
    fun shredFile(file: File) {
        if (!file.exists()) return
        try {
            val length = file.length().coerceAtMost(2 * 1024 * 1024) // up to 2MB
            if (length > 0) {
                val zeros = ByteArray(length.toInt())
                Arrays.fill(zeros, 0.toByte())
                RandomAccessFile(file, "rws").use { raf ->
                    raf.seek(0)
                    raf.write(zeros)
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Zero-fill wipe encountered issue: ${e.message}")
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Securely deletes the encrypted cookie file for a specific provider.
     */
    @Synchronized
    fun shredAndPurge(context: Context, type: CookieType) {
        val file = getEncryptedFile(context, type)
        if (file.exists()) {
            shredFile(file)
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Purged ${type.displayName} encrypted session.")
            }
        }
    }

    /**
     * Checks all cookie types and auto-purges any expired sessions.
     */
    fun purgeExpiredSessions(context: Context) {
        CookieType.entries.forEach { type ->
            getSessionMeta(context, type) // Auto-shreds if expired
        }
    }

    /**
     * Cleans up any orphaned ephemeral cookie files left behind by app crashes or process kills.
     * Skips cleanup if any ephemeral files are actively in use by a download to prevent races.
     */
    fun cleanupOrphanedEphemeralFiles(context: Context) {
        if (activeEphemeralCount.get() > 0) return
        try {
            val ephemDir = ephemeralDirProvider(context)
            if (ephemDir.exists()) {
                ephemDir.listFiles()?.forEach { shredFile(it) }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed cleaning orphaned ephemeral files: ${e.message}")
            }
        }
    }

    /**
     * Performs a complete session purge:
     * 1. Shreds all encrypted session files in noBackupFilesDir.
     * 2. Clears all WebView CookieManager cookies.
     * 3. Clears WebStorage and WebView cache.
     * 4. Cleans any leftover ephemeral files in cacheDir (if no downloads are active).
     */
    @Synchronized
    fun purgeAllSessions(context: Context) {
        CookieType.entries.forEach { type ->
            shredAndPurge(context, type)
        }

        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed clearing CookieManager: ${e.message}")
            }
        }

        try {
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed clearing WebStorage: ${e.message}")
            }
        }

        // Only clean ephemeral files if no downloads are actively using them
        if (activeEphemeralCount.get() == 0) {
            try {
                val ephemDir = ephemeralDirProvider(context)
                if (ephemDir.exists()) {
                    ephemDir.listFiles()?.forEach { shredFile(it) }
                    ephemDir.delete()
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Failed wiping ephemeral temp dir: ${e.message}")
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Completed full session and credential purge.")
        }
    }

    /**
     * Migrates legacy unencrypted .txt cookie files into the encrypted vault,
     * then shreds the legacy files only after confirming successful encryption.
     *
     * If the vault already contains valid cookies for a type, the legacy file is shredded
     * without re-importing. If encryption fails, the legacy file is preserved for the next attempt.
     */
    @Synchronized
    fun migrateLegacyPlaintextCookies(context: Context) {
        CookieType.entries.forEach { type ->
            val legacyFile = File(legacyFilesDirProvider(context), type.legacyFileName)
            if (legacyFile.exists() && legacyFile.length() > 0) {
                if (hasValidCookies(context, type)) {
                    // Already migrated — shred the leftover legacy file
                    shredFile(legacyFile)
                } else {
                    try {
                        val rawContent = legacyFile.readText(Charsets.UTF_8)
                        if (rawContent.isNotBlank()) {
                            val saved = saveCookies(context, type, rawContent, DEFAULT_EXPIRATION_DAYS)
                            if (saved) {
                                if (BuildConfig.DEBUG) {
                                    Log.i(TAG, "Migrated legacy plaintext cookies for ${type.displayName} to SecureCookieVault.")
                                }
                                shredFile(legacyFile)
                            }
                            // If !saved, leave legacy file intact for next attempt
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate legacy cookies for ${type.displayName}", e)
                        // Leave legacy file intact on failure
                    }
                }
            }
        }
    }
}
