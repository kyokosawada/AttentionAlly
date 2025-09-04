package com.example.attentionally.di

import android.app.Application
import com.example.attentionally.data.AuthRepository
import com.example.attentionally.data.SessionManager
import com.example.attentionally.domain.FirebaseAuthRepository
import com.example.attentionally.presentation.auth.AuthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * AuthModule: Provides SessionManager, FirebaseAuthRepository, and AuthViewModel for Koin DI.
 * Apply this module in Application startup.
 */
val authModule = module {
    // Persist session state, user role, and anonymous status
    single { SessionManager(androidContext()) }
    // Provide AuthRepository implementation using Firebase
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    // Provide AuthViewModel with repository injected
    viewModel { AuthViewModel(get()) }
}
