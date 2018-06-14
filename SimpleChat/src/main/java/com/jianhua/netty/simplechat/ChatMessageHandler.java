package com.jianhua.netty.simplechat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author lijianhua
 */
public class ChatMessageHandler extends SimpleChannelInboundHandler<String> {

    private static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        group.add(ctx.channel());
        final Channel currentChannel = ctx.channel();
        group.writeAndFlush("用户：["+ ctx.channel().remoteAddress() +"]加入, 当前在线人数:" + group.size() +"\n", new ChannelMatcher() {
            @Override
            public boolean matches(Channel channel) {
                return channel != currentChannel;
            }
        });
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        for (Channel channel : group) {
            if(channel != ctx.channel()){
                channel.writeAndFlush("[" + ctx.channel().remoteAddress() + "]:" + msg);
                channel.writeAndFlush("\n");
            }else{
                ctx.writeAndFlush("[you]:" + msg);
                ctx.writeAndFlush("\n");
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final Channel currentChannel = ctx.channel();
        group.writeAndFlush("用户：[" + ctx.channel().remoteAddress() + "]下线\n", new ChannelMatcher() {
            @Override
            public boolean matches(Channel channel) {
                return channel != currentChannel;
            }
        });
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            if(event.state() == IdleState.READER_IDLE){
                ctx.close();
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }
}