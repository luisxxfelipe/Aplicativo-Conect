package com.conect.aplicativoconect.view.telaprincipal.dominio;

public class CategoriasDominio{
    String titulo;
    String fotos;

    public CategoriasDominio(String titulo, String fotos){
        this.titulo = titulo;
        this.fotos = fotos;
    }

    public String getTitulo(){
        return titulo;
    }

    public void setTitulo(String titulo){
        this.titulo = titulo;
    }

    public String getFotos(){
        return fotos;
    }

    public void setFotos(String fotos){
        this.fotos = fotos;
    }
}
