#ifndef OBJECT_POOL_HPP
#define OBJECT_POOL_HPP

#include <cstdint>
#include <memory>
#include <utility>
#include <vector>

#define INITIAL_CHUNK_COUNT 128

template <typename T> class ObjectPool {
private:
  union Chunk {
    Chunk *next;
    alignas(T) std::byte data[sizeof(T)];
  };
  Chunk *freeList_;
  std::vector<Chunk *> allocatedChunks_;
  uint32_t capacity_;

  void allocateChunk(uint32_t chunkSize);

public:
  ObjectPool(uint32_t capacity);
  ~ObjectPool();
  template <typename... Args> T *acquire(Args &&...args);
  void release(T *obj);
};

#endif // OBJECT_POOL_HPP