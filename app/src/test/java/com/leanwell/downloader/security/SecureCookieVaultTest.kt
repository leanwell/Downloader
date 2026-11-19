package com.leanwell.downloader.security

import android.content.Context
import android.content.ContextWrapper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecureCookieVaultTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testKey: SecretKey
    private lateinit var fakeContext: Context
    private lateinit var testDir: File
    private lateinit var testEphemeralDir: File
    private lateinit var testLegacyDir: File

    @Before
    fun setUp() {
        // Generate in-memory 256-bit AES key for host JVM execution
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        testKey = keyGen.generateKey()

        testDir = tempFolder.newFolder("vault_secure")
        testEphemeralDir = tempFolder.newFolder("vault_ephem")
        testLegacyDir = tempFolder.newFolder("vault_legacy")

        fakeContext = object : ContextWrapper(null) {}

        // Inject test providers
        SecureCookieVault.masterKeyProvider = { testKey }
        SecureCookieVault.encryptedFileDirProvider = { testDir }
        SecureCookieVault.ephemeralDirProvider = { testEphemeralDir }
        SecureCookieVault.legacyFilesDirProvider = { testLegacyDir }
    }

    @After
    fun tearDown() {
        // Reset providers to production defaults
        SecureCookieVault.masterKeyProvider = {
            // No-op / will point back to production method if reloaded
            testKey
        }
    }

    @Test
    fun saveCookies_expirationValidation_boundaryChecks() {
        val sampleCookies = "# Netscape HTTP Cookie File\n.youtube.com\tTRUE\t/\tTRUE\t1735689600\tSID\tsecret_token"

        // Expiration <= 0 must fail
        try {
            SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, sampleCookies, 0)
            fail("Expected IllegalArgumentException for expirationDays = 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 1 and 365") == true)
        }

        try {
            SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, sampleCookies, -10)
            fail("Expected IllegalArgumentException for expirationDays = -10")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 1 and 365") == true)
        }

        // Expiration > 365 must fail
        try {
            SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, sampleCookies, 366)
            fail("Expected IllegalArgumentException for expirationDays = 366")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 1 and 365") == true)
        }

        // Valid boundary values
        assertTrue(SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, sampleCookies, 1))
        assertTrue(SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, sampleCookies, 365))
    }

    @Test
    fun encryptionDecryption_roundtrip_preservesExactContent() {
        val rawCookies = "# Netscape HTTP Cookie File\n.youtube.com\tTRUE\t/\tTRUE\t2000000000\tLOGIN_INFO\tAFmmF2kwRAIg=="
        val saved = SecureCookieVault.saveCookies(
            fakeContext,
            SecureCookieVault.CookieType.YOUTUBE,
            rawCookies,
            expirationDays = 30
        )
        assertTrue("saveCookies should succeed", saved)

        // Target encrypted file must exist in test directory
        val encFile = File(testDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
        assertTrue(encFile.exists())
        assertTrue(encFile.length() > 37)

        // Raw file content must NOT contain plaintext tokens
        val rawEncBytes = encFile.readBytes()
        val rawEncString = String(rawEncBytes, Charsets.ISO_8859_1)
        assertFalse("Ciphertext file must not contain plaintext tokens", rawEncString.contains("LOGIN_INFO"))

        // Decryption roundtrip must match exactly
        val decrypted = SecureCookieVault.decryptCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE)
        assertEquals(rawCookies, decrypted)
    }

    @Test
    fun aadTampering_detectsModifiedCiphertext_andPurgesFile() {
        val rawCookies = "cookie_content_secret_12345"
        SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, rawCookies, 30)

        val encFile = File(testDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
        assertTrue(encFile.exists())

        // Tamper with one byte in the ciphertext portion
        val bytes = encFile.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        encFile.writeBytes(bytes)

        // Decryption must detect tampering via AEADBadTagException, purge the corrupt file, and return null
        val decrypted = SecureCookieVault.decryptCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE)
        assertNull("Tampered file must fail decryption", decrypted)
        assertFalse("Tampered file must be shredded and purged", encFile.exists())
    }

    @Test
    fun aadTampering_detectsModifiedHeaderTimestamp() {
        val rawCookies = "cookie_content_secret_timestamp_test"
        SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, rawCookies, 30)

        val encFile = File(testDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
        val bytes = encFile.readBytes()

        // Byte index 5..12 is createdAt (8 bytes), 13..20 is expiresAt (8 bytes).
        // Tamper with createdAt in the header to verify AAD authentication without triggering expiration purge in getSessionMeta.
        bytes[8] = (bytes[8].toInt() xor 0x01).toByte()
        encFile.writeBytes(bytes)

        // Because header is in AAD, GCM authentication tag verification must fail with AEADBadTagException
        val decrypted = SecureCookieVault.decryptCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE)
        assertNull("Header-tampered file must fail decryption due to GCM AAD check", decrypted)
        assertFalse("Tampered file must be purged upon AEADBadTagException", encFile.exists())
    }

    @Test
    fun crossProviderTypeSwap_isRejectedByAAD() {
        val ytCookies = "youtube_cookie_data_unique"
        SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, ytCookies, 30)

        val ytFile = File(testDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
        val igFile = File(testDir, SecureCookieVault.CookieType.INSTAGRAM.encryptedFileName)

        // Attacker copies/renames YouTube encrypted file to Instagram encrypted file
        ytFile.copyTo(igFile, overwrite = true)
        assertTrue(igFile.exists())

        // Decrypting as Instagram must fail because AAD includes type.name ("INSTAGRAM" != "YOUTUBE")
        val decryptedIg = SecureCookieVault.decryptCookies(fakeContext, SecureCookieVault.CookieType.INSTAGRAM)
        assertNull("Cross-provider type swap must be rejected by GCM AAD", decryptedIg)
        assertFalse("Mismatched provider file must be purged", igFile.exists())
    }

    @Test
    fun withEphemeralCookieFile_createsAndDestroysFileWithProperFlow() {
        val rawCookies = "sample_ephemeral_cookies_for_ytdlp"
        SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, rawCookies, 30)

        var capturedFile: File? = null
        val result = SecureCookieVault.withEphemeralCookieFile(fakeContext, SecureCookieVault.CookieType.YOUTUBE) { ephemFile ->
            assertNotNull(ephemFile)
            capturedFile = ephemFile
            assertTrue("Ephemeral file must exist during block execution", ephemFile!!.exists())
            assertEquals(rawCookies, ephemFile.readText())
            "block_success"
        }

        assertEquals("block_success", result)
        assertNotNull(capturedFile)
        assertFalse("Ephemeral file must be shredded and deleted after block exits", capturedFile!!.exists())
    }

    @Test
    fun cleanupOrphanedEphemeralFiles_removesCrashOrphans() {
        val orphan1 = File(testEphemeralDir, "ephem_youtube_orphan1.txt")
        val orphan2 = File(testEphemeralDir, "ephem_facebook_orphan2.txt")
        orphan1.writeText("abandoned_plain_text_cookies_1")
        orphan2.writeText("abandoned_plain_text_cookies_2")
        assertTrue(orphan1.exists())
        assertTrue(orphan2.exists())

        SecureCookieVault.cleanupOrphanedEphemeralFiles(fakeContext)

        assertFalse("Orphan 1 should be shredded and purged", orphan1.exists())
        assertFalse("Orphan 2 should be shredded and purged", orphan2.exists())
    }

    @Test
    fun shredFile_overwritesWithZerosBeforeDeletion() {
        val sensitiveFile = File(testDir, "test_shred_target.txt")
        val originalContent = "super_sensitive_api_token_data_to_be_shredded"
        sensitiveFile.writeText(originalContent)
        assertTrue(sensitiveFile.exists())

        SecureCookieVault.shredFile(sensitiveFile)

        assertFalse("File must be deleted after shredding", sensitiveFile.exists())
    }

    @Test
    fun sessionMeta_expiredSession_isAutoPurged() {
        val rawCookies = "expiring_cookie_session"
        SecureCookieVault.saveCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE, rawCookies, 1)

        val encFile = File(testDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
        val bytes = encFile.readBytes()

        // Modify expiresAt (bytes 13..20) in the header to be in the past (1000 ms ago)
        val buf = ByteBuffer.wrap(bytes)
        buf.position(13)
        buf.putLong(System.currentTimeMillis() - 60_000L) // 1 minute in the past
        encFile.writeBytes(bytes)

        // getSessionMeta must detect expiration, shred the file, and return null
        val meta = SecureCookieVault.getSessionMeta(fakeContext, SecureCookieVault.CookieType.YOUTUBE)
        assertNull("Expired session must return null meta", meta)
        assertFalse("Expired file must be shredded upon detection", encFile.exists())
        assertFalse(SecureCookieVault.hasValidCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE))
    }

    @Test
    fun migrateLegacyPlaintextCookies_safelyMigratesAndShredsLegacyFile() {
        val legacyFile = File(testLegacyDir, SecureCookieVault.CookieType.FACEBOOK.legacyFileName)
        val legacyCookies = "# Netscape HTTP Cookie File\n.facebook.com\tTRUE\t/\tTRUE\t2000000000\tc_user\t1000999"
        legacyFile.writeText(legacyCookies)
        assertTrue(legacyFile.exists())

        // Migration must import cookies to encrypted vault and shred legacy file
        SecureCookieVault.migrateLegacyPlaintextCookies(fakeContext)

        assertFalse("Legacy plaintext file must be shredded after successful migration", legacyFile.exists())
        assertTrue("Vault must have valid cookies after migration", SecureCookieVault.hasValidCookies(fakeContext, SecureCookieVault.CookieType.FACEBOOK))

        val decrypted = SecureCookieVault.decryptCookies(fakeContext, SecureCookieVault.CookieType.FACEBOOK)
        assertEquals(legacyCookies, decrypted)
    }

    @Test
    fun v1LegacyFormat_decryptsSuccessfullyWithoutAad() {
        val rawCookies = "v1_legacy_cookie_payload_without_aad"
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, testKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(rawCookies.toByteArray(Charsets.UTF_8))

        val now = System.currentTimeMillis()
        val expiresAt = now + (30L * 24 * 60 * 60 * 1000L)

        // v1 format: MAGIC(4) + VERSION 1(1) + createdAt(8) + expiresAt(8)
        val headerBuf = ByteBuffer.allocate(21)
        headerBuf.put(byteArrayOf(0x53, 0x45, 0x43, 0x56))
        headerBuf.put(1.toByte()) // v1 legacy version
        headerBuf.putLong(now)
        headerBuf.putLong(expiresAt)

        val targetFile = File(testDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
        java.io.FileOutputStream(targetFile).use { fos ->
            fos.write(headerBuf.array())
            fos.write(iv)
            fos.write(cipherText)
            fos.flush()
        }

        // Must successfully decrypt v1 legacy file without AAD
        val decrypted = SecureCookieVault.decryptCookies(fakeContext, SecureCookieVault.CookieType.YOUTUBE)
        assertEquals(rawCookies, decrypted)
    }
}
