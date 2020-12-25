package com.morrisware.flutter.net.core;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ResponseManager {

    private final Executor ioExecutor;
    private final Executor uiExecutor;

    public ResponseManager(final Handler mainHandler) {
        this.ioExecutor = Executors.newCachedThreadPool();
        this.uiExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                mainHandler.post(command);
            }
        };
    }

    public Executor getExecutor(Request<?> request) {
        return request != null && request.isResponseOnMain() ? uiExecutor : ioExecutor;
    }

    public void responseResult(Request<?> request, Response<?> response) {
        this.getExecutor(request).execute(new ResponseCallbackRunnable(request, response));
    }

    public void responseError(Request<?> request, Exception httpError) {
        this.getExecutor(request).execute(new ResponseCallbackRunnable(request, Response.error(httpError)));
    }

    private static class ResponseCallbackRunnable implements Runnable {
        private final Request request;
        private final Response response;

        ResponseCallbackRunnable(Request request, Response response) {
            this.request = request;
            this.response = response;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (request.isCanceled()) {
                request.finishRequest();
            } else {
                if (response.getError() == null) {
                    try {
                        request.deliverContent(response);
                    } catch (Exception e) {
                        request.deliverError(Response.error(e));
                    }
                } else {
                    try {
                        request.deliverError(response);
                    } catch (Exception e) {
                        request.deliverError(Response.error(e));
                    }
                }
                request.finishRequest();
            }
        }
    }
}
