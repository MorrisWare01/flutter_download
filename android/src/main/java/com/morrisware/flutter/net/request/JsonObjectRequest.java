package com.morrisware.flutter.net.request;


import androidx.annotation.Nullable;

import com.morrisware.flutter.net.core.HttpResult;
import com.morrisware.flutter.net.core.Request;
import com.morrisware.flutter.net.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class JsonObjectRequest extends Request<JSONObject> {

    public JsonObjectRequest(int method, String url, @Nullable Response.ResponseCallback<JSONObject> responseCallback) {
        super(method, url, responseCallback);
    }

    @Override
    public Response parse(HttpResult httpResult) {
        try {
            String data = new String(httpResult.bytes, getCharset(httpResult.headerMap, "utf-8"));
            return Response.success(new JSONObject(data));
        } catch (UnsupportedEncodingException | JSONException e) {
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

    @Override
    public void deliverError(Response<JSONObject> response) {
        super.deliverError(response);
        // TODO 使用拦截器
        Exception exception = response.getError();
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    protected String optString(JSONObject jsonObject, String name) {
        return jsonObject.isNull(name) ? null : jsonObject.optString(name);
    }

    protected Long optLong(JSONObject jsonObject, String name) {
        return jsonObject.isNull(name) ? null : jsonObject.optLong(name);
    }


}
