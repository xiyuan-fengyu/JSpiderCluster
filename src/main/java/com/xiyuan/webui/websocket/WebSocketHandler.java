package com.xiyuan.webui.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;

import java.util.HashMap;

/**
 * Created by xiyuan_fengyu on 2017/3/9.
 */
public class WebSocketHandler {

    public static boolean hasClients() {
        return handshakers.size() > 0;
    }

    public static void sendDataToWebSocketClients(JsonObject msg) {
        if (msg == null) return;
        if (handshakers.size() > 0) {
            JsonObject wrappedMsg = new JsonObject();
            wrappedMsg.addProperty("key", "clusterInfo");
            wrappedMsg.add("value", msg);
            String str = wrappedMsg.toString();
            for (ChannelHandlerContext client : handshakers.keySet()) {
                try {
                    TextWebSocketFrame frame = new TextWebSocketFrame(str);
                    client.writeAndFlush(frame);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final HashMap<ChannelHandlerContext, WebSocketServerHandshaker> handshakers = new HashMap<>();

    public static void handshake(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(request.uri(), null, false);
            WebSocketServerHandshaker handshaker = factory.newHandshaker(request);
            handshaker.handshake(ctx.channel(), request);
            handshakers.put(ctx, handshaker);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final JsonParser jsonParser = new JsonParser();

    public static void handler(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            if (handshakers.containsKey(ctx)) {
                handshakers.get(ctx).close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
                handshakers.remove(ctx);
            }
        }
        else if(frame instanceof PingWebSocketFrame || frame instanceof PongWebSocketFrame)
        {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }
        else if(frame instanceof TextWebSocketFrame)
        {
            String msgStr = ((TextWebSocketFrame)frame).text();
            try {
                JsonObject msg = jsonParser.parse(msgStr).getAsJsonObject();
                UserRequestHandler.dispatchRequest(ctx, msg);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            throw new UnsupportedOperationException(String.format("%s types not supported!", frame.getClass().getName()));
        }

    }

}
