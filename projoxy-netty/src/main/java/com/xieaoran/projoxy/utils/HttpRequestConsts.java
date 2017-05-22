package com.xieaoran.projoxy.utils;

import java.util.regex.Pattern;

public final class HttpRequestConsts {

    public static final String METHOD_CONNECT = "CONNECT";
    public static final String RESPONSE_AUTHORED = "HTTP/1.1 200 Connection Established\r\n\r\n";
    static final Pattern URL_PATTERN = Pattern.compile("^http://([A-Za-z0-9.]+):?(\\d*)");
}
