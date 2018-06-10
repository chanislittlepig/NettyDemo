package com.jianhua.netty.http.web;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * @author lijianhua
 */
public class HttpWebServer {

    public void startAt(int port, boolean ssl){
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup child = new NioEventLoopGroup();

        try{

            SslContext sslContext = null;
            if(ssl){
                SelfSignedCertificate certificate = new SelfSignedCertificate();
                sslContext = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey()).build();
            }

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, child)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpWebServerInitializeHandler(sslContext));

            bootstrap.bind(port).sync()
                    .channel().closeFuture().sync();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            child.shutdownGracefully();
        }


    }

    public static void main(String[] args) {
        new HttpWebServer().startAt(80, false);
    }

}