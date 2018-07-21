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

public class SunInfoFragment extends Fragment {

    private final String LOG_TAG = SunInfoFragment.class.getSimpleName();

    private String sunrise;
    private String sunset;
    private String latitude;
    private String longitude;
    private String timezone;

    private TextView tvSunrise;
    private TextView tvSunset;
    private TextView tvErrorMessage;
    LinearLayout contentLayout;
    ProgressBar progressBar;

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
            tvSunrise.setText("Sunrise time\n" + getLocalTime(sunrise));
            tvSunset.setText("Suntset time\n" + getLocalTime(sunset));
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

    public class FetchSunInfoTask extends AsyncTask<String, Void, String[]> {

        private String[] getDataFromJson(String sunInfoJsonStr, String timeInfoJsonStr)
                throws JSONException {

            final String RESULTS_ROW = "results";
            final String SUNRISE_ROW = "sunrise";
            final String SUNSET_ROW = "sunset";
            final String STATUS_ROW = "status";
            final String TIMEZONE_ROW = "timeZoneId";

            String[] resultStrs = new String[4];

            JSONObject sunInfoJson = new JSONObject(sunInfoJsonStr);
            resultStrs[0] = sunInfoJson.getString(STATUS_ROW);

            switch (resultStrs[0]){
                case "OK":
                    JSONObject sunInfoJsonObj = sunInfoJson.getJSONObject(RESULTS_ROW);
                    resultStrs[1] = sunInfoJsonObj.getString(SUNRISE_ROW);
                    resultStrs[2] = sunInfoJsonObj.getString(SUNSET_ROW);

                    JSONObject timeInfoJson = new JSONObject(timeInfoJsonStr);
                    resultStrs[3] = timeInfoJson.getString(TIMEZONE_ROW);

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
            return resultStrs;
        }

        private String apiRequest(Uri builtUri){

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {

                URL url = new URL(builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream;

                try {

                    inputStream = urlConnection.getInputStream();

                } catch (IOException e) {

                    inputStream = urlConnection.getErrorStream();
                    Log.i(LOG_TAG, "Failed to load resource" + e.getMessage());

                }

                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                return buffer.toString();
            } catch (IOException e){

                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        protected void onPreExecute() {
            contentLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String[] doInBackground(String... params) {

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

            Uri builtUri = Uri.parse(BASE_URL_SUN).buildUpon()
                    .appendQueryParameter(LATITUDE_PARAM, latitude)
                    .appendQueryParameter(LONGITUDE_PARAM, longitude)
                    .appendQueryParameter(FORMATTED_PARAM, formatted)
                    .build();

            sunInfoJsonStr = apiRequest(builtUri);

            builtUri = Uri.parse(BASE_URL_TIME).buildUpon()
                    .appendQueryParameter(LOCATION_PARAM, location)
                    .appendQueryParameter(TIMESTAMP_PARAM, timestamp)
                    .appendQueryParameter(API_KEY_PARAM, apiKey)
                    .build();

            timeInfoJsonStr = apiRequest(builtUri);

            try {
                return getDataFromJson(sunInfoJsonStr, timeInfoJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {

            progressBar.setVisibility(View.GONE);

            if (result[2] != null) {
                sunrise = result[1];
                sunset = result[2];
                timezone = result[3];
                updateUI(Constants.SUCCESS_RESULT);
            } else {
                updateUI(Constants.FAILURE_RESULT);
            }
        }
    }
}
