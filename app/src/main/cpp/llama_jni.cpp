#include <algorithm>
#include <android/log.h>
#include <jni.h>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "llama.h"

namespace {
constexpr int kContextSize = 4096;
constexpr int kBatchSize = 512;
constexpr int kMaxGeneratedTokens = 512;

std::mutex g_mutex;
llama_model *g_model = nullptr;

std::string from_java(JNIEnv *env, jstring value) {
    if (value == nullptr) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring to_java(JNIEnv *env, const std::string &value) {
    return env->NewStringUTF(value.c_str());
}

int decode_tokens(llama_context *context, const std::vector<llama_token> &tokens) {
    for (size_t offset = 0; offset < tokens.size();) {
        const size_t count = std::min(static_cast<size_t>(kBatchSize), tokens.size() - offset);
        llama_batch batch = llama_batch_get_one(
            const_cast<llama_token *>(tokens.data() + offset),
            static_cast<int32_t>(count)
        );
        if (llama_decode(context, batch) != 0) return -1;
        offset += count;
    }
    return 0;
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dmitroai_app_QwenLocalModel_nativeLoadModel(JNIEnv *env, jobject, jstring path) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model != nullptr) return 0;

    llama_backend_init();
    const std::string model_path = from_java(env, path);
    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(model_path.c_str(), params);
    return g_model == nullptr ? -1 : 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitroai_app_QwenLocalModel_nativeGenerate(JNIEnv *env, jobject, jstring prompt_value) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model == nullptr) return to_java(env, "Локальная модель ещё не загружена.");

    const std::string prompt = from_java(env, prompt_value);
    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    const int32_t token_count = -llama_tokenize(
        vocab,
        prompt.data(),
        static_cast<int32_t>(prompt.size()),
        nullptr,
        0,
        true,
        true
    );
    if (token_count <= 0 || token_count >= kContextSize - kMaxGeneratedTokens) {
        return to_java(env, "Слишком длинный запрос. Сократи текст страницы или сообщения.");
    }

    std::vector<llama_token> prompt_tokens(token_count);
    if (llama_tokenize(
            vocab,
            prompt.data(),
            static_cast<int32_t>(prompt.size()),
            prompt_tokens.data(),
            token_count,
            true,
            true
        ) < 0) {
        return to_java(env, "Не удалось обработать текст запроса.");
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = kContextSize;
    context_params.n_batch = kBatchSize;
    const unsigned int cores = std::thread::hardware_concurrency();
    context_params.n_threads = static_cast<int32_t>(std::clamp(cores > 1 ? cores - 1 : 2, 2u, 6u));
    context_params.n_threads_batch = context_params.n_threads;
    context_params.no_perf = true;

    llama_context *context = llama_init_from_model(g_model, context_params);
    if (context == nullptr) return to_java(env, "Не хватило памяти для запуска модели.");

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string answer;
    int result = decode_tokens(context, prompt_tokens);
    if (result == 0) {
        answer.reserve(2048);
        for (int index = 0; index < kMaxGeneratedTokens; ++index) {
            const llama_token token = llama_sampler_sample(sampler, context, -1);
            if (llama_vocab_is_eog(vocab, token)) break;

            char token_buffer[256];
            int32_t piece_size = llama_token_to_piece(
                vocab, token, token_buffer, sizeof(token_buffer), 0, false
            );
            std::vector<char> large_buffer;
            if (piece_size < 0) {
                large_buffer.resize(static_cast<size_t>(-piece_size));
                piece_size = llama_token_to_piece(
                    vocab, token, large_buffer.data(), static_cast<int32_t>(large_buffer.size()), 0, false
                );
                if (piece_size > 0) answer.append(large_buffer.data(), static_cast<size_t>(piece_size));
            } else if (piece_size > 0) {
                answer.append(token_buffer, static_cast<size_t>(piece_size));
            }

            llama_token next_token = token;
            llama_batch batch = llama_batch_get_one(&next_token, 1);
            if (llama_decode(context, batch) != 0) break;
        }
    }

    llama_sampler_free(sampler);
    llama_free(context);
    return to_java(env, result == 0 ? answer : "Не удалось обработать запрос локальной моделью.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_dmitroai_app_QwenLocalModel_nativeRelease(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
        llama_backend_free();
    }
}