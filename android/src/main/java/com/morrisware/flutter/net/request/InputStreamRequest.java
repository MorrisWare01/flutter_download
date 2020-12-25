package com.morrisware.flutter.net.request;



import androidx.annotation.Nullable;

import com.morrisware.flutter.net.core.HttpResult;
import com.morrisware.flutter.net.core.Request;
import com.morrisware.flutter.net.core.Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author mmw
 * @date 2020/3/19
 **/
public class InputStreamRequest extends Request<InputStream> {

    public InputStreamRequest(int method, String url, @Nullable Response.ResponseCallback<InputStream> responseCallback) {
        super(method, url, responseCallback);
    }

    @Override
    public Response parse(HttpResult httpResult) {
        return Response.success(new ByteArrayInputStream(httpResult.bytes));
    }
}
