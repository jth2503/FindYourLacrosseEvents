package de.joristhiele.findyourlacrosseevents;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.joristhiele.findyourlacrosseevents.data.Event;

public class MyBackendConnection {

    private final RequestQueue queue;
    private final String MAPS_API_KEY;

    private MutableLiveData<List<ParseObject>> allGenders;
    private MutableLiveData<List<ParseObject>> allDisciplines;
    private MutableLiveData<List<ParseObject>> allEventTypes;

    private final MutableLiveData<Pair<String, ParseGeoPoint>> addressCheckResult = new MutableLiveData<>();

    private final MutableLiveData<Boolean> newEventSaved = new MutableLiveData<>();

    public MyBackendConnection(RequestQueue queue, String apiKey) {
        this.queue = queue;
        this.MAPS_API_KEY = apiKey;
    }

    public LiveData<List<ParseObject>> getAllGenders() {
        // fetch data from server if none is available
        if (allGenders == null) {
            allGenders = new MutableLiveData<>();
            List<ParseObject> result = new ArrayList<>();
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Genders");
            query.findInBackground((objects, e) -> {
                if (e == null) {
                    result.addAll(objects);
                    allGenders.postValue(result);
                }
            });
        }

        return allGenders;
    }

    public LiveData<List<ParseObject>> getAllDisciplines() {
        // fetch data from server if none is available
        if (allDisciplines == null) {
            allDisciplines = new MutableLiveData<>();
            List<ParseObject> result = new ArrayList<>();
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Disciplines");
            query.findInBackground((objects, e) -> {
                if (e == null) {
                    result.addAll(objects);
                    allDisciplines.postValue(result);
                }
            });
        }

        return allDisciplines;
    }

    public LiveData<List<ParseObject>> getAllEventTypes() {
        // fetch data from server if none is available
        if (allEventTypes == null) {
            allEventTypes = new MutableLiveData<>();
            List<ParseObject> result = new ArrayList<>();
            ParseQuery<ParseObject> query = ParseQuery.getQuery("EventType");
            query.findInBackground((objects, e) -> {
                if (e == null) {
                    result.addAll(objects);
                    allEventTypes.postValue(result);
                }
            });
        }

        return allEventTypes;
    }

    public LiveData<Pair<String, ParseGeoPoint>> getAddressCheckResult() {
        return addressCheckResult;
    }
    // method to validate an address via Google Address Validation API, result is posted to LiveData
    public void validateAddress(String addressToCheck) {
        // build RequestBody: https://developers.google.com/maps/documentation/address-validation/reference/rest/v1/TopLevel/validateAddress#request-body
        JSONObject requestBody = new JSONObject(
                Map.of(
                        "address",
                        new JSONObject(
                                Map.of(
                                        "revision", 0,
                                        "addressLines", new JSONArray(List.of(addressToCheck))
                                )
                        )
                )
        );

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Constants.URL_ADDRESS_VALIDATION + MAPS_API_KEY,
                requestBody,
                response -> {
                    try {
                        String parsedAddress = response.getJSONObject("result").getJSONObject("address").getString("formattedAddress");
                        double latitude = response.getJSONObject("result").getJSONObject("geocode").getJSONObject("location").getDouble("latitude");
                        double longitude = response.getJSONObject("result").getJSONObject("geocode").getJSONObject("location").getDouble("longitude");
                        addressCheckResult.postValue(Pair.create(parsedAddress, new ParseGeoPoint(latitude, longitude)));
                    } catch (JSONException e) {
                        addressCheckResult.postValue(Pair.create(Constants.PARSE_ERROR + e.getMessage(), null));
                    }
                },
                error -> addressCheckResult.postValue(Pair.create(Constants.RESPONSE_ERROR + error.networkResponse.statusCode, null))
        );

        queue.add(request);
    }

    public LiveData<Boolean> getNewEventSaved() {
        return newEventSaved;
    }
    public void saveNewEvent(Event event) {
        ParseObject newEvent = new ParseObject("Event");
        newEvent.put("Name", event.getName());
        newEvent.put("Address", event.getAddress());
        newEvent.put("Location", event.getLocation());
        newEvent.put("StartDate", Date.from(event.getStartDate().atStartOfDay(ZoneId.of("UTC")).toInstant()));       // use UTC to always have correct date
        newEvent.put("EndDate", Date.from(event.getEndDate().atStartOfDay(ZoneId.of("UTC")).toInstant()));           // use UTC to always have correct date
        newEvent.put("EventType", event.getEventType());
        ParseRelation<ParseObject> genderRelation = newEvent.getRelation("Genders");
        for (ParseObject gender : event.getGenders())
            genderRelation.add(gender);
        ParseRelation<ParseObject> disciplineRelation = newEvent.getRelation("Disciplines");
        for (ParseObject discipline : event.getDisciplines())
            disciplineRelation.add(discipline);
        newEvent.saveInBackground(e -> newEventSaved.setValue(e == null));
    }
}
