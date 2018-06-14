package com.jianhua.netty;

import com.jianhua.springmvc.Dispatcher;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;
import java.util.concurrent.TimeUnit;


/**
 * @author lijianhua
 */
public class HttpServerHandlerInitialize extends ChannelInitializer<SocketChannel> {

    private DispatcherServlet dispatcher = Dispatcher.getDispatcher();

    private SslContext sslContext;

    public HttpServerHandlerInitialize() throws ServletException {
    }

    public HttpServerHandlerInitialize(SslContext sslCtx) throws ServletException {
        this.sslContext = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new IdleStateHandler(30, 0 ,0 , TimeUnit.SECONDS));
        if(sslContext != null){
            ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
        }
        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024));
        ch.pipeline().addLast(new HttpContentCompressor());
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new HttpServerHandler(dispatcher));
    }

}