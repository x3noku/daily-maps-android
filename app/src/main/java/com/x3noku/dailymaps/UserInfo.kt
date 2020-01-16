package com.x3noku.dailymaps

import com.google.firebase.firestore.DocumentSnapshot
import java.util.*

class UserInfo {
    var nickname: String = ""
    var taskIds: MutableList<String> = mutableListOf()
    var templateIds: MutableList<String> = mutableListOf()

    constructor(nickname: String = "User" ) {
        this.nickname = nickname
        this.taskIds = LinkedList()
        this.templateIds = LinkedList()
    }

    constructor(userInfoSnapshot: DocumentSnapshot) {
        val userInfoMap = userInfoSnapshot.data
        userInfoMap?.let {
            this.nickname = userInfoMap["nickname"] as String
            this.taskIds = userInfoMap["taskIds"] as MutableList<String>
            this.templateIds = userInfoMap["templateIds"] as MutableList<String>
        }
    }
}