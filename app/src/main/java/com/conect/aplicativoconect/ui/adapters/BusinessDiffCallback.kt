package com.conect.aplicativoconect.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import com.conect.aplicativoconect.data.models.Business

class BusinessDiffCallback(
    private val oldList: List<Business>,
    private val newList: List<Business>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Verifica se o id do item é o mesmo
        return oldList[oldItemPosition].name == newList[newItemPosition].name
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Verifica se o conteúdo do item é o mesmo
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}