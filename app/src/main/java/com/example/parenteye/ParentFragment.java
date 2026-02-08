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

public class ParentFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final Map<String, Marker> mMarkers = new HashMap<>();

    public ParentFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // וודא ש-fragment_parent.xml מכיל את ה-FragmentContainerView של המפה כמו שסיכמנו
        return inflater.inflate(R.layout.fragment_parent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 1. הצגת המיקום של ההורה (הנקודה הכחולה)
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // 2. האזנה לילדים
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.orderByChild("parentUid").equalTo(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                boolean hasChildren = false;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    User child = childSnapshot.getValue(User.class);
                    if (child == null || (child.getLatitude() == 0 && child.getLongitude() == 0)) continue;

                    LatLng location = new LatLng(child.getLatitude(), child.getLongitude());
                    hasChildren = true;
                    builder.include(location);

                    if (mMarkers.containsKey(child.getUid())) {
                        Marker marker = mMarkers.get(child.getUid());
                        marker.setPosition(location);
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(
                                child.isSosActive() ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN));
                    } else {
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(child.getEmail())
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        child.isSosActive() ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN)));
                        // הערה: כאן הנחתי ש-getId() מחזיר את ה-UID, אם לא, השתמש ב-childSnapshot.getKey()
                        mMarkers.put(childSnapshot.getKey(), marker);
                    }
                }

                // זום אוטומטי
                if (hasChildren) {
                    try { mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100)); } catch (Exception e) {}
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}