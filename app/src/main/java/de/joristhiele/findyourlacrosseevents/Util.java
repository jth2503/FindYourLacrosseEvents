package de.joristhiele.findyourlacrosseevents;

import android.content.Context;
import android.location.LocationManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

public class Util {

    // custom TextWatcher to save lines of code
    public static class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }

    public static Boolean StringIsNullOrEmpty(Editable s) {
        return s == null || s.toString().isEmpty();
    }

    public static Boolean isLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
