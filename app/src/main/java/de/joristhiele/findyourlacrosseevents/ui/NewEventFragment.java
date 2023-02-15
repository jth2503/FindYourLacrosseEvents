package de.joristhiele.findyourlacrosseevents.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.parse.ParseObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import de.joristhiele.findyourlacrosseevents.Constants;
import de.joristhiele.findyourlacrosseevents.MyViewModel;
import de.joristhiele.findyourlacrosseevents.R;
import de.joristhiele.findyourlacrosseevents.Util;

public class NewEventFragment extends Fragment {

    private MyViewModel viewModel;
    private boolean addressValidated = false;

    TextInputLayout tilAddressCheck;
    TextInputEditText etName, etAddress, etAddressCheck, etStartDate, etEndDate;
    ChipGroup cgGender, cgDiscipline, cgEventType;
    ExtendedFloatingActionButton fabCreate;

    public NewEventFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);

        // instantiate all the UI components
        tilAddressCheck = view.findViewById(R.id.new_event_address_check_til);
        etName = view.findViewById(R.id.new_event_name_et);
        etAddress = view.findViewById(R.id.new_event_address_et);
        etAddressCheck = view.findViewById(R.id.new_event_address_check_et);
        etStartDate = view.findViewById(R.id.new_event_dates_start_et);
        etEndDate = view.findViewById(R.id.new_event_dates_end_et);
        cgGender = view.findViewById(R.id.new_event_genders);
        cgDiscipline = view.findViewById(R.id.new_event_discipline);
        cgEventType = view.findViewById(R.id.new_event_event_type);
        fabCreate = view.findViewById(R.id.create_event_fab);

        // setup the datepicker functionality
        setupDatePickers(etStartDate, R.string.new_event_start_dialog, viewModel.getNewEvent().getStartDate());
        setupDatePickers(etEndDate, R.string.new_event_end_dialog, viewModel.getNewEvent().getEndDate());

        // setup the chip groups with all available event categories from server
        setupChipGroupWithData(cgGender, viewModel.getAllGenders(), "GenderName", viewModel.getNewEvent().getGenders());
        setupChipGroupWithData(cgDiscipline, viewModel.getAllDisciplines(), "DisciplineName", viewModel.getNewEvent().getDisciplines());
        setupChipGroupWithData(cgEventType, viewModel.getAllEventTypes(), "EventTypeName", null);

        // fill the views with stored viewmodel data
        etName.setText(viewModel.getNewEvent().getName());
        etAddress.setText(viewModel.getNewEvent().getAddress());
        if (viewModel.getNewEvent().getStartDate() != null)
            etStartDate.setText(viewModel.getNewEvent().getStartDate().format(DateTimeFormatter.ISO_DATE));
        if (viewModel.getNewEvent().getEndDate() != null)
            etEndDate.setText(viewModel.getNewEvent().getEndDate().format(DateTimeFormatter.ISO_DATE));

        // setup TextChangedListeners for all EditTexts to automatically update ViewModel
        setupEditTextTextWatchers();

        // listener to validate the entered address and send the data to the server
        fabCreate.setOnClickListener(v -> {
            if (addressValidated) {
                viewModel.saveNewEvent(viewModel.getNewEvent());
            } else if (
                    ! Util.StringIsNullOrEmpty(etName.getText())
                    && ! Util.StringIsNullOrEmpty(etAddress.getText())
                    && ! Util.StringIsNullOrEmpty(etStartDate.getText())
                    && ! Util.StringIsNullOrEmpty(etEndDate.getText())
                    && ! cgGender.getCheckedChipIds().isEmpty()
                    && ! cgDiscipline.getCheckedChipIds().isEmpty()
                    && cgEventType.getCheckedChipId() != View.NO_ID
            ){
                viewModel.validateAddress(viewModel.getNewEvent().getAddress());
            } else {
                Toast.makeText(getContext(), R.string.new_event_values_required, Toast.LENGTH_SHORT).show();
            }
        });

        // define reaction to address validation
        viewModel.getAddressCheckResult().observe(getViewLifecycleOwner(), stringParseGeoPointPair -> {
            if (stringParseGeoPointPair.first.startsWith(Constants.PARSE_ERROR)         // display error
                    || stringParseGeoPointPair.first.startsWith(Constants.RESPONSE_ERROR)) {
                Toast.makeText(getContext(), stringParseGeoPointPair.first, Toast.LENGTH_LONG).show();
            } else {
                tilAddressCheck.setVisibility(View.VISIBLE);                                // show confirmation text field
                tilAddressCheck.setEndIconOnClickListener(v -> {                            // confirmation click listener:
                    etAddress.setText(stringParseGeoPointPair.first);                           // enter confirmed address into address field
                    viewModel.getNewEvent().setLocation(stringParseGeoPointPair.second);        // set the location accordingly
                    tilAddressCheck.setVisibility(View.GONE);                                   // make confirmation field disappear
                    addressValidated = true;                                                    // address got validated
                    fabCreate.setText(R.string.new_event_create);                               // change the text on the action button
                });
                etAddressCheck.setText(stringParseGeoPointPair.first);                      // display API response address
                etAddress.requestFocus();                                                   // focus to the address field
            }
        });

        // define reaction to saving of new event
        viewModel.getNewEventSaved().observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                Toast.makeText(getContext(), R.string.new_event_save_successful, Toast.LENGTH_LONG).show();
                etName.setText(Constants.EMPTY_STRING);
                etAddress.setText(Constants.EMPTY_STRING);
                etStartDate.setText(Constants.EMPTY_STRING);
                etEndDate.setText(Constants.EMPTY_STRING);
                cgGender.clearCheck();
                cgDiscipline.clearCheck();
                cgEventType.clearCheck();
                viewModel.clearNewEventData();
            } else {
                Toast.makeText(getContext(), R.string.new_event_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePickers(TextInputEditText editText, int titleResource, LocalDate selectedDateSource) {
        // disable keyboard
        editText.setShowSoftInputOnFocus(false);
        // show datepicker when editText receives focus
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText(titleResource)
                        .setSelection(selectedDateSource != null
                                ? selectedDateSource.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()              // use UTC to always have correct date
                                : LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()                 // use UTC to always have correct date
                        ).build();
                datePicker.addOnPositiveButtonClickListener(selection -> {
                    editText.setText(Instant.ofEpochMilli(selection).atZone(ZoneId.of("UTC")).toLocalDate().toString());    // use UTC to always have correct date
                    v.clearFocus();
                });
                datePicker.addOnCancelListener(_ignored -> v.clearFocus());
                datePicker.addOnDismissListener(_ignored -> v.clearFocus());
                datePicker.show(getParentFragmentManager(), Constants.EMPTY_STRING);
            }
        });
    }

    private void setupChipGroupWithData(ChipGroup chipGroup, LiveData<List<ParseObject>> dataList, String columnName, List<ParseObject> selectedCategoriesSource) {
        dataList.observe(getViewLifecycleOwner(), itemList -> {
            // create chip for each list item
            for (ParseObject item : itemList) {
                // inflate layout
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_category, chipGroup, false);
                chip.setText(item.getString(columnName));

                // check the chip depending on ViewModel
                if (selectedCategoriesSource == null && viewModel.getNewEvent().getEventType() != null) {   // there is no list to update, so the ChipGroup has to be for EventType
                    chip.setChecked(viewModel.getNewEvent().getEventType().getObjectId().equals(item.getObjectId()));
                } else if (selectedCategoriesSource != null) {                                              // check if the list contains any item with the same id as the Chip
                    chip.setChecked(selectedCategoriesSource.stream().anyMatch(category -> category.getObjectId().equals(item.getObjectId())));
                }

                // update ViewModel when Chips are un/checked
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (selectedCategoriesSource == null) {
                        viewModel.getNewEvent().setEventType(isChecked ? item : null);
                    } else {
                        if (isChecked) {
                            selectedCategoriesSource.add(item);
                        } else {
                            selectedCategoriesSource.removeIf(category -> category.getObjectId().equals(item.getObjectId()));
                        }
                    }
                });
                chipGroup.addView(chip);
            }
        });
    }

    private void setupEditTextTextWatchers() {
        etName.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.getNewEvent().setName(s.toString());
            }
        });
        etAddress.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // address is no longer validated after edit
                if (addressValidated) {
                    addressValidated = false;
                    fabCreate.setText(R.string.new_event_validate_address);
                }
                viewModel.getNewEvent().setAddress(s.toString());
            }
        });
        etStartDate.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Util.StringIsNullOrEmpty(s))
                    viewModel.getNewEvent().setStartDate(LocalDate.parse(s.toString(), DateTimeFormatter.ISO_DATE));
            }
        });
        etEndDate.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Util.StringIsNullOrEmpty(s))
                    viewModel.getNewEvent().setEndDate(LocalDate.parse(s.toString(), DateTimeFormatter.ISO_DATE));
            }
        });
    }
}