package com.example.parenteye;

import android.os.Bundle;
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

        // Init Firebase Auth and Database reference
        mAuth = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance().getReference("Users");

        // Setup Navigation Controller
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.navgraph);
            // Start Login screen
            navGraph.setStartDestination(R.id.loginFragment);
            navController.setGraph(navGraph);
        }
    }

    // Handle login using Firebase Auth
    public void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login success - check if user is parent or child
                            String uid = mAuth.getCurrentUser().getUid();
                            checkRoleAndNavigate(uid);
                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Register new user
    // If it's a child, we need to link them to a parent
    public void register(String email, String password, String phone, boolean isChild, String parentEmail) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();

                            if (isChild) {
                                // Find parent UID before saving
                                findParentAndSave(uid, email, phone, parentEmail);
                            } else {
                                // Parents don't need a parentUid
                                saveUserToDb(uid, email, phone, true, null);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Register failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Search for parent by email in DB
    public void findParentAndSave(String childUid, String email, String phone, String parentEmail) {
        mDbRef.orderByChild("email").equalTo(parentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Parent found, get their UID
                            DataSnapshot parentSnapshot = snapshot.getChildren().iterator().next();
                            String parentUid = parentSnapshot.getKey();

                            // Save child with the parent's UID
                            saveUserToDb(childUid, email, phone, false, parentUid);
                        } else {
                            Toast.makeText(MainActivity.this, "Parent email not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    // Save User object to Realtime Database
    public void saveUserToDb(String uid, String email, String phone, boolean isParent, String parentUid) {
        User user = new User(email, phone, uid, isParent, parentUid);
        mDbRef.child(uid).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                    // Go back to login after registration
                    navController.navigate(R.id.action_registerFragment_to_loginFragment);
                }
            }
        });
    }

    // Check user role (Parent vs Child) and update UI
    public void checkRoleAndNavigate(String uid) {
        mDbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.navgraph);

                        // Set start destination based on role
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