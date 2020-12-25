package com.morrisware.flutter.net.core;

import android.os.Handler;
import android.os.Looper;

import com.morrisware.flutter.net.request.FileRequest;

import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分发请求管理
 */
public class RequestLooper {
    private final AtomicInteger sequence;
    private HashSet<Request> requests;
    private final PriorityBlockingQueue<Request> httpCacheRequestQueue;
    private final PriorityBlockingQueue<Request> httpRequestQueue;
    private final PriorityBlockingQueue<Request> httpFileRequestQueue;
    private final HttpThread[] httpThreads;
    private HttpCacheThread httpCacheThread;
    private final HttpDispatcher httpDispatcher;
    private final ResponseManager responseManager;

    public RequestLooper() {
        sequence = new AtomicInteger();
        requests = new HashSet<>();
        httpCacheRequestQueue = new PriorityBlockingQueue<>();
        httpRequestQueue = new PriorityBlockingQueue<>();
        httpFileRequestQueue = new PriorityBlockingQueue<>();
        httpThreads = new HttpThread[6];
        httpDispatcher = new HttpDispatcher();
        responseManager = new ResponseManager(new Handler(Looper.getMainLooper()));
    }

    public void loop() {
        this.httpCacheThread = new HttpCacheThread(httpCacheRequestQueue, httpRequestQueue);
        this.httpCacheThread.start();

        for (int i = 0; i < this.httpThreads.length / 2; i++) {
            HttpThread httpThread = new HttpThread(httpRequestQueue, httpDispatcher, responseManager);
            this.httpThreads[i] = httpThread;
            httpThread.start();
        }
        for (int i = this.httpThreads.length / 2; i < this.httpThreads.length; i++) {
            HttpThread httpThread = new HttpThread(httpFileRequestQueue, httpDispatcher, responseManager);
            this.httpThreads[i] = httpThread;
            httpThread.start();
        }
    }

    public int createSequence() {
        return this.sequence.incrementAndGet();
    }

    public Disposable sendRequest(final Request request) {
        request.setStartTime();
        request.setRequestLooper(this);
        request.setSequence(createSequence());
        requests.add(request);
        if (request instanceof FileRequest) {
            httpFileRequestQueue.add(request);
        } else {
            if (request.shouldCache()) {
                httpCacheRequestQueue.add(request);
            } else {
                httpRequestQueue.add(request);
            }
        }
        return new Disposable() {
            @Override
            public void dispose() {
                request.cancel();
            }

            @Override
            public boolean isDisposed() {
                return request.isCanceled();
            }
        };
    }

    <T> void finishRequest(Request<T> request) {
        requests.remove(request);
    }

    public ResponseManager getResponseManager() {
        return responseManager;
    }

}
