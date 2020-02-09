package com.x3noku.dailymaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateTemplateDialogFragment(private val currentUserId: String, private val editableTaskId: String) :  DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.FullWidthDialogStyle)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_create_template, container, false)

        rootView.findViewById<TextView>(R.id.template_cancel_textview).setOnClickListener {
            dismiss()
        }
        rootView.findViewById<TextView>(R.id.template_save_textview).setOnClickListener {
            val templateName = rootView.findViewById<EditText>(R.id.template_create_edittext).text.toString()

            if( templateName.isNotBlank() ) {
                val template = Template(templateName, currentUserId, editableTaskId)

                val firestore = FirebaseFirestore.getInstance()
                val userDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUserId)
                firestore
                    .collection(resources.getString(R.string.firestore_templates_collection))
                    .add(template)
                    .addOnSuccessListener { documentReference ->
                        userDocumentReference
                            .update("templateIds", FieldValue.arrayUnion(documentReference.id) )
                            .addOnSuccessListener {
                                dismiss()
                            }
                    }
            }
            else
                Toast
                    .makeText(context!!, "Пожалуйста, назовите шаблон!", Toast.LENGTH_SHORT)
                    .show()

        }

        return rootView
    }
}