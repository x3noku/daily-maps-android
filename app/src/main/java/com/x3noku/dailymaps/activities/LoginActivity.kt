package com.x3noku.dailymaps.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.shashank.sony.fancytoastlib.FancyToast
import com.x3noku.dailymaps.R
import com.x3noku.dailymaps.RegisterActivity
import com.x3noku.dailymaps.data.UserInfo

class LoginActivity : AppCompatActivity(),  View.OnClickListener {

    companion object {
        const val TAG = "LoginActivity"
        const val RC_SIGN_IN = 9001

        private lateinit var rootView: View
        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var firestore: FirebaseFirestore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //declare the animation
        val ttb = AnimationUtils.loadAnimation(this,R.anim.ttb)
        val headerTitle  = findViewById<TextView>(R.id.hi)
        //set animation
        headerTitle.startAnimation(ttb)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val registerLinkTextView = findViewById<TextView>(R.id.register_link_textview)
        registerLinkTextView.setOnClickListener(){
            Log.e("test_REG_DEB","registration_debug")
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
        }

        val logInButton = findViewById<Button>(R.id.log_in_button)
        logInButton.setOnClickListener(this)

        val signInGoogleButton = findViewById<SignInButton>(R.id.sign_in_google_button)
        signInGoogleButton.setOnClickListener(this)

        rootView = findViewById(
            R.id.activity_login_rootview
        )
    }

    override fun onStart() {
        super.onStart()

        val currentUser = firebaseAuth.currentUser
        updateUI(currentUser)
    }

    override fun onResume() {
        super.onResume()

        val currentUser = firebaseAuth.currentUser
        updateUI(currentUser)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                account?.let { googleSignInAccount ->
                    authWithGoogle(googleSignInAccount)
                }
            }
            catch (e: ApiException) {
                FancyToast
                    .makeText(
                        baseContext,
                        "Не удалось авторизоваться: $e",
                        FancyToast.LENGTH_LONG,
                        FancyToast.ERROR,
                        false
                    ).show()
                Log.e(TAG, "Google sign in failed", e)
            }
        }
    }

    override fun onClick(v: View?) {
        when( v?.id ) {
            R.id.log_in_button -> {
                val email = findViewById<EditText>(R.id.email_field_edittext).text.toString()
                val password = findViewById<EditText>(R.id.password_field_edittext).text.toString()

                if( isEmailValid(email) and password.isNotBlank() ) {
                    authWithEmailAndPassword(email, password)
                }
                else {
                    Snackbar
                        .make(rootView, "Неправильно указана почта или поле пароля не заполнено!", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
            R.id.sign_in_google_button -> {
                chooseGoogleSignInAccount()
            }
        }
    }

    private fun chooseGoogleSignInAccount() {
        val googleSignInOptions = GoogleSignInOptions.Builder( GoogleSignInOptions.DEFAULT_SIGN_IN )
            .requestIdToken( getString(R.string.default_web_client_id) )
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent,
            RC_SIGN_IN
        )
    }

    private fun authWithGoogle(account: GoogleSignInAccount ) {
        val authCredential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth
            .signInWithCredential( authCredential )
            .addOnSuccessListener {
                val firestore = FirebaseFirestore.getInstance()

                val currentUser = firebaseAuth.currentUser
                if( currentUser != null ) {
                    val currentUserDocumentReference =
                        firestore.collection(getString(R.string.firestore_users_collection)).document(currentUser.uid)
                    currentUserDocumentReference.get()
                        .addOnSuccessListener {
                            if( !it.exists() ) {
                                val currentUserId = currentUser.uid
                                Log.w(TAG, currentUserId)
                                val currentUserName =
                                    if (account.displayName != null) account.displayName!! else "User"

                                firestore
                                    .collection(getString(R.string.firestore_users_collection))
                                    .document(currentUserId)
                                    .set(
                                        UserInfo(
                                            currentUserName
                                        )
                                    )
                                    .addOnSuccessListener {
                                        updateUI(currentUser)
                                    }
                                    .addOnFailureListener {
                                        FancyToast
                                            .makeText(
                                                this,
                                                "Can not create user's document",
                                                FancyToast.LENGTH_LONG,
                                                FancyToast.ERROR,
                                                false
                                            ).show()
                                    }
                            }
                            else {
                                updateUI(currentUser)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                FancyToast
                    .makeText(
                        baseContext,
                        "Не удалось авторизоваться: $e",
                        FancyToast.LENGTH_LONG,
                        FancyToast.ERROR,
                        false
                    ).show()
            }
    }

    private fun authWithEmailAndPassword(email: String, password: String ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if( task.isSuccessful ) {
                    val currentUser = firebaseAuth.currentUser
                    updateUI(currentUser)
                }
                else {
                    FancyToast
                        .makeText(
                            baseContext,
                            "Не удалось авторизоваться!",
                            FancyToast.LENGTH_LONG,
                            FancyToast.ERROR,
                            false
                        ).show()
                }
            }
    }

    private fun isEmailValid(email: String): Boolean =  android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun updateUI( currentUser: FirebaseUser? ) =
        currentUser?.let {
            startActivity( Intent(baseContext, MainActivity::class.java) )
        }

}
