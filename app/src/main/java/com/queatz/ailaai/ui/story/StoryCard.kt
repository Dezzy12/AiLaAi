package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCard(
    story: Story?,
    navController: NavController,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = {
            onClick()
        },
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PaddingDefault / 2),
            modifier = Modifier
                .padding(PaddingDefault * 2)
        ) {
            story?.let { story ->
                Text(
                    story.title?.notBlank ?: stringResource(R.string.empty_story_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                StoryAuthors(
                    navController,
                    story.publishDate,
                    story.authors ?: emptyList()
                )
                Text(story.textContent(), maxLines = 3, overflow = TextOverflow.Ellipsis)
            } ?: run {
                Text(
                    if (!isLoading) stringResource(R.string.story_not_found) else stringResource(R.string.please_wait),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }
}
