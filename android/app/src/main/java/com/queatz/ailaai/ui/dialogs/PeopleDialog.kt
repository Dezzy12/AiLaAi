package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.PersonMember
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person

@Composable
fun PeopleDialog(
    title: String,
    onDismissRequest: () -> Unit,
    people: List<Person>,
    showCountInTitle: Boolean = true,
    infoFormatter: (Person) -> String? = { null },
    actions: @Composable RowScope.() -> Unit = {},
    extraButtons: @Composable RowScope.() -> Unit = {},
    onClick: (Person) -> Unit,
) {
    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    if (showCountInTitle) "$title (${people.size})" else title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 1.pad)
                )
                actions()
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                items(people, key = { it.id!! }) {
                    PersonMember(it, infoFormatter = infoFormatter) { onClick(it) }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                extraButtons()
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
