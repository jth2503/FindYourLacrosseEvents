package de.joristhiele.findyourlacrosseevents;

import android.text.Editable;

public class Util {
    public static Boolean StringIsNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
    public static Boolean StringIsNullOrEmpty(Editable s) {
        return s == null || s.toString().isEmpty();
    }
}
