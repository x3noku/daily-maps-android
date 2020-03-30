package com.x3noku.dailymaps

import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.x3noku.dailymaps.utils.toDigitalView
import com.x3noku.dailymaps.utils.toTextView

class MarkedTask(task: Task, val routeTime: Int) : Task(
    task.text,
    task.startTime,
    task.duration,
    task.priority,
    task.coords,
    task.completed
) {

    constructor(markedTask: MarkedTask) : this(markedTask, markedTask.routeTime)

    fun toMarkerTitle(): String {
        val firstPart = if(text.length > 20) "${text.substring(0..20)}..." else text
        val secondPart = startTime.toDigitalView()
        val thirdPart = duration.toTextView()

        return "$firstPart в $secondPart за $thirdPart"
    }

    fun drawPoint(googleMap: GoogleMap) {
        Handler(Looper.getMainLooper()).post {
            googleMap.addMarker(
                MarkerOptions()
                    .position(this.coords!!)
                    .title( this.toMarkerTitle() )
            )
        }
    }

}