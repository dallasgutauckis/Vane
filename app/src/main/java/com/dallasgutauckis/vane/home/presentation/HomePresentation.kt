package com.dallasgutauckis.vane.home.presentation

import com.dallasgutauckis.vane.AppData
import com.dallasgutauckis.vane.Location
import com.dallasgutauckis.vane.Person
import com.dallasgutauckis.vane.Profile
import com.dallasgutauckis.vane.api.twitch.TwitchApi
import com.dallasgutauckis.vane.common.model.Async
import com.dallasgutauckis.vane.common.model.fold
import com.dallasgutauckis.vane.data.Persisted
import com.dallasgutauckis.vane.ktx.exhaustive
import oolong.Dispatch
import oolong.Effect
import oolong.effect
import oolong.effect.none
import oolong.runtime

class HomePresentation(
    private val appDataPersistence: Persisted<AppData>,
    private val twitchApi: TwitchApi,
    private val twitchAuth: TwitchAuthenticator
) {
    private val init: () -> Pair<Model, Effect<Msg>> = {
        //
        Model(Async.Loading, Async.Loading) to Effects.loadAppData(appDataPersistence)
    }

    private val update: (Msg, Model) -> Pair<Model, Effect<Msg>> = { msg, model ->
        when (msg) {
            is Msg.AppState.NotSetup ->
                Model(Async.Loaded(null), Async.Loading) to none()

            is Msg.AppState.Setup -> {
                if (msg.appData.twitchData.token != "") {
                    Model(Async.Loaded(msg.appData), Async.Loading) to Effects.loadTwitchUsers(
                        twitchApi,
                        // TODO pull Twitch users from AppData
                        msg.appData.twitchData.token,
                        listOf("ExfilCamper", "LVNDMARK", "RazzleDis", "MrPalmBeach", "hyperrattv")
                    )
                } else {
                    Model(Async.Loaded(msg.appData), Async.Loaded(Model.TwitchModel.NotAuthenticated)) to none()
                }
            }

            is Msg.AppState.NotSetup.Profile.SetUpProfile ->
                Model(Async.Loading, Async.Loading) to Effects.addProfile(
                    appDataPersistence,
                    msg.firstName,
                    msg.lastName
                )
            is Msg.AppState.NotSetup.Profile.OnProfileSetUp ->
                Model(Async.Loading, Async.Loading) to Effects.loadAppData(appDataPersistence)

            // Locations
            is Msg.AppState.Setup.LocationChanges -> {
                when (msg) {
                    is Msg.AppState.Setup.LocationChanges.AddLocation ->
                        model to Effects.addLocation(appDataPersistence, msg.lat, msg.lon, msg.name)

                    is Msg.AppState.Setup.LocationChanges.OnAdded ->
                        Model(Async.Loading, Async.Loading) to Effects.loadAppData(appDataPersistence)

                    is Msg.AppState.Setup.LocationChanges.RemoveLocation ->
                        model.copy(appData = Async.Loading) to Effects.removeLocation(
                            appDataPersistence,
                            msg.location
                        )
                    is Msg.AppState.Setup.LocationChanges.OnRemoved -> TODO()
                }.exhaustive
            }

            // Twitch stuff
            is Msg.AppState.Setup.Twitch.Authenticated.Users.OnLoaded ->
                model.copy(twitchInfo = Async.Loaded(Model.TwitchModel.Authenticated(msg.users))) to none()

            is Msg.AppState.Setup.Twitch.NotAuthenticated -> {
                when (msg) {
                    is Msg.AppState.Setup.Twitch.NotAuthenticated.Authenticate ->
                        model.copy(twitchInfo = Async.Loading) to Effects.authenticateTwitch(appDataPersistence, twitchAuth)

                    is Msg.AppState.Setup.Twitch.NotAuthenticated.OnAuthenticated ->
                        Model(Async.Loading, Async.Loading) to Effects.loadAppData(appDataPersistence)

                    is Msg.AppState.Setup.Twitch.NotAuthenticated.OnFailedAuthentication ->
                        model.copy(twitchInfo = Async.Loaded(Model.TwitchModel.NotAuthenticated)) to none()
                }
            }
        }.exhaustive
    }

    @Suppress("MoveLambdaOutsideParentheses")
    private val view: (Model) -> Props = { model ->
        model.appData.fold({ Props.Loading }) {
            if (it == null) {
                Props.NotSetup(
                    setup = { dispatch, firstName, lastName ->
                        dispatch(Msg.AppState.NotSetup.Profile.SetUpProfile(firstName, lastName))
                    }
                )
            } else {
                Props.Setup(
                    appData = it,
                    twitchModel = model.twitchInfo.let { asyncModel ->
                        when (asyncModel) {
                            is Async.Loading -> Async.Loading
                            is Async.Loaded -> {
                                when (asyncModel.data) {
                                    is Model.TwitchModel.Authenticated -> Async.Loaded(Props.Setup.TwitchModel.Authenticated(asyncModel.data.streams))
                                    is Model.TwitchModel.NotAuthenticated -> Async.Loaded(Props.Setup.TwitchModel.NotAuthenticated(
                                        requestLogin = { dispatch ->
                                            dispatch(Msg.AppState.Setup.Twitch.NotAuthenticated.Authenticate)
                                        }
                                    ))
                                }
                            }
                        }
                    },
                    addLocation = { dispatch, lat, lon, name ->
                        dispatch(Msg.AppState.Setup.LocationChanges.AddLocation(lat, lon, name))
                    },
                    removeLocation = { dispatch, location ->
                        dispatch(Msg.AppState.Setup.LocationChanges.RemoveLocation(location))
                    }
                )
            }
        }
    }

    fun runtime(render: (Props, Dispatch<Msg>) -> Any?) = runtime(init, update, view, render)

    private object Effects {
        fun loadAppData(appDataPersistence: Persisted<AppData>) = effect<Msg> { dispatch ->
            val appData = appDataPersistence.get().takeIf { it.hasProfile() }

            dispatch(appData
                ?.let { Msg.AppState.Setup(it) }
                ?: Msg.AppState.NotSetup)
        }

        fun loadTwitchUsers(
            twitchApi: TwitchApi,
            twitchToken: String,
            usernames: List<String>
        ) = effect<Msg> { dispatch ->
            dispatch(Msg.AppState.Setup.Twitch.Authenticated.Users.OnLoaded(usernames.let {
                twitchApi.getUsers(twitchToken, it).orNull()?.data ?: emptyList()
            }))
        }

        fun addLocation(appDataPersistence: Persisted<AppData>, lat: Float, lon: Float, name: String) = effect<Msg> { dispatch ->
            val locationAdded = Location.newBuilder()
                .setLat(lat)
                .setLon(lon)
                .setName(name)
                .build()

            appDataPersistence.update {
                it.toBuilder()
                    .apply {
                        profile = profile.toBuilder()
                            .addLocations(locationAdded)
                            .build()
                    }
                    .build()
            }

            dispatch(Msg.AppState.Setup.LocationChanges.OnAdded(locationAdded))
        }


        fun removeLocation(appDataPersistence: Persisted<AppData>, location: Location) = effect<Msg> { dispatch ->
            val newAppData = appDataPersistence.update {
                it.toBuilder()
                    .apply {
                        profile = profile.toBuilder().apply {
                            removeLocations(locationsList.indexOf(location))
                        }.build()
                    }
                    .build()
            }

            dispatch(Msg.AppState.Setup(newAppData))
        }

        fun addProfile(persistence: Persisted<AppData>, firstName: String, lastName: String) = effect<Msg> { dispatch ->
            val newAppData = persistence.update {
                it.toBuilder()
                    .setProfile(
                        Profile.newBuilder()
                            .setPerson(
                                Person.newBuilder()
                                    .setFirstName(firstName)
                                    .setLastName(lastName)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            }

            dispatch(Msg.AppState.Setup(newAppData))
        }

        fun authenticateTwitch(appDataPersistence: Persisted<AppData>, twitchAuth: TwitchAuthenticator) = effect<Msg> { dispatch ->
            twitchAuth.requestAuthentication(
                onSuccess = { token ->
                    appDataPersistence.update {
                        it.toBuilder()
                            .setTwitchData(it.twitchData.toBuilder().setToken(token).build())
                            .build()
                    }
                    dispatch(Msg.AppState.Setup.Twitch.NotAuthenticated.OnAuthenticated(token))
                },
                onFailure = { _, _, _ ->
                    dispatch(Msg.AppState.Setup.Twitch.NotAuthenticated.OnFailedAuthentication)
                }
            )
        }
    }

    data class Model(
        val appData: Async<AppData?>,
        val twitchInfo: Async<TwitchModel>
    ) {
        sealed class TwitchModel {
            object NotAuthenticated : TwitchModel()
            data class Authenticated(
                val streams: List<TwitchApi.Stream>
            ) : TwitchModel()
        }
    }

    sealed class Msg {
        sealed class AppState : Msg() {
            data class Setup(val appData: AppData) : AppState() {
                sealed class LocationChanges : Msg() {
                    data class AddLocation(
                        val lat: Float,
                        val lon: Float,
                        val name: String
                    ) : LocationChanges()

                    data class RemoveLocation(
                        val location: Location
                    ) : LocationChanges()

                    data class OnAdded(val newLocation: Location) : LocationChanges()
                    data class OnRemoved(val removedLocation: Location) : LocationChanges()
                }

                sealed class Twitch : Msg() {
                    sealed class Authenticated : Twitch() {
                        sealed class Users : Authenticated() {
                            data class OnLoaded(val users: List<TwitchApi.Stream>) : Users()
                        }
                    }

                    sealed class NotAuthenticated : Twitch() {
                        object Authenticate : NotAuthenticated()
                        object OnFailedAuthentication : NotAuthenticated()

                        data class OnAuthenticated(val token: String) : NotAuthenticated()
                    }
                }
            }

            object NotSetup : AppState() {
                sealed class Profile : Msg() {
                    // Set up a new profile
                    data class SetUpProfile(
                        val firstName: String,
                        val lastName: String
                    ) : Profile()

                    // A new profile has been set up
                    // Takes user to Setup state
                    data class OnProfileSetUp(
                        val firstName: String,
                        val lastName: String
                    ) : Profile()
                }
            }
        }
    }

    sealed class Props {
        data class NotSetup(
            val setup: (dispatch: Dispatch<Msg>, firstName: String, lastName: String) -> Unit,
        ) : Props()

        object Loading : Props()

        data class Setup(
            val appData: AppData,
            val twitchModel: Async<TwitchModel>,
            val addLocation: (dispatch: Dispatch<Msg>, lat: Float, lon: Float, name: String) -> Unit,
            val removeLocation: (dispatch: Dispatch<Msg>, location: Location) -> Unit,
        ) : Props() {
            sealed class TwitchModel {
                data class NotAuthenticated(
                    val requestLogin: (dispatch: Dispatch<Msg>) -> Unit
                ) : TwitchModel()

                data class Authenticated(
                    val twitchStreams: List<TwitchApi.Stream>
                ) : TwitchModel()
            }
        }
    }
}

interface TwitchAuthenticator {
    fun requestAuthentication(onSuccess: OnSuccess, onFailure: OnFailure)

    fun interface OnSuccess {
        operator fun invoke(token: String)
    }

    fun interface OnFailure {
        operator fun invoke(code: Int, error: String?, errorDescription: String?)
    }
}
