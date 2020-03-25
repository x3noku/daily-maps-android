package com.x3noku.dailymaps

import android.util.Log

class FragmentTaskList(type: Int) {

    companion object {
        const val TAG = "FragmentTaskList"

        const val DELAYED_NONE = 9000
        const val DELAYED_LEFT = 9001
        const val DELAYED_RIGHT = 9002
        const val DELAYED_BOTH_SIDES = 9003

        const val LEFT = 7001
        const val RIGHT = 7002
    }

    var type: Int = type
        set(value) {
            if( value in DELAYED_NONE..DELAYED_BOTH_SIDES)
                field = value
        }
    private val _taskList: ArrayList<MarkedTask> = arrayListOf<MarkedTask>()
    val taskList: List<MarkedTask> get() = _taskList.toList()
    var limiterLeft: MarkedTask? = null
    var limiterRight: MarkedTask? = null

    fun addTask(task: MarkedTask) {
        if(task.priority > 0) {
            _taskList.add(task)
        }
        else {
            throw IllegalArgumentException("Task Priority Mustn't be Maximum")
        }
    }

    private fun findMaxPriority(fromIndex: Int = 0, toIndex: Int = this._taskList.size): Int {
        var maximumPriority = 0
        val taskListSlice = this._taskList.subList(fromIndex, toIndex)

        try {
            maximumPriority = taskListSlice.minBy { it.priority }!!.priority
        }
        catch (e: Exception) {
            Log.e(TAG, "Can't find min, Error type is ${e.javaClass}")
        }

        return maximumPriority
    }

    private fun countMaxPriority(fromIndex: Int = 0, toIndex: Int = this._taskList.size): Int {
        val taskListSlice = this._taskList.subList(fromIndex, toIndex)
        val maximumPriority = this.findMaxPriority(fromIndex, toIndex)

        return taskListSlice.count {
                task -> task.priority == maximumPriority
        }
    }

    private fun movePart(fromIndex: Int = 0, toIndex: Int = this._taskList.size, moveValue: Int, side: Int) {
        val taskListSlice = this._taskList.subList(fromIndex, toIndex)

        for(task in taskListSlice) {
            when(side) {
                LEFT -> task.startTime -= moveValue
                RIGHT -> task.startTime += moveValue
            }
        }
    }

    private fun resolveConflict(conflictValue: Int, rightBorderIndex: Int) {
        if( this.type in DELAYED_NONE..DELAYED_RIGHT ) {
            when {
                this.findMaxPriority(toIndex = rightBorderIndex) < this.findMaxPriority(fromIndex = rightBorderIndex) -> {
                    // MOVE LEFT PART ON @conflictValue TO THE LEFT
                    this.movePart(toIndex = rightBorderIndex, moveValue = conflictValue, side = LEFT)
                }
                this.findMaxPriority(toIndex = rightBorderIndex) == this.findMaxPriority(fromIndex = rightBorderIndex) -> {
                    // MOVE PARTS ON PARTS OF @conflictValue according to numbers of priorities

                    val sumReversed =
                        1/this.countMaxPriority(toIndex = rightBorderIndex) + 1/this.countMaxPriority(fromIndex = rightBorderIndex)

                    val leftMoveValue = ((1/this.countMaxPriority(toIndex = rightBorderIndex)) / sumReversed) * conflictValue
                    val rightMoveValue = ((1/this.countMaxPriority(fromIndex= rightBorderIndex)) / sumReversed) * conflictValue

                    this.movePart(toIndex = rightBorderIndex, moveValue = leftMoveValue, side = LEFT)
                    this.movePart(fromIndex = rightBorderIndex, moveValue = rightMoveValue, side = RIGHT)
                }
                this.findMaxPriority(toIndex = rightBorderIndex) < this.findMaxPriority(fromIndex = rightBorderIndex) -> {
                    // MOVE RIGHT PART ON @conflictValue TO THE RIGHT
                    this.movePart(fromIndex = rightBorderIndex, moveValue = conflictValue, side = RIGHT)
                }
            }
        }
        else {
            // ToDo: resolve conflict to delayed both sides fragment
        }
    }

    private fun moveAllTasks(conflictValue: Int) {
        when(this.type) {
            DELAYED_LEFT -> {
                this._taskList.first().startTime += conflictValue
                for(i in 0 until this._taskList.size) {
                    val leftTaskEndTime =
                        this._taskList[i].startTime + this._taskList[i].duration
                    val rightTaskStartTime =
                        this._taskList[i+1].startTime - this._taskList[i+1].routeTime
                    if(leftTaskEndTime > rightTaskStartTime) {
                        val confVal = leftTaskEndTime - rightTaskStartTime
                        this._taskList[i+1].startTime += confVal
                    }
                }
            }
            DELAYED_RIGHT -> {
                this._taskList.last().startTime -= conflictValue
                for(i in this._taskList.size downTo 1) {
                    val rightTaskStartTime =
                        this._taskList[i].startTime - this._taskList[i].routeTime
                    val leftTaskEndTime =
                        this._taskList[i-1].startTime + this._taskList[i-1].duration
                    if(leftTaskEndTime > rightTaskStartTime) {
                        val confVal = leftTaskEndTime - rightTaskStartTime
                        this._taskList[i-1].startTime -= confVal
                    }
                }
            }
        }
    }

    fun optimizeFragment() {
        for( (i, task) in this._taskList.withIndex() ) {
            if( i < this._taskList.size-1 ) {
                val leftTaskEndTime = task.startTime + task.duration
                val rightTaskStartTime = this._taskList[i+1].startTime - this._taskList[i+1].routeTime
                if( leftTaskEndTime > rightTaskStartTime ) {
                    val conflictValue = leftTaskEndTime - rightTaskStartTime
                    resolveConflict(conflictValue, i+1)
                }
            }
        }
        when(this.type) {
            DELAYED_LEFT -> {
                val limiterEndTime =
                    limiterLeft!!.startTime + limiterLeft!!.duration
                val boundaryTaskStartTime =
                    this._taskList.first().startTime - this._taskList.first().routeTime

                if(limiterEndTime > boundaryTaskStartTime) {
                    val conflictValue = limiterEndTime - boundaryTaskStartTime
                    moveAllTasks(conflictValue)
                }
            }
            DELAYED_RIGHT -> {
                val boundaryTaskEndTime =
                    this._taskList.last().startTime + this._taskList.last().duration
                val limiterStartTime =
                    limiterRight!!.startTime - limiterRight!!.routeTime

                if(boundaryTaskEndTime > limiterStartTime) {
                    val conflictValue = boundaryTaskEndTime- limiterStartTime
                    moveAllTasks(conflictValue)
                }
            }
        }
    }
}

fun List<MarkedTask?>.splitToFragments(): ArrayList<FragmentTaskList> {
    val taskList = this
    val fragmentList = arrayListOf<FragmentTaskList>()
    var fragmentTaskList = FragmentTaskList( FragmentTaskList.DELAYED_NONE )

    for( (index, task) in taskList.withIndex() ) {
        task!!
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