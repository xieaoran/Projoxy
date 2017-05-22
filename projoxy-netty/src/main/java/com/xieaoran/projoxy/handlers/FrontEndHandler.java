package com.xieaoran.projoxy.handlers;

import com.xieaoran.projoxy.listeners.ReadNextListener;
import com.xieaoran.projoxy.rules.RuleSet;
import com.xieaoran.projoxy.utils.HttpRequest;
import com.xieaoran.projoxy.utils.HttpRequestConsts;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import java.io.IOException;

public class FrontEndHandler extends ChannelInboundHandlerAdapter {

    private final Bootstrap bootstrap = new Bootstrap();

    private Channel outboundChannel;
    private boolean isSSL;

    private RuleSet ruleSet;

    public FrontEndHandler(RuleSet ruleSet){
        this.ruleSet = ruleSet;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.isSSL = false;
        final Channel inboundChannel = ctx.channel();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(inboundChannel.getClass())
                .handler(new BackEndHandler(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);
        inboundChannel.read();
    }

    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws IOException {
        if (null != outboundChannel && this.isSSL) {
            outboundChannel.writeAndFlush(msg).addListener(new ReadNextListener(ctx.channel()));
            return;
        }

        final ByteBuf buffer = (ByteBuf) msg;
        HttpRequest request = new HttpRequest(ctx.channel().remoteAddress(), buffer);

        if (!request.isValid()) {
            if (null != outboundChannel){
                outboundChannel.writeAndFlush(msg).addListener(new ReadNextListener(ctx.channel()));
                return;
            }
            else throw new IOException("Invalid HTTP Request.");
        }

        buffer.clear();
        String filterResponse = this.ruleSet.getResponse(request);
        if (null != filterResponse){
            buffer.writeBytes(filterResponse.getBytes());
            ctx.channel().writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        request.PreProcess();
        ChannelFuture future = bootstrap.connect(request.getRemoteHost());
        if (null != outboundChannel) {
            buffer.writeBytes(request.toString().getBytes());
            outboundChannel.writeAndFlush(buffer).addListener(new ReadNextListener(ctx.channel()));
        } else if (request.getMethod().equals(HttpRequestConsts.METHOD_CONNECT)) {
            this.isSSL = true;
            buffer.writeBytes(HttpRequestConsts.RESPONSE_AUTHORED.getBytes());
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    outboundChannel = future.channel();
                    ctx.channel().writeAndFlush(buffer).addListener(new ReadNextListener(ctx.channel()));
                }
            });
        } else {
            buffer.writeBytes(request.toString().getBytes());
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    outboundChannel = future.channel();
                    outboundChannel.writeAndFlush(buffer).addListener(new ReadNextListener(ctx.channel()));
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
