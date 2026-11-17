package com.leanwell.downloader.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.UUID
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
 * - Enforces automatic expiration (default 30 days) with automated cryptographic shredding upon expiry.
 * - Stored exclusively in context.noBackupFilesDir to prevent cloud or device backup exfiltration.
 * - Ephemeral file decryption for yt-dlp execution with zero-fill byte shredding and immediate deletion.
 * - Full session purging (Keystore encrypted files, WebView cookies, and WebStorage).
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
    private const val FORMAT_VERSION: Byte = 1

    const val DEFAULT_EXPIRATION_DAYS = 30
    const val NOTICE_AUTO_DELETE = "Sessions automatically expire and are securely wiped after 30 days."

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

    private fun getEncryptedFile(context: Context, type: CookieType): File {
        return File(context.noBackupFilesDir, type.encryptedFileName)
    }

    /**
     * Saves raw cookie data into the encrypted vault with an expiration date.
     * Overwrites any prior session and securely isolates it in noBackupFilesDir.
     */
    @Synchronized
    fun saveCookies(
        context: Context,
        type: CookieType,
        content: String,
        expirationDays: Int = DEFAULT_EXPIRATION_DAYS
    ): Boolean {
        if (content.isBlank()) return false
        return try {
            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv

            val plainBytes = content.toByteArray(Charsets.UTF_8)
            val cipherText = cipher.doFinal(plainBytes)

            val now = System.currentTimeMillis()
            val expiresAt = now + (expirationDays.toLong() * 24 * 60 * 60 * 1000L)

            val targetFile = getEncryptedFile(context, type)
            FileOutputStream(targetFile).use { fos ->
                fos.write(MAGIC_HEADER)
                fos.write(byteArrayOf(FORMAT_VERSION))

                // Write 8-byte creation timestamp and 8-byte expiration timestamp
                val timeBuf = ByteBuffer.allocate(16)
                timeBuf.putLong(now)
                timeBuf.putLong(expiresAt)
                fos.write(timeBuf.array())

                // Write 12-byte IV
                fos.write(iv)

                // Write ciphertext + authentication tag
                fos.write(cipherText)
                fos.flush()
            }

            Log.i(TAG, "Encrypted and saved ${type.displayName} session. Expires in $expirationDays days.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save cookies for ${type.displayName}", e)
            false
        }
    }

    /**
     * Reads session metadata (creation time, expiration, remaining days) without decrypting the payload.
     * If the session has expired, automatically purges the file.
     */
    @Synchronized
    fun getSessionMeta(context: Context, type: CookieType): SessionMeta? {
        val file = getEncryptedFile(context, type)
        if (!file.exists() || file.length() < 37) return null // 4 + 1 + 16 + 12 + 4 min

        try {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                if (fis.read(magic) != 4 || !magic.contentEquals(MAGIC_HEADER)) {
                    Log.w(TAG, "Corrupt or unrecognized magic header for ${type.displayName}")
                    return null
                }

                val version = fis.read()
                if (version != FORMAT_VERSION.toInt()) {
                    Log.w(TAG, "Unsupported format version $version for ${type.displayName}")
                    return null
                }

                val timeBytes = ByteArray(16)
                if (fis.read(timeBytes) != 16) return null
                val timeBuf = ByteBuffer.wrap(timeBytes)
                val createdAt = timeBuf.long
                val expiresAt = timeBuf.long

                val now = System.currentTimeMillis()
                val isExpired = now >= expiresAt
                val remainingMs = (expiresAt - now).coerceAtLeast(0)
                val remainingDays = (remainingMs / (24 * 60 * 60 * 1000L)).toInt()

                if (isExpired) {
                    Log.w(TAG, "${type.displayName} session has expired on ${Date(expiresAt)}. Securely shredding...")
                    shredAndPurge(context, type)
                    return null
                }

                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                return SessionMeta(
                    type = type,
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    isExpired = false,
                    remainingDays = remainingDays,
                    formattedExpiration = dateFormat.format(Date(expiresAt))
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting session metadata for ${type.displayName}", e)
            return null
        }
    }

    /**
     * Checks if active, unexpired cookies exist for the specified type.
     * Auto-deletes expired data if found.
     */
    fun hasValidCookies(context: Context, type: CookieType): Boolean {
        return getSessionMeta(context, type) != null
    }

    /**
     * Decrypts cookies directly into memory. Returns null if unconfigured or expired.
     */
    @Synchronized
    fun decryptCookies(context: Context, type: CookieType): String? {
        val meta = getSessionMeta(context, type) ?: return null
        val file = getEncryptedFile(context, type)

        return try {
            val bytes = file.readBytes()
            val headerOffset = 4 + 1 + 16 // 21 bytes
            val iv = bytes.copyOfRange(headerOffset, headerOffset + GCM_IV_LENGTH_BYTES)
            val cipherText = bytes.copyOfRange(headerOffset + GCM_IV_LENGTH_BYTES, bytes.size)

            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt cookies for ${type.displayName}", e)
            null
        }
    }

    /**
     * Decrypts cookies into an ephemeral, temporary file inside the app cache solely for yt-dlp execution.
     * In a finally block, the file's bytes are systematically overwritten with zeros and deleted immediately.
     */
    fun <T> withEphemeralCookieFile(
        context: Context,
        type: CookieType,
        block: (File?) -> T
    ): T {
        val content = decryptCookies(context, type)
        if (content.isNullOrBlank()) {
            return block(null)
        }

        val tempDir = File(context.cacheDir, ".vault_ephemeral").apply { mkdirs() }
        val tempFile = File(tempDir, "ephem_${type.name.lowercase()}_${UUID.randomUUID().toString().take(8)}.txt")

        try {
            tempFile.writeText(content, Charsets.UTF_8)
            return block(tempFile)
        } finally {
            shredFile(tempFile)
        }
    }

    /**
     * Securely shreds a file by overwriting its content with zeros before deletion.
     */
    fun shredFile(file: File) {
        if (!file.exists()) return
        try {
            val length = file.length().coerceAtMost(2 * 1024 * 1024) // up to 2MB
            if (length > 0) {
                val zeros = ByteArray(length.toInt())
                Arrays.fill(zeros, 0.toByte())
                FileOutputStream(file).use { fos ->
                    fos.write(zeros)
                    fos.flush()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Zero-fill wipe encountered issue: ${e.message}")
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
            Log.i(TAG, "Purged ${type.displayName} encrypted session.")
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
     * Performs a complete session purge:
     * 1. Shreds all encrypted session files in noBackupFilesDir.
     * 2. Clears all WebView CookieManager cookies.
     * 3. Clears WebStorage and WebView cache.
     * 4. Cleans any leftover ephemeral files in cacheDir.
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
            Log.w(TAG, "Failed clearing CookieManager: ${e.message}")
        }

        try {
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            Log.w(TAG, "Failed clearing WebStorage: ${e.message}")
        }

        try {
            val ephemDir = File(context.cacheDir, ".vault_ephemeral")
            if (ephemDir.exists()) {
                ephemDir.listFiles()?.forEach { shredFile(it) }
                ephemDir.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed wiping ephemeral temp dir: ${e.message}")
        }

        Log.i(TAG, "Completed full session and credential purge.")
    }

    /**
     * Migrates legacy unencrypted .txt cookie files into the encrypted vault,
     * then shreds the legacy files immediately.
     */
    @Synchronized
    fun migrateLegacyPlaintextCookies(context: Context) {
        CookieType.entries.forEach { type ->
            val legacyFile = File(context.filesDir, type.legacyFileName)
            if (legacyFile.exists() && legacyFile.length() > 0) {
                try {
                    val rawContent = legacyFile.readText(Charsets.UTF_8)
                    if (rawContent.isNotBlank() && !hasValidCookies(context, type)) {
                        saveCookies(context, type, rawContent, DEFAULT_EXPIRATION_DAYS)
                        Log.i(TAG, "Migrated legacy plaintext cookies for ${type.displayName} to SecureCookieVault.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate legacy cookies for ${type.displayName}", e)
                } finally {
                    shredFile(legacyFile)
                }
            }
        }
    }
}
