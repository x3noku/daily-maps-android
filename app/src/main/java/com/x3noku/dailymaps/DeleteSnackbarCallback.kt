package com.x3noku.dailymaps

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DeleteSnackbarCallback( val documentId: String ) : Snackbar.Callback() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onShown(sb: Snackbar?) {
        super.onShown(sb)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = firebaseAuth.currentUser!!
    }

    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
        super.onDismissed(transientBottomBar, event)

        if( event == DISMISS_EVENT_TIMEOUT ) {
            firestore
                .collection("tasks")
                .document(documentId)
                .delete()
                .addOnSuccessListener {
                    val userDocumentReference = firestore.collection("users").document(currentUser.uid)
                    userDocumentReference.update("taskIds", FieldValue.arrayRemove(documentId))
                }
        }
    }
}