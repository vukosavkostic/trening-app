package com.vukos.trening.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.vukos.trening.data.Exercise

/** Sličica vežbe na beloj podlozi (slike iz baze imaju belu pozadinu). */
@Composable
fun ExerciseThumb(exercise: Exercise, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val url = exercise.thumbnailUrl
        if (url == null) {
            Icon(Icons.Filled.FitnessCenter, null, tint = Color.Gray)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = exercise.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { Icon(Icons.Filled.FitnessCenter, null, tint = Color.Gray) },
            )
        }
    }
}

@Composable
fun ComingSoon(title: String, text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "$title\n\n$text",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
