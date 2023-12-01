#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_bojogae_bojogae_1app_test_native_1cpp_NativeTestActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}