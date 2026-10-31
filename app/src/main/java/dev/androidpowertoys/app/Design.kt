package dev.androidpowertoys.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
private val outfit=FontFamily(
    Font(R.font.outfit,FontWeight.Normal,variationSettings=FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.outfit,FontWeight.Medium,variationSettings=FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.outfit,FontWeight.SemiBold,variationSettings=FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.outfit,FontWeight.Bold,variationSettings=FontVariation.Settings(FontVariation.weight(700)))
)
internal val powerToysTypography=Typography().let { t ->
    t.copy(
        displayLarge=t.displayLarge.copy(fontFamily=outfit),displayMedium=t.displayMedium.copy(fontFamily=outfit),displaySmall=t.displaySmall.copy(fontFamily=outfit),
        headlineLarge=t.headlineLarge.copy(fontFamily=outfit),headlineMedium=t.headlineMedium.copy(fontFamily=outfit),headlineSmall=t.headlineSmall.copy(fontFamily=outfit,fontWeight=FontWeight.SemiBold,letterSpacing=(-0.5).sp),
        titleLarge=t.titleLarge.copy(fontFamily=outfit),titleMedium=t.titleMedium.copy(fontFamily=outfit,fontWeight=FontWeight.SemiBold),titleSmall=t.titleSmall.copy(fontFamily=outfit,fontWeight=FontWeight.Medium),
        bodyLarge=t.bodyLarge.copy(fontFamily=outfit),bodyMedium=t.bodyMedium.copy(fontFamily=outfit,fontSize=15.sp,lineHeight=22.sp),bodySmall=t.bodySmall.copy(fontFamily=outfit,fontSize=13.sp,lineHeight=19.sp),
        labelLarge=t.labelLarge.copy(fontFamily=outfit),labelMedium=t.labelMedium.copy(fontFamily=outfit),labelSmall=t.labelSmall.copy(fontFamily=outfit,fontSize=12.sp,lineHeight=16.sp)
    )
}
internal val powerToysShapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(28.dp))

/** Shared 48 dp masthead; the full title area is a touch-sized table picker. */
@Composable internal fun ProductHeader(screen: String, table: Table?, editing: Boolean, onTable: (Table)->Unit, actions: @Composable ()->Unit) {
    var choosing by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().testTag("app-header").heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_launcher),"Android PowerToys app logo",Modifier.size(36.dp).clip(CircleShape))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxWidth().heightIn(min=48.dp).then(if(table!=null) Modifier.clickable(role=Role.Button) { choosing=true } else Modifier),verticalArrangement=Arrangement.Center) {
                Text("Android PowerToys",style=MaterialTheme.typography.headlineSmall,fontSize=26.sp,lineHeight=30.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(table!=null) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("${table.label} ▾",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    Text(if(editing) "Editing enabled" else "Read-only mode",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text("${if(screen=="Defaults") "Starting values" else screen} · by MooseAI",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(table!=null) DropdownMenu(expanded=choosing,onDismissRequest={ choosing=false }) {
                Table.entries.forEach { t -> DropdownMenuItem(text={ Text(t.label) },onClick={ onTable(t);choosing=false }) }
            }
        }
        actions()
    }
}
@Composable internal fun RiskLabel(risk: Risk) {
    val important=risk in setOf(Risk.HIGH,Risk.BLOCKED)
    Surface(color=if(important) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor=if(important) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape=RoundedCornerShape(8.dp)) {
        Text(when(risk) {
            Risk.UNKNOWN->"Effects not reviewed"
            Risk.CAUTION->"Changes system behavior"
            Risk.DISPLAY->"15-second screen check"
            Risk.HIGH->"High impact"
            Risk.BLOCKED->"Use Android Settings"
        },Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall)
    }
}

@Composable internal fun OperationFeedback(message: String, onDismiss: ()->Unit) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            SelectionContainer { Text(message,style=MaterialTheme.typography.bodyMedium) }
            TextButton(onClick=onDismiss) { Text("Dismiss") }
        }
    }
}
