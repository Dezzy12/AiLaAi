package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.updateGroup
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Group
import com.queatz.db.GroupConfig
import com.queatz.db.GroupEditsConfig
import com.queatz.db.GroupMessagesConfig
import kotlinx.coroutines.launch

@Composable
fun GroupSettingsDialog(
    onDismissRequest: () -> Unit,
    group: Group
) {
    var groupConfig by remember(group) { mutableStateOf(group.config ?: GroupConfig()) }
    val recomposeScope = currentRecomposeScope
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)
    var isModified by rememberStateOf(false)

    fun modified() {
        recomposeScope.invalidate()
        isModified = true
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = true,
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(bottom = 1.pad)
                        )
                    }
                    Text(
                        stringResource(R.string.who_sends_messages_to_this_group),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = groupConfig.messages == GroupMessagesConfig.Hosts,
                            onClick = {
                                groupConfig.messages = GroupMessagesConfig.Hosts
                                modified()
                            }
                        )
                        Column(
                            Modifier.clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {
                                groupConfig.messages = GroupMessagesConfig.Hosts
                                modified()
                            }
                        ) {
                            Text(stringResource(R.string.hosts))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = groupConfig.messages == null,
                            onClick = {
                                groupConfig.messages = null
                                modified()
                            }
                        )
                        Column(
                            Modifier.clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {
                                groupConfig.messages = null
                                modified()
                            }
                        ) {
                            Text(stringResource(R.string.everyone))
                        }
                    }
                    Column {
                        Text(
                            stringResource(R.string.who_edits_this_group),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.name_introduction_photo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = groupConfig.edits == GroupEditsConfig.Hosts,
                            onClick = {
                                groupConfig.edits = GroupEditsConfig.Hosts
                                modified()
                            }
                        )
                        Column(
                            Modifier.clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {
                                groupConfig.edits = GroupEditsConfig.Hosts
                                modified()
                            }
                        ) {
                            Text(stringResource(R.string.hosts))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = groupConfig.edits == null,
                            onClick = {
                                groupConfig.edits = null
                                modified()
                            }
                        )
                        Column(
                            Modifier.clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {
                                groupConfig.edits = null
                                modified()
                            }
                        ) {
                            Text(stringResource(R.string.everyone))
                        }
                    }
                }
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                Button(
                    {
                        isLoading = true
                        scope.launch {
                            api.updateGroup(group.id!!, Group(config = groupConfig)) {
                                onDismissRequest()
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && isModified
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        )
    }
}
