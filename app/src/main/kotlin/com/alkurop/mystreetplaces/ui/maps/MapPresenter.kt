package com.alkurop.mystreetplaces.ui.maps

import com.alkurop.mystreetplaces.ui.navigation.NavigationAction
import com.alkurop.mystreetplaces.utils.LocationTracker
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import io.reactivex.subjects.Subject

interface MapPresenter {
    val viewBus: Subject<MapViewModel>
    val navBus: Subject<NavigationAction>
    var locationTracker:LocationTracker
    var isPermissionGranted:Boolean

    fun onGoToStreetView(location:LatLng?)

    fun onAddMarker()

    fun unsubscribe()

    fun onCameraPositionChanged(visibleRegion: VisibleRegion?)

    fun onPinClick (it: MapClusterItem)

}

