package com.example.uberapp_tim9.driver.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.uberapp_tim9.R;
import com.example.uberapp_tim9.model.Passenger;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DriverAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DriverAccountFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public DriverAccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DriverAccountFragment newInstance(String param1, String param2) {
        DriverAccountFragment fragment = new DriverAccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_driver_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Passenger passenger = new Passenger(0, "Ivana", "Ivanovicka", "", "0693339999", "emaill@mail.com", "Resavska 23", "123456789", false);
        ((EditText)view.findViewById(R.id.name_edit_text)).setText(String.format("%s %s", passenger.getmName(), passenger.getmSurname()));
        ((EditText)view.findViewById(R.id.phone_number_edit_text)).setText(passenger.getmPhoneNumber());
        ((EditText)view.findViewById(R.id.email_edit_text)).setText(passenger.getmEmail());
        ((EditText)view.findViewById(R.id.address_edit_text)).setText(passenger.getmAddress());
        ((ImageView)view.findViewById(R.id.profile_picture_image_view)).setImageResource(R.drawable.ic_simic);
        ((CheckBox)view.findViewById(R.id.blocked_checkbox)).setChecked(passenger.ismIsBlocked());
    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState) {
        LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);
        Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Theme_UberApp_Tim9_Basic);
        return inflater.cloneInContext(contextThemeWrapper);
    }
}