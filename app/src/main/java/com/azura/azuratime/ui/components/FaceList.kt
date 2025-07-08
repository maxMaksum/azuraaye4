package com.azura.azuratime.ui.face

import androidx.compose.runtime.Composable
import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.ui.components.AzuraLazyList

@Composable
fun FaceList(
    faces: List<FaceEntity>,
    onEdit: (FaceEntity) -> Unit,
    onEditPhoto: (FaceEntity) -> Unit,
    onDelete: (FaceEntity) -> Unit
) {
    AzuraLazyList(items = faces) { face ->
        FaceCard(
            face = face,
            onEdit = onEdit,
            onEditPhoto = onEditPhoto,
            onDelete = onDelete
        )
    }
}