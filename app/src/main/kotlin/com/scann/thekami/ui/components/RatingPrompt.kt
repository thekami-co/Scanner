package com.scann.thekami.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.scann.thekami.R
import com.scann.thekami.util.Haptics

@Composable
fun RatingPromptDialog(
    onRate: (Int) -> Unit,
    onNotNow: () -> Unit,
    onNeverAsk: () -> Unit
) {
    val view = LocalView.current
    var stars by rememberSaveable { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text("Enjoying Scann?") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tap a star to rate your experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = {
                                Haptics.tick(view)
                                stars = i
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.hgi_star),
                                contentDescription = "$i star",
                                tint = if (i <= stars) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNeverAsk) {
                    Text("Don't ask again")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = stars > 0, onClick = { onRate(stars) }) {
                Text("Rate")
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) { Text("Not now") }
        }
    )
}
