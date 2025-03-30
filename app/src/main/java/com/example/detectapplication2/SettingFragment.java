package com.example.detectapplication2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.Locale;

public class SettingFragment extends Fragment {
    TextView txt_logout;
    TextView tv_edit_profile, tv_policies;
    private SharedPreferences sharedPreferences;
    private RadioGroup languageRadioGroup;
    private RadioButton radioEnglish, radioVietnamese;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "language";

    private String mParam1;
    private String mParam2;

    public SettingFragment() {
        // Required empty public constructor
    }

    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
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
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        txt_logout = view.findViewById(R.id.txt_logout);
        tv_edit_profile = view.findViewById(R.id.tv_edit_profile);
        tv_policies = view.findViewById(R.id.tv_policies);
        
        // Initialize language selection UI
        languageRadioGroup = view.findViewById(R.id.language_radio_group);
        radioEnglish = view.findViewById(R.id.radio_english);
        radioVietnamese = view.findViewById(R.id.radio_vietnamese);

        // Load saved language preference
        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, getActivity().MODE_PRIVATE);
            String savedLanguage = sharedPreferences.getString(LANGUAGE_KEY, "en");
            
            // Set the correct radio button based on saved preference
            if (savedLanguage.equals("vi")) {
                radioVietnamese.setChecked(true);
            } else {
                radioEnglish.setChecked(true);
            }
        }

        // Handle language change
        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String languageCode;
            if (checkedId == R.id.radio_vietnamese) {
                languageCode = "vi";
            } else {
                languageCode = "en";
            }
            
            // Save language preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(LANGUAGE_KEY, languageCode);
            editor.apply();
            
            // Change app language
            setAppLanguage(languageCode);
        });

        txt_logout.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Khởi tạo SharedPreferences
                sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", getActivity().MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Xóa dữ liệu đã lưu
                editor.clear();
                editor.apply();

                // Chuyển về màn hình đăng nhập
                Intent myintent = new Intent(getActivity(), MainActivity.class);
                startActivity(myintent);

                // Kết thúc activity hiện tại
                getActivity().finish();

                // Thông báo thành công
                Toast.makeText(getContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
            }
        });

        tv_edit_profile.setOnClickListener(v -> {
            Intent myintent = new Intent(getActivity(), Profile.class);
            startActivity(myintent);
        });

        tv_policies.setOnClickListener(v -> {
            Intent myintent = new Intent(getActivity(), Policies.class);
            startActivity(myintent);
        });

        return view;
    }

    private void setAppLanguage(String languageCode) {
        if (getActivity() == null) return;
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = getActivity().getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        
        config.setLocale(locale);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            getActivity().createConfigurationContext(config);
        }
        
        resources.updateConfiguration(config, metrics);
        
        // Restart activity to apply language change
        Intent refresh = new Intent(getActivity(), MainActivity2.class);
        refresh.putExtra("fragment", "setting");
        getActivity().finish();
        startActivity(refresh);
    }
}
