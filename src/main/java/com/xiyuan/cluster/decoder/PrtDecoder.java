package com.xiyuan.cluster.decoder;

import com.xiyuan.cluster.msg.Messages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/3/6.
 */
public class PrtDecoder extends MessageToMessageDecoder<Messages.Prt> {

    @Override
    protected void decode(ChannelHandlerContext ctx, Messages.Prt msg, List<Object> out) throws Exception {
        for (Object value : msg.getAllFields().values()) {
            out.add(value);
        }
    }

}
