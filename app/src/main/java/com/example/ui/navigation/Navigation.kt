package com.example.ui.navigation

sealed interface PixelGleamScreen {
    object Gallery : PixelGleamScreen
    data class Gameplay(val artworkId: String) : PixelGleamScreen
    object Daily : PixelGleamScreen
    object MyArt : PixelGleamScreen
    object Settings : PixelGleamScreen
    object Tutorial : PixelGleamScreen
}

interface AppNavigator {
    fun navigateTo(screen: PixelGleamScreen)
    fun navigateBack(): Boolean
    fun currentScreen(): PixelGleamScreen
}
