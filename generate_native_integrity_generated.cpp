#include <iostream>
#include <iomanip>
#include <string>

int main() {
    std::string actualHash = "db2c703b363437566ad839ef56a3b2e863c9d9831caaec35e46a8952a9615cc1";
    char xorKey = 'd'; // XOR Key: 0x64

    std::cout << "const unsigned char ENCRYPTED_EXPECTED_HASH[] = {\n  ";
    for (size_t i = 0; i < actualHash.size(); i += 2) {
        std::string byteStr = actualHash.substr(i, 2);
        unsigned char byte = static_cast<unsigned char>(std::stoi(byteStr, nullptr, 16));
        unsigned char encryptedByte = byte ^ xorKey;
        std::cout << "0x" << std::hex << std::setw(2) << std::setfill('0')
                  << (int)encryptedByte << ", ";
        if ((i / 2 + 1) % 8 == 0) std::cout << "\n  ";
    }
    std::cout << "\n};\n";
    return 0;
}

