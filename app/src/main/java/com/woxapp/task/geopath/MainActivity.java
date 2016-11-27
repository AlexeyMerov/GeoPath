package com.woxapp.task.geopath;


import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.woxapp.task.geopath.adapter.PlaceArrayAdapter;
import com.woxapp.task.geopath.map.animation.MarkerAnimation;
import com.woxapp.task.geopath.model.Direction;
import com.woxapp.task.geopath.map.MapHelper;
import com.woxapp.task.geopath.map.PolylineEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;

import static com.woxapp.task.geopath.map.MapHelper.mAutocompleteClickListener;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        View.OnClickListener, View.OnFocusChangeListener,
        GoogleApiClient.ConnectionCallbacks,  GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.loader) LinearLayout loaderContainer;
    @BindView(R.id.loaderText) TextView loaderText;
    @BindView(R.id.distanceText) TextView distanceText;
    @BindView(R.id.startButton) Button startButton;
    @BindView(R.id.menuButton) Button menuButton;
    @BindView(R.id.addMiddleButton) Button addMiddleButton;

    @BindView(R.id.fromCancel) FrameLayout cancelFrom;
    @BindView(R.id.toCancel) FrameLayout cancelTo;
    @BindView(R.id.middleCancel) FrameLayout cancelMiddle;

    @BindView(R.id.addMiddleTextContainer) RelativeLayout middleContainer;

    @BindView(R.id.addFrom) AutoCompleteTextView autocompleteTextViewFrom;
    @BindView(R.id.addTo) AutoCompleteTextView autocompleteTextViewTo;
    @BindView(R.id.addMiddle) AutoCompleteTextView autocompleteTextViewMiddle;

    private static final String LOG_TAG = "MainActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private static final LatLngBounds BOUNDS = new LatLngBounds(new LatLng(-0, 0), new LatLng(0, 0));

    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private Realm mRealm;
    private PopupMenu mPopupMenu;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initDatabase();
        initMapComponents();
        initAutoComplete();
        initPopupMenu();

        startButton.setOnClickListener(this);
        menuButton.setOnClickListener(this);
        addMiddleButton.setOnClickListener(this);
        middleContainer.setOnClickListener(this);
        cancelMiddle.setOnClickListener(this);
        cancelFrom.setOnClickListener(this);
        cancelTo.setOnClickListener(this);

    }

    private void initDatabase() {
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                .build());

        mRealm = Realm.getDefaultInstance();
    }

    private void initMapComponents() {
        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map));

        mapFragment.setRetainInstance(true);

        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();

        MarkerAnimation.setCarIcon(getBitmapCar());
    }

    private void initAutoComplete() {
        autocompleteTextViewFrom.setOnFocusChangeListener(this);
        autocompleteTextViewTo.setOnFocusChangeListener(this);
        autocompleteTextViewMiddle.setOnFocusChangeListener(this);

        autocompleteTextViewFrom.setThreshold(3);
        autocompleteTextViewTo.setThreshold(3);
        autocompleteTextViewMiddle.setThreshold(3);

        autocompleteTextViewFrom.setOnItemClickListener(mAutocompleteClickListener);
        autocompleteTextViewTo.setOnItemClickListener(mAutocompleteClickListener);
        autocompleteTextViewMiddle.setOnItemClickListener(mAutocompleteClickListener);

        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS, null);

        MapHelper.setPlaceArrayAdapter(mPlaceArrayAdapter);

        autocompleteTextViewFrom.setAdapter(mPlaceArrayAdapter);
        autocompleteTextViewTo.setAdapter(mPlaceArrayAdapter);
        autocompleteTextViewMiddle.setAdapter(mPlaceArrayAdapter);

        //show "clear" button
        autocompleteTextViewFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() >= 1) cancelFrom.setVisibility(View.VISIBLE);
                else cancelFrom.setVisibility(View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        //show "clear" button
        autocompleteTextViewTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() >= 1) cancelTo.setVisibility(View.VISIBLE);
                else cancelTo.setVisibility(View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void initPopupMenu() {
        mPopupMenu = new PopupMenu(this, menuButton);
        mPopupMenu.inflate(R.menu.menu_items);
        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onMenuPressed(item);
                return true;
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap map) {
        MapHelper.setMap(map, mGoogleApiClient);
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                changeLoaderState();
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(LOG_TAG, "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        showToast("Google Places API connection failed with error code:" +
                connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(LOG_TAG, "Google Places API connection suspended.");
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startButton:
                if (MapHelper.getStartMarker() == null) {
                    showToast(getString(R.string.toast_choose_start_point));
                    break;
                }

                if (MapHelper.getStopMarker() == null) {
                    showToast(getString(R.string.toast_choose_destination_point));
                    break;
                }

                distanceText.setText("");
                changeLoaderState();
                MapHelper.getDirections();
                break;

            case R.id.menuButton:
                mPopupMenu.show();
                break;

            case R.id.addMiddleButton:
                if (MapHelper.getWayPoints().size() >= 3) {
                    showToast(getString(R.string.toast_over_three_middle_points));
                    break;
                }

                changeMiddleButtonVisibility();
                break;

            case R.id.middleCancel:
                changeMiddleButtonVisibility();
                break;

            case R.id.fromCancel:
                MapHelper.removeStartMarker();
                autocompleteTextViewFrom.setText("");
                cancelFrom.setVisibility(View.INVISIBLE);
                distanceText.setText("");
                break;

            case R.id.toCancel:
                MapHelper.removeStopMarker();
                autocompleteTextViewTo.setText("");
                cancelTo.setVisibility(View.INVISIBLE);
                distanceText.setText("");
                break;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onMenuPressed(item);
        return super.onOptionsItemSelected(item);
    }

    private void onMenuPressed(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.clearMap:
                MapHelper.clearMap();
                autocompleteTextViewFrom.setText("");
                autocompleteTextViewTo.setText("");
                autocompleteTextViewMiddle.setText("");
                break;

            case R.id.history:
                showHistory();
                break;

            case R.id.clearHistory:
                mRealm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.deleteAll();
                    }
                });
                break;
        }
    }


    private void changeMiddleButtonVisibility() {

        if (addMiddleButton.getVisibility() == View.VISIBLE) {
            addMiddleButton.setVisibility(View.INVISIBLE);
            middleContainer.setVisibility(View.VISIBLE);
        } else {
            middleContainer.setVisibility(View.INVISIBLE);
            autocompleteTextViewMiddle.setText("");
            addMiddleButton.setVisibility(View.VISIBLE);
        }

    }

    private void changeLoaderState() {
        if (loaderContainer.getVisibility() == View.INVISIBLE) {
            loaderContainer.setVisibility(View.VISIBLE);
            loaderText.setText(R.string.loading);
        } else loaderContainer.setVisibility(View.INVISIBLE);
    }

    private void showHistory() {
        long directionSize = mRealm.where(Direction.class).count();

        if (directionSize != 0) {
            final List<Direction> directionList = mRealm.where(Direction.class).findAll();
            final List<String> historyList = new ArrayList<>();
            String[] historyArray;

            for (Direction d : directionList) historyList.add(d.getWay());

            historyArray = new String[historyList.size()];
            historyArray = historyList.toArray(historyArray);

            AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
            myDialog.setTitle(R.string.history_str);

            myDialog.setItems(historyArray, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int index) {

                    distanceText.setText("");
                    String[] waypointsArray = historyList.get(index).split(" - ");
                    autocompleteTextViewFrom.setText(waypointsArray[0]);
                    autocompleteTextViewFrom.clearFocus();
                    autocompleteTextViewTo.setText(waypointsArray[waypointsArray.length - 1]);
                    autocompleteTextViewTo.clearFocus();

                    changeLoaderState();
                    MapHelper.setMarkersFromHistory(index );

                }});

            myDialog.setNegativeButton(R.string.cancel_alert_dialog_button, null);
            myDialog.show();

        } else showToast(getString(R.string.toast_empty_history));
    }


    @Override
    public void onFocusChange(View view, boolean b) {
        switch (view.getId()) {

            case R.id.addFrom:
                MapHelper.setChosenFiledId(R.id.addFrom);
                break;

            case R.id.addTo:
                MapHelper.setChosenFiledId(R.id.addTo);
                break;

            case R.id.addMiddle:
                MapHelper.setChosenFiledId(R.id.addMiddle);
                break;

        }
    }

    @Subscribe
    public void onEvent(String event) {
        switch (event) {
            case MapHelper.KEY_LOADING:
                changeLoaderState();
                break;

            case MapHelper.KEY_KEYBOARD:
                if (getCurrentFocus() != null) {
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                break;

            case MapHelper.KEY_MIDDLE:
                changeMiddleButtonVisibility();
                break;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(PolylineEvent event) {
        MapHelper.drawPolyline(event.getPolylineOptions());
        showDistance(event.getDistance());
    }

    private void showDistance(long distance) {
        String result;

        if (distance < 1000) {
            result = String.valueOf(distance) + " " + getString(R.string.metric_system_meters);
        } else {
            result = String
                    .valueOf(distance / 1000) + " " + getString(R.string.metric_system_kilometers);
        }

        distanceText.setText(result);
    }

    private Bitmap getBitmapCar() {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(),
                R.drawable.ic_directions_car_black_24px, null);
        Bitmap bitmap = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            DrawableCompat.setTint(vectorDrawable, ContextCompat
                    .getColor(getApplicationContext(), R.color.black));
            vectorDrawable.draw(canvas);
        }
        return bitmap;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}