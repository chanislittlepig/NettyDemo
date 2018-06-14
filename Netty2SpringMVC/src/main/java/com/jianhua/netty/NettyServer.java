package com.jianhua.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lijianhua
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    public void start() throws Exception {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup childs = new NioEventLoopGroup();

        try{

            SslContext sslCtx = null;
            if(enableSSL){
                SelfSignedCertificate certificate = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey()).build();
            }

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, childs)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpServerHandlerInitialize(sslCtx));

            ChannelFuture future = bootstrap.bind(port).sync();
            if(future.isSuccess()){
                System.out.println("服务器启动成功");
            }
            future.channel().closeFuture().sync();

        }catch (Exception e){
            logger.error("启动Netty服务异常，端口：[{}], Error:{}", port, e);
            throw  e;
        }finally {
            boss.shutdownGracefully();
            childs.shutdownGracefully();
        }
    }


    private int port;

    private boolean enableSSL = false;

    public NettyServer port(int port){
        this.port = port;
        return this;
    }

    public NettyServer enableSSL(boolean isEnableSSl){
        this.enableSSL = isEnableSSl;
        return this;
    }



}