package com.example.maskinfo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maskinfo.model.Store;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MainViewModel viewModel;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                List<String> deniedList = new ArrayList<>();
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    if (!entry.getValue()) {
                        deniedList.add(entry.getKey());
                    }
                }

                if (!deniedList.isEmpty()) {
                    for (String permission : deniedList) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                            AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
                            localBuilder.setTitle("권한 설정")
                                    .setMessage("권한을 거부할 경우 본 서비스를 이용하실 수 없습니다.\n\n[설정] > [권한]에서 권한을 켜주세요.")
                                    .setPositiveButton("설정", (dialogInterface, i) -> {
                                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); // 설정 화면
                                    })
                                    .setNegativeButton("닫기", (dialogInterface, i) -> {
                                    })
                                    .create()
                                    .show();
                        }
                    }
                    return;
                }

                // 모든 권한 승인 (All request are permitted)
                performAction();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkPermissions();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void checkPermissions() {
        requestPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        });
    }

    @SuppressLint("MissingPermission")
    private void performAction() {
        // 추가
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
            localBuilder.setTitle("위치 정보 설정") // ("위치 서비스 설정")
                    .setMessage("위치 서비스 설정이 꺼져 있어 현재 위치를 확인할 수 없습니다. 설정을 변경하시겠습니까?")
                    .setPositiveButton("설정", (dialogInterface, i) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); // 위치정보 켜기 화면
                    })
                    .setNegativeButton("닫기", (dialogInterface, i) -> {
                    })
                    .create()
                    .show();

            return;
        }

        // 위치 정보 가져오기
        fusedLocationClient.getLastLocation()
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "performAction: ", e.getCause());
                })
                .addOnSuccessListener(this, location -> {
                    Log.d(TAG, "performAction: " + location);
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        Log.d(TAG, "getLatitude: " + location.getLatitude());
                        Log.d(TAG, "getLongitude: " + location.getLongitude());

                        //location.setLatitude(37.188078);
                        //location.setLongitude(127.043002);
                        viewModel.location = location;
                        viewModel.fetchStoreInfo();
                    } else {
                        LocationRequest locationRequest = LocationRequest.create();
                        //locationRequest.setInterval(10000);
                        //locationRequest.setFastestInterval(5000);

                        locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                if (locationResult == null) {
                                    return;
                                }
                                for (Location location : locationResult.getLocations()) {
                                    viewModel.location = location;
                                    viewModel.fetchStoreInfo();
                                }
                            }
                        };

                        // 위치 업데이트 요청
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                    }
                });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        final StoreAdapter adapter = new StoreAdapter();
        recyclerView.setAdapter(adapter);

        // UI 변경 감지 업데이트
        viewModel.itemLiveData.observe(this, stores -> {
            adapter.updateItems(stores);
            getSupportActionBar().setTitle("마스크 재고 있는 곳: " + stores.size());
        });


        viewModel.loadingLiveData.observe(this, isLoading -> {
            if (isLoading) {
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                checkPermissions();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private List<Store> mItems = new ArrayList<>();

    // 아이템 뷰 정보를 가지고 있는 클래스
    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView addressTextView;
        TextView distanceTextView;
        TextView remainTextView;
        TextView countTextView;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_text_view);
            addressTextView = itemView.findViewById(R.id.addr_text_view);
            distanceTextView = itemView.findViewById(R.id.distance_text_view);
            remainTextView = itemView.findViewById(R.id.remain_text_view);
            countTextView = itemView.findViewById(R.id.count_text_view);
        }
    }

    public void updateItems(List<Store> items) {
        mItems = items;
        notifyDataSetChanged();         // UI 갱신
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = mItems.get(position);

        holder.nameTextView.setText(store.getName());
        holder.addressTextView.setText(store.getAddr());
        holder.distanceTextView.setText(String.format("%.2fkm", store.getDistance()));

        String remainStat = "충분";
        String count = "100개 이상";
        int color = Color.GREEN;
        switch (store.getRemainStat()) {
            case "plenty":
                remainStat = "충분";
                count = "100개 이상";
                color = Color.GREEN;
                break;
            case "some":
                remainStat = "여유";
                count = "30개 이상";
                color = Color.YELLOW;
                break;
            case "few":
                remainStat = "매진 임박";
                count = "2개 이상";
                color = Color.RED;
                break;
            case "empty":
                remainStat = "재고 없음";
                count = "1개 이하";
                color = Color.GRAY;
                break;
            default:
        }

        holder.remainTextView.setText(remainStat);
        holder.countTextView.setText(count);

        holder.remainTextView.setTextColor(color);
        holder.countTextView.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

}