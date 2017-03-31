package com.xiyuan.spider.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xiyuan_fengyu on 2017/2/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface OnTime {

    /**
     * 任务名字
     * @return
     */
    String name() default "";

    /**
     * quartz cron表达式, 默认为 每天中午12点 的cron表达式
     * cron表达式在线生成：http://cron.qqe2.com/
     * @return
     */
    String cron() default "0 0 12 * * ? ";

    /**
     * 要爬取的url
     * @return
     */
    String url();

    /**
     * 用于爬取网页信息的js路径（相对于classes目录的路径）
     * @return
     */
    String js();

    /**
     * 爬取任务超时时间，单位：秒
     * @return
     */
    int timeout() default 30;

}
