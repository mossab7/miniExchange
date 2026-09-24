# The builder and runtime use the same Alpine release so the musl-linked gRPC,
# protobuf, OpenSSL, c-ares, and librdkafka libraries are compatible.
FROM alpine:3.21 AS builder

RUN apk add --no-cache \
    build-base \
    cmake \
    ninja \
    pkgconf \
    protobuf \
    protobuf-dev \
    grpc-dev \
    grpc-plugins \
    librdkafka-dev

WORKDIR /workspace
COPY proto ./proto
COPY matching-engine ./matching-engine

WORKDIR /workspace/matching-engine/build
RUN cmake .. -G Ninja \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_GRPC=ON \
        -DBUILD_KAFKA=ON \
        -DBUILD_TESTING=OFF \
    && cmake --build . --target matching_engine_server --parallel

FROM alpine:3.21 AS runtime

RUN apk add --no-cache \
    ca-certificates \
    libstdc++ \
    protobuf \
    grpc \
    grpc-cpp \
    librdkafka

COPY --from=builder /workspace/matching-engine/build/matching_engine_server /usr/local/bin/matching_engine_server

EXPOSE 50051
ENTRYPOINT ["/usr/local/bin/matching_engine_server"]
