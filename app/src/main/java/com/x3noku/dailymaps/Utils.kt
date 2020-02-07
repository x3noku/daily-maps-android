package com.x3noku.dailymaps

const val HOURS = 0
const val MINUTES = 1

fun Int.toDigitalView(): String {
    val h = this.toHours()
    val m = this.toMinutes()
    return "$h:${if(m<10)"0$m" else "$m"}"
}

fun Int.toHours(): Int = this/60
fun Int.toMinutes(): Int = this%60