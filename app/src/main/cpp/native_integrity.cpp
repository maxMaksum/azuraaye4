#include <jni.h>
#include <string>
#include <fstream>
#include <sstream>
#include <android/log.h>
#include <sys/stat.h>
#include "external/sha256/sha256.h"
#include <vector>
#include <dlfcn.h>
#include <iomanip>
#include <cctype>
#ifdef USE_LIBZIP
#include <zip.h>
#endif

#define LOG_TAG "NativeIntegrity"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Forward declarations
bool verifyHmacSignature(const std::string& data, const std::string& receivedSignature, const std::string& secret);
std::string hmac_sha256(const std::string& key, const std::string& data);

#ifndef SHA256_BLOCK_SIZE
#define SHA256_BLOCK_SIZE 32
#endif

// --- REMOVE ALL HARDCODED HASH LOGIC ---
// No more ENCRYPTED_EXPECTED_HASH, no more checkAppIntegrity JNI

// --- DYNAMIC BACKEND-DRIVEN VERIFICATION ONLY ---

// 🔐 Function to verify backend key HMAC
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_azura_protect_NativeIntegrity_verifyFetchedKey(
    JNIEnv *env,
    jobject thiz,
    jobject context,
    jstring keyJ,
    jstring signatureJ,
    jstring phoneIdJ,
    jstring uidJ
) {
    const char *keyC = env->GetStringUTFChars(keyJ, nullptr);
    const char *signatureC = env->GetStringUTFChars(signatureJ, nullptr);
    const char *phoneIdC = env->GetStringUTFChars(phoneIdJ, nullptr);
    const char *uidC = env->GetStringUTFChars(uidJ, nullptr);
    std::string data = std::string(phoneIdC) + ":" + std::string(uidC) + ":" + std::string(keyC);
    std::string receivedSignature(signatureC);
    // Fetch HMAC secret from Keystore
    std::string hmacSecret = getHmacSecretFromKeystore(env, context);
    bool isValid = verifyHmacSignature(
        data,
        receivedSignature,
        hmacSecret
    );
    env->ReleaseStringUTFChars(keyJ, keyC);
    env->ReleaseStringUTFChars(signatureJ, signatureC);
    env->ReleaseStringUTFChars(phoneIdJ, phoneIdC);
    env->ReleaseStringUTFChars(uidJ, uidC);
    return isValid ? JNI_TRUE : JNI_FALSE;
}

// --- Optionally, keep decryptBackendKey if you want to support encrypted keys ---
extern "C"
JNIEXPORT jstring JNICALL
Java_com_azura_protect_NativeIntegrity_decryptBackendKey(
    JNIEnv *env,
    jobject thiz,
    jstring encryptedKeyJ,
    jstring signatureJ,
    jobject context // <-- Add context for Keystore access
) {
    const char *encryptedKeyC = env->GetStringUTFChars(encryptedKeyJ, nullptr);
    const char *signatureC = env->GetStringUTFChars(signatureJ, nullptr);
    const char xorKey = 0x5A; // Static XOR key (if still needed)
    std::string decrypted;
    for (int i = 0; encryptedKeyC[i] != '\0'; ++i) {
        decrypted += (encryptedKeyC[i] ^ xorKey);
    }
    const std::string receivedSignature(signatureC);
    // Fetch HMAC secret from Keystore
    std::string hmacSecret = getHmacSecretFromKeystore(env, context);
    bool isValid = verifyHmacSignature(
        decrypted,
        receivedSignature,
        hmacSecret
    );
    env->ReleaseStringUTFChars(encryptedKeyJ, encryptedKeyC);
    env->ReleaseStringUTFChars(signatureJ, signatureC);
    if (isValid) {
        return env->NewStringUTF(decrypted.c_str());
    } else {
        return env->NewStringUTF("INVALID_SIGNATURE");
    }
}

// HMAC-SHA256 Verification
bool verifyHmacSignature(
    const std::string& data,
    const std::string& receivedSignature,
    const std::string& secret
) {
    std::string computed = hmac_sha256(secret, data);
    return computed == receivedSignature;
}

// Basic HMAC-SHA256 implementation
std::string hmac_sha256(const std::string& key, const std::string& data) {
    SHA256_CTX ctx;
    sha256_init(&ctx);
    std::string innerPad(64, 0x36);
    for (size_t i = 0; i < key.size(); ++i) {
        innerPad[i] ^= key[i];
    }
    sha256_update(&ctx, (const uint8_t*)innerPad.data(), innerPad.size());
    sha256_update(&ctx, (const uint8_t*)data.data(), data.size());
    unsigned char hash[SHA256_BLOCK_SIZE];
    sha256_final(&ctx, hash);
    char buf[65];
    for (int i = 0; i < 32; i++) {
        sprintf(buf + i*2, "%02x", hash[i]);
    }
    buf[64] = 0;
    return std::string(buf);
}

// Helper: Fetch HMAC secret from Android Keystore via JNI
std::string getHmacSecretFromKeystore(JNIEnv* env, jobject context) {
    jclass keyStoreHelperClass = env->FindClass("com/azura/azuratime/KeyStoreHelper");
    if (!keyStoreHelperClass) {
        LOGI("KeyStoreHelper class not found");
        return "";
    }
    jmethodID getHmacSecretMethod = env->GetStaticMethodID(
        keyStoreHelperClass,
        "getHmacSecret",
        "(Landroid/content/Context;)Ljava/lang/String;"
    );
    if (!getHmacSecretMethod) {
        LOGI("getHmacSecret method not found");
        return "";
    }
    jstring secretJ = (jstring)env->CallStaticObjectMethod(keyStoreHelperClass, getHmacSecretMethod, context);
    if (!secretJ) {
        LOGI("getHmacSecret returned null");
        return "";
    }
    const char* secretC = env->GetStringUTFChars(secretJ, nullptr);
    std::string secretStr(secretC);
    env->ReleaseStringUTFChars(secretJ, secretC);
    env->DeleteLocalRef(secretJ);
    env->DeleteLocalRef(keyStoreHelperClass);
    return secretStr;
}
