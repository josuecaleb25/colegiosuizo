package com.example.ieperuanosuizoapp.models;

public class QrScanRequest {
    private String qr_token;
    
    public QrScanRequest(String qr_token) {
        this.qr_token = qr_token;
    }
    
    public String getQr_token() { return qr_token; }
    public void setQr_token(String qr_token) { this.qr_token = qr_token; }
}