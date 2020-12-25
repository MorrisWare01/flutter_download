package com.morrisware.flutter.net.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;

/**
 * @author mmw
 * @date 2020/3/11
 **/
public class TransformationUtils {

    public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
    private static final int CIRCLE_CROP_PAINT_FLAGS = PAINT_FLAGS | Paint.ANTI_ALIAS_FLAG;
    private static Paint CIRCLE_CROP_BITMAP_PAINT;
    private static final Paint CIRCLE_CROP_SHAPE_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);

    static {
        CIRCLE_CROP_BITMAP_PAINT = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        CIRCLE_CROP_BITMAP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    /**
     * 等比例缩放
     */
    public static Bitmap centerCrop(Bitmap inBitmap, int width, int height) {
        final float scale;
        final float dx;
        final float dy;
        Matrix m = new Matrix();
        if (inBitmap.getWidth() * height > width * inBitmap.getHeight()) {
            scale = (float) height / (float) inBitmap.getHeight();
            dx = (width - inBitmap.getWidth() * scale) * 0.5f;
            dy = 0;
        } else {
            scale = (float) width / (float) inBitmap.getWidth();
            dx = 0;
            dy = (height - inBitmap.getHeight() * scale) * 0.5f;
        }

        m.setScale(scale, scale);
        m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

        Bitmap targetBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        canvas.drawBitmap(inBitmap, m, new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.setBitmap(null);

        return targetBitmap;
    }

    public static Bitmap circleCrop(Bitmap inBitmap, int destWidth, int destHeight) {
        int destMinEdge = Math.min(destWidth, destHeight);
        float radius = destMinEdge / 2f;

        int srcWidth = inBitmap.getWidth();
        int srcHeight = inBitmap.getHeight();

        float scaleX = destMinEdge / (float) srcWidth;
        float scaleY = destMinEdge / (float) srcHeight;
        float maxScale = Math.max(scaleX, scaleY);

        float scaledWidth = maxScale * srcWidth;
        float scaledHeight = maxScale * srcHeight;
        float left = (destMinEdge - scaledWidth) / 2f;
        float top = (destMinEdge - scaledHeight) / 2f;

        RectF destRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        Bitmap targetBitmap = Bitmap.createBitmap((int) scaledWidth, (int) scaledHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        // Draw a circle
        canvas.drawCircle(radius, radius, radius, CIRCLE_CROP_SHAPE_PAINT);
        // Draw the bitmap in the circle
        canvas.drawBitmap(inBitmap, null, destRect, CIRCLE_CROP_BITMAP_PAINT);
        canvas.setBitmap(null);

        return targetBitmap;
    }

    public static Bitmap roundedCorners(Bitmap inBitmap, int roundingRadius) {
        Bitmap targetBitmap = Bitmap.createBitmap(inBitmap.getWidth(), inBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        targetBitmap.setHasAlpha(true);

        BitmapShader shader = new BitmapShader(inBitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        RectF rect = new RectF(0, 0, targetBitmap.getWidth(), targetBitmap.getHeight());
        Canvas canvas = new Canvas(targetBitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawRoundRect(rect, roundingRadius, roundingRadius, paint);
        canvas.setBitmap(null);

        return targetBitmap;
    }

    public static Bitmap blurBitmap(Context context, Bitmap inBitmap, int width, int height) {
        int targetWidth = width, targetHeight = height;
        if (inBitmap.getWidth() != width || inBitmap.getHeight() != height) {
            final float widthPercentage = width / (float) inBitmap.getWidth();
            final float heightPercentage = height / (float) inBitmap.getHeight();
            final float minPercentage = Math.min(widthPercentage, heightPercentage);
            targetWidth = Math.round(minPercentage * inBitmap.getWidth());
            targetHeight = Math.round(minPercentage * inBitmap.getHeight());
        }
        return BlurBitmapUtil.instance().blurBitmap(context, inBitmap, 25, targetWidth, targetHeight);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        //注意，下面三行代码要用到，否则在View或者SurfaceView里的canvas.drawBitmap会看不到图
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);

        return bitmap;
    }

    public static byte[] compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
