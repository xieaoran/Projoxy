package com.xieaoran.projoxy.runners;

import com.xieaoran.projoxy.schedulers.WorkerScheduler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by xieaoran on 2017/5/3.
 */
public class ProxyRunner {
    public static void main(String[] args) {
        startServer(8080);
    }

    private static void startServer(int port) {
        try {
            WorkerScheduler scheduler = new WorkerScheduler();
            scheduler.start();

            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            SocketAddress socketAddress = new InetSocketAddress(port);
            serverSocketChannel.bind(socketAddress);
            while (true) {
                SocketChannel clientChannel = serverSocketChannel.accept();
                clientChannel.configureBlocking(false);
                scheduler.register(clientChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
