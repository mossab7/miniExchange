#ifndef OBJECT_POOL_HPP
#define OBJECT_POOL_HPP

#include <cstddef>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

inline constexpr std::size_t INITIAL_CHUNK_COUNT = 128;

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
  explicit ObjectPool(uint32_t capacity);
  ~ObjectPool();

  ObjectPool(const ObjectPool &) = delete;
  ObjectPool &operator=(const ObjectPool &) = delete;
  ObjectPool(ObjectPool &&) = delete;
  ObjectPool &operator=(ObjectPool &&) = delete;

  template <typename... Args> T *acquire(Args &&...args);
  void release(T *obj);
};

template <typename T>
ObjectPool<T>::ObjectPool(uint32_t capacity) : freeList_(nullptr), capacity_(capacity) {
  if (capacity_ == 0) {
    throw std::invalid_argument("ObjectPool capacity must be greater than zero");
  }
  allocatedChunks_.reserve(INITIAL_CHUNK_COUNT);
  allocateChunk(capacity_);
}

template <typename T>
ObjectPool<T>::~ObjectPool() {
  for (Chunk *chunk : allocatedChunks_) {
    delete[] chunk;
  }
}

template <typename T>
void ObjectPool<T>::allocateChunk(uint32_t chunkSize) {
  Chunk *newChunk = new Chunk[chunkSize];
  allocatedChunks_.push_back(newChunk);
  for (uint32_t i = 0; i + 1 < chunkSize; ++i) {
    newChunk[i].next = &newChunk[i + 1];
  }
  newChunk[chunkSize - 1].next = nullptr;
  freeList_ = newChunk;
}

template <typename T>
template <typename... Args>
T *ObjectPool<T>::acquire(Args &&...args) {
  if (freeList_ == nullptr) [[unlikely]] {
    allocateChunk(capacity_);
  }
  Chunk *chunk = freeList_;
  freeList_ = freeList_->next;
  T *obj = reinterpret_cast<T *>(chunk);
  std::construct_at(obj, std::forward<Args>(args)...);
  return obj;
}

template <typename T>
void ObjectPool<T>::release(T *obj) {
  if (obj == nullptr) [[unlikely]] {
    return;
  }
  std::destroy_at(obj);
  Chunk *node = reinterpret_cast<Chunk *>(obj);
  node->next = freeList_;
  freeList_ = node;
}




#endif // OBJECT_POOL_HPP
