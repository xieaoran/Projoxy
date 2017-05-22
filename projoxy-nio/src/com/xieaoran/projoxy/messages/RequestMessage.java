package com.xieaoran.projoxy.messages;

import com.xieaoran.projoxy.utils.HttpRequestPart;

import java.nio.channels.SocketChannel;

/**
 * Created by xieaoran on 2017/5/3.
 */
public class RequestMessage {
    private HttpRequestPart requestPart;
    private SocketChannel clientChannel;
    private volatile StatusEnum currentStatus;
    private boolean isSSL;

    public HttpRequestPart getRequestPart() {
        return requestPart;
    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    public StatusEnum getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(StatusEnum currentStatus){
        this.currentStatus = currentStatus;
    }

    public boolean isSSL() {
        return isSSL;
    }

    public void setSSL(boolean SSL) {
        this.isSSL = SSL;
    }

    public RequestMessage(HttpRequestPart requestPart, SocketChannel clientChannel) {
        this.requestPart = requestPart;
        this.clientChannel = clientChannel;
        this.currentStatus = StatusEnum.ClientRead;
        this.isSSL = false;
    }

    public void nextStatus(){
        switch (this.currentStatus){
            case ClientRead:
                currentStatus = StatusEnum.RemoteSent;
                break;
            case RemoteSent:
                currentStatus = StatusEnum.ClientSent;
                break;
        }
    }

}
