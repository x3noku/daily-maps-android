package com.x3noku.dailymaps

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.bottom_navigation_home_item -> {
                    Log.d(TAG, "HOME")
                    true
                }
                R.id.bottom_navigation_add_task_item -> {
                    Log.d(TAG, "ADD TASK")
                    true
                }
                R.id.bottom_navigation_profile_item -> {
                    Log.d(TAG, "PROFILE")
                    true
                }
                else -> false
            }
        }
    }

}
