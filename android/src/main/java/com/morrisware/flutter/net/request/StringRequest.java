package com.morrisware.flutter.net.request;

import androidx.annotation.Nullable;

import com.morrisware.flutter.net.core.HttpResult;
import com.morrisware.flutter.net.core.Request;
import com.morrisware.flutter.net.core.Response;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * @author mmw
 * @date 2020/3/18
 **/
public class StringRequest extends Request<String> {

    public StringRequest(int method, String url, @Nullable Response.ResponseCallback<String> responseCallback) {
        super(method, url, responseCallback);
    }

    @Override
    public Response<String> parse(HttpResult httpResult) {
        try {
            String data = new String(httpResult.bytes, getCharset(httpResult.headerMap, "utf-8"));
            return Response.success(data);
        } catch (UnsupportedEncodingException e) {
            return Response.error(e);
        }
    }

    private String getCharset(Map<String, String> headers, String defaultCharset) {
        String charset = headers.get("Content-Type");
        if (charset != null) {
            String[] values = charset.split(";", 0);
            for (int i = 1; i < values.length; i++) {
                String[] data = values[i].split("=", 0);
                if (data.length == 2 && "charset".equals(data[0])) {
                    return data[1];
                }
            }
        }
        return defaultCharset;
    }

}
