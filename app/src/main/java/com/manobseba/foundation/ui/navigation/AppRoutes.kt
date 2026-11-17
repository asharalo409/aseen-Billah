package com.manobseba.foundation.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable data object Home : AppRoute
    @Serializable data object Overview : AppRoute
    @Serializable data object AiHub : AppRoute
    @Serializable data object Finance : AppRoute
    @Serializable data object Blood : AppRoute
    @Serializable data object Islamic : AppRoute
    @Serializable data object Weather : AppRoute
    @Serializable data object ProjectsWorksMap : AppRoute
    @Serializable data object Members : AppRoute
    @Serializable data object MyDashboard : AppRoute
    @Serializable data object Community : AppRoute
    @Serializable data object Admin : AppRoute
    @Serializable data object AllMenu : AppRoute
}

enum class SubTabCommunity { EVENTS, POLLS, CHAT, NOTICES, VOICES, DUTIES, PUBLIC_FORM }
enum class SubTabIslamic { SALAT, QIBLA, TASBIH, AMOL, ZAKAT, QURAN, HADITH }
enum class SubTabFinance { LEDGER, DONATIONS, EXPENSES, FEE_GRID, FUNDS }
enum class SubTabWorksMap { PROJECTS, WORKS, RELIEF_MAP }
enum class SubTabMembers { DIRECTORY, COMMITTEE, ROLES_GUIDE, THEME_STUDIO, APP_LOCK }
