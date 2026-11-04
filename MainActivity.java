package com.example.myapplicationdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatActive;
    private TextView tvStatReady;
    private TextView tvStatTotal;
    private RecyclerView rvOrders;
    private TextView tvEmptyOrders;
    private ChipGroup chipGroupFilter;
    private ExtendedFloatingActionButton fabAddOrder;

    private OrderAdapter adapter;
    private final List<LaundryOrder> currentOrdersList = new ArrayList<>();
    private String currentFilter = "All";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification permission denied. Updates will only show in app.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        LaundryNotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();

        initViews();
        setupRecyclerView();
        setupFilters();
        setupListeners();

        loadData();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initViews() {
        tvStatActive = findViewById(R.id.tvStatActive);
        tvStatReady = findViewById(R.id.tvStatReady);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        rvOrders = findViewById(R.id.rvOrders);
        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        fabAddOrder = findViewById(R.id.fabAddOrder);
    }

    private void setupRecyclerView() {
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, currentOrdersList, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onAdvanceStage(LaundryOrder order) {
                OrderStatus nextStatus = LaundryRepository.getInstance().advanceOrderStatus(order.getOrderId());
                if (nextStatus != null) {
                    LaundryNotificationHelper.sendStatusNotification(MainActivity.this, order);
                    Toast.makeText(MainActivity.this, "Order #" + order.getOrderId() + " -> " + nextStatus.getDisplayName(), Toast.LENGTH_SHORT).show();
                    loadData();
                }
            }

            @Override
            public void onViewDetails(LaundryOrder order) {
                OrderDetailDialog.show(MainActivity.this, order, () -> loadData());
            }
        });
        rvOrders.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipActive)) {
                currentFilter = "Active";
            } else if (checkedIds.contains(R.id.chipReady)) {
                currentFilter = "Ready";
            } else if (checkedIds.contains(R.id.chipDelivered)) {
                currentFilter = "Delivered";
            } else {
                currentFilter = "All";
            }
            loadData();
        });
    }

    private void setupListeners() {
        fabAddOrder.setOnClickListener(v -> {
            NewOrderDialog.show(MainActivity.this, newOrder -> {
                loadData();
            });
        });
    }

    private void loadData() {
        LaundryRepository repository = LaundryRepository.getInstance();

        // Update Stats
        tvStatActive.setText(String.valueOf(repository.getActiveCount()));
        tvStatReady.setText(String.valueOf(repository.getReadyCount()));
        tvStatTotal.setText(String.valueOf(repository.getOrders().size()));

        // Update List
        List<LaundryOrder> filtered = repository.getOrdersByFilter(currentFilter);
        currentOrdersList.clear();
        currentOrdersList.addAll(filtered);
        adapter.notifyDataSetChanged();

        if (currentOrdersList.isEmpty()) {
            tvEmptyOrders.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
        } else {
            tvEmptyOrders.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
        }
    }
}
