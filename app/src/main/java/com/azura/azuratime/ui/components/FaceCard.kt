package com.azura.azuratime.ui.face

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.ui.components.AzuraAvatar
import com.azura.azuratime.ui.components.AzuraCard

@Composable
fun FaceCard(
    face: FaceEntity,
    onEdit: (FaceEntity) -> Unit,
    onEditPhoto: (FaceEntity) -> Unit,
    onDelete: (FaceEntity) -> Unit
) {
    AzuraCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzuraAvatar(photoPath = face.photoUrl, size = 64)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = face.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${face.studentId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = { onEdit(face) }) { Text("Quick Edit") }
            TextButton(onClick = { onEditPhoto(face) }) { Text("Edit + Photo") }
            TextButton(onClick = { onDelete(face) }) { Text("Delete") }
        }
    }
}