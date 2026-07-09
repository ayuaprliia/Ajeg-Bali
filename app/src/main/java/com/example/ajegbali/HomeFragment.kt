package com.example.ajegbali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardJejahitan = view.findViewById<CardView>(R.id.cardJejahitan)
        val cardKetupat   = view.findViewById<CardView>(R.id.cardKetupat)
        val cardWayang    = view.findViewById<CardView>(R.id.cardWayang)

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)

        cardJejahitan.setOnClickListener { bottomNav.selectedItemId = R.id.menu_jejahitan }
        cardKetupat.setOnClickListener   { bottomNav.selectedItemId = R.id.menu_ketupat }
        cardWayang.setOnClickListener    { bottomNav.selectedItemId = R.id.menu_wayang }
    }
}