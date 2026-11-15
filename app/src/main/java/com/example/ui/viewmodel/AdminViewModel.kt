package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdGatePolicy
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.AdminSecurityPolicy
import com.example.data.model.CustomMediaEntity
import com.example.data.model.MediaItem
import com.example.data.model.PasswordChangeValidation
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class AdminTab(val title: String) {
    ANALYTICS("Analytics"),
    ADD_CONTENT("Add Content"),
    MANAGE_CONTENT("Manage Items"),
    ANNOUNCEMENTS("Announcements"),
    AD_CONFIG("Ad Gate"),
    SECURITY("Security")
}

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val isSecurityLoaded: Boolean = false,
    val isUsingDefaultPasscode: Boolean = true,
    val authError: String? = null,
    val currentTab: AdminTab = AdminTab.ANALYTICS,
    
    // Add/Edit Content Form
    val formId: String = "",
    val formTmdbId: Long? = null,
    val formTitle: String = "",
    val formOverview: String = "",
    val formPosterUrl: String = "",
    val formBackdropUrl: String = "",
    val formMediaType: String = "movie", // "movie" or "tv"
    val formReleaseYear: String = "",
    val formVoteAverage: String = "",
    val formGenres: String = "",
    val formVidlyId: String = "",
    val formSeasons: String = "1",
    val formEpisodes: String = "10",
    val isEditing: Boolean = false,
    val formStatusMessage: String? = null,
    val isFormSuccess: Boolean = false,

    // TMDb Auto-Fill Search
    val tmdbSearchQuery: String = "",
    val tmdbSearchResults: List<MediaItem> = emptyList(),
    val isTmdbSearching: Boolean = false,
    val hasSearchedTmdb: Boolean = false,
    val tmdbSearchError: String? = null,
    val selectedTmdbPreview: MediaItem? = null,

    // Live Analytics & Connectivity
    val liveTmdbCount: Int = 0,
    val isTmdbConnected: Boolean? = null,
    val isCheckingTmdb: Boolean = false,

    // Announcement Form
    val announcementTitle: String = "",
    val announcementMessage: String = "",
    val announcementType: String = "INFO", // "INFO", "WARNING", "UPDATE"
    val announcementStatusMessage: String? = null,

    // Adsterra Pop-under Ad Gate Config
    val adIsEnabled: Boolean = false,
    val adFormat: String = "Pop-under",
    val adUrl: String = "",
    val adPopUnderScript: String = "",
    val adHeadline: String = "Sponsored Access Pass",
    val adDescription: String = "Tap the sponsor ad below and stay on the ad page for 30 seconds to unlock 6 hours of access.",
    val adCtaText: String = "Open Sponsor Ad",
    val adRequiredStaySeconds: String = "30",
    val adCooldownHours: String = "6",
    val adLastCompletedAtMillis: Long = 0L,
    val adStatusMessage: String? = null,
    val isAdStatusSuccess: Boolean = true,

    // Admin Panel Password Change Form
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmPasswordInput: String = "",
    val passwordStatusMessage: String? = null,
    val isPasswordStatusSuccess: Boolean = false
)

class AdminViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    val customMediaList: StateFlow<List<CustomMediaEntity>> = repository.customMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcementsList: StateFlow<List<AdminAnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tmdbSearchHistory: StateFlow<List<TmdbSearchHistoryEntity>> = repository.tmdbSearchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _watchlistCount = MutableStateFlow(0)
    val watchlistCount: StateFlow<Int> = _watchlistCount.asStateFlow()

    private val _continueWatchingCount = MutableStateFlow(0)
    val continueWatchingCount: StateFlow<Int> = _continueWatchingCount.asStateFlow()

    private var hasInitializedAdForm = false
    private var storedPasscodeHash: String =
        AdminSecurityPolicy.hashPasscode(AdminSecurityPolicy.DEFAULT_PASSCODE)

    init {
        viewModelScope.launch {
            repository.initSeedDataIfEmpty()
        }
        observeAdConfig()
        observeAdminSecurity()
        observeLibraryCounts()
        refreshSystemHealth()
    }

    private fun observeLibraryCounts() {
        viewModelScope.launch {
            repository.watchlist.collect { _watchlistCount.value = it.size }
        }
        viewModelScope.launch {
            repository.continueWatching.collect { _continueWatchingCount.value = it.size }
        }
    }

    fun refreshSystemHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingTmdb = true)
            val (connected, count) = repository.checkTmdbHealth()
            _uiState.value = _uiState.value.copy(
                isTmdbConnected = connected,
                liveTmdbCount = count,
                isCheckingTmdb = false
            )
        }
    }

    private fun observeAdminSecurity() {
        viewModelScope.launch {
            repository.adminSecurity.collect { security ->
                if (security != null) {
                    storedPasscodeHash = security.passcodeHash
                    _uiState.value = _uiState.value.copy(
                        isSecurityLoaded = true,
                        isUsingDefaultPasscode = security.isDefault
                    )
                }
            }
        }
    }

    private fun observeAdConfig() {
        viewModelScope.launch {
            repository.adConfig.collect { config ->
                if (config != null) {
                    if (!hasInitializedAdForm) {
                        hasInitializedAdForm = true
                        _uiState.value = _uiState.value.copy(
                            adIsEnabled = config.isEnabled,
                            adFormat = config.adFormat,
                            adUrl = config.adUrl,
                            adPopUnderScript = config.popUnderScript,
                            adHeadline = config.headline,
                            adDescription = config.description,
                            adCtaText = config.ctaText,
                            adRequiredStaySeconds = config.requiredStaySeconds.toString(),
                            adCooldownHours = config.cooldownHours.toString(),
                            adLastCompletedAtMillis = config.lastCompletedAtMillis
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            adLastCompletedAtMillis = config.lastCompletedAtMillis
                        )
                    }
                }
            }
        }
    }

    fun login(pin: String): Boolean {
        return if (AdminSecurityPolicy.verifyPasscode(pin, storedPasscodeHash)) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                authError = null
            )
            true
        } else {
            val errorMsg = if (_uiState.value.isUsingDefaultPasscode) {
                "Invalid Passcode. (Hint: default is ${AdminSecurityPolicy.DEFAULT_PASSCODE})"
            } else {
                "Invalid password. Please try again."
            }
            _uiState.value = _uiState.value.copy(authError = errorMsg)
            false
        }
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = false,
            authError = null
        )
    }

    fun switchTab(tab: AdminTab) {
        _uiState.value = _uiState.value.copy(
            currentTab = tab,
            formStatusMessage = null,
            adStatusMessage = null,
            passwordStatusMessage = null
        )
    }

    // TMDb Search for Auto-Fill
    fun onTmdbSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(tmdbSearchQuery = query)
    }

    fun searchTmdb(explicitQuery: String? = null) {
        val query = (explicitQuery ?: _uiState.value.tmdbSearchQuery).trim()
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(tmdbSearchQuery = query, tmdbSearchError = null)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTmdbSearching = true, tmdbSearchError = null)
            // Persist the searched movie title in Room database (maintains top 5)
            repository.saveTmdbSearch(query)
            val result = repository.searchTitles(query)
            result.fold(
                onSuccess = { results ->
                    _uiState.value = _uiState.value.copy(
                        tmdbSearchResults = results,
                        isTmdbSearching = false,
                        hasSearchedTmdb = true,
                        tmdbSearchError = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        tmdbSearchResults = emptyList(),
                        isTmdbSearching = false,
                        hasSearchedTmdb = true,
                        tmdbSearchError = "TMDb search failed: ${err.localizedMessage ?: "Network unavailable"}"
                    )
                }
            )
        }
    }

    fun selectTmdbPreview(item: MediaItem?) {
        _uiState.value = _uiState.value.copy(selectedTmdbPreview = item)
        if (item != null && item.mediaType == "tv" && item.seasons.isEmpty()) {
            viewModelScope.launch {
                val enriched = repository.fetchEnrichedTvSeries(item)
                if (_uiState.value.selectedTmdbPreview?.id == item.id) {
                    _uiState.value = _uiState.value.copy(selectedTmdbPreview = enriched)
                }
            }
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            repository.deleteTmdbSearch(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearTmdbSearchHistory()
        }
    }

    fun autoFillFromTmdb(item: MediaItem) {
        _uiState.value = _uiState.value.copy(
            formTmdbId = item.tmdbId,
            formTitle = item.title,
            formOverview = item.overview,
            formPosterUrl = item.fullPosterUrl,
            formBackdropUrl = item.fullBackdropUrl,
            formMediaType = item.mediaType,
            formReleaseYear = item.releaseDate.take(4),
            formVoteAverage = String.format(Locale.US, "%.1f", item.voteAverage),
            formGenres = item.genres.joinToString(", "),
            formVidlyId = item.streamId,
            formSeasons = item.numberOfSeasons.toString(),
            formEpisodes = item.episodesPerSeason.toString(),
            selectedTmdbPreview = null,
            formStatusMessage = "✓ Auto-filled details from TMDb for \"${item.title}\"!",
            isFormSuccess = true
        )
    }

    // Form inputs
    fun updateTitle(v: String) { _uiState.value = _uiState.value.copy(formTitle = v) }
    fun updateOverview(v: String) { _uiState.value = _uiState.value.copy(formOverview = v) }
    fun updatePosterUrl(v: String) { _uiState.value = _uiState.value.copy(formPosterUrl = v) }
    fun updateBackdropUrl(v: String) { _uiState.value = _uiState.value.copy(formBackdropUrl = v) }
    fun updateMediaType(v: String) { _uiState.value = _uiState.value.copy(formMediaType = v) }
    fun updateReleaseYear(v: String) { _uiState.value = _uiState.value.copy(formReleaseYear = v) }
    fun updateVoteAverage(v: String) { _uiState.value = _uiState.value.copy(formVoteAverage = v) }
    fun updateGenres(v: String) { _uiState.value = _uiState.value.copy(formGenres = v) }
    fun updateVidlyId(v: String) { _uiState.value = _uiState.value.copy(formVidlyId = v) }
    fun updateSeasons(v: String) { _uiState.value = _uiState.value.copy(formSeasons = v) }
    fun updateEpisodes(v: String) { _uiState.value = _uiState.value.copy(formEpisodes = v) }

    fun clearForm(keepStatusMessage: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            formId = "",
            formTmdbId = null,
            formTitle = "",
            formOverview = "",
            formPosterUrl = "",
            formBackdropUrl = "",
            formMediaType = "movie",
            formReleaseYear = "",
            formVoteAverage = "",
            formGenres = "",
            formVidlyId = "",
            formSeasons = "1",
            formEpisodes = "10",
            isEditing = false,
            formStatusMessage = if (keepStatusMessage) _uiState.value.formStatusMessage else null
        )
    }

    fun saveContent() {
        val s = _uiState.value
        if (s.formTitle.isBlank() || s.formVidlyId.isBlank()) {
            _uiState.value = s.copy(
                formStatusMessage = "Title and Vidly ID / Link are required!",
                isFormSuccess = false
            )
            return
        }

        viewModelScope.launch {
            val entity = CustomMediaEntity(
                id = if (s.isEditing && s.formId.isNotBlank()) s.formId else "custom_${UUID.randomUUID().toString().take(8)}",
                title = s.formTitle.trim(),
                overview = s.formOverview.trim(),
                posterPath = s.formPosterUrl.trim(),
                backdropPath = s.formBackdropUrl.trim(),
                mediaType = s.formMediaType,
                releaseDate = s.formReleaseYear.trim(),
                voteAverage = s.formVoteAverage.replace(",", ".").toDoubleOrNull() ?: 0.0,
                genres = s.formGenres.trim(),
                vidlyId = s.formVidlyId.trim(),
                tmdbId = s.formTmdbId ?: s.formVidlyId.trim().toLongOrNull(),
                numberOfSeasons = s.formSeasons.toIntOrNull() ?: 1,
                episodesPerSeason = s.formEpisodes.toIntOrNull() ?: 10
            )

            if (s.isEditing) {
                repository.updateCustomMedia(entity)
                _uiState.value = _uiState.value.copy(
                    formStatusMessage = "Successfully updated \"${entity.title}\"!",
                    isFormSuccess = true
                )
            } else {
                repository.insertCustomMedia(entity)
                _uiState.value = _uiState.value.copy(
                    formStatusMessage = "Successfully added \"${entity.title}\" to Cinetra catalog!",
                    isFormSuccess = true
                )
            }
            clearForm(keepStatusMessage = true)
        }
    }

    fun startEdit(item: CustomMediaEntity) {
        _uiState.value = _uiState.value.copy(
            formId = item.id,
            formTmdbId = item.tmdbId,
            formTitle = item.title,
            formOverview = item.overview,
            formPosterUrl = item.posterPath,
            formBackdropUrl = item.backdropPath,
            formMediaType = item.mediaType,
            formReleaseYear = item.releaseDate,
            formVoteAverage = item.voteAverage.toString(),
            formGenres = item.genres,
            formVidlyId = item.vidlyId,
            formSeasons = item.numberOfSeasons.toString(),
            formEpisodes = item.episodesPerSeason.toString(),
            isEditing = true,
            currentTab = AdminTab.ADD_CONTENT,
            formStatusMessage = "Editing \"${item.title}\""
        )
    }

    fun deleteContent(id: String) {
        viewModelScope.launch {
            repository.deleteCustomMedia(id)
        }
    }

    // Announcement actions
    fun updateAnnouncementTitle(v: String) { _uiState.value = _uiState.value.copy(announcementTitle = v) }
    fun updateAnnouncementMessage(v: String) { _uiState.value = _uiState.value.copy(announcementMessage = v) }
    fun updateAnnouncementType(v: String) { _uiState.value = _uiState.value.copy(announcementType = v) }

    fun saveAnnouncement() {
        val s = _uiState.value
        if (s.announcementTitle.isBlank() || s.announcementMessage.isBlank()) {
            _uiState.value = s.copy(announcementStatusMessage = "Please enter both Title and Message.")
            return
        }
        viewModelScope.launch {
            repository.insertAnnouncement(
                title = s.announcementTitle.trim(),
                message = s.announcementMessage.trim(),
                type = s.announcementType
            )
            _uiState.value = _uiState.value.copy(
                announcementTitle = "",
                announcementMessage = "",
                announcementStatusMessage = "✓ Announcement published to user home screen!"
            )
        }
    }

    fun toggleAnnouncementActive(id: Int, active: Boolean) {
        viewModelScope.launch {
            repository.toggleAnnouncementActive(id, active)
        }
    }

    fun deleteAnnouncement(id: Int) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }

    // Adsterra Pop-under Ad Gate Config actions
    fun updateAdEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(adIsEnabled = enabled, adStatusMessage = null)
    }

    fun updateAdFormat(format: String) {
        _uiState.value = _uiState.value.copy(adFormat = format, adStatusMessage = null)
    }

    fun updateAdUrl(url: String) {
        _uiState.value = _uiState.value.copy(adUrl = url, adStatusMessage = null)
    }

    fun updateAdPopUnderScript(script: String) {
        _uiState.value = _uiState.value.copy(adPopUnderScript = script, adStatusMessage = null)
    }

    fun updateAdHeadline(headline: String) {
        _uiState.value = _uiState.value.copy(adHeadline = headline, adStatusMessage = null)
    }

    fun updateAdDescription(description: String) {
        _uiState.value = _uiState.value.copy(adDescription = description, adStatusMessage = null)
    }

    fun updateAdCtaText(ctaText: String) {
        _uiState.value = _uiState.value.copy(adCtaText = ctaText, adStatusMessage = null)
    }

    fun updateAdRequiredStaySeconds(seconds: String) {
        _uiState.value = _uiState.value.copy(
            adRequiredStaySeconds = seconds.filter { it.isDigit() },
            adStatusMessage = null
        )
    }

    fun updateAdCooldownHours(hours: String) {
        _uiState.value = _uiState.value.copy(
            adCooldownHours = hours.filter { it.isDigit() },
            adStatusMessage = null
        )
    }

    fun saveAdConfig() {
        val s = _uiState.value
        val trimmedUrl = s.adUrl.trim()
        val trimmedScript = s.adPopUnderScript.trim()

        if (s.adIsEnabled && trimmedUrl.isBlank() && trimmedScript.isBlank()) {
            _uiState.value = s.copy(
                adStatusMessage = "Please enter an Adsterra Pop-under URL or Pop-under Script code before enabling the ad gate.",
                isAdStatusSuccess = false
            )
            return
        }

        val staySeconds = (s.adRequiredStaySeconds.toIntOrNull() ?: AdGatePolicy.DEFAULT_STAY_SECONDS)
            .coerceIn(5, 300)
        val cooldownHours = (s.adCooldownHours.toIntOrNull() ?: AdGatePolicy.DEFAULT_COOLDOWN_HOURS)
            .coerceIn(1, 168)

        val entity = AdConfigEntity(
            id = 1,
            isEnabled = s.adIsEnabled,
            adNetwork = "Adsterra",
            adFormat = s.adFormat.ifBlank { "Pop-under" },
            adUrl = if (trimmedUrl.isNotBlank()) AdGatePolicy.normalizeUrl(trimmedUrl) else "",
            popUnderScript = trimmedScript,
            headline = s.adHeadline.trim().ifBlank { "Sponsored Access Pass" },
            description = s.adDescription.trim().ifBlank {
                "Tap the sponsor ad below and stay on the ad page for $staySeconds seconds to unlock $cooldownHours hours of access."
            },
            ctaText = s.adCtaText.trim().ifBlank { "Open Sponsor Ad" },
            requiredStaySeconds = staySeconds,
            cooldownHours = cooldownHours,
            lastCompletedAtMillis = s.adLastCompletedAtMillis,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveAdConfig(entity)
            _uiState.value = _uiState.value.copy(
                adUrl = entity.adUrl,
                adRequiredStaySeconds = staySeconds.toString(),
                adCooldownHours = cooldownHours.toString(),
                adStatusMessage = if (entity.isEnabled) {
                    "✓ Saved! Adsterra ${entity.adFormat} gate enabled (${staySeconds}s stay, every ${cooldownHours}h)."
                } else {
                    "✓ Saved! Adsterra ad gate configuration updated (currently disabled)."
                },
                isAdStatusSuccess = true
            )
        }
    }

    fun resetAdCooldown() {
        viewModelScope.launch {
            repository.resetAdCooldown()
            _uiState.value = _uiState.value.copy(
                adLastCompletedAtMillis = 0L,
                adStatusMessage = "✓ 6-hour timer reset! The pop-up ad will appear immediately on next main app open.",
                isAdStatusSuccess = true
            )
        }
    }

    // Admin Panel Password Change actions
    fun updateCurrentPasswordInput(value: String) {
        _uiState.value = _uiState.value.copy(
            currentPasswordInput = value,
            passwordStatusMessage = null
        )
    }

    fun updateNewPasswordInput(value: String) {
        _uiState.value = _uiState.value.copy(
            newPasswordInput = value,
            passwordStatusMessage = null
        )
    }

    fun updateConfirmPasswordInput(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmPasswordInput = value,
            passwordStatusMessage = null
        )
    }

    fun changeAdminPassword() {
        val s = _uiState.value
        when (
            val result = AdminSecurityPolicy.validatePasswordChange(
                currentInput = s.currentPasswordInput,
                newPassword = s.newPasswordInput,
                confirmPassword = s.confirmPasswordInput,
                storedHash = storedPasscodeHash
            )
        ) {
            is PasswordChangeValidation.Invalid -> {
                _uiState.value = s.copy(
                    passwordStatusMessage = result.errorMessage,
                    isPasswordStatusSuccess = false
                )
            }
            is PasswordChangeValidation.Valid -> {
                storedPasscodeHash = result.newHash
                viewModelScope.launch {
                    repository.saveAdminPasscodeHash(
                        newHash = result.newHash,
                        isDefault = false
                    )
                    _uiState.value = _uiState.value.copy(
                        isUsingDefaultPasscode = false,
                        currentPasswordInput = "",
                        newPasswordInput = "",
                        confirmPasswordInput = "",
                        passwordStatusMessage = "✓ Admin password updated! Use your new password next time you unlock the panel.",
                        isPasswordStatusSuccess = true
                    )
                }
            }
        }
    }
}

class AdminViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
