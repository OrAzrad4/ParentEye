package com.example.parenteye;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ChildFragment extends Fragment {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isSosActive = false;
    private Button btnSOS;
    private DatabaseReference myRef;

    public ChildFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child, container, false);

        btnSOS = view.findViewById(R.id.btnSOS);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // אתחול הרפרנס לפיירבייס מראש
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // הגדרת מה קורה כשמגיע מיקום חדש
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    // יש מיקום! נעדכן את פיירבייס
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    updateLocationInFirebase(lat, lng);
                }
            }
        };

        btnSOS.setOnClickListener(v -> toggleSosState());

        return view;
    }

    private void toggleSosState() {
        isSosActive = !isSosActive;

        if (isSosActive) {
            // מצב 1: הדלקת SOS

            // א. קודם כל ולפני הכל - מעדכנים את פיירבייס שהמצב דולק!
            updateSosStatus(true);

            // ב. משנים את העיצוב
            btnSOS.setText("בטל מצוקה");
            btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

            // ג. מתחילים את שידור המיקום
            startLocationUpdates();

        } else {
            // מצב 2: כיבוי SOS

            // א. מפסיקים את השידור
            stopLocationUpdates();

            // ב. מעדכנים את פיירבייס שהמצב כבוי
            updateSosStatus(false);

            // ג. מחזירים עיצוב
            btnSOS.setText("SOS");
            btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    private void updateSosStatus(boolean isActive) {
        // עדכון ישיר ומהיר רק של הסטטוס
        myRef.child("isSosActive").setValue(isActive)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בעדכון סטטוס", Toast.LENGTH_SHORT).show());
    }

    private void updateLocationInFirebase(double lat, double lng) {
        // עדכון המיקום (אנחנו שולחים גם את ה-SOS ליתר ביטחון)
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", lat);
        updates.put("longitude", lng);
        updates.put("isSosActive", true); // מוודאים שזה נשאר דולק בזמן השידור

        myRef.updateChildren(updates);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // בקשת מיקום אגרסיבית (כל 3 שניות) לדיוק מירבי
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Toast.makeText(getContext(), "התראת מצוקה הופעלה!", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Toast.makeText(getContext(), "מצב מצוקה בוטל", Toast.LENGTH_SHORT).show();
    }
}