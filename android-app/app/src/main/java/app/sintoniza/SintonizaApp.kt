package app.sintoniza

import android.app.Application
import app.sintoniza.config.ServerConfigStore

class SintonizaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServerConfigStore.initializeFirebase(this)
    }
}
