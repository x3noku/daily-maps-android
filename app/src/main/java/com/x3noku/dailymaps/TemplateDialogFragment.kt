package com.x3noku.dailymaps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TemplateDialogFragment(val templateId: String, val type: Byte = OWN ) : DialogFragment(), PopupMenu.OnMenuItemClickListener {

    companion object {
        private lateinit var rootView: View
        private lateinit var template: Template
        private lateinit var currentUserId: String
        private const val TAG = "TemplateDialogFragment"
        const val OWN: Byte = 0
        const val SHARED: Byte = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        val dialogFragment = dialog
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialogFragment?.window?.setLayout(width, height)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_template, container, false)

        val firestore = FirebaseFirestore.getInstance()
        val templateDocumentReference =
            firestore.collection(getString(R.string.firestore_templates_collection)).document(templateId)
        templateDocumentReference.addSnapshotListener { snapshot, e ->
            e?.let {
                Log.w(TAG, "Listen failed with '${e.message}' exception!")
            }
            if( snapshot != null && snapshot.exists() ) {
                template = Template(snapshot)
                currentUserId = template.ownerId

                rootView
                    .findViewById<TextView>(R.id.template_toolbar_title_text_view)
                    .text = template.text
                val templateTaskListLinearLayout =
                    rootView.findViewById<LinearLayout>(R.id.template_task_list_linear_layout)
                templateTaskListLinearLayout.buildTaskCards( template.taskIds, template.ownerId )
            }
        }

        val secondaryActionImageButton =
            rootView.findViewById<ImageButton>(R.id.template_toolbar_action_image_button)
        when(type) {
            OWN -> {
                secondaryActionImageButton.setOnClickListener {
                    val popup = PopupMenu(context, secondaryActionImageButton)
                    popup.setOnMenuItemClickListener(this)
                    popup.menuInflater.inflate(R.menu.template_own_menu, popup.menu)
                    popup.show()
                }
            }
            SHARED -> {
                secondaryActionImageButton.setOnClickListener {
                    val popup = PopupMenu(context, secondaryActionImageButton)
                    popup.setOnMenuItemClickListener(this)
                    popup.menuInflater.inflate(R.menu.template_shared_menu, popup.menu)
                    popup.show()
                }
            }
        }

        rootView
            .findViewById<Toolbar>(R.id.template_toolbar)
            .setNavigationOnClickListener {
                dismiss()
            }

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
                taskViewSecondaryTextView.text = task.startTime.toDigitalView()

                taskViewImageButton.setOnClickListener( createBottomSheetListeners(taskId, templateId, userId) )

                addView(taskView)
            }
        }
    }

    private fun createBottomSheetListeners(taskId: String, templateId: String, userId: String): View.OnClickListener? =
        when(type) {
            OWN -> View.OnClickListener {
                val bottomSheetView = LayoutInflater.from(context)
                    .inflate(R.layout.task_bottom_sheet_layout, null, false)
                val bottomSheetDialog = BottomSheetDialog(context!!)
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
                    val addToTemplate = AddToTemplateDialogFragment(userId, taskId)
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
                    val addTask = AddTaskDialogFragment(taskId)
                    addTask.show(activity!!.supportFragmentManager, "AddTask")
                }
                deleteOptionTextView.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    FirebaseFirestore
                        .getInstance()
                        .collection("templates")
                        .document(templateId)
                        .update("taskIds", FieldValue.arrayRemove(taskId) )

                    Snackbar
                        .make(rootView, "Задание будет удалено из шаблона!", Snackbar.LENGTH_LONG)
                        .setAction("Отмена", ({
                            FirebaseFirestore
                                .getInstance()
                                .collection("templates")
                                .document(templateId)
                                .update("taskIds", FieldValue.arrayUnion(taskId) )
                        }))
                        .setActionTextColor( ContextCompat.getColor(context!!, R.color.colorAccent) )
                        .show()
                }
                bottomSheetDialog.show()
            }
            SHARED -> View.OnClickListener{
                val bottomSheetView = LayoutInflater.from(context)
                    .inflate(R.layout.task_shared_bottom_sheet_layout, null, false)
                val bottomSheetDialog = BottomSheetDialog(context!!)
                bottomSheetDialog.setContentView(bottomSheetView)

                val addToFavoriteOptionView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_add_to_favorite)
                val addToTemplateOptionTextView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_add_to_template)
                val shareOptionTextView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_share)

                addToFavoriteOptionView.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    val addTask = AddTaskDialogFragment(taskId)
                    addTask.show(activity!!.supportFragmentManager, "AddTask")
                }
                addToTemplateOptionTextView.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    val addToTemplate = AddToTemplateDialogFragment(userId, taskId)
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
                bottomSheetDialog.show()
            }
            else -> null
        }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        item?.let {
            when (item.itemId) {
                R.id.option_share -> {
                    Toast.makeText(context!!, "fbahbfhsa", Toast.LENGTH_LONG).show()
                    FirebaseDynamicLinks
                        .getInstance()
                        .createDynamicLink()
                        .setLink(Uri.parse("https://dailymaps.h1n.ru/templates/$templateId"))
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
                    return true
                }
                R.id.option_delete -> {
                    FirebaseFirestore
                        .getInstance()
                        .collection("users")
                        .document(currentUserId)
                        .update("templateIds", FieldValue.arrayRemove(templateId))

                    Snackbar
                        .make(rootView, "Шаблон будет удален!", Snackbar.LENGTH_LONG)
                        .setAction("Отмена", ({
                            FirebaseFirestore
                                .getInstance()
                                .collection("users")
                                .document(currentUserId)
                                .update("templateIds", FieldValue.arrayUnion(templateId))
                        }))
                        .setActionTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
                        .setCallback(DeleteSnackbarCallback(dialog))
                        .show()
                    return true
                }
                R.id.option_add -> {
                    FirebaseFirestore
                        .getInstance()
                        .collection(getString(R.string.firestore_templates_collection))
                        .document(templateId)
                        .get()
                        .addOnSuccessListener {
                            val template = Template(it)
                            template.ownerId = currentUserId

                            FirebaseFirestore
                                .getInstance()
                                .collection(getString(R.string.firestore_templates_collection))
                                .add(template)
                                .addOnSuccessListener { reference ->
                                    FirebaseFirestore
                                        .getInstance()
                                        .collection(getString(R.string.firestore_users_collection))
                                        .document(currentUserId)
                                        .update("templateIds", FieldValue.arrayUnion(reference.id))
                                        .addOnSuccessListener {
                                            dismiss()
                                        }
                                }
                        }
                }
                else -> return super.onOptionsItemSelected(item)
            }
        }
        return false
    }
}