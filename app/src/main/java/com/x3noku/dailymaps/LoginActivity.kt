package com.x3noku.dailymaps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.pranavpandey.android.dynamic.toasts.DynamicToast

class LoginActivity : AppCompatActivity(),  View.OnClickListener {

    private val TAG = "LoginActivity"
    private val RC_SIGN_IN = 9001

    private lateinit var rootView: View
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val registerLinkTextView = findViewById<TextView>(R.id.register_link_textview)
        registerLinkTextView.setOnClickListener(this)

        val logInButton = findViewById<Button>(R.id.log_in_button)
        logInButton.setOnClickListener(this)

        val signInGoogleButton = findViewById<SignInButton>(R.id.sign_in_google_button)
        signInGoogleButton.setOnClickListener(this)

        rootView = findViewById(R.id.activity_login_rootview)
    }

    override fun onStart() {
        super.onStart()

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
                DynamicToast
                    .makeError(baseContext, "Не удалось авторизоваться: $e")
                    .show()
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
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun authWithGoogle(account: GoogleSignInAccount ) {
        val authCredential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth
            .signInWithCredential( authCredential )
            .addOnCompleteListener { task ->
                if( task.isSuccessful ) {
                    val firestore = FirebaseFirestore.getInstance()

                    val currentUser = firebaseAuth.currentUser
                    if( currentUser != null ) {
                        val currentUserDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUser.uid)
                        currentUserDocumentReference.get().addOnSuccessListener { documentSnapshot ->
                            if( documentSnapshot != null ) {
                                // There isn't any document for user, so let's create it
                                val currentUserId = currentUser.uid
                                val currentUserName = if(account.displayName != null) account.displayName!! else "User"

                                firestore
                                    .collection(getString(R.string.firestore_users_collection))
                                    .document(currentUserId)
                                    .set( UserInfo(currentUserName) )
                                    .addOnCompleteListener {
                                        updateUI(currentUser)
                                    }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                DynamicToast
                    .makeError(baseContext, "Авторизация не удалась: $e")
                    .show()
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
                    DynamicToast.makeError(baseContext, "Не удалось авторизироваться!")
                }
            }
    }

    private fun isEmailValid(email: String): Boolean =  android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun updateUI( currentUser: FirebaseUser? ) {
        if( currentUser != null ) {
            startActivity( Intent(baseContext, MainActivity::class.java) )
        }
    }

}
