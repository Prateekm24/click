package com.remotehost.remote.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotehost.remote.connection.PairingInfo
import com.remotehost.remote.ui.theme.Accent
import com.remotehost.remote.ui.theme.Bg

@Composable
fun ManualEntryScreen(
    onSubmit: (PairingInfo) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8765") }
    var token by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enter host address", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("Cancel") }
        }

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = port,
            onValueChange = { input -> port = input.filter { it.isDigit() } },
            label = { Text("Port") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Pairing token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Host name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let { Text(it, color = Accent, fontSize = 12.sp) }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val portInt = port.toIntOrNull()
                error = when {
                    ip.isBlank() -> "Enter an IP address"
                    portInt == null -> "Enter a valid port"
                    token.isBlank() -> "Enter the pairing token"
                    name.isBlank() -> "Enter a host name"
                    else -> null
                }
                if (error == null && portInt != null) {
                    onSubmit(PairingInfo(ip.trim(), portInt, token.trim(), name.trim()))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text("Connect")
        }
    }
}
