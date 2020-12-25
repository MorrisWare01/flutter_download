package com.morrisware.flutter.net.engine;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.morrisware.flutter.download.DownloadReceiver;
import com.morrisware.flutter.net.FileDownloader;
import com.morrisware.flutter.net.core.RequestLooper;
import com.morrisware.flutter.net.core.Response;
import com.morrisware.flutter.net.request.FileRequest;
import com.morrisware.flutter.net.utils.FileUtils;
import com.morrisware.flutter.net.utils.Md5Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mmw
 * @date 2020/3/20
 **/
public class FileDownloaderEngine {

    private static final String DOWNLOAD_FOLDER = "download";

    private RequestLooper requestLooper;
    private Map<String, FileDownloaderJob> runningMap = new HashMap<>();
    private File downloadParentFile;

    private Context context;
    private Map<String, DownloadItem> downloadItemMap = new HashMap<>();

    public FileDownloaderEngine(Context context, RequestLooper requestLooper) {
        this.context = context;
        this.requestLooper = requestLooper;
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, DOWNLOAD_FOLDER);
        if (externalFilesDirs.length > 0) {
            this.downloadParentFile = externalFilesDirs[0];
        }
        if (this.downloadParentFile == null) {
            this.downloadParentFile = new File(context.getFilesDir(), DOWNLOAD_FOLDER);
        }
    }

    /**
     * @return 是否创建新下载
     */
    public boolean downloadUrl(final String url, final String tag, FileRequest.FileResponseCallback callback) {
        FileDownloaderJob fileDownloaderJob = runningMap.get(getKey(url, tag));
        if (fileDownloaderJob != null) {
            if (callback != null) {
                fileDownloaderJob.callbacks.add(callback);
            }
            return false;
        } else {
            // 下载监听
            final DownloadItem downloadItem = new DownloadItem();
            downloadItem.tag = tag;
            downloadItem.url = url;
            downloadItem.status = FileDownloader.STATUS_PENDING;
            downloadItemMap.put(getKey(url, tag), downloadItem);
            final FileDownloaderJob job = new FileDownloaderJob();
            FileRequest fileRequest = new FileRequest(
                    url,
                    downloadParentFile.getAbsolutePath(),
                    tag,
                    new FileRequest.FileResponseCallback() {
                        @Override
                        public void onFileDownloadProgress(long length, long contentLength) {
                            downloadItem.status = FileDownloader.STATUS_RUNNING;
                            downloadItem.progress = (int) (length * 100 / contentLength);
                            onDownloadProgress(job, length, contentLength);
                        }

                        @Override
                        public void onSuccess(Response<File> response) {
                            downloadItem.status = FileDownloader.STATUS_SUCCESSFUL;
                            downloadItem.progress = 100;
                            downloadItem.downloadFile = response.getData();
                            onDownloadSuccess(url, tag, response);
                        }

                        @Override
                        public void onFailure(Response<File> response) {
                            downloadItem.status = FileDownloader.STATUS_FAILED;
                            downloadItem.progress = 0;
                            onDownloadFailure(url, tag, response);
                        }
                    });
            if (callback != null) {
                job.callbacks.add(callback);
            }
            job.fileRequest = fileRequest;
            // request
            runningMap.put(getKey(url, tag), job);
            // 发起请求
            requestLooper.sendRequest(fileRequest);
            return true;
        }
    }

    public void cancel(String url, String tag) {
        FileDownloaderJob fileDownloaderJob = runningMap.remove(getKey(url, tag));
        if (fileDownloaderJob != null) {
            fileDownloaderJob.fileRequest.cancel();
        }
    }

    private String getKey(String url, String tag) {
        return url + "-" + tag;
    }

    private void onDownloadProgress(FileDownloaderJob job, long length, long contentLength) {
        List<FileRequest.FileResponseCallback> callbacks = job.callbacks;
        for (FileRequest.FileResponseCallback callback : callbacks) {
            callback.onFileDownloadProgress(length, contentLength);
        }
    }

    private void onDownloadSuccess(String url, String tag, Response<File> response) {
        FileDownloaderJob job = runningMap.remove(getKey(url, tag));
        if (job != null) {
            List<FileRequest.FileResponseCallback> callbacks = job.callbacks;
            for (FileRequest.FileResponseCallback callback : callbacks) {
                callback.onSuccess(response);
            }
        }
        // 发送安装请求
        Intent downloadSuccessReceiver = new Intent(context, DownloadReceiver.class);
        downloadSuccessReceiver.setAction("android.intent.action.DOWNLOAD_COMPLETE");
        downloadSuccessReceiver.putExtra("file", response.getData().getAbsolutePath());
        context.sendBroadcast(downloadSuccessReceiver);
    }

    private void onDownloadFailure(String url, String tag, Response<File> response) {
        FileDownloaderJob job = runningMap.remove(getKey(url, tag));
        if (job != null) {
            List<FileRequest.FileResponseCallback> callbacks = job.callbacks;
            for (FileRequest.FileResponseCallback callback : callbacks) {
                callback.onFailure(response);
            }
        }
    }

    public File getDownloadFile(String url, String tag) {
        String infoKey = Md5Utils.md5(url + "," + tag);

        String fileName = FileUtils.getUrlName(url);
        String ext = FileUtils.getExt(fileName);
        fileName = fileName.replaceAll(ext, "");
        File[] files = downloadParentFile.listFiles();
        if (files == null) {
            return null;
        }
        for (File itemFile : files) {
            String itemName = itemFile.getName();
            if (itemName.startsWith(fileName)) {
                if (itemName.endsWith(".info")) {
                    String[] data = FileUtils.file2String(itemFile).split(",");
                    String key = null;
                    String redirectUrl = null;
                    if (data.length > 0) {
                        key = data[0];
                    }
                    if (data.length > 2) {
                        redirectUrl = data[2];
                    }
                    if (infoKey.equals(key)) {
                        if (!TextUtils.isEmpty(redirectUrl)) {
                            return getDownloadFile(redirectUrl, tag);
                        } else {
                            File file = new File(downloadParentFile, itemName.replaceAll(".info", ext));
                            if (file.exists()) {
                                return file;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public int[] checkDownloadStatus(String apkUrl, String tag) {
        DownloadItem downloadItem = downloadItemMap.get(getKey(apkUrl, tag));
        if (downloadItem != null) {
            if (downloadItem.status == FileDownloader.STATUS_SUCCESSFUL) {
                if (downloadItem.downloadFile != null && downloadItem.downloadFile.exists()) {
                    return new int[]{FileDownloader.STATUS_SUCCESSFUL, 100};
                } else {
                    return new int[]{-1, -1};
                }
            }
            return new int[]{downloadItem.status, downloadItem.progress};
        } else {
            return new int[]{-1, -1};
        }
    }

    public void clearCache(long interval) {
        try {
            File dir = downloadParentFile;
            if (dir != null) {
                File[] files = dir.listFiles();
                if (files != null) {
                    long currentTime = System.currentTimeMillis();
                    for (File file : files) {
                        if (file.isFile() && currentTime - file.lastModified() > interval) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    private class FileDownloaderJob {
        private FileRequest fileRequest;
        private List<FileRequest.FileResponseCallback> callbacks = Collections.synchronizedList(new ArrayList<FileRequest.FileResponseCallback>());
    }

    private static class DownloadItem {
        private String tag;
        private String url;
        private int status;
        private int progress;
        private File downloadFile;
    }

}
