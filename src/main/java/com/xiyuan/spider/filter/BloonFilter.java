package com.xiyuan.spider.filter;

import com.xiyuan.spider.message.Message;

import java.util.BitSet;

/**
 * Created by xiyuan_fengyu on 2017/2/21.
 */
public class BloonFilter implements Filter {

    private static final long serialVersionUID = -4154823599316858311L;

    private final int defaultSize = 2 << 24;

    private final BitSet bits = new BitSet(defaultSize);

    private final int[] seeds = {7, 11, 13, 31, 37, 61};

    protected String key(Message msg) {
        return msg.url();
    }

    @Override
    public void setExisted(Message msg) {
        String url = key(msg);
        for (int seed: seeds) {
            bits.set(hash(url, defaultSize, seed));
        }
    }

    @Override
    public boolean isExisted(Message msg) {
        String url = key(msg);
        if (url == null) return false;

        for (int seed: seeds) {
            if (!bits.get(hash(url, defaultSize, seed))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        bits.clear();
    }

    private int hash(String str, int cap, int seed) {
        if (str == null) return 0;

        int result = 0;
        for (int i = 0, len = str.length(); i < len; i++) {
            result = seed * result + str.charAt(i);
        }
        return (cap - 1) & result;
    }

}
