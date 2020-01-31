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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskList : Fragment() {

    private val TAG = "TaskList"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_task_list, container, false)

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
                    val taskListLinearLayout = rootView.findViewById<LinearLayout>(R.id.task_list_linear_layout)

                    taskListLinearLayout.buildTaskCards( userInfo.taskIds, currentUser.uid )
                    Log.d(TAG, "$userInfo")
                }
            }
        } ?: startActivity( Intent(context, LoginActivity::class.java) )

        return rootView
    }

    private fun LinearLayout.buildTaskCards(taskIdList: List<String>, userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        this.removeAllViews()

        for( taskId in taskIdList ) {
            firestore.collection(resources.getString(R.string.firestore_tasks_collection)).document(taskId).get().addOnSuccessListener { documentSnapshot ->
                val task = Task(documentSnapshot)
                val taskView = LayoutInflater.from(context).inflate(R.layout.task_layout, this, false)

                val taskViewCheckBox = taskView.findViewById<CheckBox>(R.id.task_checkbox)
                val taskViewPrimaryTextView = taskView.findViewById<TextView>(R.id.task_text_primary)
                val taskViewSecondaryTextView = taskView.findViewById<TextView>(R.id.task_text_secondary)
                val taskViewImageButton = taskView.findViewById<ImageButton>(R.id.task_image_button)

                taskViewPrimaryTextView.text = task.text
                taskViewSecondaryTextView.text = "12:00"
                taskViewImageButton.setOnClickListener {
                    val bottomSheetView = LayoutInflater.from(context)
                        .inflate(R.layout.task_bottom_sheet_layout, null)
                    val bottomSheetDialog = BottomSheetDialog(context)
                    bottomSheetDialog.setContentView(bottomSheetView)

                    val addToTemplateOptionTextView =
                        bottomSheetView.findViewById<TextView>(R.id.sheet_option_add_to_template)
                    val editOptionTextView =
                        bottomSheetView.findViewById<TextView>(R.id.sheet_option_edit)
                    val deleteOptionTextView =
                        bottomSheetView.findViewById<TextView>(R.id.sheet_option_delete)

                    addToTemplateOptionTextView.setOnClickListener {
                        bottomSheetDialog.dismiss()
                        val addToTemplate = AddToTemplate(userId, documentSnapshot.id)
                        addToTemplate.show(fragmentManager!!, "")
                    }
                    editOptionTextView.setOnClickListener {
                        bottomSheetDialog.dismiss()
                        val addTask = AddTask(taskId, bottomSheetDialog)
                        addTask.show(activity!!.supportFragmentManager, "AddTask")
                    }
                    deleteOptionTextView.setOnClickListener {
                        bottomSheetDialog.dismiss()
                        Snackbar
                            .make(rootView, "Задание будет удалено!", Snackbar.LENGTH_LONG)
                            .setAction("Отмена", ({}))
                            .setActionTextColor( ContextCompat.getColor(context!!, R.color.colorAccent) )
                            .addCallback( DeleteSnackbarCallback(taskId) )
                            .show()
                    }

                    bottomSheetDialog.show()
                }

                addView(taskView)
            }
        }
    }

}
