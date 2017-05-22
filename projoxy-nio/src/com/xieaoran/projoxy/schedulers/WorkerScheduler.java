package com.xieaoran.projoxy.schedulers;

import com.xieaoran.projoxy.configs.ProxyConfig;
import com.xieaoran.projoxy.messages.RequestMessage;
import com.xieaoran.projoxy.workers.RemoteWorker;
import com.xieaoran.projoxy.workers.ServerWorker;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WorkerScheduler extends Thread {

    private ConcurrentLinkedQueue<RequestMessage> requestQueue;
    private ServerWorker[] serverWorkers;
    private RemoteWorker[] remoteWorkers;

    public WorkerScheduler() throws IOException {
        this.requestQueue = new ConcurrentLinkedQueue<>();
        this.serverWorkers = new ServerWorker[ProxyConfig.SERVER_WORKERS];
        for (int i = 0; i < ProxyConfig.SERVER_WORKERS; i++) {
            ServerWorker current = new ServerWorker(requestQueue);
            this.serverWorkers[i] = current;
            current.start();
        }
        this.remoteWorkers = new RemoteWorker[ProxyConfig.REMOTE_WORKERS];
        for (int i = 0; i < ProxyConfig.REMOTE_WORKERS; i++) {
            RemoteWorker current = new RemoteWorker();
            this.remoteWorkers[i] = current;
            current.start();
        }
    }

    public void register(SocketChannel clientChannel) throws ClosedChannelException {
        int minConnections = Integer.MAX_VALUE;
        ServerWorker selected = null;
        for (ServerWorker serverWorker : serverWorkers) {
            if (serverWorker.getActiveConnections() >= minConnections) continue;
            minConnections = serverWorker.getActiveConnections();
            selected = serverWorker;
        }
        if (selected != null) selected.register(clientChannel);
    }

    private void refresh() throws IOException {
        while (!requestQueue.isEmpty()) {
            int minConnections = Integer.MAX_VALUE;
            int minConnectionsBusy = Integer.MAX_VALUE;
            int maxConnections = Integer.MIN_VALUE;
            RemoteWorker selected = null;
            RemoteWorker selectedBusy = null;
            for (RemoteWorker remoteWorker : remoteWorkers) {
                if (remoteWorker.isBusy() && remoteWorker.getActiveConnections() < minConnectionsBusy) {
                    minConnectionsBusy = remoteWorker.getActiveConnections();
                    selectedBusy = remoteWorker;
                }
                if (!remoteWorker.isBusy() && remoteWorker.getActiveConnections() < minConnections){
                    minConnections = remoteWorker.getActiveConnections();
                    selected = remoteWorker;
                }
                if (remoteWorker.getActiveConnections() > maxConnections)
                    maxConnections = remoteWorker.getActiveConnections();
            }
            if (selected == null) {
                selected = selectedBusy;
                minConnections = minConnectionsBusy;
            }
            if (selected == null) continue;
            int assignConnections = maxConnections - minConnections;
            if (assignConnections == 0) assignConnections = 1;
            for (int i = 0; i < assignConnections; i++) {
                if (requestQueue.isEmpty()) return;
                RequestMessage message = requestQueue.poll();
                selected.assign(message);
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(ProxyConfig.WORKER_SLEEP);
                this.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
