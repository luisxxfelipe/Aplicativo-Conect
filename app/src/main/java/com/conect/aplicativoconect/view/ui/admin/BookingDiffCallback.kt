package com.conect.aplicativoconect.view.ui.admin

import androidx.recyclerview.widget.DiffUtil
import com.conect.aplicativoconect.view.data.model.Booking

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
