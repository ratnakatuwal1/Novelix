package com.example.novelix.Fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.novelix.R;
import com.example.novelix.WelcomeActivity;

public class WelcomeFragment extends Fragment {
    public static final String ARG_LAYOUT_ID = "layout_id";

    public WelcomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getArguments().getInt(ARG_LAYOUT_ID), container, false);

        // Handle the Get Started button in the last fragment
        if (getArguments().getInt(ARG_LAYOUT_ID) == R.layout.fragment_welcome4) {
            Button btnGetStarted = view.findViewById(R.id.buttonGetStarted);
            btnGetStarted.setOnClickListener(v -> {
                if (getActivity() instanceof WelcomeActivity) {
                    ((WelcomeActivity) getActivity()).startMainActivity();
                }
            });
        }
        return view;
    }
}