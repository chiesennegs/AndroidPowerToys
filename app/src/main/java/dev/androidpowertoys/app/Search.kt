package dev.androidpowertoys.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/** Compact search with a full 48 dp touch target and no floating-label space. */
@Composable internal fun CompactSearch(value: String,onValue: (String)->Unit) {
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),
        color=MaterialTheme.colorScheme.surfaceContainerLow,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        BasicTextField(value,onValue,Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("settings-search").semantics { contentDescription="Search names and values" },
            textStyle=MaterialTheme.typography.bodyMedium.copy(color=MaterialTheme.colorScheme.onSurface),
            singleLine=true,cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions=KeyboardOptions(autoCorrectEnabled=false,imeAction=ImeAction.Search),
            keyboardActions=KeyboardActions(onSearch={ focus.clearFocus();keyboard?.hide() }),
            decorationBox={ input -> Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(start=12.dp,end=if(value.isEmpty()) 12.dp else 0.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_search),null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f),contentAlignment=Alignment.CenterStart) {
                    if(value.isEmpty()) Text("Search names and values",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    input()
                }
                if(value.isNotEmpty()) IconButton(onClick={ onValue("") },modifier=Modifier.semantics { contentDescription="Clear search" }) {
                    Icon(painterResource(R.drawable.ic_close),null,Modifier.size(20.dp))
                }
            } }
        )
    }
}
