package com.morrisware.flutter.net.utils;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author mmw
 * @date 2020/3/20
 **/
public class FileUtils {

    public static String file2String(File file) {
        FileInputStream fis = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            fis = new FileInputStream(file);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) != -1) {
                baos.write(bytes, 0, length);
            }

            return new String(baos.toByteArray());
        } catch (Exception e) {
            //
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
                //
            }
        }
        return "";
    }

    public static void writeString(File file, String content) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
        } catch (Exception e) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {

            }
        }
    }

    public static String getExt(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }
        int dotIndex = filePath.lastIndexOf(".");
        if (dotIndex != -1) {
            return filePath.substring(dotIndex);
        }
        return "";
    }

    public static String getUrlName(final String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String temp = url;
        int index = temp.indexOf("?");
        if (index > -1) {
            temp = temp.substring(0, index);
        }
        temp = temp.substring(temp.lastIndexOf("/") + 1);
        if (temp.length() > 100) {
            temp = temp.substring(temp.length() - 100);
        }
        return temp;
    }


}
