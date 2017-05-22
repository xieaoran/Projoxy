package com.xieaoran.projoxy.utils;

import com.xieaoran.projoxy.configs.ProxyConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequestPart {

    public static final String METHOD_CONNECT = "CONNECT";
    public static final String RESPONSE_AUTHORED = "HTTP/1.1 200 Connection established\r\n" +
            "Proxy-agent: Projoxy/1.0\r\n\r\n";
    private static final Pattern hostPattern = Pattern.compile("Host: (.+)");

    private String method;
    private SocketAddress address;
    private byte[] raw;
    private boolean ended;

    public String getMethod() {
        return method;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public byte[] getRaw() {
        return raw;
    }

    public boolean isEnded() {
        return ended;
    }

    public HttpRequestPart(ByteBuffer buffer, int readLength) throws IOException {
        this.raw = new byte[readLength];
        buffer.get(this.raw, 0, readLength);
        String requestPart = new String(this.raw);
        int firstSplitIndex = requestPart.indexOf(' ');
        this.method = requestPart.substring(0, firstSplitIndex);
        Matcher matcher = hostPattern.matcher(requestPart);
        if (!matcher.find()) {
            throw new IOException("Illegal HTTP Request");
        }
        String[] hostParts = matcher.group(1).split(":");
        String hostName = hostParts[0];
        int port;
        if (hostParts.length == 1) port = method.equals(METHOD_CONNECT) ? 443 : 80;
        else port = Integer.parseInt(hostParts[1]);
        this.ended = readLength < ProxyConfig.BUFFER_SIZE;
        this.address = new InetSocketAddress(hostName, port);
    }

    public HttpRequestPart(SocketAddress address, ByteBuffer buffer, int readLength) {
        this.raw = new byte[readLength];
        buffer.get(this.raw, 0, readLength);
        this.ended = readLength < ProxyConfig.BUFFER_SIZE;
        this.method = "UNKNOWN";
        this.address = address;
    }
}
