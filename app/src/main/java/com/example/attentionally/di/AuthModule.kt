package com.example.attentionally.di

import android.app.Application
import com.example.attentionally.domain.repository.AuthRepository
import com.example.attentionally.data.repository.AuthRepositoryImpl
import com.example.attentionally.presentation.auth.AuthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * AuthModule: Provides AuthRepositoryImpl and AuthViewModel for Koin DI.
 * Firebase Auth handles all persistence automatically.
 */
val authModule = module {
    // Provide AuthRepository implementation using Firebase
    single<AuthRepository> { AuthRepositoryImpl() }
    // Provide AuthViewModel with repository injected
    viewModel { AuthViewModel(get()) }
}
