package com.x3noku.dailymaps

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : Fragment() {

    private val TAG = "Profile"



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        val firestore = FirebaseFirestore.getInstance()

        currentUser?.let {
            val userDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUser.uid)
            userDocumentReference.addSnapshotListener { snapshot, e ->
                e?.let {
                    Log.w(TAG, "Listen failed with '${e.message}' exception!")
                }
                if( snapshot != null && snapshot.exists() ) {
                    val userInfo = UserInfo(snapshot)

                    for(templateId in userInfo.templateIds) {
                        val templateDocumentReference = firestore.collection(getString(R.string.firestore_templates_collection)).document(templateId)
                        templateDocumentReference.get().addOnSuccessListener { documentSnapshot ->
                            val template = Template(documentSnapshot)

                            val templateView = layoutInflater.inflate(R.layout.profile_template_layout, null)

                            templateView
                                .findViewById<TextView>(R.id.template_text_primary)
                                .text = template.text

                            templateView
                                .findViewById<TextView>(R.id.template_text_secondary)
                                .text = "${template.taskIds.size} заданий"

                            rootView
                                .findViewById<LinearLayout>(R.id.profile_template_list_linearlayout)
                                .addView(templateView)
                        }
                    }
                }
            }
        }

        return rootView
    }
}