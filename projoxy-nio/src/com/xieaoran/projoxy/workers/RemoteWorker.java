package com.xieaoran.projoxy.workers;


import com.xieaoran.projoxy.configs.ProxyConfig;
import com.xieaoran.projoxy.messages.RequestMessage;
import com.xieaoran.projoxy.messages.StatusEnum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class RemoteWorker extends Thread {
    private Selector selector;
    private ByteBuffer buffer;
    private Queue<RequestMessage> messages;

    private volatile boolean busy;
    private volatile int activeConnections;

    public boolean isBusy() {
        return busy;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public RemoteWorker() throws IOException {
        this.selector = Selector.open();
        this.buffer = ByteBuffer.allocate(ProxyConfig.BUFFER_SIZE);
        this.messages = new LinkedList<>();
        this.busy = false;
    }

    public void assign(RequestMessage message) throws IOException {
        messages.offer(message);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(ProxyConfig.WORKER_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (!messages.isEmpty()) {
                RequestMessage message = messages.poll();
                try {
                    this.busy = true;
                    SocketChannel requestChannel = SocketChannel.open();
                    requestChannel.configureBlocking(false);
                    requestChannel.connect(message.getRequestPart().getAddress());
                    for (int i = 0; i < ProxyConfig.CONNECTION_TIMEOUT / ProxyConfig.WORKER_SLEEP; i++) {
                        if (requestChannel.finishConnect()) {
                            requestChannel.register(this.selector,
                                    SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                                    message);
                            break;
                        }
                        Thread.sleep(ProxyConfig.WORKER_SLEEP);
                    }
                    if (!requestChannel.finishConnect()){
                        requestChannel.close();
                    }
                    this.busy = false;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.activeConnections = selector.keys().size();
            try {
                int count = selector.selectNow();
                if (count == 0) continue;
            } catch (IOException e) {
                e.printStackTrace();
            }

            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                if (!key.isValid()) continue;
                SocketChannel requestChannel = (SocketChannel) key.channel();
                if (!requestChannel.isConnected()) continue;

                RequestMessage message = (RequestMessage) key.attachment();
                if (!message.getClientChannel().isConnected()) {
                    try {
                        requestChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                try {
                    if (message.getCurrentStatus() == StatusEnum.ClientRead) {
                        this.buffer.clear();
                        this.buffer.put(message.getRequestPart().getRaw());
                        this.buffer.flip();
                        requestChannel.write(this.buffer);

                        if (!message.getRequestPart().isEnded()) {
                            while (true) {
                                this.buffer.clear();
                                int readLength = message.getClientChannel().read(this.buffer);
                                if (readLength == 0) break;
                                else if (readLength < 0) {
                                    requestChannel.close();
                                    break;
                                }
                                this.buffer.flip();
                                requestChannel.write(this.buffer);
                            }
                        }

                        message.nextStatus();
                    } else {
                        while (true) {
                            this.buffer.clear();
                            int readLength = requestChannel.read(this.buffer);
                            if (readLength == 0) {
                                message.nextStatus();
                                break;
                            } else if (readLength < 0) {
                                requestChannel.close();
                                message.nextStatus();
                                break;
                            }
                            this.buffer.flip();
                            message.getClientChannel().write(this.buffer);
                        }
                    }
                } catch (IOException e) {
                    try {
                        requestChannel.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
