package com.x3noku.dailymaps.data

import com.google.firebase.firestore.DocumentSnapshot

class Template() {
    var text: String = ""
    var ownerId: String = ""
    var taskIds: MutableList<String> = mutableListOf()

    constructor(text: String, ownerId: String, taskId: String) : this() {
        this.text = text
        this.ownerId = ownerId
        this.taskIds.add(taskId)
    }

    constructor(templateSnapshot: DocumentSnapshot) : this() {
        val templateMap = templateSnapshot.data
        templateMap?.let {
            this.text = templateMap["text"] as String
            this.ownerId = templateMap["ownerId"] as String
            this.taskIds = templateMap["taskIds"] as MutableList<String>
        }
    }

}