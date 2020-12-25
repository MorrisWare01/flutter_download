package com.morrisware.flutter.net.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author mmw
 * @date 2020/3/11
 **/
public class Md5Utils {

    public static String md5(String string) {
        if (string == null || string.length() <= 0) {
            return "";
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
