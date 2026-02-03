package com.example.parenteye;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
    }

    public void login() {

        String email = ((EditText) findViewById(R.id.Email)).getText().toString();
        String password = ((EditText) findViewById(R.id.Password)).getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this ,"Login OK",Toast.LENGTH_LONG).show();
                            NavHostFragment navfragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
                            if(isparent){
                               navfragment.getNavController().navigate(R.id.action_loginFragment_to_parentFragment);
                            }
                            else{
                                navfragment.getNavController().navigate(R.id.action_loginFragment_to_childFragment);
                            }

                        }


                        else {
                            Toast.makeText(MainActivity.this ,"Login failed",Toast.LENGTH_LONG).show();



                        }
                    }
                });


    }
}