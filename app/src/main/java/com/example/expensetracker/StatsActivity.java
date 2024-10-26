package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.anychart.AnyChart;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Pie;
import com.anychart.enums.Align;
import com.anychart.enums.LegendLayout;
import com.example.expensetracker.databinding.FragmentStatsBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private String selectedMonthYear;
    private FragmentStatsBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private Calendar calendar;
    private ValueEventListener transactionListener; // Declaring the ValueEventListener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        calendar = Calendar.getInstance();
        setupBottomNavigation();
        retrieveTransactionsForCurrentMonth();
        setupPieChart();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationView1.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.transactions) {
                    // Handle transaction navigation
                    openTransPage();
                } else if (item.getItemId() == R.id.floatingActionButton) {
                    // Handle the floating button action
                    new AddTransactionFragment().show(getSupportFragmentManager(), null);
                    return true;
                }
                return true;
            }
        });
    }

    private void openTransPage() {
        Intent intent = new Intent(this,TransactionsActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupPieChart() {
        Pie pieChart = AnyChart.pie();
        binding.anychart.setChart(pieChart);
    }

    private void retrieveTransactionsForCurrentMonth() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        selectedMonthYear = monthYearFormat.format(calendar.getTime());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            db = FirebaseDatabase.getInstance().getReference().child("Transactions").child(user.getUid());

            transactionListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Map<String, Double> incomeMap = new HashMap<>();
                    Map<String, Double> expenseMap = new HashMap<>();

                    if (snapshot.exists()) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            DataAdapter transaction = data.getValue(DataAdapter.class);

                            if (transaction != null) {
                                String transactionDate = transaction.getDate();
                                String transactionMonthYear = transactionDate != null ? transactionDate.substring(transactionDate.indexOf(' ') + 1) : null;

                                if (transactionMonthYear != null && transactionMonthYear.equals(selectedMonthYear)) {
                                    if ("Income".equals(transaction.getType())) {
                                        double amount = Double.parseDouble(transaction.getAmount());
                                        incomeMap.put(transaction.getCategory(), incomeMap.getOrDefault(transaction.getCategory(), 0.0) + amount);
                                    } else if ("Expense".equals(transaction.getType())) {
                                        double amount = Double.parseDouble(transaction.getAmount());
                                        expenseMap.put(transaction.getCategory(), expenseMap.getOrDefault(transaction.getCategory(), 0.0) + amount);
                                    }
                                }
                            }
                        }

                        updatePieChart(incomeMap, expenseMap);
                    } else {
                        Toast.makeText(StatsActivity.this, "No transactions found for this month.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("FirebaseData", "Error getting data", error.toException());
                    Toast.makeText(StatsActivity.this, "Error retrieving data.", Toast.LENGTH_SHORT).show();
                }
            };

            db.addValueEventListener(transactionListener);
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePieChart(Map<String, Double> incomeMap, Map<String, Double> expenseMap) {
        List<DataEntry> dataEntries = new ArrayList<>();

        // Prepare data for the pie chart
        for (Map.Entry<String, Double> entry : incomeMap.entrySet()) {
            dataEntries.add(new ValueDataEntry(entry.getKey() + " (Income)", entry.getValue()));
        }

        for (Map.Entry<String, Double> entry : expenseMap.entrySet()) {
            dataEntries.add(new ValueDataEntry(entry.getKey() + " (Expense)", entry.getValue()));
        }

        // Create the pie chart
        Pie pie = AnyChart.pie();
        pie.title("Income and Expense for " + selectedMonthYear);

        pie.labels().position("outside");

        pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);
        // Set data for the pie chart
        pie.data(dataEntries);

        // Bind the pie chart to the chart view
        binding.anychart.setChart(pie);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener when the activity is destroyed
        if (db != null && transactionListener != null) {
            db.removeEventListener(transactionListener);
        }
    }
}
