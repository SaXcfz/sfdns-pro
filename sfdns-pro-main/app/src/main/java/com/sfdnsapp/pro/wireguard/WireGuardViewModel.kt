package com.sfdnsapp.pro.wireguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WireGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WireGuardRepository(application)
    private val manager = WireGuardManager(application.applicationContext)

    private val _profiles = MutableStateFlow<List<WireGuardProfile>>(emptyList())
    val profiles: StateFlow<List<WireGuardProfile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    val connectionState: StateFlow<String> = manager.connectionState
    val rxBytes: StateFlow<Long> = manager.rxBytes
    val txBytes: StateFlow<Long> = manager.txBytes

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    val activeProfile: WireGuardProfile?
        get() = _profiles.value.find { it.id == _activeProfileId.value }

    init {
        _profiles.value = repository.loadProfiles()
        _activeProfileId.value = repository.loadActiveProfileId()
    }

    fun setConnectionStatusExternallyStopped() {
        // برای وقتی MainActivity به‌خاطر روشن‌کردن DNS مجبور شده وایرگارد رو قطع کنه
        manager.disconnect()
    }

    fun connectActiveProfile() {
        val profile = activeProfile ?: run {
            _errorMessage.value = "ابتدا یک پروفایل انتخاب یا اضافه کنید"
            return
        }
        manager.connect(profile) { message ->
            _errorMessage.value = message
        }
    }

    fun disconnect() = manager.disconnect()

    fun isRunning(): Boolean = manager.isRunning

    fun selectProfile(id: String) {
        _activeProfileId.value = id
        repository.saveActiveProfileId(id)
    }

    fun addProfile(profile: WireGuardProfile) {
        val updated = _profiles.value + profile
        _profiles.value = updated
        repository.saveProfiles(updated)
        selectProfile(profile.id)
    }

    fun addProfiles(newProfiles: List<WireGuardProfile>) {
        if (newProfiles.isEmpty()) return
        val updated = _profiles.value + newProfiles
        _profiles.value = updated
        repository.saveProfiles(updated)
        if (_activeProfileId.value == null) {
            selectProfile(newProfiles.first().id)
        }
    }

    fun deleteProfile(id: String) {
        val updated = _profiles.value.filterNot { it.id == id }
        _profiles.value = updated
        repository.saveProfiles(updated)
        if (_activeProfileId.value == id) {
            val fallback = updated.firstOrNull()?.id
            _activeProfileId.value = fallback
            repository.saveActiveProfileId(fallback)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun importFromSubscription(url: String) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            val result = runCatching {
                val content = withContext(Dispatchers.IO) { fetchText(url) }
                WireGuardConfigCodec.parseSubscriptionContent(content)
            }
            result.onSuccess { imported ->
                if (imported.isEmpty()) {
                    _importState.value = ImportState.Error("هیچ کانفیگ معتبری در این لینک پیدا نشد")
                } else {
                    addProfiles(imported)
                    _importState.value = ImportState.Success(imported.size)
                }
            }.onFailure { e ->
                _importState.value = ImportState.Error(e.message ?: "خطا در دریافت لینک")
            }
        }
    }

    fun importFromConfigText(text: String, name: String?) {
        runCatching {
            addProfile(WireGuardConfigCodec.wgQuickTextToProfile(text, name))
        }.onFailure {
            _errorMessage.value = "کانفیگ نامعتبر است: ${it.message ?: "خطای پارس"}"
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    private fun fetchText(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        return try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    sealed interface ImportState {
        data object Idle : ImportState
        data object Loading : ImportState
        data class Success(val count: Int) : ImportState
        data class Error(val message: String) : ImportState
    }
}
