package com.x3noku.dailymaps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.x3noku.dailymaps.activities.LoginActivity
import com.x3noku.dailymaps.activities.MainActivity
import com.x3noku.dailymaps.data.UserInfo
import kotlinx.android.synthetic.main.activity_login.email_field_edittext
import kotlinx.android.synthetic.main.activity_login.password_field_edittext
import kotlinx.android.synthetic.main.activity_registration.*

class RegisterActivity: AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        firebaseAuth = FirebaseAuth.getInstance()
        val ttb = AnimationUtils.loadAnimation(this,R.anim.ttb)
        val headerTitle  = findViewById<TextView>(R.id.hi)
        headerTitle.startAnimation(ttb)
        val arrow = findViewById<ImageButton>(R.id.arrow_left)
        arrow.setOnClickListener(){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        val regButton = findViewById<Button>(R.id.button_reg)
        regButton.setOnClickListener(){
        val nickname = login_field.text.toString()
        val email = email_field_edittext.text.toString()
        val password = password_field_edittext.text.toString()
        val repeatedPassword = password_repeat_field_edittext.text.toString()
        if (email.equals("",ignoreCase = false) || password.equals("",ignoreCase = false) || repeatedPassword.equals("",ignoreCase = false)){
            Snackbar
                .make(
                    findViewById(R.id.activityRegistration),
                    getString(R.string.error_empty_fields),
                    Snackbar.LENGTH_LONG
                )
                .setTextColor(ContextCompat.getColor(this,R.color.blue))
                .setBackgroundTint(ContextCompat.getColor(this,R.color.white))
                .show()
        }
        else{
            Log.d("Registration","Fields are filled correctly!")
            if(!this.isEmailValid(email)){
                Snackbar
                    .make(
                        findViewById(R.id.activityRegistration),
                        getString(R.string.not_valid_email),
                        Snackbar.LENGTH_LONG
                    )
                    .setTextColor(ContextCompat.getColor(this,R.color.blue))
                    .setBackgroundTint(ContextCompat.getColor(this,R.color.white))
                    .show()
            }
            else{
                Log.d("Registration","The Email is valid!")
                if(password.length>15 || password.length<5 ) {
                    Snackbar
                        .make(
                            findViewById(R.id.activityRegistration),
                            getString(R.string.passwords_length),
                            Snackbar.LENGTH_LONG
                        )
                        .setTextColor(ContextCompat.getColor(this, R.color.blue))
                        .setBackgroundTint(ContextCompat.getColor(this, R.color.white))
                        .show()
                }
                else{
                    if(nickname.length>12 || nickname.length<5){
                        Snackbar
                            .make(
                                findViewById(R.id.activityRegistration),
                                getString(R.string.login_length),
                                Snackbar.LENGTH_LONG
                            )
                            .setTextColor(ContextCompat.getColor(this, R.color.blue))
                            .setBackgroundTint(ContextCompat.getColor(this, R.color.white))
                            .show()
                    }
                    else{
                    Log.d("Registration","Length of password is right!")
                    if(!password.equals(repeatedPassword,ignoreCase = false)){
                    Snackbar
                        .make(
                            findViewById(R.id.activityRegistration),
                            getString(R.string.passwords_dont_match),
                            Snackbar.LENGTH_LONG
                        )
                        .setTextColor(ContextCompat.getColor(this,R.color.blue))
                        .setBackgroundTint(ContextCompat.getColor(this,R.color.white))
                        .show()
                }
                    else{
                    Log.d("Registration","Passwords match!")
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            registerUserInFireStore(firebaseAuth.currentUser!!.uid, nickname)
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                        .addOnFailureListener{
                            if(it::class == FirebaseAuthUserCollisionException::javaClass) {
                                Snackbar
                                    .make(
                                        findViewById(R.id.activityRegistration),
                                        getString(R.string.user_already_exists),
                                        Snackbar.LENGTH_LONG
                                    )
                                    .setTextColor(ContextCompat.getColor(this, R.color.blue))
                                    .setBackgroundTint(ContextCompat.getColor(this, R.color.white))
                                    .show()

                            }
                            }

                        }
                    }
                    }
                }

            }
        }
        }
    private fun isEmailValid(email: String): Boolean =  android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun registerUserInFireStore(userId: String, nickname : String){
        val fireStore = FirebaseFirestore.getInstance()
        fireStore
            .collection(getString(R.string.firestore_users_collection))
            .document(userId)
            .set(UserInfo(nickname))
    }
}

