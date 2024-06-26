import androidx.compose.runtime.*
import app.appNav
import app.call.CallLayout
import app.call.CallStyles
import app.components.Background
import app.group.GroupCoverPage
import app.softwork.routingcompose.BrowserRouter
import app.softwork.routingcompose.Router
import app.widget.WidgetStyles
import components.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposableInBody
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.get
import org.w3c.dom.set
import stories.StoryStyles

const val baseUrl = "https://api.ailaai.app"

//const val baseUrl = "http://0.0.0.0:8080"
const val webBaseUrl = "https://ailaai.app"

val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

val http = HttpClient(Js) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json)
    }
}

fun main() {
    renderComposableInBody {
        Style(Styles)
        Style(EffectStyles)
        Style(WidgetStyles)
        Style(CallStyles)
        Style(StoryStyles)

        var language by remember {
            mutableStateOf(
                when ((localStorage["language"] ?: window.navigator.language).startsWith("vi")) {
                    true -> "vi"
                    false -> "en"
                }
            )
        }

        CompositionLocalProvider(LocalConfiguration provides Configuration(language) { language = it }) {
            var title by remember { mutableStateOf<String?>(null) }
            var parentCardId by remember { mutableStateOf<String?>(null) }
            var personId by remember { mutableStateOf<String?>(null) }
            val appName = appString { appName }

            LaunchedEffect(title) {
                document.title = title ?: appName
            }

            LaunchedEffect(language) {
                localStorage["language"] = language
                application.language = language
            }

            LaunchedEffect(Unit) {
                indicator.hasIndicator.collectLatest {
                    val faviconElement = document.querySelector("link[rel*='icon']") as HTMLLinkElement

                    faviconElement.href = if (it) "/icon-new.png" else "/icon.png"
                }
            }

            LaunchedEffect(Unit) {
                delay(500)
                application.sync()
                push.start(this)
                saves.start(this)
                joins.start(this)
                call.init(this)
            }

            BrowserRouter("") {
                val router = Router.current

                LaunchedEffect(Unit) {
                    appNav.route.collectLatest {
                        router.navigate(it)
                    }
                }

                LaunchedEffect(router.currentPath) {
                    window.scrollTo(0.0, 0.0)
                    document.title = appName
                }

                route("signin") {
                    AppHeader(
                        appString { signIn },
                        showBack = true,
                        onBack = {
                            router.navigate("/")
                        },
                        showMe = false
                    )
                    SigninPage()
                    AppFooter()
                }

                route("group") {
                    string { groupId ->
                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appName)
                            GroupCoverPage(groupId) {
                                title = it.group?.name ?: appName
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                // Deprecated
                route("card") {
                    string { cardId ->
                        AppHeader(appName, showBack = parentCardId != null, onBack = {
                            router.navigate("/page/$parentCardId")
                        })
                        CardPage(cardId, onError = { parentCardId = null }) {
                            title = it.name
                            parentCardId = it.parent
                        }
                        AppFooter()
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("page") {
                    string { cardId ->

                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appName, showBack = parentCardId != null || personId != null, onBack = {
                                if (parentCardId != null) {
                                    router.navigate("/page/$parentCardId")
                                } else if (personId != null) {
                                    router.navigate("/profile/$personId")
                                }
                            })
                            CardPage(cardId, onError = { parentCardId = null }) {
                                title = it.name
                                parentCardId = it.parent
                                personId = if (it.equipped == true) it.person else null
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("story") {
                    string { storyUrl ->

                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appString { stories })
                            StoryPage(storyUrl) {
                                title = it.title
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("profile") {
                    string { profileUrl ->

                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appName)
                            val someoneString = appString { someone }
                            ProfilePage(profileUrl) {
                                title = it.person.name ?: someoneString
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("info") {
                    string { page ->
                        AppHeader(appName, showMenu = true, showBack = true) {
                            router.navigate("/")
                        }
                        InfoPage(page)
                        AppFooter()
                    }
                }

                route("cities") {
                    AppHeader(appString { chooseYourCity }, showMenu = false, showBack = true) {
                        router.navigate("/")
                    }
                    CitiesPage()
                    AppFooter()
                }

                route("privacy") {
                    AppHeader(appName, showMenu = false)
                    PrivacyPage()
                    AppFooter()
                }

                route("terms") {
                    AppHeader(appName, showMenu = false)
                    TosPage()
                    AppFooter()
                }

                string { profileUrl ->
                    Background({
                        classes(Styles.background)
                    }) {
                        AppHeader(appName)
                        val someoneString = appString { someone }
                        ProfilePage(url = profileUrl) {
                            title = it.person.name ?: someoneString
                        }
                        AppFooter()
                    }
                }

                noMatch {
                    MainPage()
                }
            }

            val activeCall by call.active.collectAsState()

            if (activeCall != null) {
                CallLayout(activeCall!!)
            }

            NotificationsLayout()
        }
    }
}
