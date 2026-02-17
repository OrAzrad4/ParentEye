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
import androidx.navigation.Navigation;

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

    // Default params
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // Location vars
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Logic vars
    private boolean isSosActive = false;
    private Button btnSOS;
    private DatabaseReference myRef;

    public ChildFragment() {
        // Required empty public constructor
    }

    public static ChildFragment newInstance(String param1, String param2) {
        ChildFragment fragment = new ChildFragment();
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
        View view = inflater.inflate(R.layout.fragment_child, container, false);

        // Init UI and Location client
        btnSOS = view.findViewById(R.id.btnSOS);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Setup Firebase reference to the current user's node
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // Define what happens when we get a new GPS location
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    // Get coordinates and push to Firebase
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    updateLocation(lat, lng);
                }
            }
        };

        // Handle SOS button
        btnSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSos();
            }
        });

        // Handle Logout
        android.widget.ImageButton btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                // Go back to login screen
                Navigation.findNavController(view).navigate(R.id.action_childFragment_to_loginFragment);
            }
        });

        return view;
    }

    private void toggleSos() {
        //  Check if we have permissions for SMS and Location
        if (androidx.core.app.ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                androidx.core.app.ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // If permissions are missing, ask the user for them
            requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, 101);

            // Stop here! Do not turn on SOS yet. The user needs to approve first.
            return;
        }

        //  If we are here, we have permissions. Now we can toggle the status.
        isSosActive = !isSosActive;

        if (isSosActive) {
            // --- SOS is ON ---

            // Update Firebase
            myRef.child("isSosActive").setValue(true);

            // Update Button UI (Change text to "Stop")
            btnSOS.setText("Stop SOS");
            btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

            // Start actions
            startLocation(); // Start GPS
            sendSms();       // Send the SMS

        } else {
            // --- SOS is OFF ---

            // Stop GPS to save battery
            stopLocation();

            // Update Firebase
            myRef.child("isSosActive").setValue(false);

            // Update Button UI (Change text back to "SOS")
            btnSOS.setText("SOS");
            btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    // Helper to save location data to Firebase
    private void updateLocation(double lat, double lng) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", lat);
        updates.put("longitude", lng);
        updates.put("isSosActive", true); // Keep status active while tracking
        myRef.updateChildren(updates);
    }

    // Start requesting GPS updates
    private void startLocation() {
        // First check if we have permission
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Setup request: High accuracy, update frequently
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 90000).build();
        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        Toast.makeText(getContext(), "SOS Started", Toast.LENGTH_SHORT).show();
    }

    // Stop GPS updates to save battery
    private void stopLocation() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Toast.makeText(getContext(), "SOS Stopped", Toast.LENGTH_SHORT).show();
    }

    // Cleanup when leaving the fragment
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Send SMS
    private static final String EMERGENCY_PHONE_NUMBER = "0526875135"; // Police or Ambulance

    private void sendSms() {
        // Check permission
        if (androidx.core.app.ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                smsManager.sendTextMessage(EMERGENCY_PHONE_NUMBER, null, "The child is in an emergency, he presses the SOS button.", null, null);
                android.widget.Toast.makeText(getContext(), "SMS sent", android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Ask for permission
            requestPermissions(new String[]{android.Manifest.permission.SEND_SMS}, 101);
        }
    }
}