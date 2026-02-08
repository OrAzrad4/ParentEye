package com.example.parenteye;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

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

        // אתחול פיירבייס
        mAuth = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance().getReference("Users");

        // הגדרת ה-NavController (האחראי על הניווט)
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView); // וודא שזה ה-ID ב-XML שלך

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.navgraph); // וודא ששם הקובץ הוא navgraph

            // בדיקה האם המשתמש כבר מחובר
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // המשתמש מחובר - נבדוק מי הוא ונעביר אותו
                checkUserRoleAndNavigate(currentUser.getUid());
            } else {
                // המשתמש לא מחובר - הגדרת מסך הכניסה כהתחלה
                navGraph.setStartDestination(R.id.loginFragment);
                navController.setGraph(navGraph);
            }
        }
    }

    private void checkUserRoleAndNavigate(String uid) {
        mDbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
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
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
