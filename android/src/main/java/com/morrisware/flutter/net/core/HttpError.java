package com.morrisware.flutter.net.core;

import java.io.IOException;

public class HttpError extends IOException {
    private String msg;

    public HttpError(String msg) {
        super(msg);
        this.msg = msg;
    }

    public HttpError(Throwable cause) {
        super(cause);
        this.msg = cause.getMessage();
    }

    public String getMsg() {
        return msg;
    }
}
