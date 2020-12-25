package com.morrisware.flutter.net;

import android.content.Context;

import com.morrisware.flutter.net.core.Disposable;
import com.morrisware.flutter.net.core.Request;
import com.morrisware.flutter.net.core.RequestLooper;
import com.morrisware.flutter.net.engine.FileDownloaderEngine;

import io.flutter.BuildConfig;


/**
 * @author mmw
 * @date 2020/3/11
 **/
public class HttpManager {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    private static volatile HttpManager httpManager;

    private volatile boolean isInitialized = false;

    private RequestLooper requestLooper;

    private FileDownloaderEngine fileDownloaderEngine;

    private HttpManager(Context context) {
        if (!isInitialized) {
            isInitialized = true;
            requestLooper = new RequestLooper();
            requestLooper.loop();
            fileDownloaderEngine = new FileDownloaderEngine(context.getApplicationContext(), requestLooper);
        }
    }

    public static HttpManager getInstance(Context context) {
        if (httpManager == null) {
            synchronized (HttpManager.class) {
                if (httpManager == null) {
                    httpManager = new HttpManager(context);
                }
            }
        }
        return httpManager;
    }

    public FileDownloaderEngine getFileDownloaderEngine() {
        return fileDownloaderEngine;
    }

    public Disposable sendRequest(final Request request) {
        return requestLooper.sendRequest(request);
    }
}
