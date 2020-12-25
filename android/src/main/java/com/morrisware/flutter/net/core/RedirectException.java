package com.morrisware.flutter.net.core;

import java.util.List;

/**
 * @author mmw
 * @date 2020/4/7
 **/
public class RedirectException extends HttpCodeError {

    private String url;

    public RedirectException(String msg, int statusCode, List<Header> headers, String url) {
        super(msg, statusCode, headers);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
