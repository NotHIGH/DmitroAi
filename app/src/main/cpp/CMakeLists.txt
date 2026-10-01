cmake_minimum_required(VERSION 3.31.6)

project(dima_llama LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_SYSTEM_ARCH ARM)
    set(GGML_CPU_KLEIDIAI ON)
    set(GGML_OPENMP ON)
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(GGML_SYSTEM_ARCH x86)
    set(GGML_CPU_KLEIDIAI OFF)
    set(GGML_OPENMP OFF)
else()
    message(FATAL_ERROR "Unsupported Android ABI: ${ANDROID_ABI}")
endif()

include(FetchContent)

set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS ON CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL ON CACHE BOOL "" FORCE)

FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG v0.5.0
    GIT_SUBMODULES ggml
    GIT_SUBMODULES_RECURSE TRUE
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(llama_cpp)

add_library(dima-llama SHARED llama_jni.cpp)
target_include_directories(dima-llama PRIVATE ${llama_cpp_SOURCE_DIR}/include)
target_link_libraries(dima-llama PRIVATE llama android log)