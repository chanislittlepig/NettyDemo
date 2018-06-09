package com.jianhua.netty.demo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author lijianhua
 */
public class NettyClient {

    public void connect(String host ,int port) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(bossGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new IdleStateHandler(0,4,0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new LineBasedFrameDecoder(65536));
                          //  ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(100, 0, 2, 0, 2));
                            ch.pipeline().addLast(new StringDecoder());
                           // ch.pipeline().addLast(new LengthFieldPrepender(2));
                            ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("与服务器连接已断开");
                                    super.channelInactive(ctx);
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("与服务器建立链接成功");
                                    for(int i = 0;i < 100;i ++){
                                        ctx.write("请求："+i+"\n");
                                    }
                                    ctx.flush();
                                    super.channelActive(ctx);
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    if(msg instanceof String){
                                        System.out.println("服务器回复：" + msg);
                                    }
                                    super.channelRead(ctx, msg);
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    if(evt instanceof IdleStateEvent){
                                        IdleStateEvent stateEvent = (IdleStateEvent) evt;
                                        if(stateEvent.state() ==  IdleState.WRITER_IDLE){

                                        }
                                    }else {
                                        super.userEventTriggered(ctx, evt);
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    super.exceptionCaught(ctx, cause);
                                    cause.printStackTrace();
                                    System.out.println("关闭链接...");
                                    ctx.channel().close();
                                }
                            });
                        }
                    });

            ChannelFuture sync = bootstrap.connect(host, port).sync();
            sync.channel().closeFuture().sync();
        }catch (Exception e){}
    }

    public static void main(String[] args) throws Exception {
        new NettyClient().connect("127.0.0.1", 8080);

    }

}