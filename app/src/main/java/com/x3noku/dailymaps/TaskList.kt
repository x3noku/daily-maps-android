package com.x3noku.dailymaps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskList : Fragment() {

    private val TAG = "TaskList"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_task_list, container, false)

        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        val currentUser = firebaseAuth.currentUser

        if( currentUser != null ) {
            val userDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUser.uid)
            userDocumentReference.addSnapshotListener { snapshot, e ->
                if(e != null) {
                    Log.w(TAG, "Listen failed with '${e.message}' exception!")
                }
                if( snapshot != null && snapshot.exists() ) {
                    val userInfo = snapshot.data

                    val taskListLinearLayout = rootView.findViewById<LinearLayout>(R.id.task_list_linear_layout)

                    Log.d(TAG, "$userInfo")
                }
            }
        }
        else {
            startActivity( Intent(context, LoginActivity::class.java) )
        }

        return rootView
    }

    private fun LinearLayout.buildTaskCards(taskList: List<Any>) {
        for( task in taskList ) {
            // ToDo: Realize card's building system
        }
    }

}
