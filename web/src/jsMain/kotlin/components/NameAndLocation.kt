package components
import androidx.compose.runtime.Composable
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun NameAndLocation(
    name: String?,
    location: String?,
    onNameClick: (() -> Unit)? = null,
    onLocationClick: (() -> Unit)? = null
) {
    Span({
        style {
            fontWeight("bold")
            fontSize(24.px)
        }

        onNameClick?.let { block ->
            onClick {
                block()
            }
        }
    }) {
        Text(name ?: "")
    }
    location?.notBlank?.let { location ->
        Span({
            style {
                marginLeft(1.r / 2)
                fontSize(18.px)
                opacity(.75f)
            }

            onLocationClick?.let { block ->
                onClick {
                    block()
                }
            }
        }) {
            Text(location)
        }
    }
}
