package com.morrisware.flutter.net.core;

import android.os.Process;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author mmw
 * @date 2020/3/10
 * 处理缓存
 **/
public class HttpCacheThread extends Thread {

    private PriorityBlockingQueue<Request> httpCacheRequestQueue;
    private PriorityBlockingQueue<Request> httpRequestQueue;

    public HttpCacheThread(PriorityBlockingQueue<Request> httpCacheRequestQueue, PriorityBlockingQueue<Request> httpRequestQueue) {
        this.httpCacheRequestQueue = httpCacheRequestQueue;
        this.httpRequestQueue = httpRequestQueue;
    }

    @Override
    public void run() {
        super.run();
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }
}
