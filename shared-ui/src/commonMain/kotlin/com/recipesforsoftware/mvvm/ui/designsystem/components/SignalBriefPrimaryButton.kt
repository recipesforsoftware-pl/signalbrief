package com.recipesforsoftware.mvvm.ui.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefShapes
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefSpacing

/**
 * Shared primary CTA button used on onboarding and other key actions.
 *
 * Fills the available width, respects a minimum touch target, and uses the
 * theme's primary container colors for high contrast.
 */
@Composable
fun SignalBriefPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { role = Role.Button },
        shape = SignalBriefShapes.medium,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = SignalBriefSpacing.xs),
        )
    }
}
