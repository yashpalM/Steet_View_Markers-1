package com.alkurop.mystreetplaces.ui.maps

import android.os.Bundle
import com.alkurop.mystreetplaces.data.pin.PinRepo
import com.alkurop.mystreetplaces.domain.pin.PinDto
import com.alkurop.mystreetplaces.ui.createNavigationSubject
import com.alkurop.mystreetplaces.ui.createViewSubject
import com.alkurop.mystreetplaces.ui.navigation.ActivityNavigationAction
import com.alkurop.mystreetplaces.ui.navigation.NavigationAction
import com.alkurop.mystreetplaces.ui.pin.activity.DropPinActivity
import com.alkurop.mystreetplaces.ui.pin.drop.DropPinFragment
import com.alkurop.mystreetplaces.ui.street.StreetActivity
import com.alkurop.mystreetplaces.ui.street.StreetFragment
import com.alkurop.mystreetplaces.utils.LocationTracker
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MapPresenterImpl(val pinRepo: PinRepo) : MapPresenter {
    override val viewBus: Subject<MapViewModel> = createViewSubject()

    override val navBus: Subject<NavigationAction> = createNavigationSubject()
    override var isPermissionGranted: Boolean = false

    override lateinit var locationTracker: LocationTracker
    val GET_LAST_KNOWN_LOCATION_TIMEOUT: Long = 1

    var pinsSet = setOf<PinDto>()

    val markersDisposable = CompositeDisposable()
    override fun onAddMarker() {
        if (isPermissionGranted) {
            val disposable = locationTracker
                    .getLastKnownLocation()
                    .compose(getLoadingStateTransformer())
                    .firstElement()
                    .timeout(GET_LAST_KNOWN_LOCATION_TIMEOUT, TimeUnit.SECONDS)
                    .subscribe({
                        addMarker(LatLng(it.latitude, it.longitude))
                    }, { Timber.e(it) })
            markersDisposable.add(disposable)
        } else {
            viewBus.onNext(MapViewModel(shouldAskForPermission = true))
        }
    }

    override fun onCameraPositionChanged(visibleRegion: VisibleRegion?) {
        visibleRegion?.let { getPinsForLocationFromRepo(it) }
    }

    fun getPinsForLocationFromRepo(visibleRegion: VisibleRegion) {
        markersDisposable.clear()
        val sub = pinRepo
                .observePinsByLocationCorners(visibleRegion.latLngBounds.southwest, visibleRegion.latLngBounds.northeast)
                .subscribeOn(Schedulers.io())
                .subscribe({ onPinsReceived(it) }, { Timber.e(it) })
        markersDisposable.add(sub)
    }

    private fun onPinsReceived(pins: Array<PinDto>) {
        val newPins = pins.filter { pinsSet.contains(it).not() }
        val removePins = pinsSet.filter { pins.contains(it).not() }

        if (newPins.isEmpty().not() or removePins.isEmpty().not()) {
            pinsSet += newPins
            pinsSet -= removePins
            val model = MapViewModel(pins = pinsSet.toList())
            viewBus.onNext(model)
        }
    }

    fun addMarker(latLng: LatLng) {
        val args = Bundle()
        args.putParcelable(DropPinFragment.LOCATION_KEY, latLng)
        val navigationAction = ActivityNavigationAction(DropPinActivity::class.java, args)
        navBus.onNext(navigationAction)
    }

    override fun onGoToStreetView(location: LatLng?) {
        if (location == null) {
            if (isPermissionGranted) {
                val disposable = locationTracker
                        .getLastKnownLocation()
                        .compose(getLoadingStateTransformer())
                        .firstElement()
                        .timeout(GET_LAST_KNOWN_LOCATION_TIMEOUT, TimeUnit.SECONDS)
                        .subscribe({
                            navigateToStreetView(LatLng(it.latitude, it.longitude))
                        }, { Timber.e(it) })
                markersDisposable.add(disposable)
            } else {
                viewBus.onNext(MapViewModel(shouldAskForPermission = true))
            }
        } else {
            navigateToStreetView(location)
        }
    }

    fun navigateToStreetView(latLng: LatLng) {
        val args = Bundle()
        args.putParcelable(StreetFragment.FOCUS_LOCATION_KEY, latLng)
        val navigationAction = ActivityNavigationAction(StreetActivity::class.java, args)
        navBus.onNext(navigationAction)
    }

    override fun onPinClick(it: MapClusterItem) {

    }

    fun <T> getLoadingStateTransformer(): ObservableTransformer<T, T> {
        return ObservableTransformer {
            it
                    .doOnSubscribe {
                        val viewModel = MapViewModel(isLoading = true)
                        viewBus.onNext(viewModel)
                    }
                    .doOnNext {
                        val viewModel = MapViewModel(isLoading = false)
                        viewBus.onNext(viewModel)
                    }
                    .doOnError {
                        val viewModel = MapViewModel(isLoading = false)
                        viewBus.onNext(viewModel)
                    }
        }
    }

    override fun unsubscribe() {
        markersDisposable.clear()
    }
}