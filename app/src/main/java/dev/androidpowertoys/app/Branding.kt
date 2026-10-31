package dev.androidpowertoys.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** MooseAI cyan/navy identity from OpenLifeSpan, adapted to Material 3 contrast roles. */
internal fun mooseAiColors(dark: Boolean): ColorScheme = if(dark) darkColorScheme(
    primary=Color(0xFF67E8F9),onPrimary=Color(0xFF00363F),
    primaryContainer=Color(0xFF164E63),onPrimaryContainer=Color(0xFFCFFAFE),
    secondary=Color(0xFF7DD3FC),onSecondary=Color(0xFF00344B),
    secondaryContainer=Color(0xFF0C4A6E),onSecondaryContainer=Color(0xFFE0F2FE),
    tertiary=Color(0xFF93C5FD),onTertiary=Color(0xFF002E53),
    background=Color(0xFF090D16),onBackground=Color(0xFFE5EDF5),
    surface=Color(0xFF090D16),onSurface=Color(0xFFE5EDF5),
    surfaceVariant=Color(0xFF1E293B),onSurfaceVariant=Color(0xFFCBD5E1),
    surfaceContainerLowest=Color(0xFF060A11),surfaceContainerLow=Color(0xFF0F1623),
    surfaceContainer=Color(0xFF111827),surfaceContainerHigh=Color(0xFF192334),
    surfaceContainerHighest=Color(0xFF243044),outline=Color(0xFF94A3B8)
) else lightColorScheme(
    primary=Color(0xFF00677D),onPrimary=Color.White,
    primaryContainer=Color(0xFFCFFAFE),onPrimaryContainer=Color(0xFF00343D),
    secondary=Color(0xFF075985),onSecondary=Color.White,
    secondaryContainer=Color(0xFFE0F2FE),onSecondaryContainer=Color(0xFF082F49),
    tertiary=Color(0xFF1D4E89),onTertiary=Color.White,
    background=Color(0xFFF4F7FB),onBackground=Color(0xFF0F172A),
    surface=Color(0xFFF4F7FB),onSurface=Color(0xFF0F172A),
    surfaceVariant=Color(0xFFE2E8F0),onSurfaceVariant=Color(0xFF475569),
    surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFEEF3F9),
    surfaceContainer=Color(0xFFE8EFF7),surfaceContainerHigh=Color(0xFFDFE8F2),
    surfaceContainerHighest=Color(0xFFD5E1ED),outline=Color(0xFF64748B)
)

@Composable internal fun MooseAiAbout() {
    val context=LocalContext.current
    val version=remember { context.packageManager.getPackageInfo(context.packageName,0).versionName.orEmpty() }
    fun open(intent: Intent) { runCatching { context.startActivity(intent) }.onFailure { Toast.makeText(context,"No app is available to open this link. The address is shown below.",Toast.LENGTH_LONG).show() } }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("About & support",style=MaterialTheme.typography.titleMedium)
            Surface(color=Color(0xFF090D16),shape=RoundedCornerShape(16.dp)) {
                Image(painterResource(R.drawable.mooseai_logo),"MooseAI, LLC logo",
                    Modifier.fillMaxWidth().height(176.dp).padding(8.dp),contentScale=ContentScale.Fit)
            }
            Text("Android PowerToys · v$version",style=MaterialTheme.typography.titleSmall)
            Text("Created by MooseAI, LLC")
            SelectionContainer { Text("mooseaillc.com\nsupport@mooseaillc.com",style=MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={ open(Intent(Intent.ACTION_VIEW,Uri.parse("https://mooseaillc.com"))) }) { Text("Website") }
                TextButton(onClick={ open(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:support@mooseaillc.com"))) }) { Text("Email support") }
            }
            Text("© 2026 MooseAI, LLC",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
