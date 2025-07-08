package com.azura.azuratime.ui.face

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.ui.components.AzuraFormField

@Composable
fun EditFaceDialog(
    face: FaceEntity,
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Edit Face Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AzuraFormField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Name",
                    isError = name.isBlank(),
                    helperText = if (name.isBlank()) "Name is required" else null
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}