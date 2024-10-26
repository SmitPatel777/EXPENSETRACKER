package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.databinding.FragmentTransBinding;
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
import java.util.Locale;

public class TransactionsActivity extends AppCompatActivity {

    private FragmentTransBinding binding;
    private Calendar calendar;
    private RecyclerView recyclerView;
    private ArrayList<DataAdapter> list;
    private MainAdapter mainAdapter;
    private FirebaseAuth mAuth;
    private DatabaseReference db;

    private double totalIncome = 0.0;
    private double totalExpense = 0.0;
    private ValueEventListener transactionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentTransBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNavigation();  // Set up BottomNavigationView
        setupRecyclerView();
        setupDateNavigation();
        retrieveTransactionsForCurrentMonth();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.stats) {
                    openStatsPage();
                } else if (item.getItemId() == R.id.floatingActionButton) {
                    new AddTransactionFragment().show(getSupportFragmentManager(), null);
                    return true;
                }
                return true;
            }
        });
    }

    private void openStatsPage() {
        Intent intent = new Intent(this, StatsActivity.class); // Change this if StatsFragment is now an Activity
        startActivity(intent);
        finish(); // Finish the current activity if needed
    }

    private void setupRecyclerView() {
        recyclerView = binding.transactionsList;
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        mainAdapter = new MainAdapter(this, list);
        recyclerView.setAdapter(mainAdapter);

        calendar = Calendar.getInstance();
        updateDate();
    }

    private void setupDateNavigation() {
        binding.nextDate.setOnClickListener(c -> {
            calendar.add(Calendar.MONTH, 1);
            updateDate();
        });

        binding.previousDate.setOnClickListener(c -> {
            calendar.add(Calendar.MONTH, -1);
            updateDate();
        });

        binding.currentMonth.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                updateDate();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        binding.currentMonth.setText(dateFormat.format(calendar.getTime()));
        retrieveTransactionsForCurrentMonth();
    }

    private void retrieveTransactionsForCurrentMonth() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        String selectedMonthYear = monthYearFormat.format(calendar.getTime());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            db = FirebaseDatabase.getInstance().getReference().child("Transactions").child(user.getUid());

            transactionListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    list.clear();
                    totalIncome = 0.0;
                    totalExpense = 0.0;

                    if (snapshot.exists()) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            DataAdapter transaction = data.getValue(DataAdapter.class);
                            if (transaction != null) {
                                String transactionDate = transaction.getDate();
                                if (transactionDate != null && !transactionDate.isEmpty()) {
                                    String transactionMonthYear = transactionDate.substring(transactionDate.indexOf(' ') + 1);
                                    if (transactionMonthYear.equals(selectedMonthYear)) {
                                        list.add(transaction);
                                        updateTotals(transaction);
                                    }
                                }
                            }
                        }
                        mainAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(TransactionsActivity.this, "No transactions found for this month.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("FirebaseData", "Error getting data", error.toException());
                    Toast.makeText(TransactionsActivity.this, "Error retrieving data.", Toast.LENGTH_SHORT).show();
                }
            };
            db.addValueEventListener(transactionListener);
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotals(DataAdapter transaction) {
        try {
            double amount = Double.parseDouble(transaction.getAmount());
            if ("Income".equals(transaction.getType())) {
                totalIncome += amount;
            } else if ("Expense".equals(transaction.getType())) {
                totalExpense += amount;
            }
        } catch (NumberFormatException e) {
            Log.e("TransactionError", "Invalid amount format for transaction: " + transaction.getAmount());
        }
        binding.incomeLbl.setText(String.valueOf(totalIncome));
        binding.expenseLbl.setText(String.valueOf(totalExpense));
        binding.totalLbl.setText(String.valueOf(totalIncome - totalExpense));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && transactionListener != null) {
            db.removeEventListener(transactionListener); // Remove the listener when activity is destroyed
        }
    }
}
