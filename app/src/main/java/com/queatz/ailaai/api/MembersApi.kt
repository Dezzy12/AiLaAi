package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.Member
import com.queatz.ailaai.SuccessBlock
import io.ktor.http.*

suspend fun Api.createMember(
    member: Member,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Member> = {}
) = post("members", member, onError = onError, onSuccess = onSuccess)

suspend fun Api.updateMember(
    id: String,
    member: Member,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>
) = post("members/$id", member, onError = onError, onSuccess = onSuccess)

suspend fun Api.removeMember(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>
) = post("members/$id/delete", onError = onError, onSuccess = onSuccess)
