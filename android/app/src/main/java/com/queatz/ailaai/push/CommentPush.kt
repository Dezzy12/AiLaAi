package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.CommentPushData

fun Push.receive(data: CommentPushData) {
    // Don't notify notifications from myself, allow content to refresh
    if (data.person?.id == meId) {
        return
    }

    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/story/${data.story!!.id!!}".toUri(),
        context,
        MainActivity::class.java
    )

    send(
        deeplinkIntent,
        Notifications.Comments,
        groupKey = "comments/${data.comment!!.id!!}",
        title = context.getString(R.string.x_commented_on_your_story, personNameOrYou(data.person, inline = false)),
        text = data.comment?.comment ?: ""
    )
}
