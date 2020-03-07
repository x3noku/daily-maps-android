package com.x3noku.dailymaps.utils

fun Int.toDigitalView(): String {
    val h = this.toHours()
    val m = this.toMinutes()
    return "$h:${if(m<10)"0$m" else "$m"}"
}
fun Int.toTextView(): String {
    val hours = this.toHours()
    val minutes = this.toMinutes()

    val hoursString = when(hours) {
        0 -> ""
        1 -> "$hours час"
        in 2..4 -> "$hours часа"
        in 5..20 -> "$hours часов"
        else -> when(hours%10) {
            0 -> "$hours часов"
            1 -> "$hours час"
            in 2..4 -> "$hours часа"
            else -> "$hours часов"
        }
    }
    val minutesString = when(minutes) {
        0 -> "$minutes минут"
        1 -> "$minutes минута"
        in 2..4 -> "$minutes минуты"
        in 5..20 -> "$minutes минут"
        else -> when(minutes%10) {
            0 -> "$minutes минут"
            1 -> "$minutes минута"
            in 2..4 -> "$minutes минуты"
            else -> "$minutes минут"
        }
    }

    return when {
        hoursString.isNotBlank() -> hoursString + if(minutes == 0) "" else minutesString
        else -> minutesString
    }
}

fun Int.toHours(): Int = this/60
fun Int.toMinutes(): Int = this%60