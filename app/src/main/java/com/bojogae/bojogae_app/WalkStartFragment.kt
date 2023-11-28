package com.bojogae.bojogae_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController

class WalkStartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_walk_start, container, false)

        val homeBtn = view.findViewById<Button>(R.id.homeBtn)

        homeBtn.setOnClickListener{
            findNavController().navigate(R.id.walk_start_to_home)
        }

        return view
    }

}