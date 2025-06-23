package com.example.jarvis6;

import java.util.ArrayList;
import java.util.List;

public record Conversation(List<Message> conversation) {
    public Conversation(Message message) {
        this(new ArrayList<>());
    }

    public void addMessage(Message message) {
        this.conversation.add(message);
    }
}
