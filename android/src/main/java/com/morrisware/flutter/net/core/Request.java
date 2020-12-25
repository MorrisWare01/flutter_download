package com.morrisware.flutter.net.core;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.morrisware.flutter.net.HttpManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public abstract class Request<T> implements Comparable<Request<T>> {
    public static final int METHOD_GET = 0;
    public static final int METHOD_POST = 1;
    public static final int METHOD_PUT = 2;
    public static final int METHOD_DELETE = 3;
    public static final int METHOD_HEAD = 4;
    public static final int METHOD_OPTIONS = 5;
    public static final int METHOD_TRACE = 6;
    public static final int METHOD_PATCH = 7;

    private int method;
    private String url;
    private boolean isCancel;
    private boolean shouldCache;
    private String userAgent;

    private final Object mLock = new Object();
    @Nullable
    @GuardedBy("mLock")
    private Response.ResponseCallback<T> responseCallback;

    // 请求列表
    private RequestLooper requestLooper;
    private Integer sequence;
    private long startTime;

    // 重试
    public static final int MAX_RETRY_COUNT = 1;
    private int currentRetryCount = 0;

    // 重定向次数
    public static final int MAX_REDIRECT_COUNT = 10;
    private int currentRedirectCount = 0;

    // 主线程回调
    private boolean responseOnMain = true;

    public Request(int method, String url, @Nullable Response.ResponseCallback<T> responseCallback) {
        this.method = method;
        this.url = url;
        this.responseCallback = responseCallback;
    }

    public int getMethod() {
        return method;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimeoutMs() {
        return 10000;
    }

    public int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    public int getCurrentRetryCount() {
        return currentRetryCount;
    }

    public void setCurrentRetryCount(int currentRetryCount) {
        this.currentRetryCount = currentRetryCount;
    }

    public int getMaxRedirectCount() {
        return MAX_REDIRECT_COUNT;
    }

    public int getCurrentRedirectCount() {
        return currentRedirectCount;
    }

    public void setCurrentRedirectCount(int currentRedirectCount) {
        this.currentRedirectCount = currentRedirectCount;
    }

    public String getCacheKey() {
        String url = this.getUrl();
        int method = this.getMethod();
        return method != 0 ? Integer.toString(method) + '-' + url : url;
    }

    public Request<?> setRequestLooper(RequestLooper requestLooper) {
        this.requestLooper = requestLooper;
        return this;
    }

    public final RequestLooper getRequestLooper() {
        return this.requestLooper;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime() {
        this.startTime = SystemClock.elapsedRealtime();
    }

    public final Request<?> setSequence(int sequence) {
        this.sequence = sequence;
        return this;
    }

    public final int getSequence() {
        if (this.sequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        } else {
            return this.sequence;
        }
    }

    void finishRequest() {
        if (requestLooper != null) {
            requestLooper.finishRequest(this);
        }
    }

    @CallSuper
    public void cancel() {
        synchronized (mLock) {
            isCancel = true;
            responseCallback = null;
        }
    }

    public boolean isCanceled() {
        synchronized (mLock) {
            return this.isCancel;
        }
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public Request<?> setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public Map<String, String> getHeaders() {
        return Collections.emptyMap();
    }

    protected String getCharset() {
        return "UTF-8";
    }

    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + this.getCharset();
    }

    protected Map<String, String> getParams() throws Exception {
        return null;
    }

    private String getParamsBody(Map<String, String> map, String charset) {
        StringBuilder sb = new StringBuilder();
        Iterator iterator = map.entrySet().iterator();
        try {
            while (iterator.hasNext()) {
                Map.Entry var5 = (Map.Entry) iterator.next();
                if (var5.getKey() == null || var5.getValue() == null) {
                    throw new IllegalArgumentException(String.format("Request#getParams() returned a map" +
                                    " containing a null key or value: (%s, %s). " +
                                    "All keys and values must be non-null.",
                            var5.getKey(), var5.getValue())
                    );
                }

                sb.append(URLEncoder.encode((String) var5.getKey(), charset));
                sb.append('=');
                sb.append(URLEncoder.encode((String) var5.getValue(), charset));
                sb.append('&');
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding not supported: " + charset, e);
        }
        return sb.toString();
    }

    public String getGetParams() throws Exception {
        Map<String, String> params = getParams();
        if (params != null) {
            return getParamsBody(params, getCharset());
        } else {
            return null;
        }
    }

    public byte[] getBody() throws Exception {
        Map<String, String> map = this.getParams();
        if (map != null && map.size() > 0) {
            String charset = getCharset();
            try {
                String body = this.getParamsBody(map, charset);
                return body.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding not supported: " + charset, e);
            }
        }
        return null;
    }

    public final Request<?> setShouldCache(boolean shouldCache) {
        this.shouldCache = shouldCache;
        return this;
    }

    public final boolean shouldCache() {
        return this.shouldCache;
    }

    public boolean isResponseOnMain() {
        return responseOnMain;
    }

    public void setResponseOnMain(boolean responseOnMain) {
        this.responseOnMain = responseOnMain;
    }

    public void redirectRequest(String url) {
        this.method = METHOD_GET;
        this.url = url;
        requestLooper.sendRequest(this);
    }

    protected boolean onInterceptHttpResponse() {
        return false;
    }

    protected byte[] parseHttpResponse(HttpResponse httpResponse) throws IOException {
        return null;
    }

    protected abstract Response parse(HttpResult httpResult);

    protected void deliverError(Response<T> response) {
        Response.ResponseCallback<T> callback;
        synchronized (this.mLock) {
            callback = this.responseCallback;
        }

        if (callback != null) {
            callback.onFailure(response);
        }
    }

    protected void deliverContent(Response<T> response) {
        Response.ResponseCallback<T> callback;
        synchronized (this.mLock) {
            callback = this.responseCallback;
        }

        if (callback != null) {
            callback.onSuccess(response);
        }
    }

    @Override
    public int compareTo(Request<T> next) {
        return getSequence() - next.getSequence();
    }

    public String getMethodName() {
        switch (method) {
            case Request.METHOD_GET:
                return "GET";
            case Request.METHOD_POST:
                return "POST";
            case Request.METHOD_PUT:
                return "PUT";
            case Request.METHOD_DELETE:
                return "DELETE";
            case Request.METHOD_HEAD:
                return "HEAD";
            case Request.METHOD_OPTIONS:
                return "OPTIONS";
            case Request.METHOD_TRACE:
                return "TRACE";
            case Request.METHOD_PATCH:
                return "PATCH";
            default:
                return "";
        }
    }

    public String getUrlWithParams() {
        String url = getUrl();
        try {
            String getParams = getGetParams();
            if (!TextUtils.isEmpty(getParams) && method == Request.METHOD_GET) {
                int index = url.indexOf("?");
                if (index == -1) {
                    url = url + "?" + getParams;
                } else {
                    url = url + "&" + getParams;
                }
            }
            return url;
        } catch (Exception e) {
            if (HttpManager.DEBUG) {
                e.printStackTrace();
            }
        }
        return url;
    }

}
