package com.example.expensetracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.expensetracker.Account;
import com.example.expensetracker.AccountsAdapter;
import com.example.expensetracker.CategoriesAdapter;
import com.example.expensetracker.Category;
import com.example.expensetracker.DataAdapter;
import com.example.expensetracker.databinding.FragmentAddTransactionBinding;
import com.example.expensetracker.databinding.ListDialogBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionFragment extends BottomSheetDialogFragment {

    private FragmentAddTransactionBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference reference;
    private FirebaseDatabase db;
    private String date, amount, category, account, type = "Income", month, year;

    public AddTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater);

        binding.incomeBtn.setOnClickListener(view -> {
            type = "Income";
            binding.incomeBtn.setBackground(getContext().getDrawable(R.drawable.income_selector));
            binding.expenseBtn.setBackground(getContext().getDrawable(R.drawable.default_selector));
            binding.expenseBtn.setTextColor(getContext().getColor(R.color.textColor));
            binding.incomeBtn.setTextColor(getContext().getColor(R.color.greenColor));
        });

        binding.expenseBtn.setOnClickListener(view -> {
            type = "Expense";
            binding.incomeBtn.setBackground(getContext().getDrawable(R.drawable.default_selector));
            binding.expenseBtn.setBackground(getContext().getDrawable(R.drawable.expense_selector));
            binding.incomeBtn.setTextColor(getContext().getColor(R.color.textColor));
            binding.expenseBtn.setTextColor(getContext().getColor(R.color.redColor));
        });

        binding.date.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, monthOfYear, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
                String dateToShow = dateFormat.format(calendar.getTime());
                binding.date.setText(dateToShow);

                // Capture month and year for later use
                month = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
                year = Integer.parseInt(new SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.getTime()));
            });
            datePickerDialog.show();
        });

        binding.category.setOnClickListener(c -> {
            ListDialogBinding dialogBinding = ListDialogBinding.inflate(inflater);
            AlertDialog categoryDialog = new AlertDialog.Builder(getContext()).create();
            categoryDialog.setView(dialogBinding.getRoot());

            ArrayList<Category> categories = new ArrayList<>();
            categories.add(new Category("Food"));
            categories.add(new Category("Salary"));
            categories.add(new Category("Business"));
            categories.add(new Category("Shopping"));
            categories.add(new Category("OTT Platform"));
            categories.add(new Category("Light Bill"));
            categories.add(new Category("Trip"));
            categories.add(new Category("Other"));

            CategoriesAdapter adapter = new CategoriesAdapter(getContext(), categories, category -> {
                binding.category.setText(category.getCategoryName());
                categoryDialog.dismiss();
            });

            dialogBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            dialogBinding.recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            dialogBinding.recyclerView.setAdapter(adapter);

            categoryDialog.show();
        });

        binding.account.setOnClickListener(c -> {
            ListDialogBinding dialogBinding = ListDialogBinding.inflate(inflater);
            AlertDialog accountsDialog = new AlertDialog.Builder(getContext()).create();
            accountsDialog.setView(dialogBinding.getRoot());

            ArrayList<Account> accounts = new ArrayList<>();
            accounts.add(new Account("Cash"));
            accounts.add(new Account("Cheque"));
            accounts.add(new Account("NetBanking"));
            accounts.add(new Account("Other"));

            AccountsAdapter adapter = new AccountsAdapter(getContext(), accounts, account -> {
                binding.account.setText(account.getAccountName());
                accountsDialog.dismiss();
            });
            dialogBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            dialogBinding.recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            dialogBinding.recyclerView.setAdapter(adapter);

            accountsDialog.show();
        });

        binding.saveTransactionBtn.setOnClickListener(v -> {
            date = binding.date.getText().toString();
            amount = binding.amount.getText().toString();
            category = binding.category.getText().toString();
            account = binding.account.getText().toString();

            if (!date.isEmpty() && !amount.isEmpty() && !category.isEmpty() && !account.isEmpty()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    reference = db.getReference().child("Transactions").child(userId);

                    // Ensure transactionType is not null or empty
                    if (type == null || type.isEmpty()) {
                        Toast.makeText(getContext(), "Please select a transaction type (Income/Expense)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create an instance of DataAdapter including the transaction type, month, and year
                    DataAdapter transaction = new DataAdapter(date, amount, category, account, type, month, year);

                    // Push the transaction to Firebase (it will automatically create a unique ID)
                    reference.push().setValue(transaction).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            clearFields();
                            Toast.makeText(getContext(), "Transaction Added Successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to add transaction: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    // Method to clear input fields after successful submission
    private void clearFields() {
        binding.date.setText("");
        binding.amount.setText("");
        binding.category.setText("");
        binding.account.setText("");
    }

    // Transaction class for Firebase
    public static class Transaction {
        public String date, amount, category, account, type;

        public Transaction() {
            // Default constructor required for Firebase
        }

        public Transaction(String date, String amount, String category, String account, String type) {
            this.date = date;
            this.amount = amount;
            this.category = category;
            this.account = account;
            this.type = type;
        }
    }
}