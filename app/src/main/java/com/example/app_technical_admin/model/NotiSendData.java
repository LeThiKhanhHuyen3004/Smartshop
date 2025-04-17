package com.example.app_technical_admin.model;

import java.util.Map;

public class NotiSendData {
    private Message message;

    public NotiSendData(String token, Map<String, String> notification) {
        this.message = new Message(token, notification);
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public static class Message {
        private String token;
        private Map<String, String> notification;

        public Message(String token, Map<String, String> notification) {
            this.token = token;
            this.notification = notification;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Map<String, String> getNotification() {
            return notification;
        }

        public void setNotification(Map<String, String> notification) {
            this.notification = notification;
        }
    }
}
