cmake_minimum_required(VERSION 3.22)

project(mini_exchange_matching_engine LANGUAGES CXX)

include(CTest)

option(BUILD_GRPC "Build the gRPC transport and server" ON)
option(BUILD_KAFKA "Build the librdkafka trade publisher when available" ON)

set(MATCHING_ENGINE_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src")

add_library(matching_engine_core STATIC
    src/engine/ObjectPool/ObjectPool.cpp
    src/engine/Order/Order.cpp
    src/engine/PriceLevel/PriceLevel.cpp
    src/engine/OrderBook/OrderBook.cpp
    src/engine/Trade/Trade.cpp
    src/engine/matchingEngine/matchingEngine.cpp
)

target_compile_features(matching_engine_core PUBLIC cxx_std_20)
target_compile_options(matching_engine_core PRIVATE -Wall -Wextra -Wpedantic)
target_include_directories(matching_engine_core PUBLIC "${MATCHING_ENGINE_SRC_DIR}")

if(BUILD_GRPC)
    # Keep protoc, protobuf, and gRPC on the same pkg-config toolchain. This
    # avoids mixing a /usr/local installation with system runtime libraries.
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(PROTOBUF REQUIRED IMPORTED_TARGET protobuf)
    pkg_check_modules(GRPCXX REQUIRED IMPORTED_TARGET grpc++)

    find_program(MATCHING_ENGINE_PROTOC protoc HINTS /usr/bin /usr/local/bin REQUIRED)
    find_program(MATCHING_ENGINE_GRPC_PLUGIN grpc_cpp_plugin HINTS /usr/bin /usr/local/bin REQUIRED)

    set(MATCHING_ENGINE_PROTO "${CMAKE_CURRENT_SOURCE_DIR}/../proto/miniExchange.proto")
    set(MATCHING_ENGINE_GENERATED_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
    set(MATCHING_ENGINE_PROTO_SRCS
        "${MATCHING_ENGINE_GENERATED_DIR}/miniExchange.pb.cc"
        "${MATCHING_ENGINE_GENERATED_DIR}/miniExchange.grpc.pb.cc")
    set(MATCHING_ENGINE_PROTO_HDRS
        "${MATCHING_ENGINE_GENERATED_DIR}/miniExchange.pb.h"
        "${MATCHING_ENGINE_GENERATED_DIR}/miniExchange.grpc.pb.h")

    add_custom_command(
        OUTPUT ${MATCHING_ENGINE_PROTO_SRCS} ${MATCHING_ENGINE_PROTO_HDRS}
        COMMAND ${CMAKE_COMMAND} -E make_directory "${MATCHING_ENGINE_GENERATED_DIR}"
        COMMAND "${MATCHING_ENGINE_PROTOC}"
            "-I${CMAKE_CURRENT_SOURCE_DIR}/../proto"
            "--cpp_out=${MATCHING_ENGINE_GENERATED_DIR}"
            "--grpc_out=${MATCHING_ENGINE_GENERATED_DIR}"
            "--plugin=protoc-gen-grpc=${MATCHING_ENGINE_GRPC_PLUGIN}"
            "${MATCHING_ENGINE_PROTO}"
        DEPENDS "${MATCHING_ENGINE_PROTO}"
        COMMENT "Generating C++ protobuf and gRPC sources"
        VERBATIM)

    add_custom_target(generate-cpp-proto
        DEPENDS ${MATCHING_ENGINE_PROTO_SRCS} ${MATCHING_ENGINE_PROTO_HDRS})

    add_library(exchange_proto STATIC ${MATCHING_ENGINE_PROTO_SRCS})
    add_dependencies(exchange_proto generate-cpp-proto)
    target_compile_features(exchange_proto PUBLIC cxx_std_20)
    target_include_directories(exchange_proto PUBLIC "${MATCHING_ENGINE_GENERATED_DIR}")
    # /usr/local/include is an implicit compiler search path in the development
    # image, so make the pkg-config include directory explicit and earlier.
    target_compile_options(exchange_proto PUBLIC "-I${PROTOBUF_INCLUDEDIR}")
    if(EXISTS "/usr/local/include/google/protobuf/stubs/common.h")
        # Some development hosts ship a second, newer protobuf under
        # /usr/local. Exclude that implicit path so the selected pkg-config
        # headers and runtime remain a matched pair.
        target_compile_options(exchange_proto PUBLIC -nostdinc)
        foreach(system_include IN LISTS CMAKE_CXX_IMPLICIT_INCLUDE_DIRECTORIES)
            if(NOT system_include STREQUAL "/usr/local/include")
                target_compile_options(exchange_proto PUBLIC "-isystem${system_include}")
            endif()
        endforeach()
    endif()
    target_link_libraries(exchange_proto PUBLIC PkgConfig::GRPCXX PkgConfig::PROTOBUF)

    if(BUILD_KAFKA)
        pkg_check_modules(RDKAFKA_CPP QUIET IMPORTED_TARGET rdkafka++)
    endif()

    if(RDKAFKA_CPP_FOUND)
        add_library(matching_engine_kafka STATIC src/infra/kafka/KafkaTradePublisher.cpp)
        target_compile_definitions(matching_engine_kafka PRIVATE MATCHING_ENGINE_HAS_KAFKA=1)
        target_compile_features(matching_engine_kafka PUBLIC cxx_std_20)
        target_include_directories(matching_engine_kafka PUBLIC "${MATCHING_ENGINE_SRC_DIR}")
        target_link_libraries(matching_engine_kafka
            PUBLIC matching_engine_core exchange_proto PkgConfig::RDKAFKA_CPP)
    else()
        message(WARNING "librdkafka++ was not found; using the development no-op trade publisher")
        add_library(matching_engine_kafka STATIC src/engine/Trade/NullTradePublisher.cpp)
        target_compile_features(matching_engine_kafka PUBLIC cxx_std_20)
        target_include_directories(matching_engine_kafka PUBLIC "${MATCHING_ENGINE_SRC_DIR}")
        target_link_libraries(matching_engine_kafka PUBLIC matching_engine_core)
    endif()

    add_library(matching_engine_grpc STATIC
        src/application/grpc/ExchangeService.cpp
        src/application/grpc/ExchangeGrpcServer.cpp)
    target_compile_features(matching_engine_grpc PUBLIC cxx_std_20)
    target_compile_options(matching_engine_grpc PRIVATE -Wall -Wextra -Wpedantic)
    target_include_directories(matching_engine_grpc PUBLIC "${MATCHING_ENGINE_SRC_DIR}")
    target_link_libraries(matching_engine_grpc
        PUBLIC matching_engine_core exchange_proto matching_engine_kafka PkgConfig::GRPCXX)

    add_executable(matching_engine_server src/main.cpp)
    target_compile_features(matching_engine_server PRIVATE cxx_std_20)
    target_compile_options(matching_engine_server PRIVATE -Wall -Wextra -Wpedantic)
    target_link_libraries(matching_engine_server PRIVATE matching_engine_grpc matching_engine_kafka)
endif()

if(BUILD_TESTING)
    add_executable(matching_engine_tests tests/engine_tests.cpp)
    target_compile_features(matching_engine_tests PRIVATE cxx_std_20)
    target_compile_options(matching_engine_tests PRIVATE -Wall -Wextra -Wpedantic)
    target_link_libraries(matching_engine_tests PRIVATE matching_engine_core)
    add_test(NAME matching_engine_tests COMMAND matching_engine_tests)
endif()
