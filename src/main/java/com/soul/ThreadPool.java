package com.soul;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    private int coreSize;//核心线程数
    private long keepAliveTime;//最大超时时间
    private TimeUnit timeUnit;//时间单位
    private RejectPolicy rejectPolicy;

    private MyBlockQueue<Runnable> taskQueue;
    private HashSet<Worker> workers = new HashSet<>();


    public ThreadPool(int coreSize, long keepAliveTime,
                      TimeUnit timeUnit, int queueCapacity, RejectPolicy rejectPolicy) {
        this.coreSize = coreSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.taskQueue = new MyBlockQueue<>(queueCapacity);
        this.rejectPolicy = rejectPolicy;
    }


    public void execute(Runnable task) {
        //任务数没有超过 coreSize 时，交给Worker对象执行
        if (workers.size() <= coreSize) {
            Worker worker = new Worker(task);
            workers.add(worker);
            worker.start();
        } else {//任务数超过 coreSize 时，进入阻塞队列
            taskQueue.tryPut(rejectPolicy, task);
        }

    }

    class Worker extends Thread {

        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            while (task != null || (task = taskQueue.poll(keepAliveTime, timeUnit)) != null) {
                try {
                    System.out.println("正在执行  ..." + task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
                synchronized (workers) {
                    System.out.println("worker被移除  ..." + this);
                    workers.remove(this);
                }
            }
        }
    }
}
