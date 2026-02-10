package com.example.parenteye;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public RegisterFragment() {
        // Required empty public constructor
    }

    public static RegisterFragment newInstance(String param1, String param2) {
        RegisterFragment fragment = new RegisterFragment();
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
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        EditText etEmail = view.findViewById(R.id.EmailReg);
        EditText etPassword = view.findViewById(R.id.PasswordReg);
        EditText etPhone = view.findViewById(R.id.PhoneReg);
        CheckBox cbIsChild = view.findViewById(R.id.cbIsChild);
        EditText etParentEmail = view.findViewById(R.id.ParentEmail);
        Button btnRegister = view.findViewById(R.id.RegisterReg);

        etParentEmail.setVisibility(View.GONE);

        cbIsChild.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etParentEmail.setVisibility(View.VISIBLE);
                } else {
                    etParentEmail.setVisibility(View.GONE);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String phone = etPhone.getText().toString();
                boolean isChild = cbIsChild.isChecked();
                String parentEmailInput = etParentEmail.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(getContext(), "Empty fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isChild && TextUtils.isEmpty(parentEmailInput)) {
                    Toast.makeText(getContext(), "Enter parent email", Toast.LENGTH_SHORT).show();
                    return;
                }

                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.register(email, password, phone, isChild, parentEmailInput);
                }
            }
        });

        return view;
    }
}