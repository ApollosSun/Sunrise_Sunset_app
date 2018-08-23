package com.example.sunrisesunset.sunrisesunset;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SunInfoFragmentOkHttp extends Fragment {

    private final String LOG_TAG = SunInfoFragmentOkHttp.class.getSimpleName();

    private String latitude;
    private String longitude;
    private String timezone;

    private TextView tvSunrise;
    private TextView tvSunset;
    private TextView tvErrorMessage;
    LinearLayout contentLayout;
    ProgressBar progressBar;

    private SunInfo sunInfo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        tvSunrise = rootView.findViewById(R.id.tvSunrise);
        tvSunset = rootView.findViewById(R.id.tvSunset);
        tvErrorMessage = rootView.findViewById(R.id.tvErrorMessage);
        contentLayout = rootView.findViewById(R.id.contentLayout);
        progressBar = rootView.findViewById(R.id.frProgress_bar);

        getArgs();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    private void getArgs (){

        if(getArguments() != null) {
            latitude = getArguments().getString("latitude");
            longitude = getArguments().getString("longitude");
        } else {
            latitude = "0";
            longitude = "0";
        }
    }

    private void updateWeather() {
        FetchSunInfoTask fetchSunInfoTask = new FetchSunInfoTask();
        String[] str = new String[]{latitude, longitude};
        fetchSunInfoTask.execute(str);
    }

    private void updateUI (int result){
        if(result == Constants.SUCCESS_RESULT){
            contentLayout.setVisibility(View.VISIBLE);
            tvSunrise.setText("Sunrise time\n" + getLocalTime(sunInfo.getmSunrise()));
            tvSunset.setText("Suntset time\n" + getLocalTime(sunInfo.getmSunset()));
        } else {
            tvErrorMessage.setVisibility(View.VISIBLE);
            tvErrorMessage.setText(getResources().getString(R.string.failed_error_message));
        }
    }

    private String getLocalTime(String time){
        SimpleDateFormat initialDateFormat = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        initialDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;

        try {
            date = initialDateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat finalDateFormat = new SimpleDateFormat
                ("HH:mm", Locale.ENGLISH);
        finalDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        return finalDateFormat.format(date);
    }

    public class FetchSunInfoTask extends AsyncTask<String, Void, String> {

        private String getDataFromJson(String sunInfoJsonStr, String timeInfoJsonStr)
                throws JSONException {

            final String RESULTS_ROW = "results";
            final String STATUS_ROW = "status";
            final String TIMEZONE_ROW = "timeZoneId";
            String resultStr = "";

            Gson gson = new GsonBuilder().create();

            JSONObject timeInfoJson = new JSONObject(timeInfoJsonStr);
            timezone = timeInfoJson.getString(TIMEZONE_ROW);

            JSONObject sunInfoJson = new JSONObject(sunInfoJsonStr);

            switch (sunInfoJson.getString(STATUS_ROW)){
                case "OK":
                    JSONObject sunInfoJsonObj = sunInfoJson.getJSONObject(RESULTS_ROW);
                    sunInfo = gson.fromJson(sunInfoJsonObj.toString(), SunInfo.class);
                    resultStr = "Success";
                    Log.i(LOG_TAG,"Fetching data is successful.");
                    break;
                case "INVALID_REQUEST":
                    Log.i(LOG_TAG,"Fetch sun info result: INVALID_REQUEST");
                    break;
                case "INVALID_DATE":
                    Log.i(LOG_TAG,"Fetch sun info result: INVALID_DATE");
                    break;
                case "UNKNOWN_ERROR":
                    Log.i(LOG_TAG,"Fetch sun info result: UNKNOWN_ERROR");
                    break;
            }
            return resultStr;
        }

        private String apiRequest(String url){

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try {

                Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                return responseData;

            } catch (IOException e){
                Log.e(LOG_TAG, "Error ", e);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            contentLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {

            String sunInfoJsonStr;
            String timeInfoJsonStr;
            String latitude = params[0];
            String longitude = params[1];
            String formatted = String.valueOf(0);

            String location = latitude + "," + longitude;
            String apiKey="";
            String timestamp = String.valueOf(System.currentTimeMillis()/1000);

            try {
                ApplicationInfo ai = getActivity().getPackageManager()
                        .getApplicationInfo(getActivity()
                                .getPackageName(), PackageManager.GET_META_DATA);

                apiKey = ai.metaData.getString("com.google.android.geo.API_KEY");
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            final String BASE_URL_SUN = "https://api.sunrise-sunset.org/json?";
            final String LATITUDE_PARAM = "lat";
            final String LONGITUDE_PARAM = "lng";
            final String FORMATTED_PARAM = "formatted";

            final String BASE_URL_TIME = "https://maps.googleapis.com/maps/api/timezone/json?";
            final String LOCATION_PARAM = "location";
            final String TIMESTAMP_PARAM = "timestamp";
            final String API_KEY_PARAM = "key";

            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL_SUN).newBuilder();
            urlBuilder.addQueryParameter(LATITUDE_PARAM, latitude);
            urlBuilder.addQueryParameter(LONGITUDE_PARAM, longitude);
            urlBuilder.addQueryParameter(FORMATTED_PARAM, formatted);

            String url = urlBuilder.build().toString();

            sunInfoJsonStr = apiRequest(url);

            urlBuilder = HttpUrl.parse(BASE_URL_TIME).newBuilder();
            urlBuilder.addQueryParameter(LOCATION_PARAM, location);
            urlBuilder.addQueryParameter(TIMESTAMP_PARAM, timestamp);
            urlBuilder.addQueryParameter(API_KEY_PARAM, apiKey);

            url = urlBuilder.build().toString();

            timeInfoJsonStr = apiRequest(url);

            try {
                return getDataFromJson(sunInfoJsonStr, timeInfoJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            progressBar.setVisibility(View.GONE);

            if (result.length() > 0) {
                updateUI(Constants.SUCCESS_RESULT);
            } else {
                updateUI(Constants.FAILURE_RESULT);
            }
        }
    }
}
