package com.xieaoran.projoxy.workers;

import com.xieaoran.projoxy.configs.ProxyConfig;
import com.xieaoran.projoxy.messages.RequestMessage;
import com.xieaoran.projoxy.messages.StatusEnum;
import com.xieaoran.projoxy.utils.HttpRequestPart;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerWorker extends Thread {

    private Selector selector;
    private ByteBuffer buffer;

    private ConcurrentLinkedQueue<RequestMessage> requestQueue;

    private volatile int activeConnections;

    public int getActiveConnections() {
        return activeConnections;
    }

    public ServerWorker(ConcurrentLinkedQueue<RequestMessage> requestQueue) throws IOException {
        this.selector = Selector.open();
        this.buffer = ByteBuffer.allocate(ProxyConfig.BUFFER_SIZE);
        this.requestQueue = requestQueue;
        setDaemon(true);
    }

    public void register(SocketChannel clientChannel) throws ClosedChannelException {
        clientChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(ProxyConfig.WORKER_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.activeConnections = selector.keys().size();
            try {
                int count = selector.selectNow();
                if (count == 0) continue;
            } catch (IOException e) {
                e.printStackTrace();
            }

            RequestMessage message;

            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                if (!key.isValid() || !key.isReadable() || !key.isWritable()) continue;
                if (key.attachment() != null) {
                    message = (RequestMessage) key.attachment();
                    if (message.getCurrentStatus() != StatusEnum.ClientSent) continue;
                } else message = null;

                SocketChannel clientChannel = (SocketChannel) key.channel();

                this.buffer.clear();
                int readLength;
                try {
                    readLength = clientChannel.read(this.buffer);
                    if (readLength == 0) continue;
                    else if (readLength < 0) {
                        clientChannel.close();
                        continue;
                    }
                } catch (IOException exception) {
                    try {
                        clientChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                this.buffer.flip();
                try {
                    if (message != null && message.isSSL()) {
                        RequestMessage newMessage = new RequestMessage(
                                new HttpRequestPart(message.getRequestPart().getAddress(),
                                        this.buffer,readLength),
                                clientChannel);
                        newMessage.setSSL(true);
                        key.attach(newMessage);
                        requestQueue.offer(newMessage);
                    } else {
                        HttpRequestPart requestPart = new HttpRequestPart(this.buffer, readLength);
                        if (requestPart.getMethod().equals(HttpRequestPart.METHOD_CONNECT)) {
                            this.buffer.clear();
                            this.buffer.put(HttpRequestPart.RESPONSE_AUTHORED.getBytes());
                            this.buffer.flip();
                            clientChannel.write(this.buffer);

                            message = new RequestMessage(requestPart, clientChannel);
                            message.setSSL(true);
                            message.setCurrentStatus(StatusEnum.ClientSent);
                            key.attach(message);

                        } else {
                            message = new RequestMessage(requestPart, clientChannel);
                            key.attach(message);
                            requestQueue.offer(message);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
