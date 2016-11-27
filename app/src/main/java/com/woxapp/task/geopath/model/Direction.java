package com.woxapp.task.geopath.model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;

public class Direction extends RealmObject {

    @SerializedName("routes")
    @Expose
    private RealmList<Route> mRoutes = new RealmList<>();

    private String mWay;

    public String getWay() {
        return mWay;
    }

    public void setWay(String way) {
        mWay = way;
    }

    public RealmList<Route> getRoutes() {
        return mRoutes;
    }

    public void setRoutes(RealmList<Route> routes) {
        mRoutes = routes;
    }
}
