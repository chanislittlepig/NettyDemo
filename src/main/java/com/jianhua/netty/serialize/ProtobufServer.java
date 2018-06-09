package com.jianhua.netty.serialize;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author lijianhua
 */
public class ProtobufServer {

    private int port;

    public ProtobufServer(int port){
        this.port = port;
    }

    public void start(){
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup childs = new NioEventLoopGroup();
        try{

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, childs)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 120)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelHandlerInitial());

            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            channelFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.cause() != null){
                        System.out.println("服务器启动失败");
                        future.cause().printStackTrace();
                        return;
                    }
                    System.out.println("服务器启动成功");
                }
            });

           channelFuture.channel().closeFuture().sync();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            childs.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new ProtobufServer(8080).start();
    }

}