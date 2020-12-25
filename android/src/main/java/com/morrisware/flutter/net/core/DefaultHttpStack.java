package com.morrisware.flutter.net.core;

import android.text.TextUtils;

import com.morrisware.flutter.net.HttpManager;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultHttpStack {

    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws Exception {
        String url = request.getUrlWithParams();

        HashMap<String, String> headers = new HashMap<>();
        if (!TextUtils.isEmpty(request.getUserAgent())) {
            headers.put("User-Agent", request.getUserAgent());
        }
        headers.putAll(request.getHeaders());
        headers.putAll(additionalHeaders);

        HttpResponse httpResponse;
        boolean hasContent = false;
        HttpURLConnection conn = null;
        try {
            if (HttpManager.DEBUG) {
                conn = (HttpURLConnection) new URL(url).openConnection();
            } else {
                conn = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
            }
            // add header
            Iterator<String> iterator = headers.keySet().iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                conn.setRequestProperty(name, headers.get(name));
            }

            // 超时时间
            conn.setReadTimeout(request.getTimeoutMs());
            conn.setConnectTimeout(request.getTimeoutMs());
            // 默认不进行重定向
            conn.setInstanceFollowRedirects(false);

            setMethod(conn, request);
            int statusCode = conn.getResponseCode();
            if (statusCode == -1) {
                throw new IOException("response code error from HttpUrlConnection.");
            }
            String method = conn.getRequestMethod();

            if (hasContent(request.getMethod(), statusCode)) {
                hasContent = true;
                httpResponse = new HttpResponse(statusCode, method, convertHeader(conn.getHeaderFields()), conn.getContentLength(), new HttpInputStream(conn));
                return httpResponse;
            }

            httpResponse = new HttpResponse(statusCode, method, convertHeader(conn.getHeaderFields()));
        } finally {
            if (!hasContent) {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return httpResponse;
    }

    private static boolean hasContent(int method, int statusCode) {
        return method != Request.METHOD_HEAD && (100 > statusCode || statusCode >= 200) && statusCode != 204 && statusCode != 304;
    }

    private void setMethod(HttpURLConnection conn, Request<?> request) throws Exception {
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                conn.setRequestMethod("GET");
                break;
            case Request.METHOD_POST:
                conn.setRequestMethod("POST");
                setParams(conn, request);
                break;
            case Request.METHOD_PUT:
                conn.setRequestMethod("PUT");
                setParams(conn, request);
                break;
            case Request.METHOD_DELETE:
                conn.setRequestMethod("DELETE");
                break;
            case Request.METHOD_HEAD:
                conn.setRequestMethod("HEAD");
                break;
            case Request.METHOD_OPTIONS:
                conn.setRequestMethod("OPTIONS");
                break;
            case Request.METHOD_TRACE:
                conn.setRequestMethod("TRACE");
                break;
            case Request.METHOD_PATCH:
                conn.setRequestMethod("PATCH");
                setParams(conn, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private void setParams(HttpURLConnection conn, Request<?> request) throws Exception {
        conn.setDoOutput(true);
        if (!conn.getRequestProperties().containsKey("Content-Type")) {
            conn.setRequestProperty("Content-Type", request.getBodyContentType());
        }

        DataOutputStream var3 = new DataOutputStream(conn.getOutputStream());
        var3.write(request.getBody());
        var3.close();
    }

    private static InputStream getContent(HttpURLConnection var0) {
        InputStream var1;
        try {
            var1 = var0.getInputStream();
        } catch (IOException var3) {
            var1 = var0.getErrorStream();
        }

        return var1;
    }

    static class HttpInputStream extends FilterInputStream {
        private final HttpURLConnection conn;

        HttpInputStream(HttpURLConnection conn) {
            super(getContent(conn));
            this.conn = conn;
        }

        public void close() throws IOException {
            super.close();
            this.conn.disconnect();
        }
    }

    private static List<Header> convertHeader(Map<String, List<String>> headerFields) {
        ArrayList<Header> headers = new ArrayList<>(headerFields.size());
        Iterator<Map.Entry<String, List<String>>> iterator = headerFields.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            List<String> list = entry.getValue();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (entry.getKey() != null) {
                        headers.add(new Header(entry.getKey(), list.get(i)));
                    }
                }
            }
        }
        return headers;
    }


}
