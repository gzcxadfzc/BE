package com.pkg.openai.api.request;

import com.pkg.openai.api.common.ChatRole;

public class ChatMessage {

    private String role;
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage ofDeveloper(String content) {
        return new ChatMessage(ChatRole.DEVELOPER.getValue(), content);
    }

    public static ChatMessage ofUser(String content) {
        return new ChatMessage(ChatRole.DEVELOPER.getValue(), content);
    }

    public static ChatMessage ofAssistant(String content) {
        return new ChatMessage(ChatRole.DEVELOPER.getValue(), content);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

