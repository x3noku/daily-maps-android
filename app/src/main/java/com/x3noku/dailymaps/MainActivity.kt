package com.x3noku.dailymaps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private var taskListFragment = TaskListFragment()
    private var profileFragment = ProfileFragment()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val data = intent.data
        data?.let {
            val type = it.pathSegments[it.pathSegments.lastIndex-1]
            val id = it.pathSegments.last()

            when(type) {
                "tasks" ->
                    AddTaskDialogFragment(id)
                        .show(supportFragmentManager, "AddTask")
                "templates" ->
                    TemplateDialogFragment(id, TemplateDialogFragment.SHARED)
                        .show(supportFragmentManager, "Template")

            }
        }

        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.main_frame_layout, taskListFragment)
        fragmentTransaction.commit()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            when(menuItem.itemId) {
                R.id.bottom_navigation_home_item -> {
                    transaction.replace(R.id.main_frame_layout, taskListFragment)
                    transaction.commit()
                    true
                }
                R.id.bottom_navigation_add_task_item -> {
                    val addTask = AddTaskDialogFragment( bottomNavigationView.selectedItemId )
                    addTask.show(supportFragmentManager, "AddTask")

                    true
                }
                R.id.bottom_navigation_profile_item -> {
                    transaction.replace(R.id.main_frame_layout, profileFragment)
                    transaction.commit()
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
                R.id.bottom_navigation_profile_item -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    if (Build.VERSION.SDK_INT >= 26) {
                        transaction.setReorderingAllowed(false)
                    }
                    transaction.detach(profileFragment).attach(profileFragment).commit()
                }
            }
        }
    }

}
