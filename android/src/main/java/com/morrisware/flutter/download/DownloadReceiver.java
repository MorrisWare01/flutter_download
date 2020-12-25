package com.morrisware.flutter.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;


/**
 * @author mmw
 */
public class DownloadReceiver extends BroadcastReceiver {

    public static final String KEY_FILE = "file";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String path = intent.getStringExtra(KEY_FILE);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    DownloadHelper.tryOpenFile(context, file);
                }
            }
        }
    }


}
