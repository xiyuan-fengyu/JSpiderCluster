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
public @interface OnMessage {

    /**
     * 任务名字
     * @return
     */
    String name() default "";

    /**
     * 从哪个队列获取任务信息
     * @return
     */
    String fromQueue() default "";

    /**
     * 用于爬取网页信息的js路径（相对于classes目录的路径）
     * @return
     */
    String js();

    /**
     * 任务最大并行数
     * @return
     */
    int parallel() default 5;

    /**
     * 通过cron来动态设置并行数的配置，格式为json字符串，key为 quartz cron 表达式，value为 并行数
     * 例如：
     * {'0 0 6 * * ? ': 10, '0 0 12 * * ? ': 20, '0 0 0 * * ? ': 50}
     * 上面的配置表示：每天早上6点把并行数设置为10，每天中午12点把并行数设置为20，每天凌晨0点把并行数设置为50
     *
     * {'0/20 * * * * ? ': 5, '10/20 * * * * ? ': 10}
     * 上面的配置表示：每隔10秒钟，并行数会在5和10之间轮换
     *
     * 如果多个 cron 表达式在某一个时间同时生效，则会使用其中最小的并行数
     * cron表达式在线生成：http://cron.qqe2.com/
     * @return
     */
    String parallelConfig() default "";

    /**
     * 爬取任务超时时间，单位：秒
     * @return
     */
    int timeout() default 30;

}
