package com.criptext.monkeychatandroid.location;

import android.view.View;

import com.criptext.monkeychatandroid.R;
import com.criptext.monkeykitui.recycler.holders.MonkeyHolder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.NotNull;

/**
 * Created by daniel on 5/23/16.
 */

public class LocationHolder extends MonkeyHolder implements OnMapReadyCallback {

    public MapView mapView;
    public GoogleMap map;

    public LocationHolder(@NotNull View view) {
        super(view);

        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        //TODO RECIBIR LAT Y LONG DESDE EL MONKEYITEM
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-2.1461342,-79.9696275), 13f));
        map.addMarker(new MarkerOptions().position(new LatLng(-2.1461342,-79.9696275)));
    }
}
