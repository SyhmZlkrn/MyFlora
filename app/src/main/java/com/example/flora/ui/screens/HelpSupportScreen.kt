package com.example.flora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.flora.data.api.FloraSupportApi
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type

private const val SUPPORT_EMAIL = "support@myfloraapp.com"

/**
 * Help & Support — submits the enquiry to the MyFlora backend
 * (forwarded server-side to [SUPPORT_EMAIL]) without opening an email app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { FloraSupportApi() }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bug report") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Space.lg),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Radii.card) {
                Column(modifier = Modifier.padding(Space.lg)) {
                    Text(
                        "Contact the maintainer",
                        fontSize = Type.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Palette.textPrimary,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Found a bug, need help with the app, or want to suggest a feature? Send a message " +
                            "directly from the app — it goes to $SUPPORT_EMAIL.",
                        fontSize = Type.bodySmall,
                        color = Palette.textSecondary,
                        lineHeight = TextUnit(20f, TextUnitType.Sp),
                    )
                    Spacer(Modifier.height(Space.lg))

                    LabeledField("Your name", name, "Jane Doe") { name = it }
                    LabeledField("Your email", email, "you@example.com", keyboardType = androidx.compose.ui.text.input.KeyboardType.Email) { email = it }
                    CategoryRow(category) { category = it }
                    LabeledField(
                        "What's going on?",
                        message,
                        "Describe the issue, what you expected, and any steps to reproduce.",
                        singleLine = false,
                        minLines = 5,
                    ) { message = it }

                    if (error != null) {
                        Spacer(Modifier.height(Space.xs))
                        Text(error!!, color = Palette.danger, fontSize = Type.caption)
                    }
                    if (sent) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "Message sent. We'll get back to you within 2-3 business days.",
                            color = Palette.accent,
                            fontSize = Type.caption,
                        )
                    }

                    Spacer(Modifier.height(Space.lg))
                    Button(
                        onClick = {
                            error = when {
                                name.isBlank() -> "Please enter your name."
                                email.isBlank() || "@" !in email -> "Please enter a valid email."
                                message.isBlank() -> "Please describe your enquiry."
                                else -> null
                            }
                            if (error != null) return@Button
                            sending = true
                            sent = false
                            scope.launch {
                                when (val r = api.sendMessage(name, email, category, message)) {
                                    is FloraSupportApi.Result.Success -> {
                                        sent = true
                                        // Clear form on success.
                                        message = ""
                                    }
                                    is FloraSupportApi.Result.Failure -> {
                                        error = "Couldn't send: ${r.reason}. Please try again later."
                                    }
                                }
                                sending = false
                            }
                        },
                        enabled = !sending,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(Radii.chip + 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.accent),
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(Space.sm))
                            Text("Sending…", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black)
                            Spacer(Modifier.width(Space.sm))
                            Text("Send message", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.lg))

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Radii.card) {
                Column(modifier = Modifier.padding(Space.lg)) {
                    Text("Common questions", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                    Spacer(Modifier.height(Space.sm))
                    FaqRow("Why is identification offline?", "MyFlora ships an on-device PlantNet-300K model so you can ID plants without an internet connection.")
                    FaqRow("How are watering intervals chosen?", "If the plant isn't in our curated DB, MyFlora picks a sensible default based on genus (e.g. Hibiscus every 2 days, Lavender every 10).")
                    FaqRow("Why does my plant stay in the schedule after I tick it?", "Tasks auto-renew — ticking advances the next due date by the plant's watering interval, then it reappears in the relevant date group.")
                    FaqRow("Can I see watering history?", "Yes — open a plant from My Plants and scroll to health logs. Each tick is recorded.")
                }
            }

            Spacer(Modifier.height(Space.xl))
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Text(label, fontSize = Type.caption, color = Palette.textSecondary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(Space.xs))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Palette.textTertiary, fontSize = Type.bodySmall) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Palette.textPrimary,
            unfocusedTextColor = Palette.textPrimary,
            focusedBorderColor = Palette.accent,
            unfocusedBorderColor = Palette.surfaceTintStrong,
            cursorColor = Palette.accent,
        ),
    )
    Spacer(Modifier.height(Space.md))
}

@Composable
private fun CategoryRow(current: String, onChange: (String) -> Unit) {
    Text("Category", fontSize = Type.caption, color = Palette.textSecondary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(Space.xs))
    val options = listOf("Bug report", "Feature request", "Plant care help", "Other")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        options.forEach { opt ->
            val selected = opt == current
            Button(
                onClick = { onChange(opt) },
                shape = RoundedCornerShape(Radii.chip),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Palette.accent else Palette.surfaceTintSoft,
                    contentColor = if (selected) Color.Black else Palette.textPrimary,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.sm, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(opt, fontSize = Type.micro, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Spacer(Modifier.height(Space.md))
}

@Composable
private fun FaqRow(q: String, a: String) {
    Spacer(Modifier.height(Space.sm))
    Text(q, fontSize = Type.bodySmall, fontWeight = FontWeight.SemiBold, color = Palette.textPrimary)
    Text(a, fontSize = Type.caption, color = Palette.textTertiary, lineHeight = TextUnit(18f, TextUnitType.Sp))
}
