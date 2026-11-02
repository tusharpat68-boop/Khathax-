package com.khathax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

private val Green = Color(0xFF128C7E)
private val Mint = Color(0xFFDCF8C6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green)) {
                val nav = rememberNavController()
                NavHost(nav, startDestination = if (Repo.isLoggedIn) "users" else "auth") {
                    composable("auth") {
                        AuthScreen { nav.navigate("users") { popUpTo("auth") { inclusive = true } } }
                    }
                    composable("users") {
                        UsersScreen(
                            onOpen = { u -> nav.navigate("chat/${u.uid}/${u.name}") },
                            onLogout = {
                                Repo.logout()
                                nav.navigate("auth") { popUpTo("users") { inclusive = true } }
                            }
                        )
                    }
                    composable("chat/{uid}/{name}") { e ->
                        ChatScreen(
                            other = e.arguments?.getString("uid").orEmpty(),
                            name = e.arguments?.getString("name").orEmpty(),
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(onDone: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Khathax", style = MaterialTheme.typography.displaySmall, color = Green)
        Spacer(Modifier.height(24.dp))
        if (!isLogin) {
            OutlinedTextField(name, { name = it }, label = { Text("নাম") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(email, { email = it }, label = { Text("ইমেইল") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("পাসওয়ার্ড") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                loading = true; error = ""
                scope.launch {
                    try {
                        if (isLogin) Repo.login(email.trim(), pass)
                        else Repo.register(name.trim(), email.trim(), pass)
                        onDone()
                    } catch (e: Exception) {
                        error = e.message ?: "কিছু একটা ভুল হয়েছে"
                    }
                    loading = false
                }
            }
        ) { Text(if (isLogin) "লগইন" else "অ্যাকাউন্ট খুলুন") }
        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "নতুন? অ্যাকাউন্ট খুলুন" else "আগেই অ্যাকাউন্ট আছে? লগইন")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(onOpen: (User) -> Unit, onLogout: () -> Unit) {
    val users by Repo.users().collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khathax") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green, titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = { TextButton(onClick = onLogout) { Text("লগআউট", color = Color.White) } }
            )
        }
    ) { pad ->
        if (users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("এখনো কোনো ইউজার নেই")
            }
        }
        LazyColumn(Modifier.padding(pad)) {
            items(users) { u ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(u) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(Green, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) { Text(u.name.take(1).uppercase(), color = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(u.name, style = MaterialTheme.typography.titleMedium)
                        Text(u.email, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(other: String, name: String, onBack: () -> Unit) {
    val msgs by remember(other) { Repo.messages(other) }.collectAsState(initial = emptyList())
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green, titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(8.dp).imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    text, { text = it },
                    placeholder = { Text("মেসেজ লিখুন") },
                    modifier = Modifier.weight(1f), maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        text = ""
                        scope.launch { runCatching { Repo.send(other, t) } }
                    }
                }) { Text("পাঠান") }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(msgs) { m ->
                val mine = m.senderId == Repo.myUid
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (mine) Mint else Color(0xFFEFEFEF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(m.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}
