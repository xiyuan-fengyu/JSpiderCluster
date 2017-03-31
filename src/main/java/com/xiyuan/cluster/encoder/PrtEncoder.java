package com.xiyuan.cluster.encoder;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.xiyuan.cluster.msg.Messages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.HashMap;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/3/6.
 */
public class PrtEncoder extends MessageToMessageEncoder<Object> {

    private static final HashMap<Descriptors.Descriptor, Descriptors.FieldDescriptor> fieldMap = new HashMap<>();

    static {
        for (Descriptors.FieldDescriptor fieldDescriptor : Messages.Prt.getDescriptor().getFields()) {
            fieldMap.put(fieldDescriptor.getMessageType(), fieldDescriptor);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object objs, List<Object> out) throws Exception {
        Messages.Prt.Builder builder = Messages.Prt.newBuilder();
        if (objs instanceof GeneratedMessage) {
            GeneratedMessage msg = (GeneratedMessage) objs;
            builder.setField(fieldMap.get(msg.getDescriptorForType()), msg);
        }
        else if (objs instanceof List) {
            List list = (List) objs;
            for (Object obj : list) {
                GeneratedMessage msg = (GeneratedMessage) obj;
                Descriptors.FieldDescriptor fieldDescriptor = fieldMap.get(msg.getDescriptorForType());
                if (fieldDescriptor.isRepeated()) {
                    builder.addRepeatedField(fieldDescriptor, msg);
                }
                else {
                    builder.setField(fieldDescriptor, msg);
                }
            }
        }
        out.add(builder.build());
    }

}
