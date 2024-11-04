package com.conect.aplicativoconect.view.ui.client

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DetailsPagerAdapter(activity: FragmentActivity, private val companyId: String) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ServicesFragment.newInstance(companyId)
            1 -> PhotosFragment.newInstance(companyId)
            else -> ReviewsFragment.newInstance(companyId)
        }
    }
}
