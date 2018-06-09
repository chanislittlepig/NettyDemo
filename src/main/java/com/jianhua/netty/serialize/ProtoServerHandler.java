package com.jianhua.netty.serialize;

import com.jianhua.netty.serialize.message.UserInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lijianhua
 */
public class ProtoServerHandler extends SimpleChannelInboundHandler<UserInfo.User> {

    private static final int MAX_TIMES = 3;

    private AtomicInteger timeoutTimes = new AtomicInteger(0);

    protected void channelRead0(ChannelHandlerContext ctx, UserInfo.User msg) throws Exception {
        System.out.println("收到客户端请求：" + msg);
        ctx.writeAndFlush(newUserInfo(msg.getName()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server: socket inactive");
        super.channelInactive(ctx);
    }

    private UserInfo.User newUserInfo(String name) {
        return UserInfo.User.newBuilder()
                .setName("get "+ name)
                .setPassword("password")
                .setPhoneno(1234444)
              //  .setHobby(0, "haha").setHobby(1, "hehe")
                .setSex("Male")
                .setStatus(UserInfo.User.UserStatus.ON_LINE)
                .setAddress( UserInfo.User.Address.newBuilder().setAddr("shenzhen").build())
                .build();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if(stateEvent.state() ==  IdleState.READER_IDLE){
                if(stateEvent.isFirst()){
                    System.out.println("第1次超时");
                    timeoutTimes.set(1);
                }else{
                    int current = timeoutTimes.incrementAndGet();
                    System.out.println("第"+current+"次超时");
                    if(current >= MAX_TIMES){
                        System.out.println("超时次数过多，断开此次连接");
                        ctx.channel().close();
                    }

                }
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}