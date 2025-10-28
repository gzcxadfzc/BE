package com.pkg.openai.api.request;


import com.pkg.openai.api.common.OpenAiModel;
import com.pkg.openai.api.common.TextFormat;

import java.util.ArrayList;
import java.util.List;

public record ChatRequest(
        OpenAiModel model,
        List<ChatMessage> input,
        TextFormat text,
        boolean background
) {

    public static Builder builder() {
        return new Builder();
    }

    public String getModel() {
        return model.getValue();
    }

    public List<ChatMessage> getInput() {
        return input;
    }

    public TextFormat getText() {
        return text;
    }

    public static class Builder {

        private OpenAiModel model;
        private List<ChatMessage> messages = new ArrayList<>();
        private boolean background;
        private TextFormat text;

        private Builder() {
        }

        private Builder(OpenAiModel model, List<ChatMessage> messages, boolean background) {
            this.model = model;
            this.messages = new ArrayList<>(messages);
            this.background = background;
        }

        public Builder background(boolean background) {
            this.background = background;
            return this;
        }

        public Builder model(OpenAiModel model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder addMessage(ChatMessage message) {
            messages.add(message);
            return this;
        }

        public Builder text(TextFormat text) {
            this.text = text;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(model, messages, text, background);
        }
    }
}

