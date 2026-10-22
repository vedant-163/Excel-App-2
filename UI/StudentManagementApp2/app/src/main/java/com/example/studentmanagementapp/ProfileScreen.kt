package com.example.studentmanagementapp

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProfileBlue = Color(0xFF1565C0)
private val ProfileInk = Color(0xFF172033)
private val ProfileMuted = Color(0xFF667085)
private val ProfileBackground = Color(0xFFF5F7FB)
private val ProfileBorder = Color(0xFFE2E8F0)

@Composable
fun ProfileScreen(
    username: String,
    role: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showDisplayNameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val profilePrefs = remember { context.getSharedPreferences("staff_profile_preferences", android.content.Context.MODE_PRIVATE) }
    var displayName by remember { mutableStateOf(profilePrefs.getString("display_name", username).orEmpty()) }
    var profileImageUri by remember { mutableStateOf(profilePrefs.getString("profile_image_uri", null)) }
    var displayNameDraft by remember { mutableStateOf(displayName) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profileImageUri = uri.toString()
            profilePrefs.edit().putString("profile_image_uri", uri.toString()).apply()
        }
    }
    val profileBitmap = remember(profileImageUri) {
        try {
            profileImageUri?.let { context.contentResolver.openInputStream(Uri.parse(it))?.use { stream -> BitmapFactory.decodeStream(stream) } }
        } catch (_: Exception) { null }
    }
    BackHandler(onBack = onBack)

    Surface(modifier = Modifier.fillMaxSize(), color = ProfileBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ProfileInk)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Staff Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ProfileInk)
                    Text("Your staff account and access details", fontSize = 13.sp, color = ProfileMuted)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(Color(0xFFE8F0FE), CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = "Staff profile picture",
                                modifier = Modifier.size(88.dp)
                            )
                        } else {
                        val initials = displayName.trim()
                            .split(Regex("\\s+"))
                            .filter { it.isNotBlank() }
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .ifBlank { "S" }
                        Text(
                            text = initials,
                            color = ProfileBlue,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        }
                    }
                    TextButton(onClick = { imagePicker.launch("image/*") }) { Text("Change profile photo") }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = displayName.ifBlank { username.ifBlank { "Staff account" } },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = {
                        displayNameDraft = displayName
                        showDisplayNameDialog = true
                    }) { Text("Edit display name") }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("ACTIVE SESSION", color = Color(0xFF267342), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (role.isNotBlank()) {
                            Spacer(Modifier.size(8.dp))
                            Box(
                                modifier = Modifier.background(Color(0xFFE8EEFF), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    color = ProfileBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("YOUR ACCOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfileMuted)
            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileInfoRow(
                        icon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ProfileBlue) },
                        label = "Username",
                        value = username.ifBlank { "Not provided by server" }
                    )
                    Divider(color = ProfileBorder)
                    ProfileInfoRow(
                        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = ProfileBlue) },
                        label = "Access role",
                        value = role.ifBlank { "Not provided by server" }
                    )
                    Divider(color = ProfileBorder)
                    ProfileInfoRow(
                        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = ProfileBlue) },
                        label = "Application",
                        value = "SGSITS Student Management System"
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = ProfileBlue)
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("Account security", fontWeight = FontWeight.SemiBold, color = ProfileInk)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your sign-in session is stored securely on this device. Password changes should be handled through an authenticated backend feature.",
                            color = ProfileMuted, fontSize = 12.sp, lineHeight = 17.sp
                        )
                    }
                }
            }

            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { showLogoutConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.size(9.dp))
                Text("Sign out", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "For authorized college staff only",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 11.sp,
                color = ProfileMuted
            )
        }
    }

    if (showDisplayNameDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayNameDialog = false },
            title = { Text("Edit display name") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = displayNameDraft,
                    onValueChange = { displayNameDraft = it },
                    label = { Text("Display name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleaned = displayNameDraft.trim()
                    if (cleaned.isNotBlank()) {
                        displayName = cleaned
                        profilePrefs.edit().putString("display_name", cleaned).apply()
                    }
                    showDisplayNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDisplayNameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access student records.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmation = false
                    onLogout()
                }) { Text("Sign out", color = Color(0xFFB42318)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = ProfileMuted)
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ProfileInk)
        }
    }
}
