package com.woxapp.task.geopath.map;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.woxapp.task.geopath.R;
import com.woxapp.task.geopath.adapter.PlaceArrayAdapter;
import com.woxapp.task.geopath.model.Direction;
import com.woxapp.task.geopath.model.Leg;
import com.woxapp.task.geopath.model.Route;
import com.woxapp.task.geopath.model.Step;
import com.woxapp.task.geopath.retrofit.RetrofitHelper;
import com.woxapp.task.geopath.map.animation.MarkerAnimation;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

public class MapHelper {

    private static final String LOG_TAG = "MapHelper";
    public static final String KEY_MIDDLE = "MIDDLE";
    public static final String KEY_KEYBOARD = "KEYBOARD";
    public static final String KEY_LOADING = "LOADING";

    private static final String LANGUAGE = Locale.getDefault().getLanguage();

    private static GoogleMap mMap;
    private static GoogleApiClient mGoogleApiClient;
    private static PlaceArrayAdapter mPlaceArrayAdapter;

    private static Polyline mPolyline;

    private static Marker mStartMarker;
    private static Marker mStopMarker;
    private static List<Marker> mMarkers = new ArrayList<>();
    private static List<Marker> mWayPoints = new ArrayList<>();

    private static boolean mDrawHistoryMarkersCheck = false;
    private static int mChosenFiledId;

    private MapHelper() {
    }

    public static List<Marker> getWayPoints() {
        return mWayPoints;
    }

    public static Marker getStartMarker() {
        return mStartMarker;
    }

    public static Marker getStopMarker() {
        return mStopMarker;
    }

    public static void setPlaceArrayAdapter(PlaceArrayAdapter placeArrayAdapter) {
        mPlaceArrayAdapter = placeArrayAdapter;
    }

    public static void setMap(GoogleMap map, GoogleApiClient googleApiClient) {
        mMap = map;
        mGoogleApiClient = googleApiClient;
    }

    public static void setChosenFiledId(int chosenFiledId) {
        mChosenFiledId = chosenFiledId;
    }

    public static void removeStartMarker() {
        if (mStartMarker != null) {
            mMarkers.remove(mStartMarker);
            mStartMarker.remove();
            removePolyline();
        }
    }

    public static void removeStopMarker() {
        if (mStopMarker != null) {
            mMarkers.remove(mStopMarker);
            mStopMarker.remove();
            removePolyline();
        }
    }

    public static void clearMap() {
        mMap.clear();
        mMarkers.clear();
        mWayPoints.clear();
        mStartMarker = null;
        mStopMarker = null;
        removePolyline();
    }

    //change map's camera position according to the all markers
    private static void moveCamera() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        CameraUpdate cameraUpdate;
        if (mMarkers.size() == 1) {
            cameraUpdate = CameraUpdateFactory.newLatLng(mMarkers.get(0).getPosition());
        } else {
            for (Marker m : mMarkers) {
                builder.include(m.getPosition());
            }
            LatLngBounds bounds = builder.build();

            int padding = 100; // offset from edges of the map in pixels
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        }

        mMap.animateCamera(cameraUpdate);
    }

    private static void removePolyline() {
        if (mPolyline != null) {
            mPolyline.remove();
            MarkerAnimation.stopAnimation();
        }
    }

    public static void setMarkersFromHistory(int index) {
        clearMap();
        mDrawHistoryMarkersCheck = true; //draw markers from history
        getRoutes(index); //get markers and polyline from DB by id. Also start car animation
    }

    public static void getDirections(){

        mDrawHistoryMarkersCheck = false; // markers are already exist
        removePolyline(); //remove old polyline

        //get correct strings for request and way string for check direction in database
        String startStr = mStartMarker.getPosition().latitude + "," + mStartMarker.getPosition().longitude;
        String destinationStr = mStopMarker.getPosition().latitude + "," + mStopMarker.getPosition().longitude;
        String waypointsStr = "";
        String wayStr = mStartMarker.getTitle();

        if (!mWayPoints.isEmpty()) {
            waypointsStr = mWayPoints.get(0).getPosition().latitude + "," + mWayPoints.get(0).getPosition().longitude;
            wayStr += " - " + mWayPoints.get(0).getTitle();

            if (mWayPoints.size() > 1) {

                for (int i = 1; i < mWayPoints.size(); i++) {
                    Marker m = mWayPoints.get(i);
                    waypointsStr += "|" + m.getPosition().latitude + "," + m.getPosition().longitude;
                    wayStr += " - " + m.getTitle();
                }
            }
        }

        wayStr += " - " + mStopMarker.getTitle();

        //check is there a direction with current way, if not - send request
        Realm realm = Realm.getDefaultInstance();
        List<Direction> directionList = realm.where(Direction.class).findAll();

        if (!directionList.isEmpty()) {
            List<String> historyList = new ArrayList<>();
            for (Direction d : directionList) historyList.add(d.getWay());

            if (historyList.contains(wayStr)) {
                getRoutes(historyList.indexOf(wayStr));
            } else RetrofitHelper.getDirection(startStr, destinationStr, waypointsStr, wayStr, LANGUAGE);
        } else RetrofitHelper.getDirection(startStr, destinationStr, waypointsStr, wayStr, LANGUAGE);
    }

    //get routes for new direction from retrofit response
    public static void getRoutes(Direction direction) {
        getRoutes(direction, -1);
    }

    //get routes by the index in DB
    private static void getRoutes(int index) {
        getRoutes(null, index);
    }

    //asynchronously get polylineoptions, markersoptions and distance
    private static void getRoutes(final Direction newDirection, final int index) {
        new AsyncTask<Void, Void, List<MarkerOptions>>() {

            @Override
            protected List<MarkerOptions> doInBackground(Void... voids) {
                Realm realm = Realm.getDefaultInstance();
                Direction direction;

                if (index >= 0) {
                    List<Direction> directionList = realm.where(Direction.class).findAll();
                    direction = directionList.get(index);
                } else direction = newDirection;

                List<MarkerOptions> markerOptions = new ArrayList<>();
                List<Route> rRoutes = direction.getRoutes();
                List<Leg> legs;
                List<Step> steps;

                PolylineOptions lineOptions = new PolylineOptions();
                ArrayList<LatLng> points = new ArrayList<>();
                LatLng position;

                long distance = 0;

                for (int i = 0; i < rRoutes.size(); i++){
                    legs = rRoutes.get(i).getLegs();

                    for (int j = 0; j < legs.size(); j++) {
                        Leg leg = legs.get(j);
                        steps = leg.getSteps();
                        distance += leg.getDistance().getValue();

                        if (mDrawHistoryMarkersCheck) {
                            double lat = leg.getStartLocation().getLat();
                            double lng = leg.getStartLocation().getLng();
                            LatLng latLng = new LatLng(lat, lng);
                            if (j == 0) {
                                markerOptions.add(new MarkerOptions()
                                        .position(latLng)
                                        .title(leg.getStartAddress())
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            } else {
                                markerOptions.add(new MarkerOptions()
                                        .position(latLng)
                                        .title(leg.getStartAddress())
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                            }

                            if (j == legs.size() - 1) {
                                lat = leg.getEndLocation().getLat();
                                lng = leg.getEndLocation().getLng();
                                latLng = new LatLng(lat, lng);

                                markerOptions.add(new MarkerOptions()
                                        .position(latLng)
                                        .title(leg.getEndAddress())
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            }
                        }

                        for (int k = 0; k < steps.size(); k++){
                            String polyline = steps.get(k).getPolyline().getPoints();
                            List<LatLng> list = decodePoly(polyline);

                            for(int l = 0; l < list.size(); l++){
                                position = new LatLng((list.get(l)).latitude, (list.get(l)).longitude);
                                points.add(position);
                            }
                        }
                    }
                }

                lineOptions.addAll(points);
                lineOptions.width(7);
                lineOptions.color(Color.RED);

                // need this for calling from another thread. draw polyline and show distance
                EventBus.getDefault().postSticky(new PolylineEvent(lineOptions, distance));

                return markerOptions;
            }

            @Override
            protected void onPostExecute(List<MarkerOptions> markerOptions) {
                super.onPostExecute(markerOptions);

                if (mDrawHistoryMarkersCheck) {
                    setMarkersFromMarkerOptions(markerOptions);
                }
            }
        }.execute();
    }

    //draw polyline and start the car animation
    public static void drawPolyline(PolylineOptions lineOptions) {
        try {
            mPolyline = mMap.addPolyline(lineOptions);
            EventBus.getDefault().post(KEY_LOADING);
            List<LatLng> routePoints = mPolyline.getPoints();
            MarkerAnimation.animateLine(routePoints, mMap);
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Polyline is null.");
        }
    }

    //parse marker options list and set new markers
    private static void setMarkersFromMarkerOptions(List<MarkerOptions> markerOptions) {

        int size = markerOptions.size();

        mStartMarker = mMap.addMarker(markerOptions.get(0));
        mMarkers.add(mStartMarker);

        mStopMarker = mMap.addMarker(markerOptions.get(size - 1));
        mMarkers.add(mStopMarker);

        if (size > 2) {
            for (int i = 1; i < size - 1; i++) {
                Marker marker = mMap.addMarker(markerOptions.get(i));
                mWayPoints.add(marker);
                mMarkers.add(marker);
            }
        }

        moveCamera();
    }

    //some magic from the internet
    private static List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    //listener for AutocompleteTextViews, get predictions
    public static AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId;
            if (item != null) {
                placeId = String.valueOf(item.placeId);
                Log.i(LOG_TAG, "Selected: " + item.description);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
                Log.i(LOG_TAG, "Fetching details for ID: " + item.placeId);
            }
        }
    };

    //callback for AutocompleteTextViews, sets markers for new places
    private static ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(LOG_TAG, "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }

            final Place place = places.get(0);
            Marker marker = null;

            switch (mChosenFiledId) {
                case R.id.addFrom:
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName().toString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory
                                    .HUE_GREEN)));

                    mStartMarker = marker;
                    break;

                case R.id.addTo:
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName().toString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory
                                    .HUE_RED)));

                    mStopMarker = marker;
                    break;

                case R.id.addMiddle:
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName().toString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory
                                    .HUE_YELLOW)));

                    mWayPoints.add(marker);
                    break;
            }

            mMarkers.add(marker);

            EventBus.getDefault().post(KEY_KEYBOARD);
            if (mChosenFiledId == R.id.addMiddle) EventBus.getDefault().post(KEY_MIDDLE);

            moveCamera();
        }
    };
}