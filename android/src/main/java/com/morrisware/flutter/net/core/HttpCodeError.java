package com.morrisware.flutter.net.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author mmw
 * @date 2020/4/15
 **/
public class HttpCodeError extends HttpError {

    private int statusCode;
    private Map<String, String> headers;

    public HttpCodeError(String msg, int statusCode, List<Header> headers) {
        super(msg);
        this.statusCode = statusCode;
        this.headers = convertMap(headers);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    private static Map<String, String> convertMap(List<Header> headers) {
        if (headers == null) {
            return Collections.emptyMap();
        } else {
            Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Header header : headers) {
                map.put(header.getName(), header.getValue());
            }
            return map;
        }
    }
}
