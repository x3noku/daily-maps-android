package com.x3noku.dailymaps

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot

open class Task constructor(
    var text: String = "",
    var startTime: Int = 720,
    var duration: Int = 30,
    var priority: Int = 2,
    var coords: LatLng? = null,
    var completed: Boolean = false
) {

    var documentId: String? = null

    constructor(taskDocumentSnapshot: DocumentSnapshot?) : this() {
        val taskMap = taskDocumentSnapshot?.data

        taskMap?.let {
            this.documentId = taskDocumentSnapshot.id
            this.text = taskMap["text"] as String
            this.startTime = (taskMap["startTime"] as Long).toInt()
            this.duration = (taskMap["duration"] as Long).toInt()
            this.priority = (taskMap["priority"] as Long).toInt()
            this.coords = if( taskMap["coords"] != null ) {
                val coordsHashMap = taskMap["coords"] as HashMap<String, Double>
                LatLng( coordsHashMap["latitude"]!!, coordsHashMap["longitude"]!! )
            } else {
                null
            }
            this.completed = taskMap["completed"] as Boolean
        }
    }
}