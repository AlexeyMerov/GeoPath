
package com.woxapp.task.geopath.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

public class Distance extends RealmObject {

    @SerializedName("text")
    @Expose
    private String mText;
    @SerializedName("value")
    @Expose
    private Integer mValue;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public Integer getValue() {
        return mValue;
    }

    public void setValue(Integer value) {
        mValue = value;
    }

}
