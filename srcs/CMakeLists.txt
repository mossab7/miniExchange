cmake_minimum_required(VERSION 3.22)

project(miniExchange LANGUAGES CXX)

include(CTest)

option(BUILD_MATCHING_ENGINE "Build the C++ matching engine" ON)

if(BUILD_MATCHING_ENGINE)
    add_subdirectory(matching-engine)
endif()

if(BUILD_MATCHING_ENGINE AND BUILD_GRPC)
    find_program(DOCKER_EXECUTABLE docker)

    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/backend/gradlew")
        set(BACKEND_GRADLE_COMMAND "${CMAKE_CURRENT_SOURCE_DIR}/backend/gradlew")
    else()
        find_program(BACKEND_GRADLE_COMMAND gradle REQUIRED)
    endif()

    add_custom_target(generate-proto
        DEPENDS generate-cpp-proto
        COMMAND "${BACKEND_GRADLE_COMMAND}" :app:generateProto --no-daemon
        WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/backend"
        COMMENT "Generating C++ and Java protobuf/gRPC sources")

    add_custom_target(run-matching-engine
        DEPENDS matching_engine_server
        COMMAND "$<TARGET_FILE:matching_engine_server>"
        USES_TERMINAL
        COMMENT "Running the local matching-engine gRPC server")

    add_custom_target(run-backend
        COMMAND "${BACKEND_GRADLE_COMMAND}" :app:bootRun --no-daemon
        WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/backend"
        USES_TERMINAL
        COMMENT "Running the local Spring Boot backend")

    if(DOCKER_EXECUTABLE)
        add_custom_target(run-all
            COMMAND "${DOCKER_EXECUTABLE}" compose up --build
            WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
            USES_TERMINAL
            COMMENT "Running the complete Docker Compose development stack")
    endif()

    add_custom_target(clean-generated
        COMMAND ${CMAKE_COMMAND} -E rm -rf
            "${CMAKE_CURRENT_BINARY_DIR}/matching-engine/generated"
            "${CMAKE_CURRENT_SOURCE_DIR}/backend/app/build/generated"
        COMMENT "Removing generated protobuf/gRPC sources")
endif()
