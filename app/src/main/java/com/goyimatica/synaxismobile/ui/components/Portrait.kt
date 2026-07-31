package com.goyimatica.synaxismobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.goyimatica.synaxismobile.data.WikiRepo
import com.goyimatica.synaxismobile.ui.theme.Syn

/*
 * A saint's face, wherever a saint is listed.
 *
 * WikiRepo.thumbs is a snapshot state map held in memory, so this costs a
 * hash lookup and nothing else - safe to call from inside a LazyColumn item.
 * When the sync writes a picture for a saint already on screen, that one row
 * re-draws itself and the rest of the list is untouched.
 *
 * No picture yet: the medallion, exactly as before, so a list never has a
 * hole in it while the synaxarion is still downloading.
 */
@Composable
fun SaintPortrait(
    saintId: String,
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    corner: Dp = 14.dp,
) {
    val c = Syn.colors
    val url = WikiRepo.thumbs[saintId]
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(c.raised)
            .border(1.dp, c.goldDim.copy(alpha = 0.45f), shape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = c.gold,
            )
        } else {
            val inner = RoundedCornerShape(corner - 2.dp)
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size - 4.dp)
                    .clip(inner)
                    .background(c.surface)
                    .border(1.dp, c.rule, inner),
            )
        }
    }
}