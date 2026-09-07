#include "ObjectPool.hpp"

template <typename T>
ObjectPool<T>::ObjectPool(uint32_t capacity) : capacity_(capacity), freeList_(nullptr) {
    allocatedChunks_.reserve(INITIAL_CHUNK_COUNT);
    allocateChunk(capacity_);
}

template <typename T>
ObjectPool<T>::~ObjectPool() {
    for (Chunk* chunk : allocatedChunks_) {
        delete[] chunk;
    }
}

template <typename T>
void ObjectPool<T>::allocateChunk(uint32_t chunkSize) {

    Chunk* newChunk = new Chunk[chunkSize];
    allocatedChunks_.push_back(newChunk);
    for (uint32_t i = 0; i < chunkSize - 1; ++i) {
        newChunk[i].next = &newChunk[i + 1];
    }
    newChunk[chunkSize - 1].next = nullptr;
    freeList_ = newChunk; 
}

template <typename T>
template <typename... Args>
T* ObjectPool<T>::acquire(Args&&... args) {
    
    if (freeList_ == nullptr) [[unlikely]] {
        allocateChunk(capacity_);
    }
    Chunk* chunk = freeList_;
    freeList_ = freeList_->next;
    T* obj = reinterpret_cast<T*>(chunk);
    std::construct_at(obj, std::forward<Args>(args)...);
    return obj;
}

template <typename T>
void ObjectPool<T>::release(T* obj) {
    if (obj == nullptr) [[unlikely]] {
        return;
    }
    std::destroy_at(obj);
    Chunk* node = reinterpret_cast<Chunk*>(obj);
    node->next = freeList_;
    freeList_ = node;
}