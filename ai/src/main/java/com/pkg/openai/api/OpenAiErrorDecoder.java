package com.pkg.openai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.openai.api.exception.OpenAiApiException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiErrorDecoder implements ErrorDecoder {

    private static final Pattern CHARSET_IN_CONTENT_TYPE =
            Pattern.compile("charset=([\\w\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Override
    public Exception decode(String s, Response response) {
        String reason = response.reason() != null ? response.reason() : "";
        String bodyText = readBodyAsString(response);
        String message = reason + ":" + extractErrorMessage(bodyText);
        return new OpenAiApiException(response.status(), message);
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isEmpty()) return null;
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node.has("error") && node.get("error").has("message")) {
                return node.get("error").get("message").asText();
            }
        } catch (Exception ignore) {
            // JSON이 아니면 그냥 무시
        }
        return null;
    }

    private String readBodyAsString(Response response) {
        if (response.body() == null) return "";
        Charset cs = detectCharset(response.headers(), StandardCharsets.UTF_8);
        try {
            return Util.toString(response.body().asReader(cs));
        } catch (Exception e) {
            return "";
        }
    }

    private Charset detectCharset(java.util.Map<String, Collection<String>> headers, Charset fallback) {
        Collection<String> cts = headers.getOrDefault("Content-Type", headers.getOrDefault("content-type", null));
        if (cts != null) {
            for (String ct : cts) {
                Matcher m = CHARSET_IN_CONTENT_TYPE.matcher(ct);
                if (m.find()) {
                    try { return Charset.forName(m.group(1)); } catch (Exception ignore) {}
                }
            }
        }
        return fallback;
    }
}
