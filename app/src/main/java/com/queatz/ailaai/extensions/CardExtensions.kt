package com.queatz.ailaai.extensions

import com.queatz.ailaai.*

val Card.url get() = "$appDomain/card/$id"

suspend fun Card.reply(conversation: List<String>, onSuccess: (groupId: String) -> Unit = {}) {
    try {
        val groupId = api.cardGroup(id!!).id!!
        api.sendMessage(
            groupId,
            Message(
                text = conversation.filterNotBlank().ifNotEmpty?.joinToString(" → "),
                attachment = gson.toJson(CardAttachment(id!!))
            )
        )
        onSuccess(groupId)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}