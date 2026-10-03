package com.proj.automation.agp

import com.proj.automation.plugin.PackageSignature
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KeyFileException(message: String) : Exception(message)

/**
 * Developer signing keys (specs/006-package-signing). The private key file is encrypted with a passphrase
 * (PBKDF2-HMAC-SHA256 → AES-256-GCM) and must never be committed or shared. The `.pub` file
 * holds only the public key and its fingerprint, which the developer can share with users.
 *
 * ```
 * agp-key 1                         agp-pub 1
 * name <developer name>             name <developer name>
 * kdf <iterations> <salt>           public-key <base64>
 * iv <base64>                       fingerprint <XXXX XXXX ...>
 * data <base64 encrypted PKCS#8>
 * public-key <base64>
 * ```
 */
object KeyFiles {

    private const val ITERATIONS = 310_000
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()

    fun generate(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    fun write(keyFile: File, pubFile: File, keys: KeyPair, name: String, password: CharArray) {
        if (keyFile.exists() || pubFile.exists()) throw KeyFileException("Refusing to overwrite ${keyFile.path} or ${pubFile.path}")
        if (password.size < 8) throw KeyFileException("Use a passphrase of at least 8 characters")
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, derive(password, salt, ITERATIONS), GCMParameterSpec(128, iv))
        }
        val data = cipher.doFinal(keys.private.encoded)
        val pub = b64.encodeToString(keys.public.encoded)
        keyFile.absoluteFile.parentFile.mkdirs()
        keyFile.writeText("agp-key 1\nname $name\nkdf $ITERATIONS ${b64.encodeToString(salt)}\niv ${b64.encodeToString(iv)}\ndata ${b64.encodeToString(data)}\npublic-key $pub\n")
        keyFile.setReadable(false, false)
        keyFile.setReadable(true, true)
        pubFile.writeText("agp-pub 1\nname $name\npublic-key $pub\nfingerprint ${PackageSignature.fingerprint(keys.public.encoded)}\n")
    }

    fun read(keyFile: File, password: CharArray): KeyPair {
        val f = fields(keyFile, "agp-key 1")
        val (iterations, salt) = (f["kdf"] ?: throw KeyFileException("${keyFile.path}: missing kdf")).split(' ').let { it[0].toInt() to unb64.decode(it[1]) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, derive(password, salt, iterations), GCMParameterSpec(128, unb64.decode(f["iv"])))
        }
        val pkcs8 = try {
            cipher.doFinal(unb64.decode(f["data"]))
        } catch (e: AEADBadTagException) {
            throw KeyFileException("Wrong passphrase for ${keyFile.path}")
        }
        val kf = KeyFactory.getInstance("EC")
        return KeyPair(
            kf.generatePublic(X509EncodedKeySpec(unb64.decode(f["public-key"]))),
            kf.generatePrivate(PKCS8EncodedKeySpec(pkcs8))
        )
    }

    /** Fingerprint and name from a `.pub` or `.key` file (no passphrase needed) */
    fun describe(file: File): Pair<String, String> {
        val f = fields(file, null)
        val key = unb64.decode(f["public-key"] ?: throw KeyFileException("${file.path}: no public key"))
        return PackageSignature.fingerprint(key) to (f["name"] ?: "")
    }

    /** Passphrase from AGP_KEY_PASSWORD, else asked on the terminal */
    fun password(prompt: String): CharArray =
        System.getenv("AGP_KEY_PASSWORD")?.toCharArray()
            ?: System.console()?.readPassword(prompt)
            ?: throw KeyFileException("No passphrase: set AGP_KEY_PASSWORD or run in a terminal")

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int) =
        SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password, salt, iterations, 256)).encoded,
            "AES"
        )

    private fun fields(file: File, header: String?): Map<String, String> {
        if (!file.isFile) throw KeyFileException("No such file: ${file.path}")
        val lines = file.readLines().filter { it.isNotBlank() }
        if (header != null && lines.firstOrNull() != header) throw KeyFileException("${file.path} is not an agp key file")
        return lines.drop(1).associate { line -> line.substringBefore(' ') to line.substringAfter(' ', "") }
    }
}
