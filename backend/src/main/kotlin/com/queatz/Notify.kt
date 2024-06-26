package com.queatz

import com.queatz.api.ellipsize
import com.queatz.db.*
import com.queatz.db.Call
import com.queatz.plugins.db
import com.queatz.plugins.push
import com.queatz.push.*
import kotlinx.datetime.Clock
import java.util.logging.Logger

class Notify {
    fun trade(trade: Trade, person: Person, people: List<Person>? = null, event: TradeEvent) {
        val pushData = PushData(
            PushAction.Trade,
            TradePushData(
                trade = Trade().apply {
                    id = trade.id
                },
                people = people?.map {
                    Person().apply {
                        id = it.id
                        name = it.name
                    }
                },
                person = Person().apply {
                    id = person.id
                },
                event = event
            )
        )

        notifyPeople(trade.people!!, pushData)
    }

    fun reminder(reminder: Reminder, occurrence: ReminderOccurrence?) {
        val pushData = PushData(
            PushAction.Reminder,
            ReminderPushData(
                Reminder().apply {
                    id = reminder.id
                    person = reminder.person
                    attachment = reminder.attachment
                    title = reminder.title
                    note = reminder.note
                    start = reminder.start
                    end = reminder.end
                    timezone = reminder.timezone
                    utcOffset = reminder.utcOffset
                    schedule = reminder.schedule
                },
                occurrence?.let { occurrence ->
                    ReminderOccurrence(
                        note = occurrence.note,
                        done = occurrence.done
                    ).apply {
                        id = occurrence.id
                    }
                },
                show = occurrence?.done != true
            )
        )

        notifyPeople(listOf(reminder.person!!) + (reminder.people ?: emptyList()), pushData)
    }

    fun callStatus(group: Group, call: Call) {
        val pushData = PushData(
            PushAction.CallStatus,
            CallStatusPushData(
                Call().apply {
                    id = call.id
                    this.group = call.group
                    participants = call.participants
                }
            )
        )

        notifyGroupMembers(null, group, pushData)
    }

    fun call(group: Group, from: Person) {
        val pushData = PushData(
            PushAction.Call,
            CallPushData(
                Group().apply {
                    id = group.id
                    name = group.name
                },
                Person().apply {
                    name = from.name
                    id = from.id
                }
            )
        )

        notifyGroupMembers(from, group, pushData)
    }

    fun story(story: Story, authors: List<Person>, event: StoryEvent, subscribers: List<Person>) {
        val pushData = PushData(
            PushAction.Story,
            StoryPushData(
                story = Story().apply {
                    id = story.id
                    title = story.title
                },
                authors = authors.map { person ->
                    Person().apply {
                        id = person.id
                        name = person.name
                    }
                },
                event = event
            )
        )

        notifyPeople(subscribers.map { it.id!! }, pushData)
    }

    fun comment(story: Story, comment: Comment, person: Person) {
        val pushData = PushData(
            PushAction.Comment,
            CommentPushData(
                comment = Comment().apply {
                    id = comment.id
                    this.comment = comment.comment?.ellipsize()
                },
                story = Story().apply {
                    id = story.id
                    title = story.title
                },
                person = person
            )
        )

        notifyPeople(listOf(story.person!!), pushData)
    }

    fun commentReply(comment: Comment, onComment: Comment, story: Story?, person: Person) {
        val pushData = PushData(
            PushAction.CommentReply,
            CommentReplyPushData(
                comment = Comment().apply {
                    id = comment.id
                    this.comment = comment.comment?.ellipsize()
                },
                onComment = Comment().apply {
                    id = onComment.id
                    this.comment = onComment.comment?.ellipsize()
                },
                story = story?.let {
                    Story().apply {
                        id = story.id
                        title = story.title
                    }
                },
                person = person
            )
        )

        notifyPeople(listOf(onComment.from!!.asKey()), pushData)
    }

    fun message(group: Group, from: Person, message: Message) {
        val pushData = PushData(
            PushAction.Message,
            MessagePushData(
                Group().apply {
                    id = group.id
                    name = group.name
                },
                Person().apply {
                    name = from.name
                    id = from.id
                },
                message
            )
        )

        notifyGroupMembers(from, group, pushData)
    }

    fun newJoinRequest(person: Person, joinRequest: JoinRequest, group: Group) {
        val pushData = PushData(
            PushAction.JoinRequest,
            JoinRequestPushData(
                Person().apply {
                    name = person.name
                    id = person.id
                },
                Group().apply {
                    name = group.name
                    id = group.id!!
                },
                JoinRequest().apply {
                    id = joinRequest.id
                    message = joinRequest.message
                },
                JoinRequestEvent.Request
            )
        )

        notifyGroupMembers(person, group, pushData)
    }

    fun newMember(invitor: Person, person: Person, group: Group) {
        val pushData = PushData(
            PushAction.Group,
            GroupPushData(
                Person().apply {
                    name = person.name
                    id = person.id
                },
                Group().apply {
                    id = group.id
                    name = group.name
                },
                GroupEvent.Join,
                GroupEventData(
                    invitor = Person().apply {
                        name = invitor.name
                        id = invitor.id
                    },
                )
            )
        )

        notifyGroupMembers(invitor, group, pushData)
    }

    private fun notifyGroupMembers(from: Person?, group: Group, pushData: PushData) {
        db.memberDevices(group.id!!).apply {
            unhide()

            // Send push
            forEach {
                it.devices?.forEach { device ->
                    push.sendPush(
                        device,
                        pushData.show(it.member?.from != from?.id?.asId(Person::class) && it.member?.isSnoozedNow != true)
                    )
                }
            }
        }
    }

    private fun notifyPeople(people: List<String>, pushData: PushData) {
        db.peopleDevices(people).forEach { device ->
            push.sendPush(device, pushData)
        }
    }

    // Un-hide any groups
    private fun List<MemberDevice>.unhide() {
        filter { it.member?.hide == true }.forEach {
            it.member!!.hide = false
            db.update(it.member!!)
        }
    }
}

fun PushData.show(show: Boolean) = PushData(
    action,
    data?.let {
        when (it) {
            is MessagePushData -> it.copy(show = show)
            is CallPushData -> it.copy(show = show)
            else -> data
        }
    }
)

private val Member.isSnoozedNow get() = snoozed == true || snoozedUntil?.let { it > Clock.System.now() } == true
