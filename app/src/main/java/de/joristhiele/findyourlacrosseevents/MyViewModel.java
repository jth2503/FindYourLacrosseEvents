package de.joristhiele.findyourlacrosseevents;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.List;

import de.joristhiele.findyourlacrosseevents.data.Event;

public class MyViewModel extends AndroidViewModel {

    private final MyBackendConnection backendConnection;

    private LiveData<List<ParseObject>> allGenders;
    private LiveData<List<ParseObject>> allDisciplines;
    private LiveData<List<ParseObject>> allEventTypes;

    private LiveData<Pair<String, ParseGeoPoint>> addressCheckResult;

    private LiveData<Boolean> newEventSaved;

    private Event newEvent;

    public MyViewModel(@NonNull Application application) {
        super(application);
        String apiKey = Constants.EMPTY_STRING;
        try {
            apiKey = application
                    .getApplicationContext()
                    .getPackageManager()
                    .getApplicationInfo(application.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData
                    .getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        RequestQueue queue = Volley.newRequestQueue(application);
        this.backendConnection = new MyBackendConnection(queue, apiKey);
    }

    public LiveData<List<ParseObject>> getAllGenders() {
        // pull the data from the backend unchanged
        if (allGenders == null) {
            allGenders = Transformations.map(backendConnection.getAllGenders(), allGenders -> allGenders);
        }
        return allGenders;
    }
    public LiveData<List<ParseObject>> getAllDisciplines() {
        // pull the data from the backend unchanged
        if (allDisciplines == null) {
            allDisciplines = Transformations.map(backendConnection.getAllDisciplines(), allDisciplines -> allDisciplines);
        }
        return allDisciplines;
    }
    public LiveData<List<ParseObject>> getAllEventTypes() {
        // pull the data from the backend unchanged
        if (allEventTypes == null) {
            allEventTypes = Transformations.map(backendConnection.getAllEventTypes(), allEventTypes -> allEventTypes);
        }
        return allEventTypes;
    }

    public void instantiateNewEvent() {
        newEvent = new Event();
    }
    public Event getNewEvent() {
        if (newEvent == null)
            instantiateNewEvent();
        return newEvent;
    }

    public LiveData<Pair<String, ParseGeoPoint>> getAddressCheckResult() {
        if (addressCheckResult == null)
            addressCheckResult = Transformations.map(backendConnection.getAddressCheckResult(), pair -> pair);
        return addressCheckResult;
    }
    public void validateAddress(String address) {
        backendConnection.validateAddress(address);
    }

    public LiveData<Boolean> getNewEventSaved() {
        if (newEventSaved == null)
            newEventSaved = Transformations.map(backendConnection.getNewEventSaved(), bool -> bool);
        return newEventSaved;
    }
    public void saveNewEvent(Event event) {
        backendConnection.saveNewEvent(event);
    }
}
