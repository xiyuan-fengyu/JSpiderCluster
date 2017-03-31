package com.xiyuan.spider.queue;

import com.xiyuan.spider.filter.Filter;

import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/3/6.
 */
public abstract class AbsQueue implements Queue {

    private static final long serialVersionUID = -5202121347176485608L;

    private HashMap<Class<? extends Filter>, Filter> filters = new HashMap<>();

    @Override
    public Filter getFilter(Class<? extends Filter> filterType) {
        return filters.get(filterType);
    }

    @Override
    public void setFilter(Class<? extends Filter> filterType, Filter filter) {
        filters.put(filterType, filter);
    }

}
