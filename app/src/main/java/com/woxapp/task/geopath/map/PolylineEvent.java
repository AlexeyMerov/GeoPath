package com.woxapp.task.geopath.map;

import com.google.android.gms.maps.model.PolylineOptions;

public class PolylineEvent {

    private PolylineOptions mPolylineOptions;
    private long mDistance;

    PolylineEvent(PolylineOptions polylineOptions, long distance) {
        mPolylineOptions = polylineOptions;
        mDistance = distance;
    }

    public PolylineOptions getPolylineOptions() {
        return mPolylineOptions;
    }

    public void setPolylineOptions(PolylineOptions polylineOptions) {
        mPolylineOptions = polylineOptions;
    }

    public long getDistance() {
        return mDistance;
    }

    public void setDistance(long distance) {
        mDistance = distance;
    }
}
