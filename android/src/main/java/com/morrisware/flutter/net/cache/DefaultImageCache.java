package com.morrisware.flutter.net.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mmw
 * @date 2020/3/11
 **/
public class DefaultImageCache {
    private static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    private LruCache<String, Bitmap> lruMemoryCache;
    private DiskLruCache diskLruCache;

    public DefaultImageCache(File diskFile) {
        int maxMemory = Long.valueOf(Runtime.getRuntime().maxMemory()).intValue();
        lruMemoryCache = new LruCache<String, Bitmap>(maxMemory / 16) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        try {
            diskLruCache = DiskLruCache.open(diskFile, APP_VERSION, VALUE_COUNT, DEFAULT_DISK_CACHE_SIZE);
        } catch (IOException e) {
        }
    }

    public Bitmap getBitmap(String key) {
        Bitmap bitmap = lruMemoryCache.get(key);
        if (bitmap != null) {
            return bitmap;
        }
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskLruCache.get(key);
            if (snapshot != null) {
                InputStream inputStream = snapshot.getInputStream(0);
                if (inputStream instanceof FileInputStream) {
                    FileInputStream fis = (FileInputStream) inputStream;
                    FileDescriptor fileDescriptor = fis.getFD();
                    Bitmap diskBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, (Rect) null, (BitmapFactory.Options) null);
                    if (diskBitmap != null) {
                        return diskBitmap;
                    }
                }
            }
        } catch (IOException e) {

        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return null;
    }

    public void putBitmap(String key, Bitmap bitmap) {
        lruMemoryCache.put(key, bitmap);
        OutputStream outputStream = null;
        try {
            DiskLruCache.Editor editor = diskLruCache.edit(key);
            outputStream = editor.newOutputStream(0);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            editor.commit();
        } catch (Exception e) {

        } finally {
            Util.closeQuietly(outputStream);
        }
    }

}
