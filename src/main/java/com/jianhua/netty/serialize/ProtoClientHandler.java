package com.jianhua.netty.serialize;

import com.jianhua.netty.serialize.message.UserInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author lijianhua
 */
public class ProtoClientHandler extends SimpleChannelInboundHandler<UserInfo.User> {

    protected void channelRead0(ChannelHandlerContext ctx, UserInfo.User msg) throws Exception {
        System.out.println("收到服务器的回复：" + msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client: channel inactive");
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(UserInfo.User.newBuilder()
                .setName("lijianhua")
                .setPassword("password")
                .setPhoneno(1234444)
             //   .setHobby(0, "haha").setHobby(1, "hehe")
                .setSex("Male")
                .setStatus(UserInfo.User.UserStatus.ON_LINE)
                .setAddress(UserInfo.User.Address.newBuilder().setAddr("shenzhen").build())
                .build());
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}