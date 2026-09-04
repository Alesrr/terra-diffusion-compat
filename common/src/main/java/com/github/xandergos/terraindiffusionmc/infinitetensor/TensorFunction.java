package com.github.xandergos.terraindiffusionmc.infinitetensor;

import java.util.List;

// Function that computes a window of an InfiniteTensor
@FunctionalInterface
public interface TensorFunction {
    // Computes one tensor window
    FloatTensor apply(int[] windowIndex, List<FloatTensor> args);
}
