package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ThirdFragment()
            1 -> FirstFragment()
            else -> SecondFragment()
        }
    }
}