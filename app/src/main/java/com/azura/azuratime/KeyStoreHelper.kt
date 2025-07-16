package com.azura.azuratime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "AzuraDynamicKey"
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val HMAC_SECRET_ALIAS = "AzuraHmacSecret"
    private const val HMAC_KEY_SIZE = 32 // 256 bits

    fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return iv to cipherText
    }

    fun decrypt(iv: ByteArray, cipherText: ByteArray): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }

    @JvmStatic
    fun getHmacSecret(context: Context): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(HMAC_SECRET_ALIAS)) {
            // Generate random HMAC secret
            val random = SecureRandom()
            val secret = ByteArray(HMAC_KEY_SIZE)
            random.nextBytes(secret)
            // Store in Keystore as a symmetric key
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                HMAC_SECRET_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            // Encrypt and store the secret using the generated AES key
            val (iv, cipherText) = encryptWithAlias(secret, HMAC_SECRET_ALIAS)
            // Store iv and cipherText in SharedPreferences (or EncryptedSharedPreferences for extra security)
            val prefs = context.getSharedPreferences("azura_keystore", Context.MODE_PRIVATE)
            prefs.edit().putString("hmac_iv", Base64.encodeToString(iv, Base64.DEFAULT))
                .putString("hmac_secret", Base64.encodeToString(cipherText, Base64.DEFAULT)).apply()
            return Base64.encodeToString(secret, Base64.NO_WRAP)
        } else {
            // Retrieve encrypted secret from SharedPreferences
            val prefs = context.getSharedPreferences("azura_keystore", Context.MODE_PRIVATE)
            val ivB64 = prefs.getString("hmac_iv", null)
            val secretB64 = prefs.getString("hmac_secret", null)
            if (ivB64 == null || secretB64 == null) throw IllegalStateException("HMAC secret not found")
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            val cipherText = Base64.decode(secretB64, Base64.DEFAULT)
            val secret = decryptWithAlias(iv, cipherText, HMAC_SECRET_ALIAS)
            return Base64.encodeToString(secret, Base64.NO_WRAP)
        }
    }

    private fun encryptWithAlias(plain: ByteArray, alias: String): Pair<ByteArray, ByteArray> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(alias, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        return iv to cipherText
    }

    private fun decryptWithAlias(iv: ByteArray, cipherText: ByteArray, alias: String): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(alias, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(cipherText)
    }
}
