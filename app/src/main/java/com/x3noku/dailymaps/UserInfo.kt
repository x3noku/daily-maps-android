package com.x3noku.dailymaps

import com.google.firebase.firestore.DocumentSnapshot
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

    constructor(userInfoSnapshot: DocumentSnapshot) {
        val userInfoMap = userInfoSnapshot.data

        this.nickname = if (userInfoMap != null) userInfoMap["nickname"] as String else ""
        this.taskIds = if (userInfoMap != null) userInfoMap["taskIds"] as MutableList<String> else mutableListOf()
        this.templateIds = if (userInfoMap != null) userInfoMap["templateIds"] as MutableList<String> else mutableListOf()
    }
}