package com.example.intimesimple.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.intimesimple.MainActivity
import com.example.intimesimple.R
import com.example.intimesimple.services.TimerService
import com.example.intimesimple.utils.Constants.ACTION_CANCEL
import com.example.intimesimple.utils.Constants.ACTION_PAUSE
import com.example.intimesimple.utils.Constants.ACTION_RESUME
import com.example.intimesimple.utils.Constants.ACTION_SHOW_MAIN_ACTIVITY
import com.example.intimesimple.utils.Constants.NOTIFICATION_CHANNEL_ID
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Singleton

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @MainActivityPendingIntent
    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent(
        @ApplicationContext app: Context
    ): PendingIntent = PendingIntent.getActivity(
        app,
        0,
        Intent(app, MainActivity::class.java).also {
            it.action = ACTION_SHOW_MAIN_ACTIVITY
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )


    @CancelActionPendingIntent
    @ServiceScoped
    @Provides
    fun provideCancelActionPendingIntent(
            @ApplicationContext app: Context
    ): PendingIntent = PendingIntent.getService(
            app,
            1,
            Intent(app, TimerService::class.java).also {
                it.action = ACTION_CANCEL
            },
            PendingIntent.FLAG_UPDATE_CURRENT
    )


    @ResumeActionPendingIntent
    @ServiceScoped
    @Provides
    fun provideResumeActionPendingIntent(
            @ApplicationContext app: Context
    ): PendingIntent = PendingIntent.getService(
            app,
            2,
            Intent(app, TimerService::class.java).also {
                it.action = ACTION_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT
    )


    @PauseActionPendingIntent
    @ServiceScoped
    @Provides
    fun providePauseActionPendingIntent(
            @ApplicationContext app: Context
    ): PendingIntent = PendingIntent.getService(
            app,
            3,
            Intent(app, TimerService::class.java).also {
                it.action = ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT
    )


    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context,
        @MainActivityPendingIntent pendingIntent: PendingIntent
    ): NotificationCompat.Builder = NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_alarm)
        .setContentTitle("INTime")
        .setContentText("00:00:00")
        .setContentIntent(pendingIntent)
}