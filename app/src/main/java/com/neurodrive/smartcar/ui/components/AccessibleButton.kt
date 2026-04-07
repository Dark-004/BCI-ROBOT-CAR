package com.neurodrive.smartcar.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,  // ← no default value; callers must provide
    contentDescription: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(text)
    }
}