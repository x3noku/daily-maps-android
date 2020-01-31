package com.x3noku.dailymaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class CreateTemplate :  DialogFragment() {

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

        }

        return rootView
    }
}