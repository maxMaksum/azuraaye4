import hashlib

# Ganti dengan path ke file .so kamu
SO_PATH = "libazura_face_lib.so"
XOR_KEY = 0x36  # ganti kalau kamu pakai key lain

with open(SO_PATH, "rb") as f:
    data = f.read()

hash_bytes = hashlib.sha256(data).digest()
xor_bytes = [b ^ XOR_KEY for b in hash_bytes]

print("const uint8_t ENCRYPTED_EXPECTED_HASH[32] = {")
for i in range(0, 32, 8):
    print("    " + ', '.join(f"0x{b:02x}" for b in xor_bytes[i:i+8]) + ",")
print("};")
