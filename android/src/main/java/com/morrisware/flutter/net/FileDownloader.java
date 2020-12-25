package com.morrisware.flutter.net;

import android.content.Context;

import com.morrisware.flutter.net.request.FileRequest;

import java.io.File;

/**
 * @author mmw
 * @date 2020/3/20
 **/
public class FileDownloader {

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_RUNNING = 1 << 1;
    public static final int STATUS_SUCCESSFUL = 1 << 3;
    public static final int STATUS_FAILED = 1 << 4;

    public static final String NO_TAG = "";

    public static boolean downloadUrl(Context context, String url, FileRequest.FileResponseCallback callback) {
        return downloadUrl(context, url, "", callback);
    }

    public static boolean downloadUrl(Context context, String url, String tag, FileRequest.FileResponseCallback callback) {
        return HttpManager.getInstance(context).getFileDownloaderEngine().downloadUrl(url, tag, callback);
    }

    public static void cancelDownload(Context context, String url, String tag) {
        HttpManager.getInstance(context).getFileDownloaderEngine().cancel(url, tag);
    }

    public static File getDownloadFile(Context context, String url, String tag) {
        return HttpManager.getInstance(context).getFileDownloaderEngine().getDownloadFile(url, tag);
    }

    public static int[] getDownloadStatus(Context context, String url, String tag) {
        return HttpManager.getInstance(context).getFileDownloaderEngine().checkDownloadStatus(url, tag);
    }

    public static void clearCache(Context context, long interval) {
        HttpManager.getInstance(context).getFileDownloaderEngine().clearCache(interval);
    }

}
