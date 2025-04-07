package com.example.detectapplication2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

public class OnboardingFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";
    private static final String ARG_IMAGE = "image";

    /**
     * Creates a new instance of the onboarding fragment with the provided content
     */
    public static OnboardingFragment newInstance(String title, String desc, int imageResId) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, desc);
        args.putInt(ARG_IMAGE, imageResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        TextView title = view.findViewById(R.id.tvTitle);
        TextView desc = view.findViewById(R.id.tvDesc);
        ImageView image = view.findViewById(R.id.imgOnboard);

        // Áp dụng font Montserrat từ code
        title.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold));
        desc.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.montserrat_regular));

        if (getArguments() != null) {
            title.setText(getArguments().getString(ARG_TITLE));
            desc.setText(getArguments().getString(ARG_DESC));
            image.setImageResource(getArguments().getInt(ARG_IMAGE));
        }

        return view;
    }
}

