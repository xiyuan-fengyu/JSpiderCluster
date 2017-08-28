package com.xiyuan.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/8/17.
 */
public class DateUtil {

    private static final HashMap<String, SimpleDateFormat> formats = new HashMap<>();

    public static final String fDefault = "yyyy-MM-dd HH:mm:ss";

    public static final String fNoDivider = "yyyyMMddHHmmss";

    public static final String fDay = "yyyy-MM-dd";

    public static final String fDayNoDivider = "yyyyMMdd";

    private static SimpleDateFormat getFormat(String formatStr) {
        SimpleDateFormat format = formats.get(formatStr);
        if (format == null) {
            format = new SimpleDateFormat(formatStr);
            formats.put(formatStr, format);
        }
        return format;
    }

    public static String format(Date date) {
        return format(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String format(Date date, String format) {
        return getFormat(format).format(date);
    }

    public static Date parse(String str) {
        return parse(str, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date parse(String str, String format) {
        try {
            return getFormat(format).parse(str);
        } catch (ParseException e) {
            return new Date();
        }
    }

}
