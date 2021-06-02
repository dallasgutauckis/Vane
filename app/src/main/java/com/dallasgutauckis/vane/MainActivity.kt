package com.dallasgutauckis.vane

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dallasgutauckis.vane.data.PersistedViaDataStore
import com.dallasgutauckis.vane.data.personDataStore
import com.dallasgutauckis.vane.home.presentation.HomePresentation
import com.dallasgutauckis.vane.ktx.exhaustive
import com.dallasgutauckis.vane.ui.theme.VaneTheme
import oolong.Dispatch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val personPersistence = PersistedViaDataStore(personDataStore)

        HomePresentation(personPersistence)
            .runtime { props, dispatch ->
                Log.v("DALLAS", "props: $props")

                setContent {
                    VaneTheme {
                        // A surface container using the 'background' color from the theme
                        VaneWidgetContainer(props, dispatch)
                    }
                }
            }

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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VaneSetupContainer(
    props: HomePresentation.Props.Setup = HomePresentation.Props.Setup(
        AppData.newBuilder()
            .addProfiles(
                Profile.newBuilder()
                    .setPerson(
                        Person.newBuilder()
                            .setFirstName("Dallas")
                            .setLastName("Gutauckis")
                            .build()
                    )
                    .addLocations(
                        Location.newBuilder()
                            .setName("New York City, NY")
                            .setLat(0.0003f)
                            .setLon(1.2345f)
                            .build()
                    )
                    .build()
            )
            .build(),
        addLocation = { _, _, _, _ -> }
    ),
    dispatch: (HomePresentation.Msg) -> Unit = {}
) {
    val profile = props.appData.profilesList[0]

    Surface(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val name = props.appData.profilesList.first().person.let { it.firstName + " " + it.lastName }
            val formattedWelcome = stringResource(id = R.string.welcome_format, name)

            Text(
                formattedWelcome,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )

            profile.locationsList.map {
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(it.name + " [${it.lat}, ${it.lon}}]")
                    }
                }
            }

            val newLocation = remember { mutableStateOf("") }

            OutlinedTextField(
                value = newLocation.value,
                onValueChange = { newLocation.value = it },
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = {
                    props.addLocation(dispatch, 0.1234f, 4.321f, newLocation.value)
                }),
                label = { Text("New location") }
            )
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
