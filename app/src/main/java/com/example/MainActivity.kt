package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SavingCloudRepository
import com.example.data.SavingRepository
import com.example.ui.SavingTrackerScreen
import com.example.ui.SavingTrackerViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this)
            } catch (ignored: Exception) {
            }
        }
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val firebaseAuth: FirebaseAuth? = remember {
        try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }
    }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(firebaseAuth?.currentUser) }
    var isOfflineLocalMode by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(firebaseAuth) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isOfflineLocalMode = false
            }
        }
        firebaseAuth?.addAuthStateListener(listener)
        onDispose {
            firebaseAuth?.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null && !isOfflineLocalMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = firebaseAuth?.currentUser
                isOfflineLocalMode = false
            },
            onContinueWithLocalBackup = {
                isOfflineLocalMode = true
            }
        )
    } else if (user != null) {
        // Authenticated session: instantiate ViewModel keyed by user.uid with R.string.firestore_database_id
        val trackerViewModel: SavingTrackerViewModel = viewModel(
            key = "cloud_${user.uid}",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    val localRepo = SavingRepository(app, userId = user.uid)
                    val cloudRepo = SavingCloudRepository(db)
                    SavingTrackerViewModel(
                        localRepository = localRepo,
                        cloudRepository = cloudRepo,
                        currentUserId = user.uid,
                        userDisplayName = user.displayName ?: "",
                        userEmail = user.email ?: ""
                    )
                }
            }
        )
        SavingTrackerScreen(
            viewModel = trackerViewModel,
            onAuthActionClick = {
                signOutUser(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                        isOfflineLocalMode = false
                    },
                    scope = scope
                )
            }
        )
    } else {
        // Offline / Local Backup mode: strictly local Room & SharedPreferences without unauthenticated Firestore calls
        val localViewModel: SavingTrackerViewModel = viewModel(
            key = "local_offline_session",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val localRepo = SavingRepository(app, userId = SavingRepository.DEFAULT_LOCAL_USER_ID)
                    SavingTrackerViewModel(
                        localRepository = localRepo,
                        cloudRepository = null,
                        currentUserId = null
                    )
                }
            }
        )
        SavingTrackerScreen(
            viewModel = localViewModel,
            onAuthActionClick = {
                isOfflineLocalMode = false
            }
        )
    }
}
