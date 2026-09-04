package com.github.xandergos.terraindiffusionmc.infinitetensor;

import java.util.List;

// Batched variant of TensorFunction
@FunctionalInterface
public interface BatchTensorFunction {
    // Applies the function to a batch of tensor windows
    List<FloatTensor> apply(List<int[]> windowIndices, List<List<FloatTensor>> args);
}
