package com.example.nfcautomation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.nfcautomation.constants.NfcMessages
import com.example.nfcautomation.exceptions.*
import com.example.nfcautomation.services.QuickAccessService

/**
 * Actividad principal que gestiona la lectura de etiquetas NFC y su 
 * procesamiento mediante un motor de lógica en Python.
 */
class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var resultText by mutableStateOf(NfcMessages.WAITING_TAG)
    private var showPermissionButton by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showPermissionButton = false
            startQuickAccessService()
        } else {
            showPermissionButton = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            if (nfcAdapter == null) {
                throw NfcHardwareNotFoundException(NfcMessages.ERR_HARDWARE_NOT_FOUND)
            }
        } catch (e: NfcHardwareNotFoundException) {
            resultText = e.message ?: NfcMessages.ERR_UNEXPECTED
        }

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        checkNotificationPermission()
        handleIntent(intent)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start
                    )
                    
                    if (showPermissionButton) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Falta el permiso de Notificaciones",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Para que el acceso rápido desde la pantalla de bloqueo funcione, debes permitir las notificaciones.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { openNotificationSettings() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Abrir Ajustes")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startQuickAccessService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startQuickAccessService()
        }
    }

    private fun startQuickAccessService() {
        val serviceIntent = Intent(this, QuickAccessService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Verificar si el usuario dio el permiso mientras estaba en ajustes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted && showPermissionButton) {
                showPermissionButton = false
                startQuickAccessService()
            }
        }

        try {
            val adapter = nfcAdapter ?: return
            if (!adapter.isEnabled) {
                throw NfcDisabledException(NfcMessages.ERR_NFC_DISABLED)
            }
            adapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or 
                NfcAdapter.FLAG_READER_NFC_B or 
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        } catch (e: NfcDisabledException) {
            resultText = e.message ?: NfcMessages.ERR_UNEXPECTED
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val idBytes = tag?.id ?: return
            val tagId = bytesToHex(idBytes)
            
            Log.d("NFC", "Detección vía Intent: $tagId")

            if (isTagRegistered(tagId)) {
                processTag(tagId)
            } else {
                resultText = "Tag detectado vía Intent ($tagId), pero no está registrado."
            }
        }
    }

    private fun isTagRegistered(tagId: String): Boolean {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("bridge")
            module.callAttr("is_tag_registered", tagId).toBoolean()
        } catch (e: Exception) {
            false
        }
    }

    private fun processTag(tagId: String) {
        runOnUiThread {
            resultText = "${NfcMessages.TAG_DETECTED} $tagId\n${NfcMessages.PROCESSING}"
            try {
                val py = Python.getInstance()
                val module = py.getModule("bridge")
                val response = module.callAttr("execute", tagId).toString()
                resultText = "${NfcMessages.SUCCESS}\n\n$response"
            } catch (e: Exception) {
                resultText = "${NfcMessages.ERR_PYTHON_EXECUTION}${e.message}"
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.d("NFC", "Etiqueta detectada en primer plano")
        val idBytes = tag?.id ?: return
        val tagId = bytesToHex(idBytes)
        
        if (isTagRegistered(tagId)) {
            processTag(tagId)
        } else {
            runOnUiThread {
                resultText = "Tag detectado ($tagId), pero no está registrado en tu configuración."
            }
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
}
