package com.example.parenteye;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDbRef;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance().getReference("Users");

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.navgraph);

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                checkRoleAndNavigate(currentUser.getUid(), navGraph);
            } else {
                navGraph.setStartDestination(R.id.loginFragment);
                navController.setGraph(navGraph);
            }
        }
    }

    // פונקציית התחברות - מועתקת מהלוגיקה של החבר
    public void login(String email, String password, View view) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            checkRoleAndNavigate(uid, null); // ניווט לפי תפקיד
                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // פונקציית הרשמה ראשית
    public void register(String email, String password, String phone, boolean isChild, String parentEmail, View view) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();

                            if (isChild) {
                                // לוגיקה לילד - חיפוש הורה
                                findParentAndSave(uid, email, phone, parentEmail, view);
                            } else {
                                // לוגיקה להורה - שמירה ישירה
                                saveUserToDb(uid, email, phone, true, null, view);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Register failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void findParentAndSave(String childUid, String email, String phone, String parentEmail, View view) {
        mDbRef.orderByChild("email").equalTo(parentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot d : snapshot.getChildren()) {
                                String parentUid = d.getKey();
                                saveUserToDb(childUid, email, phone, false, parentUid, view);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Parent email not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    public void saveUserToDb(String uid, String email, String phone, boolean isParent, String parentUid, View view) {
        User user = new User(email, phone, uid, isParent, parentUid);
        mDbRef.child(uid).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                    navController.navigate(R.id.action_registerFragment_to_loginFragment);
                }
            }
        });
    }

    public void checkRoleAndNavigate(String uid, NavGraph graph) {
        mDbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        NavGraph navGraph = (graph != null) ? graph : navController.getNavInflater().inflate(R.navigation.navgraph);

                        if (user.isParent()) {
                            navGraph.setStartDestination(R.id.parentFragment);
                        } else {
                            navGraph.setStartDestination(R.id.childFragment);
                        }
                        navController.setGraph(navGraph);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}