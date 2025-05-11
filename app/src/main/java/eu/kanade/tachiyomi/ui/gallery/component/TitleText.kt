package eu.kanade.tachiyomi.ui.gallery.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R

@Composable
fun TitleText(
    title: String,
    paddingValues: PaddingValues = PaddingValues(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val actionBarSize = dimensionResource(R.dimen.mainActionBarSize) - paddingValues.calculateTopPadding()

    Column(
        modifier = Modifier
            .padding(top = actionBarSize),
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineLarge.merge(
                TextStyle(
                    fontFamily = FontFamily.Default,
                    color = LocalTextStyle.current.color,
                    lineHeight = 32.sp, // 根据设计系统调整
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            ),
            modifier = Modifier
                .padding(
                    top = (
                        24 + WindowInsets.statusBars.getTop(
                            LocalDensity.current,
                        )
                        ).dp,
                )
                .heightIn(min = 48.dp),
        )
        content?.invoke(this)
    }
}
