package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DueCrimsonLight
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldLight
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondarySlate
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

object AuthSessionPreferences {
    private const val PREF_FILE = "my_saving_tracker_auth_prefs"
    private const val KEY_REMEMBER_ME = "remember_me_enabled"
    private const val KEY_SAVED_EMAIL = "remembered_email"

    fun isRememberMeEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMEMBER_ME, true)
    }

    fun setRememberMe(context: Context, enabled: Boolean, email: String = "") {
        val prefs = context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_REMEMBER_ME, enabled)
            .putString(KEY_SAVED_EMAIL, if (enabled) email else "")
            .apply()
    }

    fun getSavedEmail(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAVED_EMAIL, "") ?: ""
    }
}

/**
 * Silent Auto-Sign-In on App Startup using GetGoogleIdOption (one credential option per request).
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    if (!AuthSessionPreferences.isRememberMeEnabled(context)) {
        onUnauthenticated()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Silent Google Sign-In cancelled or dismissed: ${e.message}", e)
            onUnauthenticated()
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption (one credential option per request).
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In কনফিগারেশন পাওয়া যায়নি (default_web_client_id missing)")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("অপ্রত্যাশিত ক্রেডেনশিয়াল টাইপ (Unexpected credential type)")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google সাইন-ইন সম্পন্ন হয়নি।")
        }
    }
}

/**
 * Signs out of Firebase Auth and clears Jetpack Credential Manager state.
 */
fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueWithLocalBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var authMode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(AuthSessionPreferences.getSavedEmail(context)) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable {
        mutableStateOf(AuthSessionPreferences.isRememberMeEnabled(context))
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBackground)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .testTag("auth_screen_container"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Branding Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_saving_vault_hero),
                            contentDescription = "Saving Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RoyalBlueDark.copy(alpha = 0.9f),
                                border = BorderStroke(1.5.dp, CyanAccent),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = "App Icon",
                                        tint = PaidEmeraldLight,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "My Saving Tracker",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "মাই সেভিং ট্র্যাকার • ক্লাউড সিঙ্ক ও অ্যাকাউন্ট সুরক্ষা",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = CyanAccent
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ডিসেম্বর ২০২৬ – জুন ২০২৭ • লক্ষ্য ₹10,400",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = GoldHighlight,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Main Authentication Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Mode Selector Tabs (Login / Register / Forgot Password)
                        val selectedTabIndex = when (authMode) {
                            AuthMode.LOGIN -> 0
                            AuthMode.REGISTER -> 1
                            AuthMode.FORGOT_PASSWORD -> 2
                        }

                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = NavySurfaceElevated,
                            contentColor = TextPrimaryWhite,
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        color = CyanAccent
                                    )
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("auth_mode_tabs")
                        ) {
                            Tab(
                                selected = authMode == AuthMode.LOGIN,
                                onClick = {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    infoMessage = null
                                },
                                text = { Text("লগইন (Login)", fontSize = 13.sp) },
                                modifier = Modifier.testTag("tab_login")
                            )
                            Tab(
                                selected = authMode == AuthMode.REGISTER,
                                onClick = {
                                    authMode = AuthMode.REGISTER
                                    errorMessage = null
                                    infoMessage = null
                                },
                                text = { Text("রেজিস্টার (Register)", fontSize = 13.sp) },
                                modifier = Modifier.testTag("tab_register")
                            )
                            Tab(
                                selected = authMode == AuthMode.FORGOT_PASSWORD,
                                onClick = {
                                    authMode = AuthMode.FORGOT_PASSWORD
                                    errorMessage = null
                                    infoMessage = null
                                },
                                text = { Text("পাসওয়ার্ড রিসেট", fontSize = 12.sp) },
                                modifier = Modifier.testTag("tab_forgot_password")
                            )
                        }

                        // Primary Google Sign-In Button (Jetpack Credential Manager)
                        Button(
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                isLoading = true
                                AuthSessionPreferences.setRememberMe(context, rememberMe, email)
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaidEmerald,
                                contentColor = NavyBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = NavyBackground,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "যাচাই করা হচ্ছে...",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Sign in with Google",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with Google • Google দিয়ে প্রবেশ করুন",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NavyBorder)
                            Text(
                                text = "  অথবা ইমেইল ও পাসওয়ার্ড  ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondarySlate
                                )
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NavyBorder)
                        }

                        AnimatedContent(
                            targetState = authMode,
                            label = "auth_form_transition"
                        ) { mode ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (mode == AuthMode.REGISTER) {
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("আপনার নাম (Full Name)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Full Name",
                                                tint = CyanAccent
                                            )
                                        },
                                        singleLine = true,
                                        colors = authTextFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_name_input")
                                    )
                                }

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("ইমেইল ঠিকানা (Email Address)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            tint = CyanAccent
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_email_input")
                                )

                                if (mode != AuthMode.FORGOT_PASSWORD) {
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("পাসওয়ার্ড (Password)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Password",
                                                tint = CyanAccent
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle password visibility",
                                                    tint = TextSecondarySlate
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = if (mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                                        ),
                                        singleLine = true,
                                        colors = authTextFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_password_input")
                                    )
                                }

                                if (mode == AuthMode.REGISTER) {
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("পুনরায় পাসওয়ার্ড দিন (Confirm Password)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Confirm Password",
                                                tint = CyanAccent
                                            )
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        singleLine = true,
                                        colors = authTextFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_confirm_password_input")
                                    )
                                }

                                // Remember Me & Forgot Password Row
                                if (mode != AuthMode.FORGOT_PASSWORD) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable {
                                                    rememberMe = !rememberMe
                                                    AuthSessionPreferences.setRememberMe(
                                                        context,
                                                        rememberMe,
                                                        email
                                                    )
                                                }
                                                .testTag("remember_me_checkbox")
                                        ) {
                                            Checkbox(
                                                checked = rememberMe,
                                                onCheckedChange = { checked ->
                                                    rememberMe = checked
                                                    AuthSessionPreferences.setRememberMe(
                                                        context,
                                                        checked,
                                                        email
                                                    )
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = RoyalBluePrimary,
                                                    uncheckedColor = TextSecondarySlate
                                                )
                                            )
                                            Text(
                                                text = "আমাকে মনে রাখুন (Remember Me)",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = TextPrimaryWhite
                                                )
                                            )
                                        }

                                        if (mode == AuthMode.LOGIN) {
                                            TextButton(
                                                onClick = {
                                                    authMode = AuthMode.FORGOT_PASSWORD
                                                    errorMessage = null
                                                    infoMessage = null
                                                },
                                                modifier = Modifier.testTag("forgot_password_link")
                                            ) {
                                                Text(
                                                    text = "পাসওয়ার্ড ভুলে গেছেন?",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = CyanAccent
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Submit Action Button for Login / Register / Forgot Password
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        AuthSessionPreferences.setRememberMe(context, rememberMe, email.trim())
                                        when (mode) {
                                            AuthMode.FORGOT_PASSWORD -> {
                                                val trimmedEmail = email.trim()
                                                if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                                                    errorMessage = "অনুগ্রহ করে একটি সঠিক ইমেইল ঠিকানা লিখুন।"
                                                } else {
                                                    Firebase.auth.sendPasswordResetEmail(trimmedEmail)
                                                        .addOnSuccessListener {
                                                            infoMessage =
                                                                "✓ $trimmedEmail ঠিকানায় পাসওয়ার্ড রিসেট লিংক পাঠানো হয়েছে।"
                                                        }
                                                        .addOnFailureListener {
                                                            infoMessage =
                                                                "এই পরিবেশে Google Sign-In প্রি-কনফিগার করা আছে। পাসওয়ার্ড ছাড়া সরাসরি উপরে 'Sign in with Google' বাটনে ট্যাপ করে লগইন করুন, অথবা Firebase Console-এ Email/Password প্রোভাইডার সক্রিয় করুন।"
                                                        }
                                                }
                                            }
                                            AuthMode.LOGIN, AuthMode.REGISTER -> {
                                                if (email.isBlank() || !email.contains("@")) {
                                                    errorMessage = "অনুগ্রহ করে একটি বৈধ ইমেইল ঠিকানা দিন।"
                                                } else if (password.length < 6) {
                                                    errorMessage = "পাসওয়ার্ড অন্তত ৬ অক্ষরের হতে হবে।"
                                                } else if (mode == AuthMode.REGISTER && password != confirmPassword) {
                                                    errorMessage = "দুটি পাসওয়ার্ড মিলছে না।"
                                                } else {
                                                    // Trigger interactive Google Sign-In via Credential Manager while informing about Firebase Console Email/Password setting
                                                    infoMessage =
                                                        "এই ক্লাউড পরিবেশে Google Sign-In স্বয়ংক্রিয়ভাবে সংযুক্ত। আপনার Google অ্যাকাউন্ট দিয়ে সেশন শুরু হচ্ছে (Email/Password ম্যানুয়ালি চালু করতে Firebase Console > Authentication দেখুন)।"
                                                    onGoogleSignInClicked(
                                                        context = context,
                                                        credentialManager = credentialManager,
                                                        onAuthSuccess = onAuthSuccess,
                                                        onAuthError = { msg -> errorMessage = msg },
                                                        scope = scope
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalBluePrimary,
                                        contentColor = TextPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_submit_button")
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            AuthMode.LOGIN -> Icons.Default.CloudDone
                                            AuthMode.REGISTER -> Icons.Default.CheckCircle
                                            AuthMode.FORGOT_PASSWORD -> Icons.Default.LockReset
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (mode) {
                                            AuthMode.LOGIN -> "লগইন করুন (Login)"
                                            AuthMode.REGISTER -> "অ্যাকাউন্ট তৈরি করুন (Register)"
                                            AuthMode.FORGOT_PASSWORD -> "পাসওয়ার্ড রিসেট লিংক পাঠান"
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        // Error or Info Message Box
                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DueCrimsonLight.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, DueCrimsonLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_banner")
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = DueCrimsonLight
                                    ),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (infoMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyanAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CyanAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_info_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = infoMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimaryWhite
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline / Local Backup Mode Fallback Button
                OutlinedButton(
                    onClick = onContinueWithLocalBackup,
                    border = BorderStroke(1.dp, NavyBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("continue_local_backup_button")
                ) {
                    Text(
                        text = "অফলাইন / লোকাল ব্যাকআপ মোডে ড্যাশবোর্ড দেখুন",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondarySlate
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimaryWhite,
    unfocusedTextColor = TextPrimaryWhite,
    focusedContainerColor = NavySurfaceElevated,
    unfocusedContainerColor = NavySurfaceElevated,
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = NavyBorder,
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = TextSecondarySlate,
    cursorColor = CyanAccent
)
