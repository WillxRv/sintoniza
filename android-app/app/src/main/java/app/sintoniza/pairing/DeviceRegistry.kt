package app.sintoniza.pairing

import android.os.Build
import app.sintoniza.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date

/**
 * Identidade da TV no Firestore (ver firebase/firestore.rules):
 * login anônimo, token FCM em devices/{uid} e códigos de pareamento em pairingCodes/{code}.
 */
class DeviceRegistry(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) {
    private val random = SecureRandom()

    suspend fun uid(): String = auth.currentUser?.uid ?: auth.signInAnonymously().await().user!!.uid

    /** Publica o token FCM atual para a skill conseguir mandar comandos. */
    suspend fun register(token: String? = null) {
        val fcmToken = token ?: messaging.token.await()
        device(uid()).set(
            mapOf(
                "fcmToken" to fcmToken,
                "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "appVersion" to BuildConfig.VERSION_NAME,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    /** Cria um código de 6 dígitos (sem zero à esquerda, para a Alexa não perdê-lo). */
    suspend fun newPairingCode(): String {
        val uid = uid()
        repeat(MAX_CODE_ATTEMPTS) {
            val code = (100_000 + random.nextInt(900_000)).toString()
            try {
                db.collection("pairingCodes").document(code)
                    .set(mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp()))
                    .await()
                return code
            } catch (e: FirebaseFirestoreException) {
                // Código já em uso por outra TV: as regras negam e tentamos outro.
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        error("Não foi possível gerar um código de pareamento")
    }

    suspend fun deletePairingCode(code: String) {
        runCatching { db.collection("pairingCodes").document(code).delete().await() }
    }

    /** Emite quando a skill marca a TV como pareada (null = nunca pareada). */
    fun pairedAt(uid: String): Flow<Date?> = callbackFlow {
        val registration = device(uid).addSnapshotListener { snapshot, error ->
            if (error == null) trySend(snapshot?.getTimestamp("pairedAt")?.toDate())
        }
        awaitClose { registration.remove() }
    }

    /**
     * Esquece todas as Alexas pareadas: apaga o registro e troca de identidade.
     * Os vínculos antigos na skill ficam órfãos e são limpos no próximo comando.
     */
    suspend fun unpairAll() {
        runCatching { device(uid()).delete().await() }
        auth.signOut()
        register()
    }

    private fun device(uid: String) = db.collection("devices").document(uid)

    private companion object {
        const val MAX_CODE_ATTEMPTS = 5
    }
}
