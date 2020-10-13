package com.example.intimesimple.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.example.intimesimple.MainActivity
import com.example.intimesimple.R
import com.example.intimesimple.data.local.TimerState
import com.example.intimesimple.data.local.Workout
import com.example.intimesimple.di.CancelActionPendingIntent
import com.example.intimesimple.di.PauseActionPendingIntent
import com.example.intimesimple.di.ResumeActionPendingIntent
import com.example.intimesimple.repositories.WorkoutRepository
import com.example.intimesimple.utils.Constants
import com.example.intimesimple.utils.Constants.ACTION_CANCEL
import com.example.intimesimple.utils.Constants.ACTION_PAUSE
import com.example.intimesimple.utils.Constants.ACTION_RESUME
import com.example.intimesimple.utils.Constants.ACTION_START
import com.example.intimesimple.utils.Constants.EXTRA_WORKOUT_ID
import com.example.intimesimple.utils.Constants.NOTIFICATION_ID
import com.example.intimesimple.utils.Constants.TIMER_UPDATE_INTERVAL
import com.example.intimesimple.utils.getFormattedStopWatchTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : LifecycleService(){

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder

    @ResumeActionPendingIntent
    @Inject
    lateinit var resumeActionPendingIntent: PendingIntent

    @PauseActionPendingIntent
    @Inject
    lateinit var pauseActionPendingIntent: PendingIntent

    @CancelActionPendingIntent
    @Inject
    lateinit var cancelActionPendingIntent: PendingIntent

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var workout: Workout? = null

    private var firstRun = true
    private var isInitialized = false
    private var isKilled = false

    private var timer: CountDownTimer? = null
    private var millisToCompletion = 0L
    private var lastSecondTimestamp = 0L
    private var repetitionIndex = 0

    companion object{
        val timerState = MutableLiveData<TimerState>()
        val timeInMillis = MutableLiveData<Long>()
        val progressTimeInMillis = MutableLiveData<Long>()
        val repetitionCount = MutableLiveData<Int>()
    }


    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")

        currentNotificationBuilder = baseNotificationBuilder

        // observe timerState and update notification actions
        timerState.observe(this, Observer {
            if(!isKilled)
                timerState.value?.let {
                    updateNotificationActions(it)
                }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    @SuppressLint("BinaryOperationInTimber")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            when(it.action){
                ACTION_START -> {
                    Timber.d("ACTION_START - firstRun: $firstRun")
                    if(firstRun){
                       // First run, fetch workout from db, start service
                        it.extras?.let {bundle ->
                            val id = bundle.getLong(EXTRA_WORKOUT_ID)

                            currentNotificationBuilder
                                    .setContentIntent(buildPendingIntentWithId(id))

                            serviceScope.launch {
                                workout = workoutRepository.getWorkout(id).first()
                                if(!isInitialized){
                                            // Post new timerState
                                            timerState.postValue(TimerState.RUNNING)
                                            // Post new timeInMillis -> workout.exerciseTime
                                            timeInMillis.postValue(workout?.exerciseTime)
                                            repetitionCount.postValue(workout?.repetitions)
                                            // start foreground service + timer
                                            startForegroundService()
                                            isInitialized = true
                                }
                            }
                        }
                        firstRun = false
                    }else{
                        // Reset timerState
                        timerState.postValue(TimerState.RUNNING)
                        workout?.let {wo ->
                            // Reset timeInMillis -> workout.exerciseTime
                            timeInMillis.postValue(wo.exerciseTime)
                        }

                        // start Timer, service already running
                        startTimer(false)
                    }
                }

                ACTION_PAUSE -> {
                    Timber.d("ACTION_PAUSE")
                    // Post new timerState
                    timerState.postValue(TimerState.PAUSED)
                    stopTimer()
                }

                ACTION_RESUME -> {
                    Timber.d("ACTION_RESUME")
                    // Post new timerState
                    timerState.postValue(TimerState.RUNNING)
                    startTimer(true)
                }

                ACTION_CANCEL -> {
                    Timber.d("ACTION_CANCEL")
                    stopForegroundService()
                }
                else -> {}
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }


    private fun startTimer(wasPaused: Boolean){
        // Only start timer if workout is not null
        //Timber.d("Timer Workout - ${workout.hashCode()}")
        workout?.let {
            val time = if (wasPaused) millisToCompletion else it.exerciseTime
            timeInMillis.postValue(time)
            lastSecondTimestamp = time
            //Timber.d("Starting timer... with $time countdown")
            timer = object : CountDownTimer(time, TIMER_UPDATE_INTERVAL) {
                override fun onTick(millisUntilFinished: Long) {
                    millisToCompletion = millisUntilFinished
                    progressTimeInMillis.postValue(millisUntilFinished)
                    //Timber.d("timeInMillis $millisToCompletion")
                    if(millisUntilFinished <= lastSecondTimestamp - 1000L){
                        timeInMillis.postValue(lastSecondTimestamp - 1000L)
                        lastSecondTimestamp -= 1000L
                    }
                }

                override fun onFinish() {
                    //Timber.d("Timer finished")
                    repetitionIndex += 1
                    if((it.repetitions - repetitionIndex) > 0){
                        repetitionCount.postValue(repetitionCount.value?.minus(1))
                        startTimer(false)
                    }else stopForegroundService()
                }
            }.start()
        }
    }

    private fun stopTimer(){
        timer?.cancel()
    }

    private fun startForegroundService(){
        startTimer(false)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager = notificationManager)
        }

        Timber.d("Starting foregroundService")
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        // Observe timeInMillis and update notification
        timeInMillis.observe(this, Observer {
            if (!isKilled){
                workout?.let {wo ->
                    val notification = currentNotificationBuilder
                            .setContentTitle(wo.name)
                            .setContentText(getFormattedStopWatchTime(it))

                    notificationManager.notify(NOTIFICATION_ID, notification.build())
                }
            }
        })
    }

    private fun stopForegroundService(){
        Timber.d("Stopping foregroundService")
        timer?.cancel()
        timerState.postValue(TimerState.EXPIRED)
        workout?.let {
            // Reset timeInMillis -> workout.exerciseTime
            timeInMillis.postValue(it.exerciseTime)
            progressTimeInMillis.postValue(it.exerciseTime)
        }
        isKilled = true
        repetitionIndex = 0
        firstRun = true
        stopForeground(true)
        stopSelf()
    }

    private fun updateNotificationActions(state: TimerState){
        // Updates actions of current notification depending on TimerState
        val notificationActionText = if(state == TimerState.RUNNING) "Pause" else "Resume"

        // Build pendingIntent
        val pendingIntent = if(state == TimerState.RUNNING){
            pauseActionPendingIntent
        }else{
            resumeActionPendingIntent
        }

        // Get notificationManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Clear current actions
        currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        // Set Action, icon seems irrelevant
        currentNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_alarm, notificationActionText, pendingIntent)
                .addAction(R.drawable.ic_alarm, "Cancel", cancelActionPendingIntent)
        notificationManager.notify(NOTIFICATION_ID, currentNotificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildPendingIntentWithId(id: Long): PendingIntent {
        Timber.d("buildPendingIntentWithId - id: $id")
        return PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).also {
                    it.action = Constants.ACTION_SHOW_MAIN_ACTIVITY
                    it.putExtra(EXTRA_WORKOUT_ID, id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}