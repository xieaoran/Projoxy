package com.xieaoran.projoxy.initializers;

import com.xieaoran.projoxy.handlers.FrontEndHandler;
import com.xieaoran.projoxy.rules.RuleSet;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ProxyInitializer extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel ch) throws Exception {
        RuleSet ruleSet = new RuleSet();
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.INFO),
                new FrontEndHandler(ruleSet));
    }
}
