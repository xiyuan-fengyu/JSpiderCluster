package com.xiyuan.webui.http;

import com.xiyuan.common.util.FileUtil;
import com.xiyuan.config.ClusterCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Created by xiyuan_fengyu on 2017/3/9.
 */
public class HttpRequestHandler {

    public static void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (HttpHeaderUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        URI uri;
        try {
            uri = URI.create(request.uri());
        }
        catch (Exception e) {
            e.printStackTrace();
            uri = null;
        }

        if (uri == null) {
            response404(ctx);
        }
        else {
            String path = uri.getPath();
            if ("/".equals(path)) {
                responseStaticFile(ctx, "/index.html");
            }
            else if (isStaticFileRequest(uri)) {
                responseStaticFile(ctx, path);
            }
            else {
                response404(ctx);
            }
        }

        if (HttpHeaderUtil.isKeepAlive(request)) {
            ctx.newSucceededFuture().addListener(ChannelFutureListener.CLOSE);
        }
        else {
            ctx.close();
        }
    }

    private static boolean isStaticFileRequest(URI uri) {
        return uri.getPath().matches("^.*\\.(html|css|js|json|jpg|png|bmp|jpeg|gif|ico)$");
    }

    private static void response(ChannelHandlerContext ctx, String contentStr) {
        ByteBuf content = Unpooled.copiedBuffer(contentStr.getBytes(StandardCharsets.UTF_8));
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        ctx.writeAndFlush(response);
    }

    private static void responseStaticFile(ChannelHandlerContext ctx, String path) {
        byte[] bytes = HttpStaticFile.get(path);
        if (bytes == null) {
            response404(ctx);
        }
        else {
            ByteBuf content = Unpooled.copiedBuffer(bytes);
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, MimeType.mimeType(FileUtil.getSubffix(path)));
            ctx.writeAndFlush(response);
        }
    }

    private static void response404(ChannelHandlerContext ctx) {
        byte[] bytes = HttpStaticFile.get("/page/404.html");
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, content);
        ctx.writeAndFlush(response);
    }

}
