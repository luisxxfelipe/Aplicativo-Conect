package com.conect.aplicativoconect.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import com.conect.aplicativoconect.data.models.Booking

class BookingDiffCallback(
    private val oldList: List<Booking>,
    private val newList: List<Booking>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]
}
