package com.github.xandergos.terraindiffusionmc.infinitetensor;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

// In-memory factory and LRU cache for InfiniteTensor window outputs
public class MemoryTileStore {

    // Window cache per tensor id: access-order LinkedHashMap for LRU
    private final Map<String, LinkedHashMap<List<Integer>, FloatTensor>> windowCaches = new HashMap<>();

    // Tracked byte count per tensor id
    private final Map<String, long[]> cacheSizes = new HashMap<>();

    // All registered tensor instances, by id
    private final Map<String, InfiniteTensor> tensors = new HashMap<>();
    // Monotonic count of newly computed/cached windows across all tensors
    private final AtomicLong totalComputedWindowCount = new AtomicLong(0L);

    // Optional persistent tier beneath the LRU
    private DiskTileCache disk;

    public void setDiskCache(DiskTileCache cache) {
        this.disk = cache;
    }

    // Pulls a window off disk into the LRU
    private FloatTensor adoptFromDisk(String id, int[] windowIndex) {
        if (disk == null) {
            return null;
        }
        LinkedHashMap<List<Integer>, FloatTensor> cache = windowCaches.get(id);
        if (cache == null) {
            return null;
        }
        FloatTensor tensor = disk.load(id, windowIndex);
        if (tensor == null) {
            return null;
        }
        cache.put(toKey(windowIndex), tensor);
        cacheSizes.get(id)[0] += tensor.byteSize();
        return tensor;
    }

    // Creates a non-batched InfiniteTensor, or returns the existing one if already registered under id
    public InfiniteTensor getOrCreate(
            String id,
            Integer[] shape,
            TensorFunction function,
            TensorWindow outputWindow,
            InfiniteTensor[] deps,
            TensorWindow[] depWindows,
            long cacheLimitBytes) {

        if (tensors.containsKey(id)) return tensors.get(id);

        InfiniteTensor tensor = new InfiniteTensor(
                id, shape, outputWindow, function, null, 0,
                deps, depWindows, this, cacheLimitBytes);
        register(id, tensor);
        return tensor;
    }

    // Creates a batched InfiniteTensor, or returns the existing one
    public InfiniteTensor getOrCreateBatched(
            String id,
            Integer[] shape,
            BatchTensorFunction batchFunction,
            TensorWindow outputWindow,
            InfiniteTensor[] deps,
            TensorWindow[] depWindows,
            long cacheLimitBytes,
            int batchSize) {

        if (tensors.containsKey(id)) return tensors.get(id);

        InfiniteTensor tensor = new InfiniteTensor(
                id, shape, outputWindow, null, batchFunction, batchSize,
                deps, depWindows, this, cacheLimitBytes);
        register(id, tensor);
        return tensor;
    }

    private void register(String id, InfiniteTensor tensor) {
        tensors.put(id, tensor);
        // access-order LinkedHashMap for LRU eviction
        windowCaches.put(id, new LinkedHashMap<>(16, 0.75f, true));
        cacheSizes.put(id, new long[]{0L});
    }

    void cacheWindow(String id, int[] windowIndex, FloatTensor output) {
        List<Integer> key = toKey(windowIndex);
        LinkedHashMap<List<Integer>, FloatTensor> cache = windowCaches.get(id);
        long[] size = cacheSizes.get(id);

        if (cache.containsKey(key)) {
            cache.get(key); // triggers access-order promotion
            return;
        }

        cache.put(key, output);
        size[0] += output.byteSize();
        totalComputedWindowCount.incrementAndGet();
        if (disk != null) {
            disk.store(id, windowIndex, output);
        }
    }

    // Returns how many windows have been newly computed and cached
    public long getTotalComputedWindowCount() {
        return totalComputedWindowCount.get();
    }

    void evictIfNeeded(String id, long limitBytes) {
        if (limitBytes == Long.MAX_VALUE) return;
        LinkedHashMap<List<Integer>, FloatTensor> cache = windowCaches.get(id);
        long[] size = cacheSizes.get(id);
        if (cache == null) return;

        // Keep at least one entry even if it exceeds the limit
        Iterator<Map.Entry<List<Integer>, FloatTensor>> it = cache.entrySet().iterator();
        while (size[0] > limitBytes && cache.size() > 1 && it.hasNext()) {
            Map.Entry<List<Integer>, FloatTensor> entry = it.next();
            size[0] -= entry.getValue().byteSize();
            it.remove();
        }
    }

    FloatTensor getCachedWindow(String id, int[] windowIndex) {
        LinkedHashMap<List<Integer>, FloatTensor> cache = windowCaches.get(id);
        if (cache == null) return null;
        FloatTensor hit = cache.get(toKey(windowIndex));
        return hit != null ? hit : adoptFromDisk(id, windowIndex);
    }

    boolean isWindowCached(String id, int[] windowIndex) {
        LinkedHashMap<List<Integer>, FloatTensor> cache = windowCaches.get(id);
        if (cache == null) return false;
        if (cache.containsKey(toKey(windowIndex))) return true;
        return adoptFromDisk(id, windowIndex) != null;
    }

    // Remove all cached window outputs for every registered tensor
    public void clearAllCaches() {
        for (Map.Entry<String, LinkedHashMap<List<Integer>, FloatTensor>> e : windowCaches.entrySet()) {
            e.getValue().clear();
            cacheSizes.get(e.getKey())[0] = 0L;
        }
    }

    // Remove a single tensor and all its cached state
    public void removeTensor(String id) {
        tensors.remove(id);
        windowCaches.remove(id);
        cacheSizes.remove(id);
    }

    private static List<Integer> toKey(int[] windowIndex) {
        List<Integer> key = new ArrayList<>(windowIndex.length);
        for (int v : windowIndex) key.add(v);
        return key;
    }
}
