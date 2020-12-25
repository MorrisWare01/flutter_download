package com.morrisware.flutter.net.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author mmw
 * @date 2020/3/10
 **/
public class HttpResult {

    public final int statusCode;
    public final byte[] bytes;
    public final boolean isCache;
    public final long cost;
    public final List<Header> headers;
    public final Map<String, String> headerMap;

    public HttpResult(int statusCode, byte[] bytes, boolean isCache, long cost, List<Header> headers) {
        this.statusCode = statusCode;
        this.bytes = bytes;
        this.isCache = isCache;
        this.cost = cost;
        if (headers != null) {
            this.headers = Collections.unmodifiableList(headers);
            this.headerMap = convertMap(headers);
        } else {
            this.headers = Collections.emptyList();
            this.headerMap = Collections.emptyMap();
        }
    }

    private static List<Header> convertHeader(Map<String, String> map) {
        if (map == null) {
            return null;
        } else {
            ArrayList<Header> headers = new ArrayList<>(map.size());
            for (String key : map.keySet()) {
                headers.add(new Header(key, map.get(key)));
            }
            return headers;
        }
    }

    private static Map<String, String> convertMap(List<Header> headers) {
        if (headers == null) {
            return null;
        } else {
            Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Header header : headers) {
                map.put(header.getName(), header.getValue());
            }
            return map;
        }
    }

}
