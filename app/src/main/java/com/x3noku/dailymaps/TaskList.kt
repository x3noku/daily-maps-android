package com.x3noku.dailymaps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskList : Fragment() {

    private val TAG = "TaskList"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
                    val userInfo = UserInfo(snapshot)
                    val taskListLinearLayout = rootView.findViewById<LinearLayout>(R.id.task_list_linear_layout)

                    taskListLinearLayout.buildTaskCards( userInfo.taskIds )
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
            val taskView = LayoutInflater.from(context).inflate(R.layout.task_layout, this, false)

            val taskViewCheckBox = taskView.findViewById<CheckBox>(R.id.task_checkbox)
            val taskViewPrimaryTextView = taskView.findViewById<TextView>(R.id.task_text_primary)
            val taskViewSecondaryTextView = taskView.findViewById<TextView>(R.id.task_text_secondary)
            val taskViewImageButton = taskView.findViewById<ImageButton>(R.id.task_image_button)

            taskViewPrimaryTextView.text = "Hello World!"
            taskViewSecondaryTextView.text = "Hello World!"
            taskViewImageButton.setOnClickListener {
                val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.task_bottom_sheet_layout, null)
                val bottomSheetDialog = BottomSheetDialog(context)
                bottomSheetDialog.setContentView(bottomSheetView)
                bottomSheetDialog.show()
            }

            addView(taskView)
        }
    }

}
