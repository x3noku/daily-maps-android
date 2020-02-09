package com.x3noku.dailymaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class AddToTemplateDialogFragment(private val currentUserId: String, private val editableTaskId: String) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_add_to_template, container, false)

        val firestore = FirebaseFirestore.getInstance()
        val userDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUserId)
        userDocumentReference.get().addOnSuccessListener { snapshot ->
            val userInfo = UserInfo(snapshot)

            for(templateId in userInfo.templateIds) {
                val templateDocumentReference = firestore.collection(getString(R.string.firestore_templates_collection)).document(templateId)
                templateDocumentReference.get().addOnSuccessListener { documentSnapshot ->
                    val template = Template(documentSnapshot)

                    val templateView = layoutInflater.inflate(R.layout.template_layout, null)
                    templateView.findViewById<TextView>(R.id.template_primary_textview).text = template.text
                    templateView.findViewById<TextView>(R.id.template_secondary_textview).text = "${template.taskIds.size} заданий"

                    templateView.setOnClickListener {
                        templateDocumentReference
                            .update("taskIds", FieldValue.arrayUnion(editableTaskId) )
                            .addOnSuccessListener {
                                dismiss()
                            }
                    }

                    rootView.findViewById<LinearLayout>(R.id.tempalte_list_linearlayout).addView(templateView)
                }
            }
        }

        rootView.findViewById<TextView>(R.id.template_create_new_textview).setOnClickListener {
            dismiss()
            val createTemplate = CreateTemplateDialogFragment(currentUserId, editableTaskId)
            createTemplate.show(fragmentManager!!, "")
        }
        rootView.findViewById<TextView>(R.id.template_cancel_textview).setOnClickListener {
            dismiss()
        }

        return rootView
    }
}