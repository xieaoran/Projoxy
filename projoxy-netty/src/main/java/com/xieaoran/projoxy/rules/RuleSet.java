package com.xieaoran.projoxy.rules;

import com.xieaoran.projoxy.utils.HttpRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RuleSet {
    private List<Pattern> filterUrls;
    private List<Pattern> filterClients;
    private Map<Pattern, String> redirects;

    private static final String NOT_FOUND_RESPONSE =
            "%s 404 Not Found\r\n" +
                    "Server: Projoxy/2.0.0.Beta1\r\n" +
                    "Content-Length: 0";

    private static final String REDIRECT_RESPONSE =
            "%s 302 Moved Temporarily\r\n" +
                    "Server: Projoxy/2.0.0.Beta1\r\n" +
                    "Location: %s\r\n" +
                    "Content-Length: 0";

    public List<Pattern> getFilterUrls() {
        return this.filterUrls;
    }

    public List<Pattern> getFilterClients() {
        return this.filterClients;
    }

    public Map<Pattern, String> getRedirects() {
        return this.redirects;
    }

    public RuleSet() {
        this.filterUrls = new ArrayList<Pattern>();
        this.filterClients = new ArrayList<Pattern>();
        this.redirects = new HashMap<Pattern, String>();
    }

    public void appendFilterUrl(String filterUrlStr) {
        this.filterUrls.add(Pattern.compile(filterUrlStr));
    }

    public void appendFilterClient(String filterClientStr) {
        this.filterClients.add(Pattern.compile(filterClientStr));
    }

    public void appendRedirect(String redirectPatternStr, String redirectUrlStr) {
        this.redirects.put(Pattern.compile(redirectPatternStr), redirectUrlStr);
    }

    public void appendFilterUrls(List<String> filterUrlStrList) {
        for (String filterUrlStr : filterUrlStrList) {
            appendFilterUrl(filterUrlStr);
        }
    }

    public void appendFilterClients(List<String> filterClientStrList) {
        for (String filterClientStr : filterClientStrList) {
            appendFilterClient(filterClientStr);
        }
    }

    public void appendRedirects(Map<String, String> redirectMap) {
        for (String redirectPatternStr : redirectMap.keySet()) {
            appendRedirect(redirectPatternStr, redirectMap.get(redirectPatternStr));
        }
    }

    public String getResponse(HttpRequest request) {
        for (Pattern filterUrlPattern : this.filterUrls) {
            Matcher matcher = filterUrlPattern.matcher(request.getRemoteUrl());
            if (matcher.find()) return String.format(NOT_FOUND_RESPONSE, request.getVersion());
        }
        for (Pattern filterClientPattern : this.filterClients) {
            Matcher matcher = filterClientPattern.matcher(request.getClientAddress());
            if (matcher.find()) return String.format(NOT_FOUND_RESPONSE, request.getVersion());
        }
        for (Pattern redirectPattern : this.redirects.keySet()) {
            Matcher matcher = redirectPattern.matcher(request.getRemoteUrl());
            if (matcher.find()) return String.format(REDIRECT_RESPONSE, request.getVersion(),
                    this.redirects.get(redirectPattern));
        }
        return null;
    }
}
