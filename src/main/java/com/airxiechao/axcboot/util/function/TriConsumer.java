package com.airxiechao.axcboot.util.function;

public interface TriConsumer<K, V, S> {
    void accept(K k, V v, S s);
}

