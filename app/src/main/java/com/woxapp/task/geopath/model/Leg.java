package com.woxapp.task.geopath.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;

public class Leg extends RealmObject {

    @SerializedName("distance")
    @Expose
    private Distance distance;

    @SerializedName("end_address")
    @Expose
    private String mEndAddress;

    @SerializedName("end_location")
    @Expose
    private EndLocation mEndLocation;

    @SerializedName("start_address")
    @Expose
    private String mStartAddress;

    @SerializedName("start_location")
    @Expose
    private StartLocation mStartLocation;

    @SerializedName("steps")
    @Expose
    private RealmList<Step> mSteps = new RealmList<>();

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public RealmList<Step> getSteps() {
        return mSteps;
    }

    public void setSteps(RealmList<Step> steps) {
        mSteps = steps;
    }

    public String getEndAddress() {
        return mEndAddress;
    }

    public void setEndAddress(String endAddress) {
        mEndAddress = endAddress;
    }

    public EndLocation getEndLocation() {
        return mEndLocation;
    }

    public void setEndLocation(EndLocation endLocation) {
        mEndLocation = endLocation;
    }

    public String getStartAddress() {
        return mStartAddress;
    }

    public void setStartAddress(String startAddress) {
        mStartAddress = startAddress;
    }

    public StartLocation getStartLocation() {
        return mStartLocation;
    }

    public void setStartLocation(StartLocation startLocation) {
        mStartLocation = startLocation;
    }
}
