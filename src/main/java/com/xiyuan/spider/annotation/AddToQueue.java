package com.xiyuan.spider.annotation;

import com.xiyuan.spider.filter.Filter;
import com.xiyuan.spider.filter.NoFilter;
import com.xiyuan.spider.queue.DefaultQueue;
import com.xiyuan.spider.queue.Queue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xiyuan_fengyu on 2017/2/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AddToQueue {

    Class<? extends Queue> type() default DefaultQueue.class;

    String name() default "";

    Class<? extends Filter> filter() default NoFilter.class;

}
