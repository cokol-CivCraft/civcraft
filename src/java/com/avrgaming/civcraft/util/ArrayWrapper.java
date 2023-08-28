package com.avrgaming.civcraft.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class ArrayWrapper<E> {
    private E[] _array;

    @SuppressWarnings("unchecked")
    public ArrayWrapper(E... elements) {
        setArray(elements);
    }

    public void setArray(@NotNull E[] array) {
        this._array = array;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ArrayWrapper)) {
            return false;
        }
        return Arrays.equals(this._array, ((ArrayWrapper<?>) other)._array);
    }

    public int hashCode() {
        return Arrays.hashCode(this._array);
    }

}