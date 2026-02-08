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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChildFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChildFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
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

        btnSOS = view.findViewById(R.id.btnSOS);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    updateLocation(lat, lng);
                }
            }
        };

        btnSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSos();
            }
        });
        android.widget.ImageButton btnLogout = view.findViewById(R.id.btnLogout);

        // מגדירים מה קורה כשלוחצים עליו
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. התנתקות מפיירבייס
                FirebaseAuth.getInstance().signOut();

                // 2. מעבר למסך הכניסה
                // וודא ש-loginFragment זה השם הנכון ב-navgraph.xml שלך
                // אם יש לך action מסודר, תחליף ל-R.id.action_child_to_login
                Navigation.findNavController(view).navigate(R.id.action_childFragment_to_loginFragment);
            }
        });

        return view;
    }

    private void toggleSos() {
        isSosActive = !isSosActive;
        if (isSosActive) {
            myRef.child("isSosActive").setValue(true);
            btnSOS.setText("Stop SOS");
            btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            startLocation();
        } else {
            stopLocation();
            myRef.child("isSosActive").setValue(false);
            btnSOS.setText("SOS");
            btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void updateLocation(double lat, double lng) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", lat);
        updates.put("longitude", lng);
        updates.put("isSosActive", true);
        myRef.updateChildren(updates);
    }

    private void startLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 90000).build();
        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        Toast.makeText(getContext(), "SOS Started", Toast.LENGTH_SHORT).show();
    }

    private void stopLocation() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Toast.makeText(getContext(), "SOS Stopped", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // עצירת ה-GPS כשהמסך נסגר כדי למנוע קריסה
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}