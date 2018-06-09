package com.jianhua.netty.http.simple;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;


/**
 * @author lijianhua
 */
public class HttpSimpleServerInitializer extends ChannelInitializer<SocketChannel> {

    private SslContext sslContext;

    public HttpSimpleServerInitializer(SslContext sslCtx) {
        this.sslContext = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        ch.pipeline().addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
        if(sslContext!=null){
            ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
        }
        ch.pipeline().addLast(new ChannelDuplexHandler(){

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                ByteBuf buffer = (ByteBuf) msg;
                byte[] readmsg = new byte[buffer.readableBytes()];
                buffer.readBytes(readmsg);
                System.out.println("来自"+ ctx.channel().remoteAddress() + "的原始请求:"+ new String(readmsg, Charset.defaultCharset()));
                buffer.readerIndex(0);
                super.channelRead(ctx, msg);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                ByteBuf buffer = (ByteBuf) msg;
                byte[] writemsg = new byte[buffer.readableBytes()];
                buffer.readBytes(writemsg);
                System.out.println("来自"+ ctx.channel().remoteAddress() + "的响应:"+ new String(writemsg, Charset.defaultCharset()));
                buffer.readerIndex(0);
                super.write(ctx, msg, promise);
            }
        });
        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(65536));
        ch.pipeline().addLast(new HttpContentCompressor());
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new HttpSimpleServerHandler());

    }
}