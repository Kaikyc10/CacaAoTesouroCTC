package com.example.caaaotesouroctc

import android.app.Application
import com.google.android.gms.maps.MapsInitializer

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar componentes globais do aplicativo
        MapsInitializer.initialize(this)
        // Configurar outras inicializações necessárias
        // ...
    }
}
