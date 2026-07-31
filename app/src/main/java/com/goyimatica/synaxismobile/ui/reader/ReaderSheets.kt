package com.goyimatica.synaxismobile.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.data.Mark
import com.goyimatica.synaxismobile.ui.theme.Syn

/**
 * The bar over a live selection. Four colours, a note, a copy, and a dismiss.
 * It is drawn by the reader only while a selection exists, so it has no state
 * of its own to get stuck in.
 */
@Composable
fun SelectionBar(
    onHighlight: (String) -> Unit,
    onNote: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Syn.colors
    val shape = RoundedCornerShape(50)

    Row(
        Modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(c.raised)
            .border(1.dp, c.rule, shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        MARK_CODES.forEach { code ->
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(markColor(c, code))
                    .border(1.dp, c.rule, CircleShape)
                    .clickable { onHighlight(code) },
            )
        }

        Box(Modifier.width(1.dp).height(22.dp).background(c.rule))

        Icon(
            Icons.Outlined.EditNote, "Add a note", tint = c.text,
            modifier = Modifier.size(22.dp).clickable { onNote() },
        )
        Icon(
            Icons.Outlined.ContentCopy, "Copy", tint = c.text,
            modifier = Modifier.size(18.dp).clickable { onCopy() },
        )
        Icon(
            Icons.Outlined.Close, "Dismiss", tint = c.faint,
            modifier = Modifier.size(18.dp).clickable { onDismiss() },
        )
    }
}

/** What opens when you click a highlight that already exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkSheet(
    mark: Mark,
    onRecolour: (String) -> Unit,
    onNote: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Syn.colors
    val sheetState = rememberModalBottomSheetState()

    /* Store keeps the note nullable - an unannotated highlight has no note at
       all rather than an empty one. Read it once, here, and the rest of the
       sheet can treat it as ordinary text. */
    val note = mark.note.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp),
        ) {
            Text(
                mark.summary().uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(markColor(c, mark.color).copy(alpha = if (c.isDark) 0.22f else 0.32f))
                    .padding(14.dp),
            ) {
                Text(
                    mark.text.trim(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = c.text,
                )
            }

            if (note.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("YOUR NOTE", style = MaterialTheme.typography.labelSmall, color = c.faint)
                Spacer(Modifier.height(7.dp))
                Text(note, style = MaterialTheme.typography.bodyMedium, color = c.dim)
            }

            Spacer(Modifier.height(22.dp))
            Text("COLOUR", style = MaterialTheme.typography.labelSmall, color = c.faint)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MARK_CODES.forEach { code ->
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(markColor(c, code))
                            .border(
                                if (code == mark.color) 2.dp else 1.dp,
                                if (code == mark.color) c.gold else c.rule,
                                CircleShape,
                            )
                            .clickable { onRecolour(code) },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetAction(
                    icon = { Icon(Icons.Outlined.EditNote, null, tint = c.text, modifier = Modifier.size(20.dp)) },
                    label = if (note.isBlank()) "Add a note" else "Edit the note",
                    modifier = Modifier.weight(1f),
                    onClick = onNote,
                )
                SheetAction(
                    icon = { Icon(Icons.Outlined.ContentCopy, null, tint = c.text, modifier = Modifier.size(17.dp)) },
                    label = "Copy",
                    modifier = Modifier.weight(1f),
                    onClick = onCopy,
                )
            }
            Spacer(Modifier.height(10.dp))
            SheetAction(
                icon = { Icon(Icons.Outlined.DeleteOutline, null, tint = c.blood, modifier = Modifier.size(19.dp)) },
                label = "Remove the highlight",
                tint = c.blood,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = Syn.colors.text,
    onClick: () -> Unit,
) {
    val c = Syn.colors
    Row(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(c.raised)
            .clickable { onClick() }
            .padding(vertical = 13.dp, horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(9.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

/** One dialog for both paths - a note on a fresh highlight, or on an old one. */
@Composable
fun NoteDialog(
    initial: String,
    quoted: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Syn.colors
    var value by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.dim,
        title = { Text("A note", style = MaterialTheme.typography.headlineSmall, color = c.text) },
        text = {
            Column {
                if (quoted.isNotBlank()) {
                    Text(
                        "\u201C" + quoted.trim().take(140) + (if (quoted.trim().length > 140) "\u2026" else "") + "\u201D",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = c.faint,
                    )
                    Spacer(Modifier.height(14.dp))
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("What struck you?", color = c.faint) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }) {
                Text("Save", color = c.gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.faint) }
        },
    )
}