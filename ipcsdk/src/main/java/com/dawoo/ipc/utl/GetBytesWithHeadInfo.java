package com.dawoo.ipc.utl;

import android.util.Log;

/**
 * Created by archar
 * 报文封装
 */

public class GetBytesWithHeadInfo {

    public static int HEADLENGTH = 10;

    public static byte[] getByteArry(String content) {

        String head = null;
        int contentByteLength = content.getBytes().length;
        head = contentByteLength + "";
        Log.e("lyn_getBytes", contentByteLength + "");
        if (getNumber(contentByteLength) < HEADLENGTH) {
            int tee = HEADLENGTH - getNumber(contentByteLength);
            for (int i = 0; i < tee; i++) {
                head += " ";//用空格补到10位
            }
        }
        int allByteLength = (head + content).getBytes().length;
        Log.e("lyn_getBytes", allByteLength + "");
        return (head + content).getBytes();
    }

    public static int getNumber(int n) {
        int count = 0;
        while (Math.abs(n) % 10 > 0 || n / 10 != 0) {
            count++;
            n = n / 10;
        }
        return count;
    }
}
