package dev.androidpowertoys.app

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable fun LicenseDialog(onClose: ()->Unit) {
    val context=LocalContext.current
    val notices=remember { context.assets.open("third_party_notices.txt").bufferedReader().use { it.readText() } }
    val files=remember { context.assets.list("licenses").orEmpty().sorted() }
    AlertDialog(onDismissRequest=onClose,title={ Text("Open-source licenses") },text={ LazyColumn {
        item { SelectionContainer { Text(notices) } }
        items(files) { name ->
            Text(name,style=MaterialTheme.typography.titleSmall)
            val text=remember(name) { context.assets.open("licenses/$name").bufferedReader().use { it.readText() } }
            SelectionContainer { Text(text,style=MaterialTheme.typography.bodySmall) }
        }
    } },confirmButton={ TextButton(onClick=onClose) { Text("Close") } })
}
