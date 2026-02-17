package com.example.parenteye;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ParentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ParentFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private GoogleMap mMap;
    private Map<String, Marker> mMarkers = new HashMap<>();    // To know which sign belong to each child

    public ParentFragment() {
        // Required empty public constructor
    }

    public static ParentFragment newInstance(String param1, String param2) {
        ParentFragment fragment = new ParentFragment();
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
        return inflater.inflate(R.layout.fragment_parent, container, false);
    }

    // When the map is ready - calling to view Async
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);   // Call me back when map is ready and I call to OnMapReady

    }
    // Check permission and update sign on parent location
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        // For each matching child listen to changes
        usersRef.orderByChild("parentUid").equalTo(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                boolean hasChildren = false;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    User child = childSnapshot.getValue(User.class);
                    if (child == null) continue;
                    if (child.getLatitude() == 0 && child.getLongitude() == 0) continue;

                    LatLng loc = new LatLng(child.getLatitude(), child.getLongitude());
                    hasChildren = true;
                    builder.include(loc);
                        // If the child has color in map so update location and color , else, add a new one
                    if (mMarkers.containsKey(child.getUid())) {
                        Marker m = mMarkers.get(child.getUid());
                        m.setPosition(loc);
                    } else {
                        Marker m = mMap.addMarker(new MarkerOptions().position(loc).title(child.getEmail()));
                        mMarkers.put(child.getUid(), m);
                    }
                }
                if (hasChildren) {
                    try {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                    } catch (Exception e) {}
                }
            }

            // If have any problem from Firebase do nothing
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}