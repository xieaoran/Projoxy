package com.xieaoran.projoxy.utils;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;


public class HttpRequest {
    private String method;
    private String clientAddress;
    private SocketAddress remoteHost;
    private String remoteUrl;
    private String version;
    private Map<String, String> headers;
    private String body;

    private boolean valid;

    public String getMethod() {
        return this.method;
    }

    public String getClientAddress() {
        return this.clientAddress;
    }

    public SocketAddress getRemoteHost() {
        return this.remoteHost;
    }

    public String getRemoteUrl() {
        return this.remoteUrl;
    }

    public String getVersion() {
        return version;
    }

    public boolean isValid() {
        return this.valid;
    }

    public HttpRequest(SocketAddress clientAddress, ByteBuf buffer) throws IOException {
        this.clientAddress = clientAddress.toString().substring(1);
        byte[] rawBytes = new byte[buffer.readableBytes()];
        buffer.getBytes(0, rawBytes);
        String requestStr = new String(rawBytes);

        if (!requestStr.contains("HTTP")) {
            this.valid = false;
            return;
        }

        this.valid = true;
        String[] lines = requestStr.split("\r\n");

        String[] firstLineSplits = lines[0].split("\\s");
        this.method = firstLineSplits[0];
        this.remoteUrl = firstLineSplits[1];
        this.version = firstLineSplits[2];

        String remoteHostName;
        int remotePort;
        if (this.method.equals(HttpRequestConsts.METHOD_CONNECT)) {
            String[] hostSplits = this.remoteUrl.split(":");
            remoteHostName = hostSplits[0];
            if (hostSplits.length > 1) remotePort = Integer.valueOf(hostSplits[1]);
            else remotePort = 443;
        } else {
            Matcher matcher = HttpRequestConsts.URL_PATTERN.matcher(this.remoteUrl);
            if (!matcher.find()) {
                this.valid = false;
                return;
            }
            remoteHostName = matcher.group(1);
            String remotePortStr = matcher.group(2);
            if (remotePortStr.equals("")) remotePort = 80;
            else remotePort = Integer.valueOf(matcher.group(2));
        }
        this.remoteHost = new InetSocketAddress(remoteHostName, remotePort);

        if (lines.length == 1) return;
        this.headers = new HashMap<String, String>();
        int lineIndex;
        for (lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            String currentLine = lines[lineIndex];
            if (currentLine.equals("")) break;
            String[] currentLineSplits = lines[lineIndex].split(": ");
            this.headers.put(currentLineSplits[0], currentLineSplits[1]);
        }

        if (lineIndex == lines.length - 1) return;
        StringBuilder bodyBuilder = new StringBuilder();
        for (int i = lineIndex + 1; i < lines.length; i++) {
            bodyBuilder.append(lines[i]);
            if (i != lines.length - 1) bodyBuilder.append("\r\n");
        }
        this.body = bodyBuilder.toString();
    }

    public void PreProcess() {
        if (this.headers.containsKey("Proxy-Connection")) {
            this.headers.put("Connection", this.headers.get("Proxy-Connection"));
            this.headers.remove("Proxy-Connection");
        }
    }

    @Override
    public String toString() {
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(this.method);
        requestBuilder.append(' ');
        requestBuilder.append(this.remoteUrl);
        requestBuilder.append(' ');
        requestBuilder.append(this.version);

        if (null == this.headers) return requestBuilder.toString();
        requestBuilder.append("\r\n");
        Iterator<Map.Entry<String, String>> iterator = this.headers.entrySet().iterator();
        while (true) {
            Map.Entry<String, String> next = iterator.next();
            requestBuilder.append(next.getKey());
            requestBuilder.append(": ");
            requestBuilder.append(next.getValue());
            if (iterator.hasNext()) requestBuilder.append("\r\n");
            else break;
        }


        if (null == this.body) return requestBuilder.toString();
        requestBuilder.append("\r\n\r\n");
        requestBuilder.append(this.body);

        return requestBuilder.toString();
    }
}
