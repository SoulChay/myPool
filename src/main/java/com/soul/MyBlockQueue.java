package com.soul;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class MyBlockQueue<T> {

    private int capacity;
    private Deque<T> queue = new ArrayDeque<T>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition fullCondition = lock.newCondition();
    private Condition emptyCondition = lock.newCondition();

    public MyBlockQueue(int capcity) {
        this.capacity = capcity;
    }

    /**
     * 获取(阻塞)
     *
     * @return
     */
    public T get() {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                try {
                    emptyCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullCondition.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加(阻塞)
     *
     * @param t
     */
    public void put(T t) {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                try {
                    System.out.println("等待加入线程队列..." + t);
                    fullCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("加入任务队列..." + t);
            queue.addLast(t);
            emptyCondition.signal();
        } finally {
            lock.unlock();
        }
    }


    /**
     * 获取(阻塞 + 超时时间)
     *
     * @return
     */
    public T poll(long keepAliveTime, TimeUnit timeUnit) {
        lock.lock();
        long nanos = timeUnit.toNanos(keepAliveTime);
        try {
            while (queue.isEmpty()) {
                try {
                    if (nanos <= 0) return null;
                    nanos = emptyCondition.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeLast();
            fullCondition.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加(阻塞 + 超时时间)
     */
    public boolean offer(long timeout, TimeUnit timeUnit, T t) {
        lock.lock();
        long nanos = timeUnit.toNanos(timeout);
        try {
            while (queue.size() == capacity) {
                try {
                    if (nanos <= 0) return false;
                    nanos = fullCondition.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(t);
            emptyCondition.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取队列的大小
     *
     * @return
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加(阻塞 + 拒绝策略)
     * @param rejectPolicy
     * @param task
     */
    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            if (queue.size() == capacity) {
                rejectPolicy.reject(this, task);
            } else {
                queue.addLast(task);
                emptyCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}
