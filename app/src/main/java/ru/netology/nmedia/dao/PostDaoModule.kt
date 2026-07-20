package ru.netology.nmedia.dao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.db.AppDb

@InstallIn(SingletonComponent::class)
@Module
object PostDaoModule {
    @Provides
    fun providePostDao(db: AppDb): PostDao = db.postDao()
}