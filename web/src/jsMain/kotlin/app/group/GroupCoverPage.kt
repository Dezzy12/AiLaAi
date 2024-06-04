package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.FullPageLayout
import app.ailaai.api.group
import app.components.Empty
import appText
import application
import baseUrl
import com.queatz.db.GroupExtended
import components.Loading
import mainContent
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun GroupCoverPage(groupId: String, onGroupLoaded: (GroupExtended) -> Unit) {
    Style(AppStyles)

    val layout by application.layout.collectAsState()

    var isLoading by remember {
        mutableStateOf(true)
    }
    var isError by remember {
        mutableStateOf(false)
    }
    var group by remember {
        mutableStateOf<GroupExtended?>(null)
    }

    application.background(group?.group?.background?.let { "$baseUrl$it" })

    LaunchedEffect(groupId) {
        api.group(
            groupId,
            onError = {
                isError = true
            }
        ) {
            group = it
            onGroupLoaded(it)
        }
        isLoading = false
    }

    Div({
        mainContent(layout)
    }) {
        if (isError) {
            Empty {
                appText { groupNotFound }
            }
        } else if (isLoading) {
            Loading()
        } else if (group != null) {
            FullPageLayout {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(0.r, 1.r, 1.r, 1.r)
                    }
                }) {
                    GroupCover(group!!)
                }
            }
        }
    }
}
