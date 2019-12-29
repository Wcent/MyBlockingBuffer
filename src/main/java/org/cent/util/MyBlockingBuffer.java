package org.cent.util;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 阻塞缓冲区
 * 使用基于AQS的ReetrantLock和Condition实现的阻塞型队列缓冲区
 * 类似ArrayBlockingQueue
 * @author Vincent
 * @version 1.0 2019/12/29
 */
public class MyBlockingBuffer<T> {

    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final Object[] items;
    private int putIndex = 0;
    private int takeIndex =0;
    private int count = 0;

    public MyBlockingBuffer(int capacity) {
        this.items = new Object[capacity];
    }

    /**
     * 生产，写入数据到缓冲区
     * @param item 数据项
     * @throws InterruptedException
     */
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // 缓存区已满，则阻塞等待，直到数据被消费后有空间再写入
            while (count == items.length) {
                notFull.await();
            }
            items[putIndex] = item;
            if (++putIndex == items.length) {
                putIndex = 0;
            }
            ++count;
            // 数据写入缓冲区后，通知可消费
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 消费，从缓冲区取出数据
     * @return 数据项
     * @throws InterruptedException
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            // 缓冲区空，则阻塞等待，直到数据写入后再消费
            while (count == 0) {
                notEmpty.await();
            }
            @SuppressWarnings("unchecked")
            T item = (T) items[takeIndex];
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            --count;
            // 数据被消费后，通知有空间可写入
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 缓冲区待消费数据量
     * @return 数据量
     */
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
