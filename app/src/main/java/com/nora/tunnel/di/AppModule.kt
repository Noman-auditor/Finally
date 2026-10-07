package com.nora.tunnel

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.nora.tunnel.data.secure.SecureStorage
import com.nora.tunnel.data.repository.ProfileRepository
import com.nora.tunnel.data.repository.HistoryRepository
import com.nora.tunnel.ui.screens.home.HomeViewModel

val appModule = module {
    single { SecureStorage(get()) }
    single { (get<android.app.Application>() as NoraApp).db.profileDao() }
    single { (get<android.app.Application>() as NoraApp).db.sessionDao() }
    single { ProfileRepository(get(), get()) }
    single { HistoryRepository(get()) }
    viewModel { HomeViewModel(get()) }
}
