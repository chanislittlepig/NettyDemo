package com.jianhua.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Iterator;
import java.util.Map;

/**
 * @author lijianhua
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    private DispatcherServlet dispatcherServlet;

    public HttpServerHandler(DispatcherServlet dispatcher) {
        this.dispatcherServlet = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if(!request.decoderResult().isSuccess()){
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        if(logger.isInfoEnabled()) {
            logger.info("收到来自客户端:[{}]的请求, 请求内容：{}", ctx.channel().remoteAddress(), request);
        }

        MockHttpServletRequest httpRequest = wrapHttpRequest(request);
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        dispatcherServlet.service(httpRequest, httpResponse);

        writeResponse(ctx, request, httpResponse);

    }

    private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, MockHttpServletResponse httpResponse) {
        ByteBuf resonseBuf = null;
        byte[] contentAsByteArray = httpResponse.getContentAsByteArray();
        if(contentAsByteArray == null){
            resonseBuf = Unpooled.buffer(0);
        }else{
            resonseBuf = Unpooled.wrappedBuffer(contentAsByteArray);
        }
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(httpResponse.getStatus()),
                resonseBuf);

        for (String headerName : httpResponse.getHeaderNames()) {
            response.headers().set(headerName, httpResponse.getHeaderValue(headerName));
        }

        if(HttpUtil.isKeepAlive(request)){
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }else{
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, resonseBuf.readableBytes());

        ChannelFuture future = ctx.writeAndFlush(response);
        if(!HttpUtil.isKeepAlive(request)){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private MockHttpServletRequest wrapHttpRequest(FullHttpRequest request) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(dispatcherServlet.getServletContext());
        mockHttpServletRequest.setRequestURI(request.uri());
        mockHttpServletRequest.setMethod(request.method().name());

        HttpHeaders headers = request.headers();
        Iterator<Map.Entry<String, String>> headIter = headers.iteratorAsString();
        while(headIter.hasNext()){
            Map.Entry<String, String> header = headIter.next();
            mockHttpServletRequest.addHeader(header.getKey(), header.getValue());
        }


        return mockHttpServletRequest;
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.buffer(0));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


}