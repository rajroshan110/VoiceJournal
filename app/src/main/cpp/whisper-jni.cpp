#include <jni.h>
#include <string>
#include <vector>
#include <unordered_set>
#include <chrono>
#include <android/log.h>
#include "whisper/whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Configurable Hindi Language Group
static const std::unordered_set<std::string> HINDI_LANGUAGE_GROUP = {
    "hi", // Hindi
    "ur"  // Urdu
};

// Routing Logic:
// 1. If detected is "en" -> "en"
// 2. If detected is in HINDI_LANGUAGE_GROUP ("hi", "ur", etc.) -> "hi"
// 3. Any other detected language -> "unsupported" (Return unsupported language error)
static std::string routeTargetLanguage(const std::string &detected_code) {
    if (detected_code == "en") {
        return "en";
    }
    if (HINDI_LANGUAGE_GROUP.count(detected_code) > 0) {
        return "hi";
    }
    return "unsupported";
}

struct CallbackData {
    JNIEnv *env;
    jobject callback_obj;
    jmethodID method_id;
};

extern "C"
JNIEXPORT jlong JNICALL
Java_dev_voicejournal_transcription_WhisperLib_initContext(JNIEnv *env, jobject thiz, jstring model_path) {
    auto start_time = std::chrono::high_resolution_clock::now();
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("=== JNI initContext Called ===");
    LOGI("Model Path: %s", path);

    // 1. Verify C++ Compiler & CPU Vectorization Features
#ifdef __OPTIMIZE__
    LOGI("Build Verification: C++ Compiler Optimization = ENABLED (__OPTIMIZE__)");
#else
    LOGI("Build Verification: C++ Compiler Optimization = DISABLED (DEBUG UNOPTIMIZED!)");
#endif

#ifdef __ARM_NEON
    LOGI("Build Verification: ARM NEON Acceleration = ENABLED (__ARM_NEON)");
#else
    LOGI("Build Verification: ARM NEON Acceleration = DISABLED");
#endif

#ifdef __ARM_FEATURE_FP16_VECTOR_ARITHMETIC
    LOGI("Build Verification: ARM FP16 Vector Arithmetic = ENABLED");
#else
    LOGI("Build Verification: ARM FP16 Vector Arithmetic = DISABLED");
#endif

#ifdef __ARM_FEATURE_DOTPROD
    LOGI("Build Verification: ARM Dot Product SIMD = ENABLED");
#else
    LOGI("Build Verification: ARM Dot Product SIMD = DISABLED");
#endif

    LOGI("Whisper System Info: %s", whisper_print_system_info());

    whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(model_path, path);

    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration_ms = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();

    if (ctx == nullptr) {
        LOGE("Failed to initialize whisper_context from file: %s (Time: %lld ms)", path, (long long)duration_ms);
    } else {
        LOGI("Successfully initialized whisper_context ptr: %p (Load Time: %lld ms)", ctx, (long long)duration_ms);
    }

    return reinterpret_cast<jlong>(ctx);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_dev_voicejournal_transcription_WhisperLib_fullTranscribe(
    JNIEnv *env, jobject thiz, jlong context_ptr, jfloatArray audio_samples, jint num_threads, jstring language, jobject callback) {
    auto jni_start = std::chrono::high_resolution_clock::now();

    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (ctx == nullptr) {
        LOGE("fullTranscribe failed: context_ptr is null!");
        return env->NewStringUTF("");
    }

    const char *lang_str = env->GetStringUTFChars(language, nullptr);
    std::string req_lang = (lang_str != nullptr && strlen(lang_str) > 0) ? lang_str : "auto";
    if (lang_str != nullptr) {
        env->ReleaseStringUTFChars(language, lang_str);
    }

    jsize num_samples = env->GetArrayLength(audio_samples);
    jfloat *samples = env->GetFloatArrayElements(audio_samples, nullptr);

    LOGI("=== JNI fullTranscribe Execution Start ===");
    LOGI("Audio Samples: %d (%.2f seconds of 16kHz audio)", num_samples, (double)num_samples / 16000.0);
    LOGI("Requested Threads: %d", num_threads);
    LOGI("Requested Language Setting: '%s'", req_lang.c_str());

    std::string detected_code = req_lang;
    float top_prob = 0.0f;

    // Automatic Language Detection Pipeline
    if (req_lang == "auto") {
        LOGI("Running automatic language detection...");
        if (whisper_pcm_to_mel(ctx, samples, num_samples, num_threads) == 0) {
            int max_lang_id = whisper_lang_max_id();
            std::vector<float> lang_probs(max_lang_id + 1, 0.0f);
            int det_id = whisper_lang_auto_detect(ctx, 0, num_threads, lang_probs.data());
            if (det_id >= 0) {
                const char * det_str = whisper_lang_str(det_id);
                if (det_str != nullptr) {
                    detected_code = det_str;
                }
                top_prob = lang_probs[det_id];

                // Log candidate probabilities for diagnosis
                int hi_id = whisper_lang_id("hi");
                int ur_id = whisper_lang_id("ur");
                int en_id = whisper_lang_id("en");
                if (hi_id >= 0) LOGI("  - Prob 'hi' (Hindi): %.4f", lang_probs[hi_id]);
                if (ur_id >= 0) LOGI("  - Prob 'ur' (Urdu): %.4f", lang_probs[ur_id]);
                if (en_id >= 0) LOGI("  - Prob 'en' (English): %.4f", lang_probs[en_id]);
            } else {
                LOGE("whisper_lang_auto_detect failed!");
            }
        } else {
            LOGE("whisper_pcm_to_mel failed during auto-detection!");
        }
    }

    // Language Routing Logic (Hindi Group -> "hi", English -> "en", All Others -> "unsupported")
    std::string final_lang = routeTargetLanguage(detected_code);

    LOGI("Detected language: '%s' (prob=%.4f)", detected_code.c_str(), top_prob);
    LOGI("Final language passed to whisper_full(): '%s'", final_lang.c_str());

    if (final_lang == "unsupported") {
        LOGE("Detected language '%s' is unsupported. Only English and Hindi are supported. Halting transcription.", detected_code.c_str());
        env->ReleaseFloatArrayElements(audio_samples, samples, JNI_ABORT);
        std::string err_msg = "Unsupported language detected (" + detected_code + ")";
        return env->NewStringUTF(err_msg.c_str());
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.greedy.best_of = 1;
    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false; // Transcribe in native spoken script
    params.language = final_lang.c_str();
    params.n_threads = num_threads;
    params.no_context = true; // Prevent hallucinated English context carryover across segments
    params.suppress_blank = true;
    params.suppress_non_speech_tokens = true;
    params.temperature = 0.0f; // Deterministic greedy decoding

    CallbackData cb_data;
    cb_data.env = env;
    cb_data.callback_obj = callback;
    cb_data.method_id = nullptr;

    if (callback != nullptr) {
        jclass callback_class = env->GetObjectClass(callback);
        cb_data.method_id = env->GetMethodID(callback_class, "onNewSegment", "(Ljava/lang/String;)V");
        if (cb_data.method_id != nullptr) {
            params.new_segment_callback = [](struct whisper_context * ctx, struct whisper_state * state, int n_new, void * user_data) {
                CallbackData *data = static_cast<CallbackData *>(user_data);
                if (data && data->env && data->callback_obj && data->method_id) {
                    int n_segments = whisper_full_n_segments(ctx);
                    int start_idx = n_segments - n_new;
                    std::string new_text = "";
                    for (int i = start_idx; i < n_segments; ++i) {
                        const char *seg_text = whisper_full_get_segment_text(ctx, i);
                        if (seg_text != nullptr) {
                            new_text += seg_text;
                        }
                    }
                    if (!new_text.empty()) {
                        jstring jtext = data->env->NewStringUTF(new_text.c_str());
                        data->env->CallVoidMethod(data->callback_obj, data->method_id, jtext);
                        data->env->DeleteLocalRef(jtext);
                    }
                }
            };
            params.new_segment_callback_user_data = &cb_data;
        }
    }

    // Log Inference Strategy Details
    LOGI("Inference Strategy: WHISPER_SAMPLING_GREEDY (best_of=1, no_context=true)");
    LOGI("Translation Enabled: %s", params.translate ? "true" : "false");

    // Measure Whisper Native Full Engine Inference Time
    auto infer_start = std::chrono::high_resolution_clock::now();
    int ret = whisper_full(ctx, params, samples, num_samples);
    auto infer_end = std::chrono::high_resolution_clock::now();
    auto infer_ms = std::chrono::duration_cast<std::chrono::milliseconds>(infer_end - infer_start).count();

    if (ret != 0) {
        LOGE("whisper_full FAILED with return code: %d (Inference Time: %lld ms)", ret, (long long)infer_ms);
        env->ReleaseFloatArrayElements(audio_samples, samples, JNI_ABORT);
        return env->NewStringUTF("Transcription failed");
    }

    LOGI("whisper_full Completed Successfully in %lld ms", (long long)infer_ms);

    std::string result_text = "";
    int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            result_text += text;
        }
    }

    env->ReleaseFloatArrayElements(audio_samples, samples, JNI_ABORT);
    return env->NewStringUTF(result_text.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_voicejournal_transcription_WhisperLib_freeContext(JNIEnv *env, jobject thiz, jlong context_ptr) {
    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (ctx != nullptr) {
        LOGI("freeContext freeing ptr: %p", ctx);
        whisper_free(ctx);
    }
}
