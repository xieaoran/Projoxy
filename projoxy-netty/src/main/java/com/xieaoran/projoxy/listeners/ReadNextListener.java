package com.xieaoran.projoxy.listeners;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class ReadNextListener implements ChannelFutureListener {
    private Channel channel;

    public ReadNextListener(Channel channel){
        this.channel = channel;
    }

    public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
            this.channel.read();
        } else {
            future.channel().close();
        }
    }
}
