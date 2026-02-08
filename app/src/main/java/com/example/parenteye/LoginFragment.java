package com.example.parenteye;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDbRef;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // אתחול פיירבייס
        mAuth = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance().getReference("Users");

        // קישור לרכיבי המסך (וודא שה-IDs תואמים ל-XML שלך)
        etEmail = view.findViewById(R.id.Email);      // או etEmailLogin
        etPassword = view.findViewById(R.id.Password); // או etPasswordLogin
        Button btnLogin = view.findViewById(R.id.Login); // או btnLogin
        Button btnRegister = view.findViewById(R.id.Register); // כפתור למעבר להרשמה

        // לוגיקת כפתור התחברות
        btnLogin.setOnClickListener(v -> loginUser(view));

        // מעבר למסך הרשמה (אם יש כפתור כזה)
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment)
            );
        }

        return view;
    }

    private void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // בדיקת תקינות קלט
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("נא להזין אימייל");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("נא להזין סיסמה");
            return;
        }

        // 1. התחברות מול שרת האימות (Authentication)
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // ההתחברות הצליחה, עכשיו בודקים מי המשתמש
                    String uid = authResult.getUser().getUid();
                    checkUserRoleAndNavigate(uid, view);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "שגיאה בהתחברות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // פונקציה שבודקת האם זה הורה או ילד ומנווטת בהתאם
    private void checkUserRoleAndNavigate(String uid, View view) {
        mDbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // המרת הנתונים למודל User שיצרנו
                    User user = snapshot.getValue(User.class);

                    if (user != null) {
                        NavController navController = Navigation.findNavController(view);

                        if (user.isParent()) {
                            // משתמש הוא הורה -> למסך המפה
                            navController.navigate(R.id.action_loginFragment_to_parentFragment);
                        } else {
                            // משתמש הוא ילד -> למסך ה-SOS
                            navController.navigate(R.id.action_loginFragment_to_childFragment);
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "משתמש לא נמצא במסד הנתונים", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "שגיאה בקריאת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
