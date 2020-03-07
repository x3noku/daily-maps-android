package com.x3noku.dailymaps.utils

import android.os.AsyncTask

class doAsync(
    val handler: () -> Unit,
    val postAction: (() -> Unit)?
) : AsyncTask<Void, Void, Void>() {

    init {
        execute()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        postAction?.invoke()
    }
}