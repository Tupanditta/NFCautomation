package com.example.nfcautomation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.nfcautomation.services.QuickAccessService
import com.example.nfcautomation.utils.ConfigManager
import com.example.nfcautomation.ui.screens.ExecutionScreen
import com.example.nfcautomation.ui.screens.ManagementScreen
import com.example.nfcautomation.ui.screens.MenuScreen
import com.example.nfcautomation.ui.screens.AttendanceScreen
import com.example.nfcautomation.ui.screens.ScheduleEditorScreen
import com.example.nfcautomation.ui.screens.CampusMapScreen
import com.example.nfcautomation.ui.theme.NFCAutomationTheme
import com.example.nfcautomation.ui.viewmodel.MainViewModel
import com.example.nfcautomation.ui.viewmodel.ManagementViewModel
import com.example.nfcautomation.ui.viewmodel.Screen
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.FileProvider
import java.io.File
import android.widget.Toast

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private val viewModel: MainViewModel by viewModels()
    private val managementViewModel: ManagementViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQuickAccessService()
            viewModel.refreshState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("NFC_START", "Iniciando onCreate")

        // 1. Inicializar configuración y Python
        val configPath = ConfigManager.ensureConfigExists(this)
        if (!Python.isStarted()) {
            Log.d("NFC_START", "Iniciando Python")
            Python.start(AndroidPlatform(this))
        }

        // 2. Cargar estado inicial (incluyendo idioma)
        try {
            val py = Python.getInstance()
            val bridge = py.getModule("bridge")
            bridge.callAttr("setup_base_path", configPath)
            viewModel.refreshState()
            viewModel.fetchCampusData()
            
            // 3. Aplicar Idioma INMEDIATAMENTE (antes de setContent)
            applyLocale(viewModel.currentLanguage)
            bridge.callAttr("set_language", viewModel.currentLanguage)
        } catch (e: Exception) {
            Log.e("NFC_START", "Error en inicialización", e)
        }

        // Habilitar el dibujo debajo de las barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Configurar sincronización de ajustes en tiempo real
        managementViewModel.onDarkModeToggle = { viewModel.isDarkMode = it }
        managementViewModel.onNotificationToggle = { 
            viewModel.isNotificationEnabled = it
            if (it) checkNotificationPermission() else stopQuickAccessService()
        }
        managementViewModel.onLanguageChange = { lang ->
            setAppLanguage(lang)
        }

        checkNotificationPermission()
        handleIntent(intent)

        Log.d("NFC_START", "Cargando UI")
        setContent {
            // Observar cambios en el archivo de exportación
            LaunchedEffect(viewModel.exportResultFile) {
                viewModel.exportResultFile?.let { path ->
                    shareExcelFile(path)
                    viewModel.clearExportState()
                }
            }

            // Observar errores de exportación
            LaunchedEffect(viewModel.exportErrorMessage) {
                viewModel.exportErrorMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, getString(R.string.export_error, msg), Toast.LENGTH_LONG).show()
                    viewModel.clearExportState()
                }
            }

            NFCAutomationTheme(darkMode = viewModel.isDarkMode) {
                // Manejo del botón atrás del sistema:
                // Si no estamos en el menú, volvemos al menú. Si estamos en el menú, salimos (comportamiento por defecto).
                BackHandler(enabled = viewModel.currentScreen != Screen.MENU) {
                    viewModel.currentScreen = Screen.MENU
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (viewModel.currentScreen) {
                        Screen.MENU -> MenuScreen(viewModel, onGoToManagement = { viewModel.currentScreen = Screen.MANAGEMENT })
                        Screen.EXECUTION -> ExecutionScreen(viewModel)
                        Screen.MANAGEMENT -> ManagementScreen(managementViewModel, onBack = { viewModel.currentScreen = Screen.MENU })
                        Screen.ATTENDANCE -> AttendanceScreen(viewModel)
                        Screen.SCHEDULE_EDITOR -> ScheduleEditorScreen(viewModel)
                        Screen.CAMPUS_MAP -> CampusMapScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun applyLocale(lang: String) {
        try {
            val locale = java.util.Locale(lang)
            java.util.Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            // Actualizamos la configuración de la actividad
            resources.updateConfiguration(config, resources.displayMetrics)
            // También la del contexto de la aplicación para que persista en servicios/notificaciones
            applicationContext.resources.updateConfiguration(config, resources.displayMetrics)
        } catch (e: Exception) {
            Log.e("NFC_LANG", "Error aplicando locale: $lang", e)
        }
    }

    private fun setAppLanguage(lang: String) {
        Log.d("NFC_LANG", "Cambiando idioma a: $lang")
        // 1. Informar a Python
        try {
            val py = Python.getInstance()
            val bridge = py.getModule("bridge")
            bridge.callAttr("set_language", lang)
        } catch (e: Exception) {
            Log.e("NFC_LANG", "Error informando a Python", e)
        }

        // 2. Aplicar y recrear
        applyLocale(lang)
        recreate()
    }

    private fun checkNotificationPermission() {
        if (!viewModel.isNotificationEnabled) {
            stopQuickAccessService()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startQuickAccessService()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startQuickAccessService()
        }
    }

    private fun startQuickAccessService() {
        if (!viewModel.isNotificationEnabled) return
        val serviceIntent = Intent(this, QuickAccessService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopQuickAccessService() {
        val serviceIntent = Intent(this, QuickAccessService::class.java)
        stopService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        updateNfcStatus()
        
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or 
            NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        
        viewModel.refreshState()
        checkNotificationPermission() // Re-evaluar servicio al volver
    }

    private fun updateNfcStatus() {
        viewModel.isNfcEnabled = nfcAdapter?.isEnabled ?: false
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
        
        val action = intent.getStringExtra(QuickAccessService.EXTRA_ACTION)
        if (action == QuickAccessService.ACTION_DEACTIVATE) {
            viewModel.deactivateCurrentMode()
            return
        }

        val intentAction = intent.action
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intentAction || 
            NfcAdapter.ACTION_NDEF_DISCOVERED == intentAction || 
            NfcAdapter.ACTION_TAG_DISCOVERED == intentAction) {
            
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            }
            
            val tagId = tag?.id?.let { bytesToHex(it) }
            if (tagId != null) {
                if (managementViewModel.isNfcReadingForTag) {
                    managementViewModel.pendingTagUid = tagId
                    managementViewModel.isNfcReadingForTag = false
                    managementViewModel.updateTagWorkflow(tagId, getString(R.string.select_automation))
                } else if (viewModel.currentScreen != Screen.MANAGEMENT) {
                    // BLOQUEO: Solo procesamos si no estamos gestionando
                    viewModel.processTag(tagId)
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        val tagId = tag?.id?.let { bytesToHex(it) }
        if (tagId != null) {
            if (managementViewModel.isNfcReadingForTag) {
                managementViewModel.pendingTagUid = tagId
                managementViewModel.isNfcReadingForTag = false
                managementViewModel.updateTagWorkflow(tagId, getString(R.string.select_automation))
            } else if (viewModel.currentScreen != Screen.MANAGEMENT) {
                viewModel.processTag(tagId)
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

    private fun shareExcelFile(filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.export_title)))
            Toast.makeText(this, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("NFC_EXPORT", "Error compartiendo archivo", e)
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
