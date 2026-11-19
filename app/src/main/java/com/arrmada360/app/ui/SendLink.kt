package com.arrmada360.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.data.ConfigStore

/** Shown when a magnet / torrent / nzb link is shared into the app, or a search result is picked. */
@Composable
fun SendLinkDialog(link: String, extra: List<Pair<String, suspend () -> Unit>> = emptyList(), onDismiss: () -> Unit) {
    val run = rememberRunner()
    val isNzb = link.contains(".nzb", true) || link.contains("t=get", true)
    val clients = ConfigStore.downloaders().sortedBy {
        // usenet links first to usenet clients, torrents to torrent clients
        val usenet = DownloadClients.of(it)?.isUsenet == true
        if (usenet == isNzb) 0 else 1
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send to…") },
        text = {
            Column {
                Text(link, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(4.dp))
                extra.forEach { (label, action) ->
                    Text(label, modifier = Modifier.fillMaxWidth().clickable { run("Sent", onDismiss) { action() } }.padding(vertical = 12.dp))
                }
                if (clients.isEmpty() && extra.isEmpty()) Text("Add a download client in Settings first.")
                clients.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            run("Sent to ${c.name}", onDismiss) { DownloadClients.of(c)?.addUrl(link) }
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ServiceBadge(c.type, 30.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
