cmake_minimum_required(VERSION 3.22.1)

project("whisper_jni")

# Global compiler optimization flags for all targets
add_compile_options(-O3 -ffast-math -DNDEBUG)

# Target ABI specific hardware acceleration flags
if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_compile_options(-march=armv8.2-a+fp16+dotprod -flto)
    add_link_options(-flto)
    add_definitions(-D__ARM_NEON -D__ARM_FEATURE_FP16_VECTOR_ARITHMETIC -D__ARM_FEATURE_DOTPROD)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    add_compile_options(-mfpu=neon -mfloat-abi=softfp)
    add_definitions(-D__ARM_NEON)
endif()

# Include header search paths
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/whisper)

# Compile whisper.cpp and ggml C/C++ engine files
add_library(
    whisper_jni
    SHARED
    whisper-jni.cpp
    whisper/whisper.cpp
    whisper/ggml.c
    whisper/ggml-alloc.c
    whisper/ggml-backend.c
    whisper/ggml-quants.c
)

# Link log library for Android logging
find_library(
    log-lib
    log
)

target_link_libraries(
    whisper_jni
    ${log-lib}
)
