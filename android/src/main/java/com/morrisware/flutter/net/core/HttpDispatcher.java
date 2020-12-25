package com.morrisware.flutter.net.core;

import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;

/**
 * @author mmw
 * @date 2020/3/10
 **/
public class HttpDispatcher {

    private DefaultHttpStack httpStack;

    public HttpDispatcher() {
        this.httpStack = new DefaultHttpStack();
    }

    public HttpResult dispatch(Request<?> request) throws Exception {
        while (true) {
            long startTime = SystemClock.elapsedRealtime();
            HttpResponse httpResponse = null;
            HttpResult httpResult = null;
            try {
                httpResponse = httpStack.performRequest(request, Collections.<String, String>emptyMap());
                int statusCode = httpResponse.getStatusCode();
                List<Header> headers = httpResponse.getHeaders();
                if (statusCode != 304) {
                    if (statusCode == 300 || statusCode == 301
                            || statusCode == 302 || statusCode == 303
                            || statusCode == 307 || statusCode == 308) {
                        if (statusCode == 307 || statusCode == 308) {
                            String method = httpResponse.getMethod();
                            if (method != null && !method.equals("GET") && !method.equals("HEAD")) {
                                throw new HttpCodeError("网络异常(" + statusCode + ")", statusCode, headers);
                            }
                        }

                        // 重定向处理
                        String location = httpResponse.getHeader("Location");
                        if (location != null && location.length() > 0) {
                            int currentRedirectCount = request.getCurrentRedirectCount();
                            if (currentRedirectCount > request.getMaxRedirectCount()) {
                                throw new HttpError("重定向次数超限");
                            } else {
                                request.setCurrentRedirectCount(request.getCurrentRetryCount() + 1);
                                throw new RedirectException(String.valueOf(statusCode), statusCode, headers, location);
                            }
                        } else {
                            throw new HttpCodeError("网络异常(" + statusCode + ")", statusCode, headers);
                        }
                    } else if (statusCode >= 200 && statusCode <= 299) {
                        byte[] bytes = this.getHttpBytes(request, httpResponse);
                        httpResult = new HttpResult(statusCode, bytes, false, SystemClock.elapsedRealtime() - startTime, headers);
                        return httpResult;
                    } else {
                        throw new HttpCodeError("网络异常(" + statusCode + ")", statusCode, headers);
                    }
                } else {
                    // TODO 处理http304缓存请求
                    throw new HttpCodeError("网络异常(" + statusCode + ")", statusCode, headers);
                }
            } catch (SocketTimeoutException e) {
                retry(request, e);
                continue;
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            } catch (IOException e) {
                if (httpResponse != null) {
                    retry(request, e);
                    continue;
                }
                throw e;
            } finally {
                if (httpResponse != null && httpResponse.getContent() != null) {
                    try {
                        httpResponse.getContent().close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void retry(Request<?> request, Exception e) throws Exception {
        if (request.getCurrentRetryCount() < request.getMaxRetryCount()) {
            request.setCurrentRetryCount(request.getCurrentRetryCount() + 1);
            dispatch(request);
        } else {
            throw e;
        }
    }

    private byte[] getHttpBytes(Request<?> request, HttpResponse httpResponse) throws IOException {
        if (request.onInterceptHttpResponse()) {
            return request.parseHttpResponse(httpResponse);
        } else {
            // 默认解析
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream content = httpResponse.getContent();
            try {
                byte[] buf = new byte[1024];
                int length;
                while ((length = content.read(buf)) != -1) {
                    baos.write(buf, 0, length);
                }
                return baos.toByteArray();
            } finally {
                try {
                    if (content != null) {
                        content.close();
                    }
                } catch (Exception e) {
                }
                baos.close();
            }
        }
    }

}
