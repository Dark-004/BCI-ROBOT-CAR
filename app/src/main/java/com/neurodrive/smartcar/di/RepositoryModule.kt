package com.neurodrive.smartcar.di




import com.neurodrive.smartcar.data.repository.BioSignalRepository
import com.neurodrive.smartcar.domain.SignalProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBioSignalRepository(
        signalProcessor: SignalProcessor
    ): BioSignalRepository {
        return BioSignalRepository(signalProcessor)
    }
}