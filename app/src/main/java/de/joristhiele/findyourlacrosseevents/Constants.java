package de.joristhiele.findyourlacrosseevents;

import com.google.android.gms.maps.model.LatLng;

public class Constants {
    public static final String EMPTY_STRING = "";

    // API-Strings
    public static final String URL_ADDRESS_VALIDATION = "https://addressvalidation.googleapis.com/v1:validateAddress?key=";

    // Volley Error Strings
    public static final String PARSE_ERROR = "PARSE_ERROR: ";
    public static final String RESPONSE_ERROR = "RESPONSE_ERROR";

    // Map defaults
    public static final LatLng MAP_CENTER = new LatLng(50.1133, 9.252536); // according to https://de.wikipedia.org/wiki/Mittelpunkt_Europas#Historische_Mittelpunkte_der_Europ%C3%A4ischen_Union
    public static final float DEFAULT_ZOOM_WITH_LOCATION = 7.0f;
    public static final float DEFAULT_ZOOM_WITHOUT_LOCATION = 5.0f;
}
