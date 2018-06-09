package com.jianhua.netty.simplechat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author lijianhua
 */
public class HeartServerHandler extends ChannelInboundHandlerAdapter{

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof String) {
            if ("HEART PENG".equals(msg)) {
                return;
            }
        }
        super.channelRead(ctx, msg);
    }
}