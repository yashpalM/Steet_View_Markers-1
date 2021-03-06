package com.alkurop.mystreetplaces.di.modules.ui

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.alkurop.mystreetplaces.MyStreetPlacesApp
import com.alkurop.mystreetplaces.db.RealmProvider
import com.alkurop.mystreetplaces.db.RealmProviderImpl
import com.alkurop.mystreetplaces.domain.IntercomEvent
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.realm.RealmConfiguration
import javax.inject.Singleton

typealias IntercomBus = Subject<IntercomEvent>

@Module
open class ApplicationModule(private val application: MyStreetPlacesApp) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun application(): MyStreetPlacesApp {
        return application
    }

    @Provides
    @Singleton
    fun context(): Context {
        return application
    }

    @Provides
    @Singleton
    fun prefs(): SharedPreferences {
        return prefs
    }

    @Provides
    @Singleton
    fun provideIntercomBus(): IntercomBus {
        return PublishSubject.create<IntercomEvent>()
    }

    @Provides
    @Singleton open fun provideRealmProvider(): RealmProvider {
        return RealmProviderImpl(RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build())
    }

    @Provides
    @Singleton open fun provideGson(): Gson {
        return gson
    }
}
