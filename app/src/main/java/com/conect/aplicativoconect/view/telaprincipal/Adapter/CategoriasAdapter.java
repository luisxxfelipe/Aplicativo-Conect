package com.conect.aplicativoconect.view.telaprincipal.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.conect.aplicativoconect.R;
import com.conect.aplicativoconect.view.telaprincipal.dominio.CategoriasDominio;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CategoriasAdapter extends RecyclerView.Adapter<CategoriasAdapter.ViewHolder> {

    ArrayList<CategoriasDominio> categoriasDominios;
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_categorias, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.nome_categorias.setText(categoriasDominios.get(position).getTitulo());
    String foto_url="";
    switch (position){
        case 0:{
            foto_url  ="cat_1";
            break;
        }
        case 1:{
            foto_url="cat_2";
            break;
        }
        case 2:{
            foto_url  ="cat_3";
            break;
        }
        case 3:{
            foto_url="cat_4";
            break;
        }
    }
    int recurso_desenhavel = holder.itemView.getContext().getResources().getIdentifier(foto_url, "recurso", holder.itemView.getContext().getPackageName());
    Glide.with(holder.itemView.getContext()).load(recurso_desenhavel).into(holder.foto_categorias);
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView nome_categorias;
        ImageView foto_categorias;


        public ViewHolder(@NonNull View itemView){
            super(itemView);
            nome_categorias=itemView.findViewById(R.id.ic_barbearia);
            foto_categorias=itemView.findViewById(R.id.text_barbeiro);
        }
    }
}
