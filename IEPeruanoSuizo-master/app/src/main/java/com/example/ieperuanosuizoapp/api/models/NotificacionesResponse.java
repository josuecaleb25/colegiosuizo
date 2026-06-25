package com.example.ieperuanosuizoapp.api.models;

import java.util.List;

public class NotificacionesResponse {
    private boolean success;
    private String message;
    private List<Notificacion> data;
    private Pagination pagination;

    public boolean isSuccess() { return success; }
    public List<Notificacion> getData() { return data; }
    public Pagination getPagination() { return pagination; }

    public static class Pagination {
        private int page;
        private int limit;
        private int total;
        private int totalPages;
        public int getTotal() { return total; }
    }
}
