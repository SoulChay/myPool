package com.soul;

/**
 * 拒绝策略
 */
@FunctionalInterface
public interface RejectPolicy<T> {
    void reject(MyBlockQueue<T> queue, T task);
}

