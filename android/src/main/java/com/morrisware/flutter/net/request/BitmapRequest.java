package com.morrisware.flutter.net.request;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import android.widget.ImageView;

import com.morrisware.flutter.net.core.HttpError;
import com.morrisware.flutter.net.core.HttpResult;
import com.morrisware.flutter.net.core.Request;
import com.morrisware.flutter.net.core.Response;

/**
 * @author mmw
 * @date 2020/3/11
 **/
public class BitmapRequest extends Request<Bitmap> {
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    @Nullable
    private Response.ResponseCallback<Bitmap> responseCallback;

    private final int width;
    private final int height;
    private final ImageView.ScaleType scaleType;
    private final Bitmap.Config config;
    private static final Object mParseLock = new Object();

    public BitmapRequest(String url, @Nullable Response.ResponseCallback<Bitmap> responseCallback,
                         int width, int height, ImageView.ScaleType scaleType, Bitmap.Config config) {
        super(Request.METHOD_GET, url, responseCallback);
        this.width = width;
        this.height = height;
        this.scaleType = scaleType;
        this.config = config;
        setShouldCache(false);
    }

    @Override
    public Response parse(HttpResult httpResult) {
        synchronized (mParseLock) {
            try {
                return parseBitmap(httpResult);
            } catch (OutOfMemoryError var5) {
                return Response.error(new HttpError("out of memory"));
            }
        }
    }

    private Response<Bitmap> parseBitmap(HttpResult httpResult) {
        byte[] bytes = httpResult.bytes;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = config;
        Bitmap bitmap;
        if (width == 0 && height == 0) {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } else {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            int outWidth = options.outWidth;
            int outHeight = options.outHeight;
            int targetWidth = transform(width, height, outWidth, outHeight, scaleType);
            int targetHeight = transform(height, width, outHeight, outWidth, scaleType);
            options.inJustDecodeBounds = false;
            options.inSampleSize = getSampleSize(outWidth, outHeight, targetWidth, targetHeight);
            Bitmap targetBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (targetBitmap == null || (targetBitmap.getWidth() == targetWidth && targetBitmap.getHeight() == targetHeight)) {
                bitmap = targetBitmap;
            } else {
                bitmap = Bitmap.createScaledBitmap(targetBitmap, targetWidth, targetHeight, true);
                targetBitmap.recycle();
            }
        }
        return bitmap == null ? Response.<Bitmap>error(new HttpError("can't parse bitmap")) : Response.success(bitmap);
    }

    private static int transform(int measureWidth, int maxHeight, int outWidth, int outHeight, ImageView.ScaleType scaleType) {
        if (measureWidth == 0 && maxHeight == 0) {
            return outWidth;
        } else if (scaleType == ImageView.ScaleType.FIT_XY) {
            return measureWidth == 0 ? outWidth : measureWidth;
        } else {
            double ratio;
            if (measureWidth == 0) {
                ratio = (double) maxHeight / (double) outHeight;
                return (int) ((double) outWidth * ratio);
            } else if (maxHeight == 0) {
                return measureWidth;
            } else {
                ratio = (double) outHeight / (double) outWidth;
                int targetWidth = measureWidth;
                if (scaleType == ImageView.ScaleType.CENTER_CROP) {
                    if ((double) measureWidth * ratio < (double) maxHeight) {
                        targetWidth = (int) ((double) maxHeight / ratio);
                    }

                    return targetWidth;
                } else {
                    if ((double) measureWidth * ratio > (double) maxHeight) {
                        targetWidth = (int) ((double) maxHeight / ratio);
                    }

                    return targetWidth;
                }
            }
        }
    }

    private static int getSampleSize(int width, int height, int targetWidth, int targetHeight) {
        double widthRatio = (double) width / (double) targetWidth;
        double heightRatio = (double) height / (double) targetHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        float roundRatio = 1.0F;
        while (roundRatio * 2.0 <= ratio) {
            roundRatio *= 2.0;
        }
        return (int) roundRatio;
    }


    @Override
    public void cancel() {
        super.cancel();
        synchronized (this.mLock) {
            this.responseCallback = null;
        }
    }

    @Override
    public void deliverContent(Response<Bitmap> response) {
        super.deliverContent(response);
        Response.ResponseCallback<Bitmap> responseCallback;
        synchronized (this.mLock) {
            responseCallback = this.responseCallback;
        }

        if (responseCallback != null) {
            responseCallback.onSuccess(response);
        }
    }

}
