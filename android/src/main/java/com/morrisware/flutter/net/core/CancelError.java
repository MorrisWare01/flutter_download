package com.morrisware.flutter.net.core;

/**
 * @author mmw
 * @date 2020/3/11
 **/
public class CancelError extends HttpError {
    public CancelError() {
        super("request cancel");
    }
}
