package com.khathax.app

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class User(val uid: String = "", val name: String = "", val email: String = "")

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)

object Repo {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val myUid: String get() = auth.currentUser?.uid ?: ""
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(name: String, email: String, pass: String) {
        val r = auth.createUserWithEmailAndPassword(email, pass).await()
        val uid = r.user!!.uid
        db.collection("users").document(uid).set(User(uid, name, email)).await()
    }

    suspend fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).await()
    }

    fun logout() = auth.signOut()

    fun chatId(other: String) = listOf(myUid, other).sorted().joinToString("_")

    fun users(): Flow<List<User>> = callbackFlow {
        val reg = db.collection("users").addSnapshotListener { s, _ ->
            val list = s?.toObjects(User::class.java).orEmpty().filter { it.uid != myUid }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun messages(other: String): Flow<List<Message>> = callbackFlow {
        val reg = db.collection("chats").document(chatId(other))
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { s, _ ->
                trySend(s?.toObjects(Message::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun send(other: String, text: String) {
        db.collection("chats").document(chatId(other))
            .collection("messages")
            .add(
                hashMapOf(
                    "senderId" to myUid,
                    "text" to text,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
    }
}
