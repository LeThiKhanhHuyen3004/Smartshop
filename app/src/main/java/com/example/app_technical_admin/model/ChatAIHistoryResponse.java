package com.example.app_technical_admin.model;

import java.util.List;

public class ChatAIHistoryResponse {
    private boolean success;
    private List<ChatAIMessage> chat_history;

    public boolean isSuccess() {
        return success;
    }

    public List<ChatAIMessage> getChatHistory() {
        return chat_history;
    }
}