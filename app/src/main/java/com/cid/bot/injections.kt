package com.cid.bot

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.persistence.room.Room
import android.content.Context
import com.cid.bot.data.AppDatabase
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DaggerApplication
import dagger.android.support.AndroidSupportInjectionModule
import dagger.multibindings.IntoMap
import javax.inject.Singleton

class AppApplication : DaggerApplication() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder().create(this)
    }
}

@Module
class AppModule {
    @Provides
    fun providesContext(application: AppApplication): Context {
        return application.applicationContext
    }
}

@Module
internal abstract class ViewModelBuilder {
    @Binds
    internal abstract fun bindViewModelFactory(factory: DaggerAwareViewModelFactory): ViewModelProvider.Factory
}

@Module
class DatabaseModule {
    @Provides
    fun providesAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app.db"
        ).fallbackToDestructiveMigration().build()
    }
}

@Module
internal abstract class ChatActivityModule {
    @ContributesAndroidInjector()
    internal abstract fun chatActivity(): ChatActivity

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindChatViewModel(viewModel: ChatViewModel): ViewModel
}

@Module
internal abstract class ProfileActivityModule {
    @ContributesAndroidInjector()
    internal abstract fun profileActivity(): ProfileActivity

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(viewModel: ProfileViewModel): ViewModel
}

@Module
internal abstract class SignActivityModule {
    @ContributesAndroidInjector()
    internal abstract fun signActivity(): SignActivity

    @Binds
    @IntoMap
    @ViewModelKey(SignViewModel::class)
    abstract fun bindSignViewModel(viewModel: SignViewModel): ViewModel
}

@Singleton
@Component(modules=[
    AndroidSupportInjectionModule::class,
    AppModule::class,
    DatabaseModule::class,
    ViewModelBuilder::class,
    ChatActivityModule::class,
    ProfileActivityModule::class,
    SignActivityModule::class
])
interface AppComponent : AndroidInjector<AppApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<AppApplication>()
}
