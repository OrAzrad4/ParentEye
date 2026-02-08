package com.example.parenteye;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDbRef;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance().getReference("Users");

        EditText etEmail = view.findViewById(R.id.EmailReg); // וודא שזה ה-ID ב-XML
        EditText etPassword = view.findViewById(R.id.PasswordReg); // וודא שזה ה-ID ב-XML
        EditText etPhone = view.findViewById(R.id.PhoneReg); // אם יש לך שדה טלפון

        // רכיבים חדשים שצריך להוסיף ל-XML שלך:
        CheckBox cbIsChild = view.findViewById(R.id.cbIsChild);
        EditText etParentEmail = view.findViewById(R.id.ParentEmail);
        Button btnRegister = view.findViewById(R.id.RegisterReg);

        // הסתרה/הצגה של שדה אימייל הורה
        etParentEmail.setVisibility(View.GONE);
        cbIsChild.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etParentEmail.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String phone = etPhone != null ? etPhone.getText().toString() : "";
            boolean isChild = cbIsChild.isChecked();
            String parentEmailInput = etParentEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isChild && TextUtils.isEmpty(parentEmailInput)) {
                Toast.makeText(getContext(), "ילד חייב להזין אימייל של הורה", Toast.LENGTH_SHORT).show();
                return;
            }

            // יצירת משתמש ב-Auth
            mAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();

                if (isChild) {
                    // חיפוש ההורה לפי אימייל
                    mDbRef.orderByChild("email").equalTo(parentEmailInput)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot parentSnapshot : snapshot.getChildren()) {
                                            String parentUid = parentSnapshot.getKey();

                                            // יצירת הילד ושמירת הקישור להורה
                                            User newUser = new User(email, phone, uid, false, parentUid);
                                            mDbRef.child(uid).setValue(newUser);

                                            // סיום
                                            Toast.makeText(getContext(), "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                            Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_loginFragment);
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "לא נמצא הורה עם האימייל הזה", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) { }
                            });
                } else {
                    // רישום הורה רגיל
                    User newUser = new User(email, phone, uid, true, null);
                    mDbRef.child(uid).setValue(newUser);
                    Toast.makeText(getContext(), "הורה נרשם בהצלחה!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_loginFragment);
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "שגיאה ברישום: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        return view;
    }
}