package com.jianhua.netty.simplechat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author lijianhua
 */
public class ChatServer {

    public void start(int port){
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup childs = new NioEventLoopGroup();

        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, childs)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new IdleStateHandler(30, 0, 0, TimeUnit.MINUTES));
                            ch.pipeline().addLast(new LineBasedFrameDecoder(65536));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new HeartServerHandler());
                            ch.pipeline().addLast(new ChatMessageHandler());
                        }
                    });

            bootstrap.bind(port).addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if(future.isSuccess()) {
                                System.out.println("服务器启动成功");
                            }else if(future.cause()!=null){
                                System.out.println("服务器启动异常");
                                future.cause().printStackTrace();
                            }
                        }
                    }).channel().closeFuture().sync();


        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            childs.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new ChatServer().start(8080);
    }

}