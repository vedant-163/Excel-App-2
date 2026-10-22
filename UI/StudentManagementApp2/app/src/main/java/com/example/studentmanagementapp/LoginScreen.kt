package com.example.studentmanagementapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.widget.Toast

private val LoginBlue = Color(0xFF1565C0)
private val LoginDarkBlue = Color(0xFF0D47A1)
private val LoginBackground = Color(0xFFF4F7FB)
private val LoginText = Color(0xFF172033)
private val LoginMuted = Color(0xFF667085)
private val LoginBorder = Color(0xFFD9E1EC)
private val LoginErrorBackground = Color(0xFFFFF1F0)
private val LoginErrorText = Color(0xFFB42318)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryCodeRequested by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryMessage by remember { mutableStateOf<String?>(null) }
    var recoveryMessageIsError by remember { mutableStateOf(false) }
    var recoveryLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LoginBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand mark
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(LoginBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Student Management System",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Student Management System",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoginText
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "SGSITS • Staff Portal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoginBlue
                )

                Spacer(Modifier.height(22.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = LoginBorder,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Welcome back",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = LoginText
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "Sign in with your authorized staff account.",
                            fontSize = 13.sp,
                            color = LoginMuted
                        )

                        Spacer(Modifier.height(22.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username or registered email") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (username.contains("@")) {
                                        Icons.Default.Email
                                    } else {
                                        Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = LoginBlue
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            isError = errorMessage != null,
                            shape = RoundedCornerShape(13.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color(0xFFF8FAFC),
                                errorContainerColor = Color.White,
                                focusedIndicatorColor = LoginBlue,
                                unfocusedIndicatorColor = LoginBorder,
                                errorIndicatorColor = LoginErrorText,
                                focusedLabelColor = LoginBlue,
                                cursorColor = LoginBlue
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LoginBlue
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            isError = errorMessage != null,
                            shape = RoundedCornerShape(13.dp),
                            visualTransformation =
                                if (passwordVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    enabled = !isLoading
                                ) {
                                    Icon(
                                        imageVector =
                                            if (passwordVisible) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                        contentDescription =
                                            if (passwordVisible) "Hide password"
                                            else "Show password",
                                        tint = LoginMuted
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color(0xFFF8FAFC),
                                errorContainerColor = Color.White,
                                focusedIndicatorColor = LoginBlue,
                                unfocusedIndicatorColor = LoginBorder,
                                errorIndicatorColor = LoginErrorText,
                                focusedLabelColor = LoginBlue,
                                cursorColor = LoginBlue
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    recoveryEmail = username.takeIf { it.contains("@") } ?: ""
                                    recoveryCode = ""
                                    recoveryNewPassword = ""
                                    recoveryCodeRequested = false
                                    recoveryMessage = null
                                    recoveryMessageIsError = false
                                    showRecoveryDialog = true
                                },
                                enabled = !isLoading
                            ) {
                                Text("Forgot password?", color = LoginBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LoginErrorBackground)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = LoginErrorText,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val user = username.trim()

                                when {
                                    user.isEmpty() ->
                                        errorMessage = "Enter username."

                                    password.isEmpty() ->
                                        errorMessage = "Enter password."

                                    else -> {
                                        errorMessage = null
                                        isLoading = true

                                        scope.launch {
                                            try {
                                                val result = ApiClient.login(context, user, password)
                                                AuthStorage.saveSession(
                                                    context = context,
                                                    accessToken = result.accessToken,
                                                    expiresInSeconds = result.expiresIn,
                                                    username = result.username,
                                                    role = result.role
                                                )
                                                password = ""
                                                onLoginSuccess()
                                            } catch (e: ApiClient.ApiException) {
                                                AuthStorage.clear(context)
                                                errorMessage = e.message
                                            } catch (_: Exception) {
                                                AuthStorage.clear(context)
                                                errorMessage = "Unable to sign in. Please check the server connection."
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginBlue,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFAFC7E6),
                                disabledContentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(21.dp),
                                    color = Color.White,
                                    strokeWidth = 2.2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign In",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Authorized staff access only",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            color = LoginMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Secure access to student records and academic operations",
                    fontSize = 11.sp,
                    color = Color(0xFF98A2B3),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (showRecoveryDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!recoveryLoading) showRecoveryDialog = false
                    },
                    title = {
                        Text(if (recoveryCodeRequested) "Reset staff password" else "Forgot password?")
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (recoveryCodeRequested)
                                    "Enter the six-digit code sent to your registered staff email and choose a new password."
                                else
                                    "We will send a one-time recovery code to your registered staff email."
                            )
                            OutlinedTextField(
                                value = recoveryEmail,
                                onValueChange = { recoveryEmail = it.trim(); recoveryMessage = null },
                                label = { Text("Registered email") },
                                singleLine = true,
                                enabled = !recoveryLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (recoveryCodeRequested) {
                                OutlinedTextField(
                                    value = recoveryCode,
                                    onValueChange = { recoveryCode = it.filter { character -> character.isDigit() }.take(6); recoveryMessage = null },
                                    label = { Text("6-digit recovery code") },
                                    singleLine = true,
                                    enabled = !recoveryLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = recoveryNewPassword,
                                    onValueChange = { recoveryNewPassword = it; recoveryMessage = null },
                                    label = { Text("New password (10+ characters)") },
                                    singleLine = true,
                                    enabled = !recoveryLoading,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            recoveryMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = if (recoveryMessageIsError) LoginErrorText else LoginBlue,
                                    fontSize = 12.sp
                                )
                            }
                            if (recoveryLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !recoveryLoading,
                            onClick = {
                                val email = recoveryEmail.trim()
                                when {
                                    !email.contains("@") || !email.contains(".") -> {
                                        recoveryMessage = "Enter a valid registered email address."
                                        recoveryMessageIsError = true
                                    }
                                    recoveryCodeRequested && recoveryCode.length != 6 -> {
                                        recoveryMessage = "Enter the 6-digit code from your email."
                                        recoveryMessageIsError = true
                                    }
                                    recoveryCodeRequested && recoveryNewPassword.length < 10 -> {
                                        recoveryMessage = "New password must be at least 10 characters."
                                        recoveryMessageIsError = true
                                    }
                                    else -> {
                                        recoveryLoading = true
                                        recoveryMessage = null
                                        scope.launch {
                                            try {
                                                if (!recoveryCodeRequested) {
                                                    recoveryMessage = ApiClient.requestPasswordReset(context, email)
                                                    recoveryCodeRequested = true
                                                    recoveryMessageIsError = false
                                                } else {
                                                    val result = ApiClient.resetPassword(
                                                        context, email, recoveryCode, recoveryNewPassword
                                                    )
                                                    showRecoveryDialog = false
                                                    username = email
                                                    password = ""
                                                    recoveryCodeRequested = false
                                                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: ApiClient.ApiException) {
                                                recoveryMessage = e.message
                                                recoveryMessageIsError = true
                                            } catch (_: Exception) {
                                                recoveryMessage = "Password recovery failed. Please try again."
                                                recoveryMessageIsError = true
                                            } finally {
                                                recoveryLoading = false
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(if (recoveryCodeRequested) "Reset password" else "Send code")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !recoveryLoading,
                            onClick = { showRecoveryDialog = false }
                        ) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
