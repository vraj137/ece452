package com.appetizers.spotra.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.appetizers.spotra.presentation.theme.SpotraBorder
import com.appetizers.spotra.presentation.theme.SpotraPrimaryContainer
import com.appetizers.spotra.presentation.theme.SpotraTextMuted

@Composable
fun SpotraLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(58.dp)
            .background(SpotraPrimaryContainer, RoundedCornerShape(17.dp))
            .border(1.dp, SpotraBorder, RoundedCornerShape(17.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LogoSquare(1f)
            LogoSquare(.55f)
        }
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LogoSquare(.55f)
            LogoSquare(.8f)
        }
    }
}

@Composable
private fun LogoSquare(alpha: Float) {
    Box(
        Modifier
            .size(15.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha), RoundedCornerShape(4.dp))
    )
}

@Composable
fun SpotraWordmark() {
    Row {
        Text("sp", style = MaterialTheme.typography.headlineLarge)
        Text("o", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("tra", style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .35f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        } else {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, SpotraBorder)
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SpotraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = SpotraTextMuted) },
            leadingIcon = leadingIcon?.let { icon ->
                { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) }
            },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = SpotraBorder
            )
        )
    }
}

@Composable
fun StepHeader(step: Int, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(SpotraPrimaryContainer, CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(step / 4f)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Row(
            modifier = Modifier
                .background(SpotraPrimaryContainer, CircleShape)
                .border(1.dp, SpotraBorder, CircleShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.background(Color.Transparent).width(76.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    Box(
                        Modifier
                            .size(if (index == step - 1) 14.dp else 6.dp, 6.dp)
                            .background(
                                if (index == step - 1) MaterialTheme.colorScheme.primary else SpotraBorder,
                                CircleShape
                            )
                    )
                    if (index < 3) Spacer(Modifier.width(4.dp))
                }
            }
            Text(
                "Step $step of 4",
                modifier = Modifier.width(90.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorMessage(message: String?) {
    message?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
}
