package com.jianhua.netty.demo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author lijianhua
 */
public class NettyServer {

    public void bind(int port) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup childGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, childGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler());
                            ch.pipeline().addLast(new IdleStateHandler(5,0,0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new LineBasedFrameDecoder(65536));
                           // ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 2, 0, 2));
                            ch.pipeline().addLast(new StringDecoder());
                            //ch.pipeline().addLast(new LengthFieldPrepender(2));
                            ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){


                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("连接即将断开");
                                    super.channelInactive(ctx);
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    if(msg instanceof String){
                                        System.out.println("收到客户端发送的请求:[" + msg+"]");
                                        ctx.writeAndFlush("我收到了"+msg+"\n");
                                    }
                                    super.channelRead(ctx, msg);
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("建立连接");
                                    super.channelActive(ctx);
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    if(evt instanceof IdleStateEvent){
                                        IdleStateEvent stateEvent = (IdleStateEvent) evt;
                                        if(stateEvent.state() ==  IdleState.READER_IDLE){
                                            if(stateEvent.isFirst()){
                                                System.out.println("第一次超时");
                                            }else{
                                                System.out.println("第二次超时，即将关闭链接");
                                                ctx.channel().close();
                                            }
                                        }
                                    }else {
                                        super.userEventTriggered(ctx, evt);
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    super.exceptionCaught(ctx, cause);
                                    System.out.println("发生异常");
                                    ctx.channel().close();
                                }
                            });
                        }
                    });

            ChannelFuture sync = bootstrap.bind(port).sync();
            sync.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new NettyServer().bind(8080);
    }

}