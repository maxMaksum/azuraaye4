#ifndef SECURITY_STUB_H
#define SECURITY_STUB_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Verifies the SHA256 hash of a given file inside the APK (like .so, assets, etc).
 *
 * @param apkPath           Path to the APK file (usually base.apk)
 * @param targetEntry       Relative path inside the APK (e.g., "lib/arm64-v8a/libazura_face_lib.so")
 * @param expectedHashHex   The expected 64-character lowercase SHA256 hash in hex
 * @return                  true if hash matches, false otherwise
 */
bool verifyApkIntegrity(const char* apkPath, const char* targetEntry, const char* expectedHashHex);

#ifdef __cplusplus
}
#endif

#endif // SECURITY_STUB_H
