package com.morrisware.flutter.download;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.morrisware.flutter.net.FileDownloader;

import java.io.File;

public class DownloadService extends IntentService {
    private static final String ACTION_DOWNLOAD = "com.xiqu.box.action.download";
    private static final String EXTRA_URL = "com.xiqu.box.extra.url";
    private static final String EXTRA_PACKAGE_NAME = "com.xiqu.box.extra.package_name";

    private static OnDownloadListener onDownloadListener = null;

    public static void setOnDownloadListener(OnDownloadListener onDownloadListener) {
        DownloadService.onDownloadListener = onDownloadListener;
    }

    public DownloadService() {
        super("DownloadService");
    }

    public static Intent getDownloadIntent(Context context, String packageName, String apkUrl) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(EXTRA_URL, apkUrl);
        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadBinder();
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        DownloadHelper.runInUiThread(new Runnable() {
            @Override
            public void run() {
                if (intent != null) {
                    final String action = intent.getAction();
                    if (ACTION_DOWNLOAD.equals(action)) {
                        final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                        final String apkUrl = intent.getStringExtra(EXTRA_URL);
                        if (DownloadHelper.isHttp(apkUrl)) {
                            boolean isNewDownload = FileDownloader.downloadUrl(getApplicationContext(), apkUrl, packageName, null);
                            if (onDownloadListener != null) {
                                onDownloadListener.onStartDownload(packageName, apkUrl, isNewDownload);
                                onDownloadListener = null;
                            }
                        } else {
                            DownloadHelper.showToast(DownloadService.this, "下载地址异常");
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onUnbind(Intent intent) {
        onDownloadListener = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        onDownloadListener = null;
        super.onDestroy();
    }

    public int[] checkDownloadApkProgress(String packageName, String apkUrl) {
        return FileDownloader.getDownloadStatus(getApplicationContext(), apkUrl, packageName);
    }

    public void cancel(String packageName, String apkUrl) {
        FileDownloader.cancelDownload(getApplicationContext(), apkUrl, packageName);
    }

    public File getDownloadFile(String packageName, String apkUrl) {
        return FileDownloader.getDownloadFile(getApplicationContext(), apkUrl, packageName);
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    public interface OnDownloadListener {
        void onStartDownload(String packageName, String apkUrl, boolean isNewDownload);
    }

}
