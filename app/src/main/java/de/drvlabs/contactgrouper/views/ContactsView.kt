package de.drvlabs.contactgrouper.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupEditor(modifier: Modifier = Modifier) {
    Box{
        FloatingActionButton(
            onClick = {},
            modifier = modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Hallo ich bin toll")
        }
    }
}