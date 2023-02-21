package de.joristhiele.findyourlacrosseevents.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import de.joristhiele.findyourlacrosseevents.MyViewModel;
import de.joristhiele.findyourlacrosseevents.R;
import de.joristhiele.findyourlacrosseevents.Util;
import de.joristhiele.findyourlacrosseevents.data.EditEventState;
import de.joristhiele.findyourlacrosseevents.data.Event;

public class AddOrEditFragment extends Fragment {

    private MyViewModel viewModel;

    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private Button buttonEdit;

    public AddOrEditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_or_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);

        // initialize views
        tilPassword= view.findViewById(R.id.password_edit_til);
        etPassword = view.findViewById(R.id.password_edit_et);
        buttonEdit = view.findViewById(R.id.edit_button);

        // listeners for initial two buttons
        view.findViewById(R.id.add_button).setOnClickListener(v -> ((MainNavigationActivity) getActivity()).setWorkingOnEvent(EditEventState.NEW_EVENT));
        view.findViewById(R.id.show_password_button).setOnClickListener(v -> {
            v.setVisibility(View.GONE);
            view.findViewById(R.id.password_container).setVisibility(View.VISIBLE);
        });

        // behavior of password views
        etPassword.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (Util.StringIsNullOrEmpty(s)) {
                    tilPassword.setError(getString(R.string.add_edit_password_empty));
                    buttonEdit.setEnabled(false);
                } else {
                    tilPassword.setError(null);
                    buttonEdit.setEnabled(true);
                }
            }
        });
        buttonEdit.setOnClickListener(v -> {
            // start observing livedata in listener to avoid infinite loop

            viewModel.findEvent(etPassword.getText().toString());
        });

        viewModel.getEventToEditFromBackend().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                ((MainNavigationActivity) requireActivity()).setWorkingOnEvent(EditEventState.EDIT_EVENT);
            }
        });
    }
}