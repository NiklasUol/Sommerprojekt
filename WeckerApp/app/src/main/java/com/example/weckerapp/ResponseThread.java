package com.example.weckerapp;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ResponseThread extends Thread{
    private static Lock lock = new ReentrantLock();
    private Runnable runnable;

    public ResponseThread(Runnable runnable){
        super(runnable);
        this.runnable = runnable;
    }

    public void run()
    {
        if (ResponseThread.lock.tryLock())
        {
            try
            {
                runnable.run();
            }
            finally
            {
                ResponseThread.lock.unlock();
            }
        }
    }
}
