package com.wawa.common.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-27
 * Time: 上午9:41
 * To change this template use File | Settings | File Templates.
 */
public class BaseThreadPoolExecutor extends ThreadPoolExecutor
{
     private int chatPoolSize ;
     private int chatActiveSize ;

    public int getChatActiveSize() {
        return chatActiveSize;
    }

    public int getChatPoolSize()
    {
        return chatPoolSize;
    }

    public BaseThreadPoolExecutor(int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime,
                                  TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory)
     {
         super(corePoolSize,maximumPoolSize,keepAliveTime,unit,workQueue,threadFactory) ;
     }

    protected void afterExecute(Runnable r, Throwable t)
    {
        this.chatPoolSize = this.getPoolSize() ;
        this.chatActiveSize = this.getActiveCount();
    }

}
