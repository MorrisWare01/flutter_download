package com.morrisware.flutter.download;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;

import static android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES;

/**
 * Created by mmw on 2019/8/28.
 */
public class DownloadHelper {

    private static Toast toast;

    public static void showToast(Context context, String message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * 安装App
     *
     * @param file
     */
    public static void installAPK(Context context, File file) {
        if (file == null || !file.exists() || !file.getName().endsWith(".apk")) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        int sdkVersion = context.getApplicationInfo().targetSdkVersion;
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && sdkVersion >= 24) {
            // 即是在清单文件中配置的authorities
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            uri = Uri.fromFile(file);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        // 在服务中开启activity必须设置flag,后面解释
        context.startActivity(intent);
    }

    public static void openApp(Context activity, String packagename) {
        try {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packagename);
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "没有安装", Toast.LENGTH_SHORT).show();
        }
    }

    private static String queryAuthority(Context context) {
        PackageInfo packageInfos = null;
        try {
            PackageManager mgr = context.getPackageManager();
            if (mgr != null) {
                packageInfos = mgr.getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);
            }
        } catch (PackageManager.NameNotFoundException e) {

        }
        if (packageInfos != null && packageInfos.providers != null) {
            for (ProviderInfo providerInfo : packageInfos.providers) {
                if (providerInfo.name.equals(DownloadFileProvider.class.getName())) {
                    return providerInfo.authority;
                }
            }
        }
        return null;
    }

    private static Uri getUriFromFile(Context context, File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = queryAuthority(context);
            if (TextUtils.isEmpty(authority)) {
                authority = context.getPackageName() + ".downloadFileProvider";
            }
            uri = FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    private static String getMIMEType(File f) {
        String type = "";
        String fName = f.getName();
        /* 取得扩展名 */
        String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();

        /* 依扩展名的类型决定MimeType */
        if (end.equals("pdf")) {
            type = "application/pdf";//
        } else if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") ||
                end.equals("xmf") || end.equals("ogg") || end.equals("wav")) {
            type = "audio/*";
        } else if (end.equals("3gp") || end.equals("mp4")) {
            type = "video/*";
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png") ||
                end.equals("jpeg") || end.equals("bmp")) {
            type = "image/*";
        } else if (end.equals("apk")) {
            type = "application/vnd.android.package-archive";
        } else if (end.equals("pptx") || end.equals("ppt")) {
            type = "application/vnd.ms-powerpoint";
        } else if (end.equals("docx") || end.equals("doc")) {
            type = "application/vnd.ms-word";
        } else if (end.equals("xlsx") || end.equals("xls")) {
            type = "application/vnd.ms-excel";
        } else {
            type = "*/*";
        }
        return type;
    }

    public static void tryOpenFile(Context context, File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = getMIMEType(file);
            if (Build.VERSION.SDK_INT >= 24) {
                intent.setDataAndType(getUriFromFile(context, file), mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(file), mimeType);
            }
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {

        }
    }

    public static boolean isHttp(String url) {
        return !TextUtils.isEmpty(url) && (url.contains("http://") || url.contains("https://"));
    }

    private static Handler mHandler = null;

    public static void runInUiThread(Runnable runnable) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        mHandler.post(runnable);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void startInstallPermissionSettingActivity(Activity context, int request) {
        Intent intent = new Intent(ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivityForResult(intent, request);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void startInstallPermissionSettingActivity(Context context) {
        Intent intent = new Intent(ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    public static void jumpBrowser(Context context, String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(Intent.createChooser(intent, "请选择浏览器"));
        } else {
            showToast(context, "没有匹配的程序");
        }
    }

}
