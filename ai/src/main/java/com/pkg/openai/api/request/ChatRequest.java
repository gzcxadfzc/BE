package com.pkg.openai.api.request;


import com.pkg.openai.api.common.OpenAiModel;

import java.util.ArrayList;
import java.util.List;

public record ChatRequest(
        OpenAiModel model,
        List<ChatMessage> input,
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

    public static class Builder {

        private OpenAiModel model;
        private List<ChatMessage> messages = new ArrayList<>();
        private boolean background;

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

        public ChatRequest build() {
            return new ChatRequest(model, messages, background);
        }

        public Builder copy() {
            return new Builder(this.model, List.copyOf(this.messages), background);
        }
    }
}

