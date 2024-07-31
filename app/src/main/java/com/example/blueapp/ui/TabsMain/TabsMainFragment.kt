package com.example.blueapp.ui.TabsMain

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.blueapp.R
import com.example.blueapp.databinding.FragmentMainTabsBinding
import com.example.blueapp.ui.Interfaces.TabNavigationListener
import com.example.blueapp.ui.Jabas.JabasFragment
import com.example.blueapp.ui.ViewModel.TabViewModel
import com.example.blueapp.ui.preliminar.FragmentPreliminar
import com.example.blueapp.ui.slideshow.SlideshowFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TabsMainFragment : Fragment(), TabNavigationListener {

    private var _binding: FragmentMainTabsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabViewModel: TabViewModel
    private lateinit var viewPager: ViewPager2
    @SuppressLint("SetTextI18n", "DefaultLocale", "ResourceType", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainTabsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val tabLayout: TabLayout = binding.tabLayoutMain.findViewById(R.id.tabLayoutMain)
        val viewPager: ViewPager2 = binding.viewPager.findViewById(R.id.viewPager)

        viewPager.adapter = MainPagerAdapter(requireActivity())

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Bluetooth"
                1 -> "Pesajes"
                2 -> "Total"
                else -> null
            }
        }.attach()

        tabViewModel = ViewModelProvider(requireActivity()).get(TabViewModel::class.java)
        tabViewModel.navigateToTab.observe(viewLifecycleOwner) { position ->
            viewPager.currentItem = position
        }

        return root

    }

    override fun navigateToTab(position: Int) {
        viewPager.currentItem = position
    }
}

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SlideshowFragment()
            1 -> JabasFragment()
            2 -> FragmentPreliminar()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
