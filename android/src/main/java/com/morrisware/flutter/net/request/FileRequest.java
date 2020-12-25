package com.morrisware.flutter.net.request;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.morrisware.flutter.net.core.CancelError;
import com.morrisware.flutter.net.core.HttpError;
import com.morrisware.flutter.net.core.HttpResponse;
import com.morrisware.flutter.net.core.HttpResult;
import com.morrisware.flutter.net.core.Request;
import com.morrisware.flutter.net.core.Response;
import com.morrisware.flutter.net.utils.FileUtils;
import com.morrisware.flutter.net.utils.Md5Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author mmw
 * @date 2020/3/20
 **/
public class FileRequest extends Request<File> {
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    @Nullable
    private Response.ResponseCallback<File> responseCallback;
    private String fileRoot;
    private String tag;

    // 目标文件
    private File file;
    // 目标临时文件
    private File tempFile;

    // 原始目标文件描述
    private File originInfoFile;
    private String originInfoKey;

    // 目标文件描述，可能为重定向文件
    private File infoFile;
    private String infoKey;
    private long contentLength;

    private long lastProgressTime;

    private String redirectUrl;

    public FileRequest(String url, String fileRoot, String tag, @Nullable Response.ResponseCallback<File> responseCallback) {
        super(METHOD_GET, url, responseCallback);
        this.fileRoot = fileRoot;
        this.responseCallback = responseCallback;
        this.tag = tag;

        String fileName = FileUtils.getUrlName(url);
        String ext = FileUtils.getExt(fileName);
        originInfoFile = new File(fileRoot, fileName.replaceAll(ext, "") + ".info");
        originInfoKey = Md5Utils.md5(getUrl() + "," + tag);
    }

    private void refreshInfoFile() {
        FileUtils.writeString(infoFile, infoKey + "," + contentLength);
        if (redirectUrl != null) {
            FileUtils.writeString(originInfoFile, originInfoKey + "," + 0 + "," + redirectUrl);
        }
    }

    private String getInfoKey(File file) {
        String data = FileUtils.file2String(file);
        return data.split(",")[0];
    }

    private void initDownloadFile() {
        // 取出缓存信息
        try {
            infoKey = Md5Utils.md5(getUrl() + "," + tag);
            String filePath = new File(fileRoot, FileUtils.getUrlName(getUrl())).getAbsolutePath();
            file = new File(filePath);
            tempFile = new File(filePath + ".tmp");
            final String ext = FileUtils.getExt(filePath);
            final String fileName = file.getName().replaceAll(ext, "");
            infoFile = new File(file.getParent(), fileName + ".info");

            File parentFile = file.getParentFile();
            if (parentFile != null) {
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                // 解析信息
                if (infoFile.exists()) {
                    String itemInfoKey = getInfoKey(infoFile);
                    if (!infoKey.equals(itemInfoKey)) {
                        int index = 0;
                        for (File itemFile : parentFile.listFiles()) {
                            String itemName = itemFile.getName();
                            if (itemName.startsWith(fileName)) {
                                if (itemName.endsWith(".info")) {
                                    index++;
                                    itemInfoKey = getInfoKey(itemFile);
                                    if (infoKey.equals(itemInfoKey)) {
                                        String newName = itemName.substring(0, itemName.lastIndexOf(".info"));
                                        file = new File(parentFile, newName + ext);
                                        tempFile = new File(parentFile, file.getName() + ".temp");
                                        infoFile = new File(parentFile, newName + ".info");
                                        break;
                                    }
                                }
                            }
                        }
                        if (!infoKey.equals(itemInfoKey)) {
                            String newFileName = fileName + "$" + index;
                            file = new File(parentFile, newFileName + ext);
                            tempFile = new File(parentFile, file.getName() + ".temp");
                            infoFile = new File(parentFile, newFileName + ".info");
                            refreshInfoFile();
                        }
                    }
                } else {
                    file.delete();
                    tempFile.delete();
                    refreshInfoFile();
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public int getTimeoutMs() {
        return 30000;
    }

    @Override
    public int getMaxRetryCount() {
        return 1;
    }

    @Override
    public void redirectRequest(String url) {
        this.redirectUrl = url;
        refreshInfoFile();
        super.redirectRequest(url);
    }

    @Override
    public Map<String, String> getHeaders() {
        initDownloadFile();
        // range
        Map<String, String> map = new HashMap<>();
        map.put("Range", "bytes=" + this.tempFile.length() + "-");
        return map;
    }

    @Override
    protected boolean onInterceptHttpResponse() {
        return true;
    }

    @Override
    protected byte[] parseHttpResponse(HttpResponse httpResponse) throws IOException {
        long contentLength = httpResponse.getContentLength();

        long length = tempFile.length();
        boolean isSupportRange = isSupportRange(httpResponse);
        if (isSupportRange) {
            contentLength += length;
            // 判断文件是否发生变化，或请求头出错
            String contentRange = httpResponse.getHeader("Content-Range");
            if (!TextUtils.isEmpty(contentRange)) {
                String range = "bytes " + length + "-" + (contentLength - 1);
                if (!contentRange.contains(range)) {
                    throw new HttpError("the resource maybe changed, Content-Range: response=" + contentRange + " real=" + range);
                }
            }
        }

        // 刷新长度
        this.contentLength = contentLength;
        refreshInfoFile();

        // 判断是否已经下载完成了
        if (contentLength > 0 && contentLength == file.length()) {
            file.renameTo(tempFile);
            deliverProgress(contentLength, contentLength);
            return null;
        } else {
            if (file.exists()) {
                file.delete();
            }

            if (contentLength == 0) {
                throw new HttpError("the resource contentLength is zero");
            }

            RandomAccessFile randomAccessFile = null;
            InputStream content = null;
            try {
                randomAccessFile = new RandomAccessFile(tempFile, "rw");
                if (isSupportRange) {
                    randomAccessFile.seek(length);
                } else {
                    length = 0;
                    randomAccessFile.seek(0);
                }

                content = httpResponse.getContent();
                if (isGzipContent(httpResponse) && !(content instanceof GZIPInputStream)) {
                    content = new GZIPInputStream(content);
                }

                byte[] buff = new byte[2048];
                deliverProgress(length, contentLength);

                int size;
                while ((size = content.read(buff)) != -1) {
                    randomAccessFile.write(buff, 0, size);
                    length += size;
                    deliverProgress(length, contentLength);
                    if (isCanceled()) {
                        break;
                    }
                }
            } finally {
                try {
                    if (content != null) {
                        content.close();
                    }
                } catch (Exception e) {

                }

                try {
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                } catch (Exception e) {

                }
            }
        }
        return null;
    }

    private boolean isSupportRange(HttpResponse httpResponse) {
        if ("bytes".equals(httpResponse.getHeader("Accept-Range"))) {
            return true;
        } else {
            String contentRange = httpResponse.getHeader("Content-Range");
            return contentRange != null && contentRange.startsWith("bytes");
        }
    }

    private boolean isGzipContent(HttpResponse httpResponse) {
        return "gzip".equals(httpResponse.getHeader("Content=Encoding"));
    }

    @Override
    public Response parse(HttpResult httpResult) {
        if (isCanceled()) {
            return Response.error(new CancelError());
        } else {
            if (this.tempFile.canRead() && this.tempFile.length() > 0 && this.tempFile.length() == contentLength) {
                if (tempFile.renameTo(file)) {
                    return Response.success(file);
                } else {
                    tempFile.delete();
                    file.delete();
                    return Response.error(new HttpError("download file error"));
                }
            } else {
                tempFile.delete();
                file.delete();
                return Response.error(new HttpError("download file error"));
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (this.mLock) {
            this.responseCallback = null;
        }
    }

    @Override
    protected void deliverError(Response<File> response) {
        super.deliverError(response);
        if (response.getError() instanceof HttpError) {
            tempFile.delete();
            file.delete();
        }
    }

    @Override
    protected void deliverContent(Response<File> response) {
        Response.ResponseCallback<File> responseCallback;
        synchronized (this.mLock) {
            responseCallback = this.responseCallback;
        }

        if (responseCallback != null) {
            responseCallback.onSuccess(response);
        }
    }

    protected void deliverProgress(final long length, final long contentLength) {
        final Response.ResponseCallback<File> responseCallback;
        synchronized (this.mLock) {
            responseCallback = this.responseCallback;
        }

        if (responseCallback instanceof FileResponseCallback) {
            final FileResponseCallback callback = (FileResponseCallback) responseCallback;

            long progressInterval = callback.progressInterval;
            if (SystemClock.elapsedRealtime() - lastProgressTime > progressInterval) {
                lastProgressTime = SystemClock.elapsedRealtime();
                getRequestLooper().getResponseManager().getExecutor(this).execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileDownloadProgress(length, contentLength);
                    }
                });
            }
        }
    }

    public abstract static class FileResponseCallback implements Response.ResponseCallback<File> {

        private long progressInterval;

        public FileResponseCallback() {
            this(100);
        }

        public FileResponseCallback(long progressInterval) {
            this.progressInterval = progressInterval;
        }

        /**
         * 下载进度
         *
         * @param length        已下载大小
         * @param contentLength 全部大小
         */
        public abstract void onFileDownloadProgress(long length, long contentLength);
    }

}
