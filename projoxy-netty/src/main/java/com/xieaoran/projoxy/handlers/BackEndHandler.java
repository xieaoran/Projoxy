package com.xieaoran.projoxy.handlers;

import com.xieaoran.projoxy.listeners.ReadNextListener;
import io.netty.channel.*;

public class BackEndHandler extends ChannelInboundHandlerAdapter {
    private final Channel inboundChannel;

    public BackEndHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        inboundChannel.writeAndFlush(msg).addListener(new ReadNextListener(ctx.channel()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        FrontEndHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        FrontEndHandler.closeOnFlush(ctx.channel());
    }
}
