package com.woxapp.task.geopath.retrofit;


import android.util.Log;

import com.woxapp.task.geopath.model.Direction;
import com.woxapp.task.geopath.map.MapHelper;

import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class RetrofitHelper {

    private interface MapApiInterface {
        @GET("/maps/api/directions/json")
        Call<Direction> getDirection(@Query("origin") String origin,
                                     @Query("destination") String destination,
                                     @Query("language") String language,
                                     @Query("key") String key);

        @GET("/maps/api/directions/json")
        Call<Direction> getDirection(@Query("origin") String origin,
                                     @Query("destination") String destination,
                                     @Query("waypoints") String waypoints,
                                     @Query("language") String language,
                                     @Query("key") String key);

    }

    private final static String LOG_KEY = "Retrofit";
    private final static String KEY = "AIzaSyDxmzEqoLXRPK2-lMlR2f_ghsiJ51SqgoU";
    private final static String BASE_URL = "https://maps.googleapis.com";
    private final static Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private final static MapApiInterface SERVICE_DASHBOARD = RETROFIT.create(MapApiInterface.class);

    public static void getDirection(String origin, String destination, String waypoints, final String way,
                             String language) {
        //check if there are middle points and perform the right request
        Call<Direction> call;
        if (waypoints.isEmpty()) call = SERVICE_DASHBOARD
                .getDirection(origin, destination, language, KEY);
        else call = SERVICE_DASHBOARD.getDirection(origin, destination, waypoints, language, KEY);

        call.enqueue(new Callback<Direction>() {
            @Override
            public void onResponse(Call<Direction> call, Response<Direction> response) {
                if (response.isSuccessful()) {

                    Realm realm = Realm.getDefaultInstance();
                    final Direction direction = response.body();
                    direction.setWay(way);

                    realm.executeTransactionAsync(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealm(direction);
                        }
                    });

                    MapHelper.getRoutes(direction);

                } else {
                    Log.e(LOG_KEY, "Response not successful: 400, 401, 403 etc");
                }
            }

            @Override
            public void onFailure(Call<Direction> call, Throwable t) {
                Log.e(LOG_KEY, "Callback failure");
            }
        });
    }


}
