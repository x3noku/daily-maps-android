package com.x3noku.dailymaps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsActivity : AppCompatActivity() {
    companion object {
        private var currentUser: FirebaseUser? = null
        private lateinit var currentUserInfo: UserInfo

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            user
        }

    }
}

