package com.dallasgutauckis.vane

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dallasgutauckis.vane.api.twitch.TwitchApi
import com.dallasgutauckis.vane.common.model.Async
import com.dallasgutauckis.vane.data.PersistedViaDataStore
import com.dallasgutauckis.vane.data.personDataStore
import com.dallasgutauckis.vane.home.presentation.HomePresentation
import com.dallasgutauckis.vane.home.presentation.TwitchAuthenticator
import com.dallasgutauckis.vane.ktx.exhaustive
import com.dallasgutauckis.vane.ui.theme.VaneTheme
import net.openid.appauth.*
import oolong.Dispatch


class MainActivity : ComponentActivity() {

    private val personPersistence by lazy { PersistedViaDataStore(personDataStore) }

    private val twitchClientId = "sb1qco38inwyupu74tkbgvj0ewil4y"

    private val authService by lazy { AuthorizationService(this@MainActivity) }

    private val twitchAuth = object : TwitchAuthenticator, TwitchAuthorizationCallback {
        private var onSuccess: TwitchAuthenticator.OnSuccess = TwitchAuthenticator.OnSuccess { }
        private var onFailure: TwitchAuthenticator.OnFailure = TwitchAuthenticator.OnFailure { code, error, errorDescription -> }

        override fun requestAuthentication(onSuccess: TwitchAuthenticator.OnSuccess, onFailure: TwitchAuthenticator.OnFailure) {
            this.onSuccess = onSuccess
            this.onFailure = onFailure

            val authorizationServiceConfiguration = AuthorizationServiceConfiguration(
                Uri.parse("https://id.twitch.tv/oauth2/authorize"),
                Uri.parse("https://id.twitch.tv/oauth2/token")
            )

            val request = AuthorizationRequest.Builder(
                authorizationServiceConfiguration,
                twitchClientId,
                ResponseTypeValues.CODE,
                Uri.parse("http://localhost/vane-twitch-redirect")
            )
                .build()

            twitchAuthResult.launch(authService.getAuthorizationRequestIntent(request))
        }

        override fun onAuthorizationSuccess(resp: AuthorizationResponse) {
            authService.performTokenRequest(resp.createTokenExchangeRequest(), ClientSecretPost("baogbibodgwx6n8ikw7e2nqnqaxnp2")) { response, ex ->
                if (response != null) {
                    onSuccess(response.accessToken!!)
                } else if (ex != null) {
                    onFailure(ex)
                }
            }
        }

        override fun onAuthorizationFailure(ex: AuthorizationException) {
            onFailure(ex)
        }

        private operator fun TwitchAuthenticator.OnFailure.invoke(ex: AuthorizationException) {
            this(ex.code, ex.error, ex.errorDescription)
        }
    }

    private val twitchAuthResult: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        it.data?.let { intent ->
            val resp: AuthorizationResponse? = AuthorizationResponse.fromIntent(intent)

            if (resp != null) {
                twitchAuth.onAuthorizationSuccess(resp)
            } else {
                val ex = AuthorizationException.fromIntent(intent)
                if (ex != null) {
                    twitchAuth.onAuthorizationFailure(ex)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HomePresentation(
            appDataPersistence = personPersistence,
            twitchApi = TwitchApi(twitchClientId),
            twitchAuth = twitchAuth
        )
            .runtime { props, dispatch ->
                Log.v("DALLAS", "props: $props")

                setContent {
                    VaneTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            // A surface container using the 'background' color from the theme
                            VaneWidgetContainer(props, dispatch)
                        }
                    }
                }
            }

    }

    companion object {
        const val REQUEST_TWITCH_AUTH = 0
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Welcome back, $name!")
}

@Composable
fun VaneWidgetContainer(props: HomePresentation.Props, dispatch: Dispatch<HomePresentation.Msg>) {
    when (props) {
        is HomePresentation.Props.Loading -> VaneLoadingContainer(props, dispatch)
        is HomePresentation.Props.NotSetup -> VaneNotSetupContainer(props, dispatch)
        is HomePresentation.Props.Setup -> VaneSetupContainer(props, dispatch)
    }.exhaustive
}

@Preview
@Composable
fun VaneNotSetupContainer(
    props: HomePresentation.Props.NotSetup = HomePresentation.Props.NotSetup { _, _, _ -> },
    dispatch: (HomePresentation.Msg) -> Unit = {}
) {
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }

    Surface(modifier = Modifier.padding(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth(1f)) {

            OutlinedTextField(
                value = firstName.value,
                onValueChange = { firstName.value = it },
                label = { Text("First name") }
            )

            OutlinedTextField(
                value = lastName.value,
                onValueChange = { lastName.value = it },
                label = { Text("Last name") }
            )

            Button(onClick = { props.setup(dispatch, firstName.value, lastName.value) }) {
                Text("SHEEESH")
            }
        }
    }
}

// for LazyGrid
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VaneSetupContainer(
    props: HomePresentation.Props.Setup = HomePresentation.Props.Setup(
        AppData.newBuilder()
            .setProfile(
                Profile.newBuilder()
                    .setPerson(
                        Person.newBuilder()
                            .setFirstName("Dallas")
                            .setLastName("Gutauckis")
                            .build()
                    )
                    .addLocations(
                        Location.newBuilder()
                            .setName("New York City1, NY")
                            .setLat(0.0003f)
                            .setLon(1.2345f)
                            .build()
                    )
                    .addLocations(
                        Location.newBuilder()
                            .setName("New York City2, NY")
                            .setLat(0.0003f)
                            .setLon(1.2345f)
                            .build()
                    )
                    .addLocations(
                        Location.newBuilder()
                            .setName("New York City3, NY")
                            .setLat(0.0003f)
                            .setLon(1.2345f)
                            .build()
                    )
                    .addLocations(
                        Location.newBuilder()
                            .setName("New York City4, NY")
                            .setLat(0.0003f)
                            .setLon(1.2345f)
                            .build()
                    )
                    .build()
            )
            .build(),
        twitchModel = Async.Loaded(HomePresentation.Props.Setup.TwitchModel.NotAuthenticated { }),
        addLocation = { _, _, _, _ -> },
        removeLocation = { _, _ -> }
    ),
    dispatch: (HomePresentation.Msg) -> Unit = {}
) {
    val profile = props.appData.profile

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        val name = profile.person.let { it.firstName + " " + it.lastName }
        val formattedWelcome = stringResource(id = R.string.welcome_format, name)

        Text(formattedWelcome, style = MaterialTheme.typography.h3)

        Locations(dispatch, profile.locationsList, props.removeLocation)

        val newLocationText = remember { mutableStateOf("") }

        OutlinedTextField(
            value = newLocationText.value,
            onValueChange = { newLocationText.value = it },
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = {
                props.addLocation(dispatch, 0.1234f, 4.321f, newLocationText.value)
                newLocationText.value = ""
            }),
            label = { Text("New location") },
            modifier = Modifier.fillMaxWidth()
        )

        TwitchStreams(dispatch, props.twitchModel)
    }
}

@Preview
@ExperimentalFoundationApi
@Composable
fun TwitchStreams(
    dispatch: Dispatch<HomePresentation.Msg> = {},
    twitchModel: Async<HomePresentation.Props.Setup.TwitchModel> =
        Async.Loaded(
            HomePresentation.Props.Setup.TwitchModel.Authenticated(
                listOf(
                    TwitchApi.Stream(
                        id = "",
                        user_id = "",
                        user_login = "defNOTdallas",
                        user_name = "defNOTdallas",
                        game_id = "12312",
                        game_name = "Escape from Tarkov",
                        type = "",
                        title = "Pepega Aim? Sure! This is a longer stream title for sure!",
                        viewer_count = 1,
                        started_at = "1970-01-01 00:00:00.000",
                        thumbnail_url = "https://www.wm.edu/research/news/_images/2012/nailing-a-crab-killer/photoset/nailing-a-crab-killer-lead.jpg",
                        tag_ids = listOf(),
                        is_mature = true
                    )
                )
            )
        )
) {
    if (twitchModel is Async.Loaded) {
        if (twitchModel.data is HomePresentation.Props.Setup.TwitchModel.Authenticated) {
            LazyColumn {
                val twitchStreams = twitchModel.data.twitchStreams
                items(twitchStreams.size) { index ->
                    twitchStreams[index].let { stream ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { }
                                )
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {

                                Surface(shape = RoundedCornerShape(2.dp), color = Color.White.copy(alpha = 0.1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stream.user_login, Modifier.padding(8.dp, 2.dp), style = MaterialTheme.typography.subtitle2)
                                        Surface(shape = RoundedCornerShape(2.dp), color = Color.White.copy(alpha = 0.1f)) {
                                            Text(stream.viewer_count.toString(), Modifier.padding(4.dp, 2.dp), style = MaterialTheme.typography.subtitle2)
                                        }
                                    }

                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stream.title, style = MaterialTheme.typography.caption)
                            }
                        }
                    }
                }
            }
        } else if (twitchModel.data is HomePresentation.Props.Setup.TwitchModel.NotAuthenticated) {
            Button(onClick = {
                twitchModel.data.requestLogin(dispatch)
            }) {
                Text("Log in to Twitch")
            }
        }
    }
}

interface TwitchAuthorizationCallback {
    fun onAuthorizationSuccess(resp: AuthorizationResponse)
    fun onAuthorizationFailure(ex: AuthorizationException)
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun Locations(dispatch: Dispatch<HomePresentation.Msg>, locations: List<Location>, removeLocation: (Dispatch<HomePresentation.Msg>, Location) -> Unit) {
    LazyVerticalGrid(cells = GridCells.Fixed(2)) {
        items(locations.size) { index ->
            locations[index].let { location ->
                val expanded = remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { removeLocation(dispatch, location) },
                                onTap = { expanded.value = !expanded.value }
                            )
                        },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(location.name)
                        AnimatedVisibility(expanded.value) {
                            Text("[${location.lat}, ${location.lon}]")
                        }

                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VaneLoadingContainer(
    props: HomePresentation.Props.Loading = HomePresentation.Props.Loading,
    dispatch: (HomePresentation.Msg) -> Unit = {}
) {
    Surface {
        CircularProgressIndicator()
    }
}

@Composable
fun <T : Any?> T.withDarkMode(darkValue: T): T {
    return if (isSystemInDarkTheme()) {
        darkValue
    } else {
        this
    }
}

@Composable
fun <T> onlyIfDark(body: () -> T): T? {
    return if (isSystemInDarkTheme()) {
        body()
    } else {
        null
    }
}
