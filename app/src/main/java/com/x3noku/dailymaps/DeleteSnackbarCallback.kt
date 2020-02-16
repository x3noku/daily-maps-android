package com.x3noku.dailymaps

import android.app.Dialog
import com.google.android.material.snackbar.Snackbar

class DeleteSnackbarCallback(private val dialog: Dialog?) : Snackbar.Callback() {

    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
        super.onDismissed(transientBottomBar, event)

        if(event == DISMISS_EVENT_TIMEOUT)
            dialog?.dismiss()
    }

}