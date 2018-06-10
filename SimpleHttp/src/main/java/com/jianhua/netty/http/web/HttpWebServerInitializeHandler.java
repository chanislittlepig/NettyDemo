package com.jianhua.netty.http.file;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


/**
 * @author lijianhua
 */
public class HttpFileServerInitializeHandler extends ChannelInitializer<SocketChannel> {

    private SslContext sslCtx = null;

    public HttpFileServerInitializeHandler(SslContext sslContext) {
        this.sslCtx = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        if(sslCtx!=null){
            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
        }
        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(60 * 65536));
      //  ch.pipeline().addLast(new HttpContentCompressor());
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new HttpFileServerHandler());
    }
}