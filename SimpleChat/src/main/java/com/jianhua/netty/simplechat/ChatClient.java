package com.jianhua.netty.simplechat;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * @author lijianhua
 */
public class ChatClient {

    private String CHAT_SERVER_IP = "127.0.0.1";

    private int PORT = 8080;

    private Bootstrap bootstrap = null;

    private long RETRY_DELAY = 3000;

    private int attemps = 0;

    private int MAX_RETRY = 5;

    private boolean reConnect = true;

    private Channel channel = null;

    public ChatClient(){
        NioEventLoopGroup child = new NioEventLoopGroup();
        try{
            bootstrap = new Bootstrap();
            bootstrap.group(child)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new LineBasedFrameDecoder(65536));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    attemps = 0;
                                    super.channelActive(ctx);
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    if(ctx.executor().isShuttingDown()){
                                        return;
                                    }
                                    super.channelInactive(ctx);
                                    if(reConnect){
                                        connect();
                                    }
                                }
                            });
                            ch.pipeline().addLast(new ChatClientHandler());
                        }

                    });

            connect();
        }catch (Exception e){
            System.out.println("初始化失败:");
            e.printStackTrace();
            System.exit(-1);
        }finally {
            child.shutdownGracefully();
        }
    }

    public void connect() throws InterruptedException, IOException {
        ChannelFuture future = null;
        synchronized (bootstrap) {
            future = bootstrap.connect(CHAT_SERVER_IP, PORT);
        }

        if(future == null || future.channel() == null){
            throw new IllegalStateException("注册失败");
        }

        final ChannelPromise promise = future.channel().newPromise();
        future = future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()) {
                    System.out.println("连入服务器成功");
                    ChatClient.this.channel = f.channel();
                    promise.trySuccess();
                } else {
                    System.out.println("连入服务器失败");
                    if (f.cause() != null) {
                        f.cause().printStackTrace();
                    }

                    ChatClient.this.channel = null;
                    if (reConnect) {

                        ChannelFuture connectionFuture = null;
                       while(++attemps <= MAX_RETRY){
                           System.out.println("尝试" + RETRY_DELAY + "ms后进行重连操作");
                           Thread.sleep(RETRY_DELAY);
                            connectionFuture
                                   = bootstrap.connect(CHAT_SERVER_IP, PORT).await();
                           if(connectionFuture.isSuccess()){
                               ChatClient.this.channel = connectionFuture.channel();
                               promise.trySuccess();
                               break;
                           }
                       }
                       if(connectionFuture == null || !connectionFuture.isSuccess()) {
                           promise.setFailure(new IllegalStateException("重连失败"));
                       }
                    }
                }
            }
        });

        promise.sync();
        if(!promise.isSuccess()){
            return;
        }

        // channel = future.channel();
        channel.writeAndFlush("Hello\n").sync();

        startWork();

    }

    private void startWork() throws IOException, InterruptedException {
        ChannelFuture lastWriteFuture = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for(;;){
            String line = reader.readLine();
            if(line == null){
                continue;
            }
            lastWriteFuture = channel.writeAndFlush(line +"\n");
            if("bye".equals(line.toLowerCase())){
                break;
            }
        }
        if(lastWriteFuture!=null){
            lastWriteFuture.sync();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new ChatClient();
    }

}