package com.conect.aplicativoconect.view.telaprincipal.dominio;

public class Estabelecimentos {
    private String titulo;
    private String fotos;
    private Double estrelas;
    private int tempo;


    public Estabelecimentos(String titulo, String fotos, Double estrelas, int tempo) {
        this.titulo = titulo;
        this.fotos = fotos;
        this.estrelas = estrelas;
        this.tempo = tempo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getFotos() {
        return fotos;
    }

    public void setFotos(String fotos) {
        this.fotos = fotos;
    }

    public Double getEstrelas() {
        return estrelas;
    }

    public void setEstrelas(Double estrelas) {
        this.estrelas = estrelas;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }
}
