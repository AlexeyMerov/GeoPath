package com.woxapp.task.geopath.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;

public class Route extends RealmObject {

    @SerializedName("legs")
    @Expose
    private RealmList<Leg> mLegs = new RealmList<>();

    public RealmList<Leg> getLegs() {
        return mLegs;
    }

    public void setLegs(RealmList<Leg> legs) {
        mLegs = legs;
    }


}
