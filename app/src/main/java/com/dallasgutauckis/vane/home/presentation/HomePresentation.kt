package com.dallasgutauckis.vane.home.presentation

import android.util.Log
import com.dallasgutauckis.vane.AppData
import com.dallasgutauckis.vane.Location
import com.dallasgutauckis.vane.Person
import com.dallasgutauckis.vane.Profile
import com.dallasgutauckis.vane.data.Persisted
import com.dallasgutauckis.vane.ktx.exhaustive
import oolong.Dispatch
import oolong.Effect
import oolong.effect.none
import oolong.runtime

class HomePresentation(
    private val appDataPersistence: Persisted<AppData>
) {
    private val init: () -> Pair<Model, Effect<Msg>> = {
        Model.Loading to Effects.load(appDataPersistence)
    }

    private val update: (Msg, Model) -> Pair<Model, Effect<Msg>> = { msg, model ->
        when (msg) {
            is Msg.Loaded.NotSetup -> Model.NotSetup to none()
            is Msg.Loaded.Setup -> Model.Setup(msg.appData) to none()
            is Msg.Setup -> Model.Loading to Effects.setUp(appDataPersistence, msg)
            is Msg.SetupComplete -> Model.Setup(msg.appData) to none()
            is Msg.AddLocation -> Model.Loading to Effects.addLocation(appDataPersistence, msg)
        }.exhaustive
    }

    private val view: (Model) -> Props = { model ->
        when (model) {
            is Model.Loading -> Props.Loading

            is Model.Setup -> Props.Setup(
                appData = model.appData,
                addLocation = { dispatch, lat, lon, name ->
                    dispatch(Msg.AddLocation(lat, lon, name))
                }
            )

            is Model.NotSetup -> Props.NotSetup(
                setup = { dispatch, firstName, lastName ->
                    dispatch(Msg.Setup(firstName, lastName))
                },
            )
        }
    }

    fun runtime(render: (Props, Dispatch<Msg>) -> Any?) = runtime(init, update, view, render)

    private object Effects {
        fun setUp(personPersistence: Persisted<AppData>, info: Msg.Setup): Effect<Msg> = { dispatch ->
            Log.v("DALLAS", "setUp called: $info")

            val newAppData = personPersistence.update {
                val person = Person.newBuilder()
                    .setFirstName(info.firstName)
                    .setLastName(info.lastName)
                    .build()

                it.toBuilder()
                    .addProfiles(
                        Profile.newBuilder()
                            .setPerson(person)
                            .build()
                    )
                    .build()
            }

            dispatch(Msg.SetupComplete(newAppData))
        }

        fun load(personDataStore: Persisted<AppData>): Effect<Msg> = { dispatch ->
            dispatch(
                personDataStore.get()
                    .takeIf {
                        Log.v("DALLAS", "takeIf: $it // ${it.profilesCount}")
                        it.profilesCount == 1
                    }
                    ?.let { Msg.Loaded.Setup(it) }
                    ?: Msg.Loaded.NotSetup)
        }

        fun addLocation(appDataPersistence: Persisted<AppData>, msg: Msg.AddLocation): Effect<Msg> = { dispatch ->
            val updated = appDataPersistence.update {
                it.toBuilder()
                    .apply {
                        val newProfile = profilesList.first().toBuilder()
                            .addLocations(
                                Location.newBuilder()
                                    .setLat(msg.lat)
                                    .setLon(msg.lon)
                                    .setName(msg.name)
                                    .build()
                            )

                        setProfiles(0, newProfile)
                    }
                    .build()
            }

            dispatch(Msg.Loaded.Setup(updated))
        }
    }

    sealed class Model {
        object NotSetup : Model()
        object Loading : Model()
        data class Setup(val appData: AppData) : Model()
    }

    sealed class Msg {
        sealed class Loaded : Msg() {
            data class Setup(val appData: AppData) : Loaded()
            object NotSetup : Loaded()
        }

        data class SetupComplete(
            val appData: AppData
        ) : Msg()

        data class Setup(
            val firstName: String,
            val lastName: String
        ) : Msg()

        data class AddLocation(
            val lat: Float,
            val lon: Float,
            val name: String
        ) : Msg()
    }

    sealed class Props {
        data class NotSetup(
            val setup: (dispatch: Dispatch<Msg>, firstName: String, lastName: String) -> Unit,
        ) : Props()

        object Loading : Props()

        data class Setup(
            val appData: AppData,
            val addLocation: (dispatch: Dispatch<Msg>, lat: Float, lon: Float, name: String) -> Unit
        ) : Props()
    }
}

