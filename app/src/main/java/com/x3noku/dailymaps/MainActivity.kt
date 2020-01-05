package com.x3noku.dailymaps

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private var taskListFragment = TaskList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.main_frame_layout, taskListFragment)
        fragmentTransaction.commit()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.bottom_navigation_home_item -> {
                    fragmentTransaction.replace(R.id.main_frame_layout, taskListFragment)

                    true
                }
                R.id.bottom_navigation_add_task_item -> {
                    val addTask = AddTask( bottomNavigationView.selectedItemId )
                    addTask.show(supportFragmentManager, "AddTask")

                    true
                }
                R.id.bottom_navigation_profile_item -> {
                    Log.d(TAG, "PROFILE")
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.setOnNavigationItemReselectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.bottom_navigation_home_item -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    if (Build.VERSION.SDK_INT >= 26) {
                        transaction.setReorderingAllowed(false)
                    }
                    transaction.detach(taskListFragment).attach(taskListFragment).commit()
                }
                R.id.bottom_navigation_add_task_item -> {
                    Log.d(TAG, "ADD TASK")
                }
                R.id.bottom_navigation_profile_item -> {
                    Log.d(TAG, "PROFILE")
                }
            }
        }
    }

}
