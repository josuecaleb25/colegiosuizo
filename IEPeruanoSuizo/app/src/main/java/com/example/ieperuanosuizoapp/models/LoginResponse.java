package com.example.ieperuanosuizoapp.models;

public class LoginResponse {
    private Usuario user;
    private Tokens tokens;
    
    public Usuario getUser() { return user; }
    public void setUser(Usuario user) { this.user = user; }
    
    public Tokens getTokens() { return tokens; }
    public void setTokens(Tokens tokens) { this.tokens = tokens; }
    
    public static class Tokens {
        private String access;
        private String refresh;
        
        public String getAccess() { return access; }
        public void setAccess(String access) { this.access = access; }
        
        public String getRefresh() { return refresh; }
        public void setRefresh(String refresh) { this.refresh = refresh; }
    }
}