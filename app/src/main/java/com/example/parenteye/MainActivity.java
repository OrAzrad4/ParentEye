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

// MainActivity hosts navigation and basic authentication logic
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDbRef;
    private NavController navController;

    // Called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get FirebaseAuth instance and reference to "Users" node in Realtime Database
        mAuth = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance().getReference("Users");

        // Find the NavHostFragment that will show our fragments
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        // If the NavHostFragment exists, set the start destination to the login screen
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.navgraph);
            navGraph.setStartDestination(R.id.loginFragment);
            navController.setGraph(navGraph);
        }
    }

    // Log in with Firebase using email and password
    public void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Called when Firebase finishes the login operation
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            checkRoleAndNavigate(uid);
                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Register a new user and decide if they are a parent or child
    public void register(String email, String password, String phone, boolean isChild, String parentEmail) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Called when Firebase finishes the registration operation
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();

                            if (isChild) {
                                findParentAndSave(uid, email, phone, parentEmail);
                            } else {
                                saveUserToDb(uid, email, phone, true, null);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Register failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Find parent user by email and then save the child with a link to that parent
    public void findParentAndSave(String childUid, String email, String phone, String parentEmail) {
        mDbRef.orderByChild("email").equalTo(parentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // If a parent with this email exists in the database
                        if (snapshot.exists()) {
                            DataSnapshot parentSnapshot = snapshot.getChildren().iterator().next();
                            String parentUid = parentSnapshot.getKey();
                            saveUserToDb(childUid, email, phone, false, parentUid);
                        } else {
                            Toast.makeText(MainActivity.this, "Parent email not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    // Save a User object under Users/uid in the Realtime Database
    public void saveUserToDb(String uid, String email, String phone, boolean isParent, String parentUid) {
        User user = new User(email, phone, uid, isParent, parentUid);
        mDbRef.child(uid).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Called when Firebase finishes writing the user to the database
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                    navController.navigate(R.id.action_registerFragment_to_loginFragment);
                }
            }
        });
    }

    // Read user from database, check role and navigate to parent or child fragment
    public void checkRoleAndNavigate(String uid) {
        mDbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.navgraph);

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