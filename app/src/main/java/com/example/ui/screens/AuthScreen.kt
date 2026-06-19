package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.GlassCard
import com.example.ui.components.glassmorphic
import com.example.ui.viewmodel.BarterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: BarterViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isVerified by viewModel.isVerified.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val inAppVerificationCode by viewModel.inAppVerificationCode.collectAsState()
    val usesFirebaseAuth by viewModel.usesFirebaseAuth.collectAsState()

    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    
    // OTP states
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf(false) }
    var otpResendTimer by remember { mutableStateOf(45) }
    var otpMessage by remember {
        mutableStateOf(
            if (usesFirebaseAuth) {
                "We sent a verification link to your email. Tap the link, then return here and tap Verify."
            } else {
                "Enter the 6-digit code shown below. This code is only visible in the app — never in notifications."
            }
        )
    }

    // Start timer if logged in but unverified
    LaunchedEffect(isLoggedIn, isVerified) {
        if (isLoggedIn && !isVerified) {
            otpResendTimer = 45
            while (otpResendTimer > 0) {
                delay(1000)
                otpResendTimer--
            }
        }
    }

    // Dynamic background gradient based on theme
    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F0E13),
                Color(0xFF1C1B1F),
                Color(0xFF281C3F) // Atmospheric deep violet bloom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8DFFF), // Soft orchid mist
                Color(0xFFF6F3FF),
                Color(0xFFFFFFFF)
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glowing glass core orb backing (simulates frosted iOS light pass)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-40).dp, y = 80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD0BCFF).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 50.dp, y = (-80).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF2B8B5).copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Dynamic view selection
            if (!isLoggedIn) {
                // STEP 1: Registration or onboarding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Logo Banner
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = "Logo icon",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Barter-me",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        
                        Text(
                            text = "Swap skills with people nearby",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Glassmorphic Card containing main onboarding entry
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_glass_card"),
                        isDarkTheme = isDarkMode
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (isSignUpMode) "Create Account" else "Welcome Back",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (isSignUpMode) "Register your real neighborhood profile to start swapping skills today." else "To maintain neighborhood security, verify your profile credentials.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            if (!isSignUpMode && BuildConfig.DEBUG) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            emailInput = "admin@barter.me"
                                            passwordInput = "admin123"
                                        }
                                        .testTag("admin_credentials_backdoor"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Admin Backdoor badge label",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TESTING BACKDOOR (FOR AUDIT & SIMULATION)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Email: admin@barter.me • Pass: admin123",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "AUTOFILL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                        )
                                    }
                                }
                            }

                            if (isSignUpMode) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, "Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("auth_name_field"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Mail, "Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.testTag("auth_error_text")
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    authError = null
                                    if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        val resolvedName = if (isSignUpMode && nameInput.isNotBlank()) nameInput else "Alex Mercer"
                                        viewModel.signInWithCredentials(
                                            email = emailInput,
                                            password = passwordInput,
                                            displayName = resolvedName,
                                            isSignUp = isSignUpMode
                                        ) { success, error ->
                                            if (!success) authError = error
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("auth_submit_btn"),
                                enabled = emailInput.isNotBlank() && passwordInput.isNotBlank() && (!isSignUpMode || nameInput.isNotBlank()),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Sign Up & Verify" else "Verify Identity",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Mode Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Already have an account?" else "New to Barter-me?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSignUpMode) "Sign In" else "Register Now",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { isSignUpMode = !isSignUpMode }
                                        .testTag("auth_mode_toggle_btn")
                                )
                            }
                        }
                    }

                    // Social login (debug builds only — production uses email or Firebase OAuth)
                    if (BuildConfig.DEBUG) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "SECURE SOCIAL ID",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }

                    // Modern Google & Apple Sign-In Buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Google Login Mock
                        Card(
                            onClick = {
                                viewModel.debugSocialSignIn(
                                    email = "google.user@gmail.com",
                                    name = "Alex Mercer",
                                    method = "GOOGLE"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("social_btn_google"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Red/Blue/Yellow/Green themed Google mock icon emblem
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4285F4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Continue with Google",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                        }

                        // Apple Login Mock
                        Card(
                            onClick = {
                                viewModel.debugSocialSignIn(
                                    email = "apple.id@icloud.com",
                                    name = "Alex Mercer",
                                    method = "APPLE"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("social_btn_apple"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color.Black else Color(0xFF111111)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Continue with Apple ID",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    }
                }
            } else {
                // STEP 2: Unverified Profile OTP/Verification
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_glass_card"),
                    isDarkTheme = isDarkMode
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Authentication Shield",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Text(
                            "Account Verification",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = otpMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        if (!usesFirebaseAuth && inAppVerificationCode != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("in_app_verification_code_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Your verification code",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = inAppVerificationCode!!,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 6.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("in_app_verification_code")
                                    )
                                    Text(
                                        text = "Expires in 10 minutes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        if (BuildConfig.DEBUG && !usesFirebaseAuth) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🔑 DEBUG VERIFICATION KEY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Codes 1234 or 123456 also work in debug builds.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (!usesFirebaseAuth) {
                        // PIN Input Row
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { 
                                if (it.length <= 6) {
                                    otpInput = it.filter { char -> char.isDigit() }
                                    otpError = false
                                }
                            },
                            label = { Text(if (usesFirebaseAuth) "Optional PIN (not used for email link)" else "6-Digit Security PIN") },
                            placeholder = { Text(if (usesFirebaseAuth) "Tap Verify after email link" else "Enter code") },
                            trailingIcon = {
                                if (otpError) {
                                    Icon(Icons.Default.Cached, "Error Reset", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            isError = otpError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_otp_field"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp,
                                fontSize = 18.sp
                            )
                        )

                        if (otpError) {
                            Text(
                                if (usesFirebaseAuth) {
                                    "Email not verified yet. Open the link in your inbox, then tap Verify."
                                } else {
                                    "Invalid or expired code. Request a new code and try again."
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (otpResendTimer == 0) {
                                        viewModel.resendVerification { success, _ ->
                                            if (success) {
                                                otpResendTimer = 45
                                                otpMessage = if (usesFirebaseAuth) {
                                                    "Verification email resent. Check your inbox and spam folder."
                                                } else {
                                                    "A new verification code has been issued. Enter it below."
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = otpResendTimer == 0,
                                modifier = Modifier.testTag("otp_resend_btn")
                            ) {
                                Text(
                                    text = if (otpResendTimer > 0) {
                                        if (usesFirebaseAuth) "Resend email in (${otpResendTimer}s)" else "Resend code in (${otpResendTimer}s)"
                                    } else {
                                        if (usesFirebaseAuth) "Resend verification email" else "Resend secure code"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.verifyAccount(otpInput) { verified ->
                                        if (!verified) otpError = true
                                    }
                                },
                                modifier = Modifier.testTag("auth_otp_verify_btn"),
                                enabled = if (usesFirebaseAuth) true else otpInput.length >= 4,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Verify")
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        TextButton(
                            onClick = { viewModel.signOutUser() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("auth_abort_btn")
                        ) {
                            Text("Cancel Onboarding & Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
