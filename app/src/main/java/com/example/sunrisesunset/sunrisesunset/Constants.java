package com.example.sunrisesunset.sunrisesunset;

final class Constants {

    static final int SUCCESS_RESULT = 0;
    static final int FAILURE_RESULT = 1;

    static final String PACKAGE_NAME = "com.example.sunrisesunset.sunrisesunset";
    static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    static final String DATA_EXTRA = PACKAGE_NAME + ".DATA_EXTRA";

    static final int REQUEST_PERMISSIONS_REQUEST_CODE = 33;
    static final int REQUEST_CHECK_SETTINGS = 0x1;
    static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    final static String KEY_LOCATION = "location";
    static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    static final String LOCATION_ADDRESS_KEY = "location-address";
    static final String REQUEST_PERMISSION_KEY = "request-permission";
    static final String FRAGMENT_CREATED_KEY = "fragment-created";
    static final String PROGRESSBAR_STATE_KEY = "progress-bar-state";
}
