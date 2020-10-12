package com.example.intimesimple.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue


@Composable
fun TimeInputField(
        modifier: Modifier = Modifier,
        hoursField: @Composable () -> Unit,
        minutesField: @Composable () -> Unit,
        secondsField: @Composable () -> Unit
){
    Surface(
            modifier,
            color = Color.White
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
        ){
            hoursField()
            minutesField()
            secondsField()
        }
    }
}

@Composable
fun DigitInputField(
        modifier: Modifier = Modifier
){
    Surface(
            modifier = modifier,
            color = Color.LightGray
    ){
        var textValue by remember { mutableStateOf(TextFieldValue("00")) }
        CoreTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}


@Preview
@Composable
fun DigitInputFieldPreview(){
    DigitInputField()
}

@Preview
@Composable
fun TimeInputFieldPreview() {
    TimeInputField(
            hoursField = { DigitInputField() },
            minutesField = { DigitInputField() },
            secondsField = { DigitInputField() },
            modifier = Modifier.fillMaxWidth()
    )
}