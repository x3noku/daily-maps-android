package com.x3noku.dailymaps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"

        private var taskListFragment = TaskListFragment()
        private var profileFragment = ProfileFragment()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onResume() {
        super.onResume()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if( currentUser == null )
            startActivity( Intent(this, LoginActivity::class.java) )
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

        initBottomNavigation()
    }

    private fun initBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            when(menuItem.itemId) {
                R.id.bottom_navigation_home_item ->
                    fragmentTransaction.replace(R.id.main_frame_layout, taskListFragment)

                R.id.bottom_navigation_add_task_item ->
                    AddTaskDialogFragment( bottomNavigationView.selectedItemId )
                        .show(supportFragmentManager, "AddTask")

                R.id.bottom_navigation_profile_item ->
                    fragmentTransaction.replace(R.id.main_frame_layout, profileFragment)

            }
            fragmentTransaction.commit()
            true
        }
        bottomNavigationView.setOnNavigationItemReselectedListener { menuItem ->
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            when(menuItem.itemId) {
                R.id.bottom_navigation_home_item -> {
                    if (Build.VERSION.SDK_INT >= 26) {
                        fragmentTransaction.setReorderingAllowed(false)
                    }
                    fragmentTransaction.detach(taskListFragment).attach(taskListFragment)
                }
                R.id.bottom_navigation_profile_item -> {
                    if (Build.VERSION.SDK_INT >= 26) {
                        fragmentTransaction.setReorderingAllowed(false)
                    }
                    fragmentTransaction.detach(profileFragment).attach(profileFragment)
                }
            }
            fragmentTransaction.commit()
        }
    }

}
