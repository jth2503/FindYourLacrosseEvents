package de.joristhiele.findyourlacrosseevents;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.parse.FindCallback;
import com.parse.ParseException;
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

    public MyBackendConnection(RequestQueue queue, String apiKey) {
        this.queue = queue;
        this.MAPS_API_KEY = apiKey;
    }

    //region EventCategories
    private MutableLiveData<List<ParseObject>> allGenders;
    private MutableLiveData<List<ParseObject>> allDisciplines;
    private MutableLiveData<List<ParseObject>> allEventTypes;

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
    //endregion

    //region Map
    private MutableLiveData<List<Event>> allEvents;

    public LiveData<List<Event>> getAllEvents() {
        if (allEvents == null) {
            allEvents = new MutableLiveData<>();
            List<Event> result = new ArrayList<>();
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
            query.findInBackground((objects, e) -> {
                for (ParseObject object : objects) {
                    Event eventToAdd = new Event();
                    eventToAdd.setId(object.getObjectId());
                    eventToAdd.setName(object.getString("Name"));
                    eventToAdd.setAddress(object.getString("Address"));
                    eventToAdd.setLocation(object.getParseGeoPoint("Location"));
                    eventToAdd.setStartDate(object.getDate("StartDate").toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
                    eventToAdd.setEndDate(object.getDate("EndDate").toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
                    object.getParseObject("EventType").fetchIfNeededInBackground((eventType, e1) -> {
                        if (e1 == null)
                            eventToAdd.setEventType(eventType);
                    });
                    object.getRelation("Genders").getQuery().findInBackground((genders, e2) -> {
                        if (e2 == null)
                            eventToAdd.setGenders(genders);
                    });

                    object.getRelation("Disciplines").getQuery().findInBackground((disciplines, e3) -> {
                        if (e3 == null) {
                            eventToAdd.setDisciplines(disciplines);
                        }
                    });
                    result.add(eventToAdd);
                }
                allEvents.postValue(result);
            });
        }

        return allEvents;
    }
    //endregion

    //region EditEvent
    private boolean eventTypeLoaded = false, gendersLoaded = false, disciplinesLoaded = false;
    private boolean genderCallback = false, disciplineCallback = false;
    private final MutableLiveData<Event> eventToEdit = new MutableLiveData<>();

    public LiveData<Event> getEventToEdit() {
        return eventToEdit;
    }

    public void findEvent(String password) {
        if (password == null) {
            eventToEdit.setValue(null);
            return;
        }
        ParseQuery<ParseObject> query = new ParseQuery<>("Event");
        query.whereEqualTo("Password", password);
        query.getFirstInBackground((object, e) -> {
            if (e == null) {
                Event retrievedEvent = new Event();
                retrievedEvent.setId(object.getObjectId());
                retrievedEvent.setName(object.getString("Name"));
                retrievedEvent.setAddress(object.getString("Address"));
                retrievedEvent.setLocation(object.getParseGeoPoint("Location"));
                retrievedEvent.setStartDate(object.getDate("StartDate").toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
                retrievedEvent.setEndDate(object.getDate("EndDate").toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
                object.getParseObject("EventType").fetchIfNeededInBackground((eventType, e1) -> {
                    if (e1 == null) {
                        retrievedEvent.setEventType(eventType);
                        eventTypeLoaded = true;
                        synchronizeFindEventsCallbacks(retrievedEvent);
                    }
                });
                object.getRelation("Genders").getQuery().findInBackground((genders, e2) -> {
                    if (e2 == null) {
                        retrievedEvent.setGenders(genders);
                        gendersLoaded = true;
                        synchronizeFindEventsCallbacks(retrievedEvent);
                    }
                });
                object.getRelation("Disciplines").getQuery().findInBackground((disciplines, e3) -> {
                    if (e3 == null) {
                        retrievedEvent.setDisciplines(disciplines);
                        disciplinesLoaded = true;
                        synchronizeFindEventsCallbacks(retrievedEvent);
                    }
                });
            } else {
                eventToEdit.setValue(null);
            }
        });
    }

    private void synchronizeFindEventsCallbacks(Event event) {
        if (eventTypeLoaded && gendersLoaded && disciplinesLoaded) {
            eventToEdit.setValue(event);
            eventTypeLoaded = false;
            gendersLoaded = false;
            disciplinesLoaded = false;
        }
    }

    public void updateEvent(Event event) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
        query.getInBackground(event.getId(), (object, e) -> {
            if (e == null) {
                for (ParseObject o : event.getGenders())
                    Log.d("JORIS", "updateEvent: "+o.getObjectId());
                object.put("Name", event.getName());
                object.put("Address", event.getAddress());
                object.put("Location", event.getLocation());
                object.put("StartDate", Date.from(event.getStartDate().atStartOfDay(ZoneId.of("UTC")).toInstant()));       // use UTC to always have correct date
                object.put("EndDate", Date.from(event.getEndDate().atStartOfDay(ZoneId.of("UTC")).toInstant()));           // use UTC to always have correct date
                object.put("EventType", event.getEventType());
                ParseRelation<ParseObject> genderRelation = object.getRelation("Genders");
                ParseQuery<ParseObject> genderQuery = genderRelation.getQuery();
                genderQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> oldGenders, ParseException e) {
                        for (ParseObject oldGender : oldGenders)
                            genderRelation.remove(oldGender);
                        for (ParseObject gender : event.getGenders())
                            genderRelation.add(gender);
                        genderCallback = true;
                        synchronizeUpdateEventCallbacks(object);
                    }
                });
                ParseRelation<ParseObject> disciplineRelation = object.getRelation("Disciplines");
                ParseQuery<ParseObject> disciplineQuery = disciplineRelation.getQuery();
                disciplineQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> oldDisciplines, ParseException e) {
                        for (ParseObject oldDiscipline : oldDisciplines)
                            disciplineRelation.remove(oldDiscipline);
                        for (ParseObject discipline : event.getDisciplines())
                            disciplineRelation.add(discipline);
                        disciplineCallback = true;
                        synchronizeUpdateEventCallbacks(object);
                    }
                });
            }
        });
    }

    private void synchronizeUpdateEventCallbacks(ParseObject object) {
        if (genderCallback && disciplineCallback) {
            object.saveInBackground(e1 -> eventSaved.setValue(e1 == null));
            genderCallback = false;
            disciplineCallback = false;
        }
    }
    //endregion

    //region NewEvent
    private final MutableLiveData<Pair<String, ParseGeoPoint>> addressCheckResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> eventSaved = new MutableLiveData<>();

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

    public LiveData<Boolean> getEventSaved() {
        return eventSaved;
    }

    public void setEventSaved(boolean saved) {
        eventSaved.setValue(saved);
    }

    public void saveNewEvent(Event event, String password) {
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
        newEvent.put("Password", password);
        newEvent.saveInBackground(e -> eventSaved.setValue(e == null));
    }
    //endregion
}
