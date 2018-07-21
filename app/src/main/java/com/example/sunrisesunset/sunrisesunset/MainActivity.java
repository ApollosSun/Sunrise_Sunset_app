package com.example.sunrisesunset.sunrisesunset;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private AddressResultReceiver mResultReceiver;
    private PlaceAutocompleteFragment autocompleteFragment;

    FragmentManager mFragmentManager;
    FragmentTransaction mFragmentTransaction;

    private boolean mRequestingAddress;
    private boolean mRequestingLocationUpdates;
    private boolean mRequestingSunInfo;
    private boolean isFrCreated;
    private boolean ifShownPermissionExplanation;

    private String mAddressOutput;

    private SunInfoFragment sunInfoFragment;
    private Fragment mSavedFragment;

    private ProgressBar mProgressBar;
    private TextView mLocationAddressTextView;
    private Snackbar mNoInternetSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRequestingSunInfo = true;
        mRequestingLocationUpdates = true;
        mRequestingAddress = true;
        ifShownPermissionExplanation = false;
        isFrCreated = false;

        mCurrentLocation = new Location("");

        mAddressOutput = "";

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        mResultReceiver = new AddressResultReceiver(new Handler());
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        mProgressBar = findViewById(R.id.progress_bar);
        mLocationAddressTextView = findViewById(R.id.tvAddress);

        sunInfoFragment = new SunInfoFragment();
        mFragmentManager = getSupportFragmentManager();

        updateValuesFromBundle(savedInstanceState);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        searchPlaceByAutocompleteFr();
        makeNoInternetSnackbar();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(Constants.KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        Constants.KEY_REQUESTING_LOCATION_UPDATES);
            }
            if (savedInstanceState.keySet().contains(Constants.KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(Constants.KEY_LOCATION);
            }
            if (savedInstanceState.keySet().contains(Constants.ADDRESS_REQUESTED_KEY)) {
                mRequestingAddress = savedInstanceState.
                        getBoolean(Constants.ADDRESS_REQUESTED_KEY);
            }
            if (savedInstanceState.keySet().contains(Constants.LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(Constants.LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
            if (savedInstanceState.keySet().contains(Constants.REQUEST_PERMISSION_KEY)){
                ifShownPermissionExplanation = savedInstanceState
                        .getBoolean(Constants.REQUEST_PERMISSION_KEY);
            }
            if (savedInstanceState.keySet().contains(Constants.FRAGMENT_CREATED_KEY)){
                isFrCreated = savedInstanceState.getBoolean(Constants.FRAGMENT_CREATED_KEY);
            }
            if (getSupportFragmentManager().
                    getFragment(savedInstanceState, "sunInfoFragment") != null){

                mSavedFragment = getSupportFragmentManager().
                        getFragment(savedInstanceState, "sunInfoFragment");
                sunInfoFragment = (SunInfoFragment) mSavedFragment;
            }
            if (savedInstanceState.keySet().contains(Constants.PROGRESSBAR_STATE_KEY)){
                mProgressBar.setVisibility(savedInstanceState
                        .getInt(Constants.PROGRESSBAR_STATE_KEY));
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(Constants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();

                if(mCurrentLocation != null){

                    mRequestingLocationUpdates = true;
                    autocompleteFragment.setText("");
                    loadInfo(mCurrentLocation);
                    stopLocationUpdates();

                    Log.i(LOG_TAG,"Location was updated, current position is "
                            + mCurrentLocation);
                } else {
                    mLocationAddressTextView.setText(getResources().
                            getString(R.string.no_location_found));

                    mProgressBar.setVisibility(View.GONE);
                }
            }
        };
    }

    private void searchPlaceByAutocompleteFr(){

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .build();

        autocompleteFragment.setFilter(typeFilter);
        autocompleteFragment.setHint("Search new city");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                getDataFromPlace(place);
                loadInfo(mCurrentLocation);
                displayAddressOutput();

                Log.i(LOG_TAG, "Place was found. Address: " + place.getAddress());
            }

            @Override
            public void onError(Status status) {
                Log.i(LOG_TAG, "An error occurred: " + status);
            }
        });
    }

    private void getDataFromPlace(Place place){

        LatLng latLng = place.getLatLng();
        String address = String.valueOf(place.getAddress());
        String[] separated = address.split(", ");

        mCurrentLocation.setLatitude(latLng.latitude);
        mCurrentLocation.setLongitude(latLng.longitude);
        mAddressOutput = "Information for the next location: ";

        for (int i = 0; i<separated.length - 1; i++){
            mAddressOutput += separated[i];
            if (i < separated.length - 2){
                mAddressOutput += ", ";
            }
        }
    }

    private void loadInfo(Location location){

        if(mNoInternetSnackbar.isShown()){
            mNoInternetSnackbar.dismiss();
        }

        if(isNetAvailable()) {

            Bundle bundle = new Bundle();
            bundle.putString("latitude", String.valueOf(location.getLatitude()));
            bundle.putString("longitude", String.valueOf(location.getLongitude()));
            sunInfoFragment.setArguments(bundle);

            if (!isFrCreated){

                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.add(R.id.container, sunInfoFragment).commit();

                isFrCreated = true;

            } else {
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.detach(sunInfoFragment).attach(sunInfoFragment).commit();
            }

            if (mRequestingAddress) {
                startIntentService();
            }

            mRequestingSunInfo = false;

        } else {
            mNoInternetSnackbar.show();
        }
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(LOG_TAG,
                                "User agreed to make required location settings changes.");
                        mRequestingLocationUpdates = true;
                        mRequestingSunInfo = true;
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOG_TAG,
                                "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        mRequestingSunInfo = false;
                        mProgressBar.setVisibility(View.GONE);

                        showSnackbar(R.string.location_rationale,
                                android.R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mProgressBar.setVisibility(View.VISIBLE);
                                        startLocationUpdates();
                                    }
                                });
                        break;
                }
                break;
        }
    }

    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener
                        (this, new OnSuccessListener<LocationSettingsResponse>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                                Log.i(LOG_TAG, "All location settings are satisfied.");
                                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                        mLocationCallback, Looper.myLooper());
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(LOG_TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this,
                                            Constants.REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(LOG_TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(LOG_TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }
                    }
                });
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(LOG_TAG, "stopLocationUpdates: updates never requested.");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    private void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.DATA_EXTRA, mCurrentLocation);
        startService(intent);
    }

    private class AddressResultReceiver extends ResultReceiver {

        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode == Constants.SUCCESS_RESULT) {

                mAddressOutput = "Information for the next location: "
                        + resultData.getString(Constants.RESULT_DATA_KEY);
                mRequestingAddress = false;

            } else if (resultCode == Constants.FAILURE_RESULT && isNetAvailable()){
                mAddressOutput = getResources().getString(R.string.no_address_info);
            }

            displayAddressOutput();
        }
    }

    private void displayAddressOutput() {
        mLocationAddressTextView.setVisibility(View.VISIBLE);
        String currentAddress = mAddressOutput;
        mLocationAddressTextView.setText(currentAddress);
    }

    private void makeNoInternetSnackbar(){

        if (mNoInternetSnackbar == null){
            mNoInternetSnackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.no_Internet_message,
                    Snackbar.LENGTH_INDEFINITE);

            mNoInternetSnackbar.setAction(getString(R.string.retry), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNetAvailable()){
                        if (mRequestingLocationUpdates) {
                            startIntentService();
                        }
                        loadInfo(mCurrentLocation);
                    } else {
                        showSnackbar(R.string.no_Internet,
                                android.R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                    }
                                });
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!ifShownPermissionExplanation){
            if (mRequestingLocationUpdates && checkPermissions()) {
                startLocationUpdates();
            } else if (!checkPermissions()) {
                requestPermissions();
            } else if (mRequestingSunInfo && mCurrentLocation != null){
                loadInfo(mCurrentLocation);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_update_location:

                mRequestingLocationUpdates = true;
                mRequestingAddress = true;
                startLocationUpdates();
                return true;

            case R.id.action_refresh:

                if(mCurrentLocation == null){
                    mRequestingLocationUpdates = true;
                    mRequestingAddress = true;
                    startLocationUpdates();
                } else {
                    loadInfo(mCurrentLocation);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(Constants.KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(Constants.KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putBoolean(Constants.ADDRESS_REQUESTED_KEY, mRequestingAddress);
        savedInstanceState.putString(Constants.LOCATION_ADDRESS_KEY, mAddressOutput);
        savedInstanceState.putBoolean(Constants.REQUEST_PERMISSION_KEY, ifShownPermissionExplanation);
        savedInstanceState.putBoolean(Constants.FRAGMENT_CREATED_KEY,isFrCreated);

        if (isFrCreated) {
            getSupportFragmentManager()
                    .putFragment(savedInstanceState, "sunInfoFragment", sunInfoFragment);
        }

        savedInstanceState.putInt(Constants.PROGRESSBAR_STATE_KEY, mProgressBar.getVisibility());
        super.onSaveInstanceState(savedInstanceState);
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(LOG_TAG, "Displaying permission rationale to provide additional context.");
            mProgressBar.setVisibility(View.GONE);
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    Constants.REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(LOG_TAG, "Requesting permission");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionResult");
        if (requestCode == Constants.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(LOG_TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "Permission granted, updates requested, starting location updates");
                startLocationUpdates();
            } else {
                ifShownPermissionExplanation = true;
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ifShownPermissionExplanation = false;
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    public boolean isNetAvailable() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        }
        catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }
        return false;
    }

}
