package com.x3noku.dailymaps.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.x3noku.dailymaps.R
import com.x3noku.dailymaps.activities.LoginActivity
import com.x3noku.dailymaps.data.Task
import com.x3noku.dailymaps.data.UserInfo
import com.x3noku.dailymaps.utils.doAsync
import com.x3noku.dailymaps.utils.toDigitalView

class TaskListFragment : Fragment() {

    companion object {
        const val TAG = "TaskList"
    }

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
                    val userInfo =
                        UserInfo(snapshot)
                    val taskListLinearLayout =
                        rootView.findViewById<LinearLayout>(R.id.task_list_linear_layout)

                    taskListLinearLayout.removeAllViews()
                    doAsync(
                        handler = {
                            taskListLinearLayout
                                .buildTaskCards(userInfo.taskIds, currentUser.uid)
                        },
                        postAction = {
                            rootView
                                .findViewById<ProgressBar>(R.id.progressBar)
                                .visibility = View.GONE
                        }
                    )
                }
            }
        } ?: startActivity( Intent(context, LoginActivity::class.java) )

        return rootView
    }

    private fun LinearLayout.buildTaskCards(taskIdList: List<String>, userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        for( taskId in taskIdList ) {
            firestore
                .collection(resources.getString(R.string.firestore_tasks_collection))
                .document(taskId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val task =
                        Task(documentSnapshot)
                    val taskView =
                        LayoutInflater.from(context).inflate(R.layout.task_layout, this, false)

                    val taskViewCheckBox = taskView.findViewById<CheckBox>(R.id.task_checkbox)
                    val taskViewPrimaryTextView =
                        taskView.findViewById<TextView>(R.id.task_text_primary)
                    val taskViewSecondaryTextView =
                        taskView.findViewById<TextView>(R.id.task_text_secondary)
                    val taskViewImageButton =
                        taskView.findViewById<ImageButton>(R.id.task_image_button)

                    taskViewCheckBox.isChecked = task.completed
                    taskViewPrimaryTextView.text = task.text
                    taskViewSecondaryTextView.text = task.startTime.toDigitalView()
                    taskViewCheckBox.setOnClickListener {
                        firestore
                            .collection(getString(R.string.firestore_tasks_collection))
                            .document(taskId)
                            .update("completed", taskViewCheckBox.isChecked)
                    }
                    taskViewImageButton.setOnClickListener {
                        val bottomSheetView = LayoutInflater.from(context)
                            .inflate(R.layout.task_bottom_sheet_layout, null)
                        val bottomSheetDialog = BottomSheetDialog(context)
                        bottomSheetDialog.setContentView(bottomSheetView)

                        val addToTemplateOptionTextView =
                            bottomSheetView.findViewById<TextView>(R.id.sheet_option_add_to_template)
                        val shareOptionTextView =
                            bottomSheetView.findViewById<TextView>(R.id.sheet_option_share)
                        val editOptionTextView =
                            bottomSheetView.findViewById<TextView>(R.id.sheet_option_edit)
                        val deleteOptionTextView =
                            bottomSheetView.findViewById<TextView>(R.id.sheet_option_delete)

                        addToTemplateOptionTextView.setOnClickListener {
                            bottomSheetDialog.dismiss()
                            val addToTemplate =
                                AddToTemplateDialogFragment(
                                    userId,
                                    documentSnapshot.id
                                )
                            addToTemplate.show(fragmentManager!!, "")
                        }

                        shareOptionTextView.setOnClickListener {
                            FirebaseDynamicLinks
                                .getInstance()
                                .createDynamicLink()
                                .setLink(Uri.parse("https://dailymaps.h1n.ru/tasks/$taskId"))
                                .setDomainUriPrefix("https://dailymaps.page.link")
                                .setAndroidParameters(
                                    DynamicLink
                                        .AndroidParameters
                                        .Builder("com.x3noku.dailymaps")
                                        .build()
                                )
                                .buildShortDynamicLink()
                                .addOnSuccessListener {
                                    val dynamicLinkUri = it.shortLink.toString()
                                    val i = Intent(Intent.ACTION_SEND)
                                    i.type = "text/plain"
                                    i.putExtra(
                                        Intent.EXTRA_TEXT,
                                        dynamicLinkUri
                                    )
                                    startActivity(Intent.createChooser(i, "Share via"))
                                }
                                .addOnFailureListener {
                                    Log.e("TaskList", it.toString(), it)
                                }
                        }

                        editOptionTextView.setOnClickListener {
                            bottomSheetDialog.dismiss()
                            val addTask =
                                AddTaskDialogFragment(
                                    taskId
                                )
                            addTask.show(activity!!.supportFragmentManager, "AddTask")
                        }

                        deleteOptionTextView.setOnClickListener {
                            bottomSheetDialog.dismiss()
                            firestore
                                .collection("users")
                                .document(userId)
                                .update("taskIds", FieldValue.arrayRemove(taskId))

                            Snackbar
                                .make(rootView, "Задание будет удалено!", Snackbar.LENGTH_LONG)
                                .setAction("Отмена", ({
                                    firestore
                                        .collection("users")
                                        .document(userId)
                                        .update("taskIds", FieldValue.arrayUnion(taskId))
                                }))
                                .setActionTextColor(
                                    ContextCompat.getColor(
                                        context!!,
                                        R.color.colorAccent
                                    )
                                )
                                .show()
                        }

                        bottomSheetDialog.show()
                    }

                    addView(taskView)
                }
        }
    }

}
