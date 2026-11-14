package com.vukos.trening.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Info po uzoru na Fitpass: veliki naslov, pa kratki pasusi sivim tekstom.
 * Bitne reci se pisu kao **ovako** i prikazuju se belim (bez boja, tacaka i ikona).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(
    title: String,
    paragraphs: List<String>,
    onDismiss: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 36.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            paragraphs.forEach { p ->
                Spacer(Modifier.height(16.dp))
                InfoParagraph(p)
            }
            extra()
        }
    }
}

/** Pasus: sivi tekst, delovi izmedju ** ** beli. */
@Composable
fun InfoParagraph(text: String, modifier: Modifier = Modifier) {
    Text(highlighted(text, MaterialTheme.colorScheme.onSurface), modifier,
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun highlighted(text: String, strong: Color) = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(color = strong)) { append(part) } else append(part)
    }
}
