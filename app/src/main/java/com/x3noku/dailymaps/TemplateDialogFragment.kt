package com.x3noku.dailymaps

import android.content.DialogInterface
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.x3noku.dailymaps.utils.toDigitalView

class TemplateDialogFragment(val templateId: String, val type: Byte = OWN ) : DialogFragment(), PopupMenu.OnMenuItemClickListener {

    private var fragment: ProfileFragment? = null

    constructor(templateId: String, fragment: ProfileFragment) : this(templateId) {
        this.fragment = fragment
    }

    companion object {
        private const val TAG = "TemplateDialogFragment"

        const val OWN: Byte = 0
        const val SHARED: Byte = 1
    }

    private lateinit var rootView: View
    private lateinit var template: Template
    private lateinit var currentUserId: String

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
                currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

                rootView
                    .findViewById<TextView>(R.id.template_toolbar_title_text_view)
                    .text = template.text
                val templateTaskListLinearLayout =
                    rootView.findViewById<LinearLayout>(R.id.template_task_list_linear_layout)

                templateTaskListLinearLayout.removeAllViews()

                templateTaskListLinearLayout.buildTaskCards(template.taskIds)
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

        rootView.findViewById<Button>(R.id.template_build_route_button)
            .setOnClickListener {
                val intent = Intent(context, RouteActivity::class.java)
                intent.putExtra("templateId", templateId)
                startActivity(intent)
            }

        return rootView
    }

    private fun LinearLayout.buildTaskCards(taskIdList: List<String>) {
        val firestore = FirebaseFirestore.getInstance()
        val taskList = mutableListOf<Task>()

        for(taskId in taskIdList) {
            firestore
                .collection(context.getString(R.string.firestore_tasks_collection))
                .document(taskId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    taskList.add(Task(documentSnapshot))
                    if(taskList.size == taskIdList.size) {
                        val amountOfMarkedTask = taskList.count { it.coords != null }
                        if( amountOfMarkedTask > 0 )
                            rootView.findViewById<Button>(R.id.template_build_route_button).visibility = View.VISIBLE
                        else
                            rootView.findViewById<Button>(R.id.template_build_route_button).visibility = View.INVISIBLE

                        for(task in taskList.sortedBy { it.startTime }) {
                            val taskView = LayoutInflater.from(context).inflate(R.layout.task_layout, this, false)

                            val taskViewCheckBox = taskView.findViewById<CheckBox>(R.id.task_checkbox)
                            val taskViewPrimaryTextView = taskView.findViewById<TextView>(R.id.task_text_primary)
                            val taskViewSecondaryTextView = taskView.findViewById<TextView>(R.id.task_text_secondary)
                            val taskViewImageButton = taskView.findViewById<ImageButton>(R.id.task_image_button)

                            taskViewCheckBox.isChecked = task.completed
                            taskViewPrimaryTextView.text = task.text
                            taskViewSecondaryTextView.text = task.startTime.toDigitalView()
                            taskViewCheckBox.setOnClickListener {
                                firestore
                                    .collection(getString(R.string.firestore_tasks_collection))
                                    .document(task.documentId!!)
                                    .update("completed", taskViewCheckBox.isChecked)
                            }
                            taskViewImageButton.setOnClickListener( createBottomSheetListeners(task.documentId!!, templateId) )

                            addView(taskView)
                        }
                    }
                }
        }

        if( taskIdList.isEmpty() )
            rootView.findViewById<Button>(R.id.template_build_route_button).visibility = View.INVISIBLE
    }

    private fun createBottomSheetListeners(taskId: String, templateId: String): View.OnClickListener? =
        when(type) {
            OWN -> View.OnClickListener {
                val bottomSheetView = LayoutInflater.from(context)
                    .inflate(R.layout.task_own_bottom_sheet_layout, null, false)
                val bottomSheetDialog = BottomSheetDialog(context!!)
                bottomSheetDialog.setContentView(bottomSheetView)

                val shareOptionTextView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_share)
                val editOptionTextView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_edit)
                val deleteOptionTextView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_delete)

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
                    val addTask = AddTaskDialogFragment(taskId, templateId)
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
                val shareOptionTextView =
                    bottomSheetView.findViewById<TextView>(R.id.sheet_option_share)

                addToFavoriteOptionView.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    val addTask = AddTaskDialogFragment(taskId)
                    addTask.show(activity!!.supportFragmentManager, "AddTask")
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
                        .addCallback(DeleteSnackbarCallback(dialog, fragment!!))
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
                                            Toast
                                                .makeText(context, "Added template ${reference.id} to user ${currentUserId}", Toast.LENGTH_LONG)
                                                .show()
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        fragment?.fragmentManager?.beginTransaction()
            ?.detach(fragment!!)
            ?.attach(fragment!!)
            ?.commit()
    }
}