package com.morrisware.flutter.net.core;

import android.util.Log;

import com.morrisware.flutter.net.HttpManager;
import com.morrisware.flutter.net.request.BitmapRequest;
import com.morrisware.flutter.net.request.FileRequest;
import com.morrisware.flutter.net.request.InputStreamRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * 处理网络请求
 */
public class HttpThread extends Thread {

    private PriorityBlockingQueue<Request> httpRequestQueue;
    private HttpDispatcher httpDispatcher;
    private ResponseManager responseManager;
    private volatile boolean exit = false;

    public HttpThread(PriorityBlockingQueue<Request> httpRequestQueue, HttpDispatcher httpDispatcher, ResponseManager responseManager) {
        this.httpRequestQueue = httpRequestQueue;
        this.httpDispatcher = httpDispatcher;
        this.responseManager = responseManager;
    }

    public void exit() {
        this.exit = true;
        this.interrupt();
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            try {
                this.runQueue();
            } catch (InterruptedException var2) {
                if (this.exit) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void runQueue() throws InterruptedException {
        Request request = this.httpRequestQueue.take();
        runRequest(request);
    }

    private void runRequest(Request request) {
        try {
            if (request.isCanceled()) {
                request.cancel();
                responseManager.responseResult(request, Response.error(new CancelError()));
            } else {
                logRequest(request);
                HttpResult httpResult = httpDispatcher.dispatch(request);
                logSuccess(request, httpResult);
                Response response = request.parse(httpResult);
                responseManager.responseResult(request, response);
            }
        } catch (Exception e) {
            if (e instanceof HttpCodeError) {
                HttpCodeError error = (HttpCodeError) e;
                logError(request, error);
            }
            if (e instanceof RedirectException) {
                request.redirectRequest(((RedirectException) e).getUrl());
            } else {
                responseManager.responseError(request, e);
            }
        }
    }

    private static final String LOG_TAG = "HTTP";

    private void logRequest(Request request) {
        if (HttpManager.DEBUG) {
            Log.d(LOG_TAG, String.format("--> %s %s", request.getMethodName(), request.getUrlWithParams()));
            Map<String, String> headers = request.getHeaders();
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    Log.d(LOG_TAG, String.format("%s: %s", entry.getKey(), entry.getValue()));
                }
            }
            if (request.getMethod() == Request.METHOD_POST) {
                try {
                    byte[] body = request.getBody();
                    if (body != null) {
                        Log.d(LOG_TAG, new String(body, request.getCharset()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(LOG_TAG, String.format("--> END %s", request.getMethodName()));
        }
    }

    private void logSuccess(Request request, HttpResult httpResult) {
        if (HttpManager.DEBUG) {
            Log.d(LOG_TAG, "<-- " + httpResult.statusCode + " " + request.getUrlWithParams());
            Map<String, String> headers = httpResult.headerMap;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    Log.d(LOG_TAG, String.format("%s: %s", entry.getKey(), entry.getValue()));
                }
            }
            if (!(request instanceof BitmapRequest || request instanceof FileRequest || request instanceof InputStreamRequest)) {
                try {
                    Log.d(LOG_TAG, new String(httpResult.bytes, getCharset(headers, "utf-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            Log.d(LOG_TAG, "<-- END HTTP");
        }
    }

    private void logError(Request request, HttpCodeError e) {
        if (HttpManager.DEBUG) {
            Log.d(LOG_TAG, "<-- " + e.getStatusCode() + " " + request.getUrlWithParams());
            Map<String, String> headers = e.getHeaders();
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    Log.d(LOG_TAG, String.format("%s: %s", entry.getKey(), entry.getValue()));
                }
            }
            Log.d(LOG_TAG, "<-- END HTTP");
        }
    }

    private String getCharset(Map<String, String> headers, String defaultCharset) {
        String charset = headers.get("Content-Type");
        if (charset != null) {
            String[] values = charset.split(";", 0);
            for (int i = 1; i < values.length; i++) {
                String[] data = values[i].split("=", 0);
                if (data.length == 2 && "charset".equals(data[0])) {
                    return data[1];
                }
            }
        }
        return defaultCharset;
    }

}
