package com.morrisware.flutter.net.core;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class HttpResponse {
    private final int statusCode;
    private final String method;
    private final List<Header> headers;
    private final int contentLength;
    private final InputStream content;

    public HttpResponse(int statusCode, String method, List<Header> headers) {
        this(statusCode, method, headers, -1, null);
    }

    public HttpResponse(int statusCode, String method, List<Header> headers, int contentLength, InputStream content) {
        this.statusCode = statusCode;
        this.method = method;
        this.headers = headers;
        this.contentLength = contentLength;
        this.content = content;
    }

    public final int getStatusCode() {
        return this.statusCode;
    }

    public String getMethod() {
        return method;
    }

    public final List<Header> getHeaders() {
        return Collections.unmodifiableList(this.headers);
    }

    public final int getContentLength() {
        return this.contentLength;
    }

    public final InputStream getContent() {
        return this.content;
    }

    public String getHeader(String key) {
        List<Header> headers = this.headers;
        if (headers != null && headers.size() > 0) {
            for (Header header : headers) {
                if (header.getName() != null && header.getName().equals(key)) {
                    return header.getValue();
                }
            }
        }
        return null;
    }

}
