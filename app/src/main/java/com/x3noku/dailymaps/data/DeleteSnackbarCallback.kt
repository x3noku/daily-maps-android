package com.x3noku.dailymaps.data

import android.app.Dialog
import com.google.android.material.snackbar.Snackbar
import com.x3noku.dailymaps.fragments.ProfileFragment

class DeleteSnackbarCallback(private val dialog: Dialog?, private val fragment: ProfileFragment) : Snackbar.Callback() {

    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
        super.onDismissed(transientBottomBar, event)

        if(event == DISMISS_EVENT_TIMEOUT) {
            dialog?.dismiss()
            fragment.fragmentManager?.beginTransaction()?.detach(fragment)?.attach(fragment)?.commit()
        }
    }


}