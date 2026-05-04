package com.example.ieperuanosuizoapp.models;

public class DashboardStats {
    private String fecha;
    private AlumnosStats alumnos;
    private DocentesStats docentes;
    
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    
    public AlumnosStats getAlumnos() { return alumnos; }
    public void setAlumnos(AlumnosStats alumnos) { this.alumnos = alumnos; }
    
    public DocentesStats getDocentes() { return docentes; }
    public void setDocentes(DocentesStats docentes) { this.docentes = docentes; }
    
    public static class AlumnosStats {
        private int total;
        private int presentes;
        private int tardanzas;
        private int ausentes;
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getPresentes() { return presentes; }
        public void setPresentes(int presentes) { this.presentes = presentes; }
        
        public int getTardanzas() { return tardanzas; }
        public void setTardanzas(int tardanzas) { this.tardanzas = tardanzas; }
        
        public int getAusentes() { return ausentes; }
        public void setAusentes(int ausentes) { this.ausentes = ausentes; }
    }
    
    public static class DocentesStats {
        private int total;
        private int presentes;
        private int tardanzas;
        private int ausentes;
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getPresentes() { return presentes; }
        public void setPresentes(int presentes) { this.presentes = presentes; }
        
        public int getTardanzas() { return tardanzas; }
        public void setTardanzas(int tardanzas) { this.tardanzas = tardanzas; }
        
        public int getAusentes() { return ausentes; }
        public void setAusentes(int ausentes) { this.ausentes = ausentes; }
    }
}