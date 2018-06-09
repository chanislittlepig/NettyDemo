package com.jianhua.netty.http.simple;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.nio.charset.Charset;

/**
 * @author lijianhua
 */
public class HttpSimpleServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if(stateEvent.state() ==  IdleState.READER_IDLE){
                System.out.println("客户端长时间没发起请求，即将断开连接["+ctx.channel().remoteAddress()+"]");
                ctx.close();
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("断开连接["+ctx.channel().remoteAddress()+"]");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("建立连接["+ctx.channel().remoteAddress()+"]");
        super.channelRegistered(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if(!request.decoderResult().isSuccess()){
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    Unpooled.wrappedBuffer("BAD REQUEST".getBytes(Charset.defaultCharset())), true);
            return;
        }
        if("/favicon.ico".equals(request.uri())){
            sendError(ctx, HttpResponseStatus.OK,
                    Unpooled.buffer(0), false);
            return;
        }
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("Hello".getBytes(Charset.defaultCharset())));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if(!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.writeAndFlush(response)
                    .addListener(ChannelFutureListener.CLOSE);
        }else{
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, ByteBuf content, boolean isClose) {
        DefaultFullHttpResponse response =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        if(isClose) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }else{
            ctx.writeAndFlush(response);
        }
    }

}