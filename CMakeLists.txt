cmake_minimum_required(VERSION 3.22)

project(miniExchange LANGUAGES CXX)

include(CTest)

# ==============================================================================
# Options
# ==============================================================================

option(BUILD_MATCHING_ENGINE
    "Build the C++ matching engine"
    ON
)

option(BUILD_GRPC
    "Build gRPC support and protobuf generation"
    ON
)

# ==============================================================================
# Paths
# ==============================================================================

set(ROOT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/srcs")
set(MATCHING_ENGINE_DIR "${ROOT_DIR}/matching-engine")
set(BACKEND_DIR "${ROOT_DIR}/backend")
set(BACKEND_APP_DIR "${BACKEND_DIR}/app")

# ==============================================================================
# Matching Engine
# ==============================================================================

if(BUILD_MATCHING_ENGINE)
    add_subdirectory("${MATCHING_ENGINE_DIR}")
endif()

# ==============================================================================
# Backend / gRPC development commands
# ==============================================================================

if(BUILD_GRPC)

    # --------------------------------------------------------------------------
    # Locate Gradle wrapper
    # --------------------------------------------------------------------------

    if(EXISTS "${BACKEND_APP_DIR}/gradlew")
        set(BACKEND_GRADLE_COMMAND
            "${BACKEND_APP_DIR}/gradlew"
        )
    elseif(EXISTS "${BACKEND_DIR}/gradlew")
        set(BACKEND_GRADLE_COMMAND
            "${BACKEND_DIR}/gradlew"
        )
    else()
        find_program(BACKEND_GRADLE_COMMAND gradle REQUIRED)
    endif()

    # --------------------------------------------------------------------------
    # Generate protobuf / gRPC sources
    # --------------------------------------------------------------------------

    if(BUILD_MATCHING_ENGINE)

        add_custom_target(generate-proto
            DEPENDS generate-cpp-proto
            COMMAND
                "${BACKEND_GRADLE_COMMAND}"
                generateProto
                --no-daemon
            WORKING_DIRECTORY "${BACKEND_APP_DIR}"
            USES_TERMINAL
            COMMENT "Generating C++ and Java protobuf/gRPC sources"
        )

    else()

        add_custom_target(generate-proto
            COMMAND
                "${BACKEND_GRADLE_COMMAND}"
                generateProto
                --no-daemon
            WORKING_DIRECTORY "${BACKEND_APP_DIR}"
            USES_TERMINAL
            COMMENT "Generating Java protobuf/gRPC sources"
        )

    endif()

    # --------------------------------------------------------------------------
    # Run matching engine
    # --------------------------------------------------------------------------

    if(BUILD_MATCHING_ENGINE)

        add_custom_target(run-matching-engine
            DEPENDS matching_engine_server
            COMMAND "$<TARGET_FILE:matching_engine_server>"
            USES_TERMINAL
            COMMENT "Running the local matching-engine gRPC server"
        )

    endif()

    # --------------------------------------------------------------------------
    # Run Spring Boot backend
    # --------------------------------------------------------------------------

    add_custom_target(run-backend
        COMMAND
            "${BACKEND_GRADLE_COMMAND}"
            bootRun
            --no-daemon
        WORKING_DIRECTORY "${BACKEND_APP_DIR}"
        USES_TERMINAL
        COMMENT "Running the local Spring Boot backend"
    )

    # --------------------------------------------------------------------------
    # Run complete Docker Compose stack
    # --------------------------------------------------------------------------

    find_program(DOCKER_EXECUTABLE docker)

    if(DOCKER_EXECUTABLE)

        add_custom_target(run-all
            COMMAND
                "${DOCKER_EXECUTABLE}"
                compose
                up
                --build
            WORKING_DIRECTORY "${ROOT_DIR}"
            USES_TERMINAL
            COMMENT "Running the complete Docker Compose development stack"
        )

    endif()

    # --------------------------------------------------------------------------
    # Clean generated files
    # --------------------------------------------------------------------------

    add_custom_target(clean-generated
        COMMAND
            ${CMAKE_COMMAND}
            -E rm -rf
            "${MATCHING_ENGINE_DIR}/generated"
            "${BACKEND_APP_DIR}/build/generated"
        COMMENT "Removing generated protobuf/gRPC sources"
    )

endif()