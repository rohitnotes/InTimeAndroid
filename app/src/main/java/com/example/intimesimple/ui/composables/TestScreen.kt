package com.example.intimesimple.ui.composables

import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.intimesimple.data.local.TimerState
import com.example.intimesimple.services.TestService
import com.example.intimesimple.ui.viewmodels.WorkoutDetailViewModel
import com.example.intimesimple.utils.Constants
import com.example.intimesimple.utils.getFormattedStopWatchTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ConfigurationAmbient
import com.example.intimesimple.data.local.Workout

@Composable
fun TestScreen(
        modifier: Modifier = Modifier,
        sendCommand: (String) -> Unit,
        navigateHome: () -> Unit,
        workoutDetailViewModel: WorkoutDetailViewModel
) {
    val workout by workoutDetailViewModel.workout.observeAsState()

   Scaffold(
           modifier.fillMaxSize(),
           topBar = {
               TopAppBar(
                       title = {
                           workout?.name?.toUpperCase()?.let {
                               Text(
                                       text = it
                               )
                           }
                       },
                       navigationIcon = {
                           IconButton(onClick = {
                               navigateHome()
                           }) {
                               Icon(Icons.Filled.ArrowBack)
                           }
                       }
               )
           },
           bodyContent = {
               workout?.let { it1 ->
                   TestScreenContent(
                           modifier = modifier,
                           sendCommand,
                           it1
                   )
               }
           }
   )
}

@Composable
fun TestScreenContent(
        modifier: Modifier = Modifier,
        sendCommand: (String) -> Unit,
        workout: Workout
){
    val timerState by TestService.timerState.observeAsState(TimerState.EXPIRED)
    val timeInMillis by TestService.timeInMillis.observeAsState()
    val timerRepCount by TestService.repetitionCount.observeAsState()
    val exTimeInMillis: Long = workout.exerciseTime
    val repCount: Int = workout.repetitions
    val progressTime by TestService.progressTimeInMillis.observeAsState(exTimeInMillis)
    val configuration = ConfigurationAmbient.current
    val screenWidth = configuration.screenWidthDp
    val buttonWidth = 0.3f * screenWidth

    ConstraintLayout(modifier = modifier) {
        val buttonRow = createRef()
        val timerText = createRef()
        val stateText = createRef()
        val repText = createRef()
        val progCircle = createRef()

        val buttonModifier = Modifier.width(buttonWidth.dp)
        Text(
                text = (if (timerState == TimerState.EXPIRED)
                    getFormattedStopWatchTime(exTimeInMillis)
                else getFormattedStopWatchTime(timeInMillis)),
                modifier = Modifier.constrainAs(timerText){
                    top.linkTo(parent.top, 172.dp)
                    centerHorizontallyTo(parent)
                },
                color = Color.White,
                style = typography.h2
        )

        TimerCircle(
                modifier = Modifier.constrainAs(progCircle){
                    top.linkTo(parent.top, 8.dp)
                },
                timerState = timerState,
                elapsedTime = if(timerState == TimerState.EXPIRED) exTimeInMillis else progressTime,
                totalTime = exTimeInMillis
        )
        Text(
                text = timerState.stateName,
                modifier = Modifier.constrainAs(stateText){
                    top.linkTo(timerText.bottom,8.dp)
                    centerHorizontallyTo(parent)
                },
                color = Color.White,
                style = typography.h5
        )

        Text(
                text = "${
                    if (timerState == TimerState.EXPIRED)
                        repCount ?: ""
                    else timerRepCount
                }",
                modifier = Modifier.constrainAs(repText){
                    top.linkTo(stateText.bottom, 4.dp)
                    centerHorizontallyTo(parent)
                },
                color = Color.White,
                style = typography.h5
        )


        Row(
                modifier = Modifier.fillMaxWidth()
                        .constrainAs(buttonRow){
                            bottom.linkTo(parent.bottom, 64.dp)
                        },
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            timerState.let {
                when (it) {
                    TimerState.EXPIRED -> {
                        Button(
                                onClick = { sendCommand(Constants.ACTION_START) },
                                shape = RoundedCornerShape(50),
                                modifier = buttonModifier
                        ) {
                            Text("Start")
                        }
                    }
                    TimerState.RUNNING -> {
                        Button(
                                onClick = { sendCommand(Constants.ACTION_PAUSE) },
                                shape = RoundedCornerShape(50),
                                modifier = buttonModifier
                        ) {
                            Text("Pause")
                        }

                        Button(
                                onClick = { sendCommand(Constants.ACTION_CANCEL) },
                                shape = RoundedCornerShape(50),
                                modifier = buttonModifier
                        ) {
                            Text("Cancel")
                        }
                    }
                    TimerState.PAUSED -> {
                        Button(
                                onClick = { sendCommand(Constants.ACTION_RESUME) },
                                shape = RoundedCornerShape(50),
                                modifier = buttonModifier
                        ) {
                            Text("Resume")
                        }

                        Button(
                                onClick = { sendCommand(Constants.ACTION_CANCEL) },
                                shape = RoundedCornerShape(50),
                                modifier = buttonModifier
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
