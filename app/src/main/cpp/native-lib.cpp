#include <jni.h>
#include <string>
#include <fstream>
#include <sstream>
#include <stdint.h>
#include <android/log.h>

#define LOG_TAG "GameHub_NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_gamehub_optimizer_MainActivity_findBaseAddress(JNIEnv* env, jobject thiz, jstring lib_name) {
    const char* target_lib = env->GetStringUTFChars(lib_name, nullptr);
    uintptr_t base_address = 0;
    
    // Процестің РАМ картасын (/proc/self/maps) ашамыз
    std::ifstream maps("/proc/self/maps");
    std::string line;
    
    while (std::getline(maps, line)) {
        if (line.find(target_lib) != std::string::npos) {
            std::stringstream ss(line);
            ss >> std::hex >> base_address; // Алғашқы hex мекенжайды (Base) оқимыз
            break;
        }
    }
    
    env->ReleaseStringUTFChars(lib_name, target_lib);
    LOGI("Табылған кітапхана мекенжайы: 0x%lx", base_address);
    return (jlong)base_address;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_gamehub_optimizer_MainActivity_readMemoryFloat(JNIEnv* env, jobject thiz, jlong address) {
    if (address == 0) return 0.0f;
    
    // Тікелей сілтеме (Pointer) арқылы РАМ-нан мәнді ең жоғарғы жылдамдықпен оқу
    float* ptr = reinterpret_cast<float*>(address);
    return *ptr;
}
