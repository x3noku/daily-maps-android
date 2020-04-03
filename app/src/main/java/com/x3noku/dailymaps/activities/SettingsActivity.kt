package com.x3noku.dailymaps.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.x3noku.dailymaps.R
import com.x3noku.dailymaps.data.UserInfo

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

