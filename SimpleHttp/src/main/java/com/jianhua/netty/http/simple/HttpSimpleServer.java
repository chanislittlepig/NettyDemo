package com.jianhua.netty.http.simple;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * @author lijianhua
 */
public class HttpSimpleServer {

    public void start(int port, boolean isSsl) throws Exception{
        NioEventLoopGroup boss = new NioEventLoopGroup(2);
        NioEventLoopGroup child = new NioEventLoopGroup();

        try{

            final SslContext sslCtx;
            if (isSsl) {
                SelfSignedCertificate ssc = new SelfSignedCertificate("lijianhua.com");
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, child)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new ChannelInboundHandlerAdapter(){
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            System.out.println("客户端准备连入");
                            super.channelRead(ctx, msg);
                        }
                    })
                    .childHandler(new HttpSimpleServerInitializer(sslCtx));

            bootstrap.bind(port).sync()
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if(future.isSuccess()){
                                System.out.println("服务器启动成功");
                            }else if(future.cause()!=null){
                                System.out.println("服务器启动过程中发生异常");
                                future.cause().printStackTrace();
                            }
                        }
                    }).channel().closeFuture().sync();

        }catch (Exception e){

        }
    }

    public static void main(String[] args) throws Exception {
        new HttpSimpleServer().start(443, true);
    }

}