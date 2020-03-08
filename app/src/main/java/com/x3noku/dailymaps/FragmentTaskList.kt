package com.x3noku.dailymaps

class FragmentTaskList(type: Int) {

    companion object {
        const val DELAYED_NONE = 9000
        const val DELAYED_LEFT = 9001
        const val DELAYED_RIGHT = 9002
        const val DELAYED_BOTH_SIDES = 9003
    }

    var type: Int = type
        set(value) {
            if( value in DELAYED_NONE..DELAYED_BOTH_SIDES)
                field = value
        }
    val taskList: ArrayList<Task> = arrayListOf<Task>()
    var limiterLeft: Task? = null
    var limiterRight: Task? = null

    fun addTask(task: Task) {
        if(task.priority > 0) {
            taskList.add(task)
        }
        else {
            throw IllegalArgumentException("Task Priority Mustn't be Maximum")
        }
    }

}

fun List<Task>.split(): ArrayList<FragmentTaskList> {
    val taskList = this
    val fragmentList = arrayListOf<FragmentTaskList>()
    var fragmentTaskList = FragmentTaskList( FragmentTaskList.DELAYED_NONE )

    for( (index, task) in taskList.withIndex() ) {
        if (task.priority > 0) {
            fragmentTaskList.addTask(task)
        }
        else {
            when(fragmentTaskList.type) {
                FragmentTaskList.DELAYED_NONE -> {
                    fragmentTaskList.type = FragmentTaskList.DELAYED_RIGHT
                    fragmentTaskList.limiterRight = task
                }
                FragmentTaskList.DELAYED_LEFT -> {
                    fragmentTaskList.type = FragmentTaskList.DELAYED_BOTH_SIDES
                    fragmentTaskList.limiterRight = task
                }
            }
            fragmentList.add(fragmentTaskList)
            fragmentTaskList = FragmentTaskList( FragmentTaskList.DELAYED_LEFT )
            fragmentTaskList.limiterLeft = task
        }

        if(index == taskList.size-1 && task.priority > 0)
            fragmentList.add(fragmentTaskList)
    }

    return fragmentList
}