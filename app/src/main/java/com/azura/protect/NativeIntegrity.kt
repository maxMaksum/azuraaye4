package com.azura.protect

import android.content.Context

object NativeIntegrity {
    init {
        System.loadLibrary("azura_face_lib")
    }

    // external fun checkAppIntegrity(context: Context, key: String): Boolean // REMOVED

    // Add this line for backend key decryption
    external fun decryptBackendKey(encryptedKey: String, signature: String): String

    external fun verifyFetchedKey(
        context: Context,
        key: String,
        signature: String,
        phoneId: String,
        uid: String
    ): Boolean

    fun performSecureOperation(context: Context, key: String) {
        if (key.isNullOrEmpty()) {
            throw SecurityException("Access denied: No secret key")
        }
        // Proceed with secure operation (native call)
    }
}
