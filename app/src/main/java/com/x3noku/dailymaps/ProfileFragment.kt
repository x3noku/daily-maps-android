package com.x3noku.dailymaps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    companion object {
        const val TAG = "Profile"
        private lateinit var rootview: View
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootview = inflater.inflate(R.layout.fragment_profile, container, false)

        return rootview
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        val firestore = FirebaseFirestore.getInstance()

        currentUser?.let {
            val userDocumentReference =
                firestore.collection(getString(R.string.firestore_users_collection))
                    .document(currentUser.uid)

            userDocumentReference
                .get()
                .addOnSuccessListener {
                    val userInfo = UserInfo(it)

                    for (templateId in userInfo.templateIds) {
                        val templateDocumentReference = FirebaseFirestore.getInstance()
                            .collection(getString(R.string.firestore_templates_collection))
                            .document(templateId)
                        templateDocumentReference.get().addOnSuccessListener { documentSnapshot ->
                            val template = Template(documentSnapshot)

                            val templateView =
                                layoutInflater.inflate(R.layout.profile_template_layout, null)

                            templateView
                                .findViewById<TextView>(R.id.template_text_primary)
                                .text = template.text

                            templateView
                                .findViewById<TextView>(R.id.template_text_secondary)
                                .text = "${template.taskIds.size} заданий"

                            val secondaryActionImageButton =
                                templateView
                                    .findViewById<ImageButton>(R.id.template_secondary_action_imagebutton)
                            secondaryActionImageButton.setOnClickListener {
                                val popup = PopupMenu(context, secondaryActionImageButton)
                                popup.setOnMenuItemClickListener { menuItem ->
                                    when (menuItem.itemId) {
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
                                                    startActivity(
                                                        Intent.createChooser(
                                                            i,
                                                            "Share via"
                                                        )
                                                    )
                                                }
                                                .addOnFailureListener {
                                                    Log.e("TaskList", it.toString(), it)
                                                }
                                            true
                                        }
                                        R.id.option_delete -> {
                                            FirebaseFirestore
                                                .getInstance()
                                                .collection("users")
                                                .document(currentUser.uid)
                                                .update(
                                                    "templateIds",
                                                    FieldValue.arrayRemove(templateId)
                                                )

                                            Snackbar
                                                .make(
                                                    rootview,
                                                    "Шаблон будет удален!",
                                                    Snackbar.LENGTH_LONG
                                                )
                                                .setAction("Отмена", ({ view ->
                                                    FirebaseFirestore
                                                        .getInstance()
                                                        .collection("users")
                                                        .document(currentUser.uid)
                                                        .update(
                                                            "templateIds",
                                                            FieldValue.arrayUnion(templateId)
                                                        )
                                                }))
                                                .setActionTextColor(
                                                    ContextCompat.getColor(
                                                        context!!,
                                                        R.color.colorAccent
                                                    )
                                                )
                                                .show()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                popup.menuInflater.inflate(R.menu.template_own_menu, popup.menu)
                                popup.show()
                            }

                            templateView.setOnClickListener {
                                TemplateDialogFragment(templateId).show(fragmentManager!!, "")
                            }

                            rootview
                                .findViewById<LinearLayout>(R.id.profile_template_list_linearlayout)
                                .addView(templateView)
                        }

                    }
                }
        }
        rootview
            .findViewById<ImageButton>(R.id.profile_toolbar_action_image_button)
            .setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
    }


    override fun onStop() {
        super.onStop()
    }
}