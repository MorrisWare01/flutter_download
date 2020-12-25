package com.morrisware.flutter.net.core;

public class Response<T> {

    private T data;

    private Exception error;

    public Response(T data) {
        this.data = data;
    }

    public Response(Exception error) {
        this.error = error;
    }

    public static <T> Response<T> success(T var0) {
        return new Response<T>(var0);
    }

    public static <T> Response<T> error(Exception var0) {
        return new Response<T>(var0);
    }

    public T getData() {
        return data;
    }

    public Exception getError() {
        return error;
    }

    public interface ResponseCallback<T> {

        void onSuccess(Response<T> response);

        void onFailure(Response<T> response);
    }
}
