package com.x3noku.dailymaps

import java.util.*

class UserInfo {
    var nickname: String
    var taskIds: MutableList<String>
    var templateIds: MutableList<String>

    constructor(nickname: String = "User" ) {
        this.nickname = nickname
        this.taskIds = LinkedList()
        this.templateIds = LinkedList()
    }
}