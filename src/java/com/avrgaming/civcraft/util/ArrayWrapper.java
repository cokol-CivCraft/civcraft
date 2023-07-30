package com.avrgaming.civcraft.util;

import org.apache.commons.lang.Validate;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

public final class ArrayWrapper<E> {
    private E[] _array;

    @SuppressWarnings("unchecked")
    public ArrayWrapper(E... elements) {
        setArray(elements);
    }

    public E[] getArray() {
        return this._array;
    }

    public void setArray(E[] array) {
        Validate.notNull(array, "The array must not be null.");
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

    @SuppressWarnings({"unused", "unchecked"})
    public static <T> T[] toArray(Iterable<? extends T> list, Class<T> c) {
        int size = -1;
        if ((list instanceof Collection<? extends T> coll)) {
            size = coll.size();
        }
        if (size < 0) {
            size = 0;
            for (T element : list) {
                size++;
            }
        }
        T[] result = (T[]) Array.newInstance(c, size);
        int i = 0;
        for (T element : list) {
            result[(i++)] = element;
        }
        return result;
    }
}