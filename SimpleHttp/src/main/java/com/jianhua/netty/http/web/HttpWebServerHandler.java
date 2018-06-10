package com.jianhua.netty.http.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * @author lijianhua
 */
public class HttpWebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private String resourceRoot = null;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        if (request.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String originUri = request.uri();
        String uri = sanitizeUri(originUri);

        if (uri == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        File file = new File(uri);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }


        if (file.isDirectory()) {
            if (originUri.endsWith("/")) {
                sendListing(ctx, file, originUri);
            } else {
                sendRedirect(ctx, request.uri() + '/');
            }
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        RandomAccessFile accessFile = null;
        try {
            accessFile = new RandomAccessFile(uri, "r");
        } catch (Exception e) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, accessFile.length());
        MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        String contentType = mimetypesFileTypeMap.getContentType(file);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE,
                contentType);
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        ctx.write(response);

        ChannelFuture lastContentFuture = null;
        ChannelFuture sendFileFuture = null;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture = ctx.write(new DefaultFileRegion(accessFile.getChannel(), 0, accessFile.length()), ctx.newProgressivePromise());
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            lastContentFuture = ctx.writeAndFlush(new ChunkedFile(file), ctx.newProgressivePromise());
            sendFileFuture = lastContentFuture;
        }

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                System.out.println(future.channel() + " Transfer Complete! ");
            }

            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                if (total < 0) {
                    System.out.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.out.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }
        });
        if (!HttpUtil.isKeepAlive(request)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri) {
        // Decode the path.
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
                INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        // Convert to absolute path.
        return resourceRoot + uri;
    }

    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");

    private static void sendListing(ChannelHandlerContext ctx, File dir, String dirPath) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuilder buf = new StringBuilder()
                .append("<!DOCTYPE html>\r\n")
                .append("<html><head><meta charset='utf-8' /><title>")
                .append("Listing of: ")
                .append(dirPath)
                .append("</title></head><body>\r\n")

                .append("<h3>Listing of: ")
                .append(dirPath)
                .append("</h3>\r\n")

                .append("<ul>")
                .append("<li><a href=\"../\">..</a></li>\r\n");

        for (File f : dir.listFiles()) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }

            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            buf.append("<li><a href=\"")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a></li>\r\n");
        }

        buf.append("</ul></body></html>\r\n");
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_REQUEST);
        ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接[" + channelName(ctx) + "进入");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接[" + channelName(ctx) + "断开");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        try {
            if (resourceRoot == null) {
                resourceRoot = Thread.currentThread()
                        .getContextClassLoader().getResource(".").toURI().getPath();
                if (resourceRoot == null) {
                    resourceRoot = HttpWebServerHandler.class.getResource("/").toURI().getPath();
                }

                if (resourceRoot == null) {
                    resourceRoot = System.getProperty("user.dir");
                }

                if (resourceRoot != null && !resourceRoot.endsWith("/")) {
                    resourceRoot = resourceRoot + "/";
                }

                System.out.println("resourceRoot:" + resourceRoot);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("因长时间没收到来自客户端" + channelName(ctx) + "的请求，故关闭此连接");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public String channelName(ChannelHandlerContext ctx) {
        return ctx.channel().remoteAddress().toString();
    }

}