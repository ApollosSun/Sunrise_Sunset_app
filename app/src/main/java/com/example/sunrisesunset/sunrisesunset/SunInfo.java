package com.example.sunrisesunset.sunrisesunset;

import com.google.gson.annotations.SerializedName;

public class SunInfo {

    @SerializedName("sunrise")
    private String mSunrise;

    @SerializedName("sunset")
    private String mSunset;

    public SunInfo (String sunrise, String sunset){
        mSunrise = sunrise;
        mSunset = sunset;
    }

    public String getmSunrise() {
        return mSunrise;
    }

    public String getmSunset() {
        return mSunset;
    }

}
