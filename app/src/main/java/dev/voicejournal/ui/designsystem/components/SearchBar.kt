package dev.voicejournal.ui.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    showLeadingIcon: Boolean = true,
    requestFocusOnLaunch: Boolean = false,
    onClose: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val colors = AppTheme.colors
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Maintain a local TextFieldValue to prevent cursor jumping during rapid typing
    var textFieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }

    // Sync external updates (e.g. cleared externally) to our local state
    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        if (requestFocusOnLaunch) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Focus requester might not be attached yet in some edge cases, ignore
            }
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onQueryChange(it.text)
        },
        placeholder = {
            Text(
                text = placeholder,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        singleLine = true,
        leadingIcon = if (showLeadingIcon) {
            {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        trailingIcon = {
            if (textFieldValue.text.isNotEmpty() || onClose != null) {
                IconButton(onClick = {
                    if (textFieldValue.text.isNotEmpty()) {
                        textFieldValue = textFieldValue.copy(text = "", selection = TextRange.Zero)
                        onQueryChange("")
                    } else {
                        onClose?.invoke()
                        keyboardController?.hide()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (textFieldValue.text.isNotEmpty()) "Clear search" else "Close search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedContainerColor = colors.surfaceVariant,
            unfocusedContainerColor = colors.surfaceVariant,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        shape = RoundedCornerShape(Radius.RadiusPill),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
            keyboardController?.hide()
        }),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) }
    )
}
