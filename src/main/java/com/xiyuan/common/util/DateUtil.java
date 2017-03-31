package com.xiyuan.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xiyuan_fengyu on 2017/3/9.
 */
public class DateUtil {

    public static String formatDate(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(date);
        }
        catch (Exception e) {
            return "";
        }
    }

    public static Date parse(String strDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.parse(strDate);
        }
        catch (Exception e) {
            return new Date();
        }
    }

}
