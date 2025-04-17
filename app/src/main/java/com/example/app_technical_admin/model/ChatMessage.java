package com.example.app_technical_admin.model;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {
    public String sendId, receivedId, message, datetime;
    public Date dateObj;
}