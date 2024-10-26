package com.example.expensetracker;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<DataAdapter> list;
    private DatabaseReference reference;

    public MainAdapter(Context context, ArrayList<DataAdapter> list) {
        this.context = context;
        this.list = list;
        reference = FirebaseDatabase.getInstance().getReference("Transactions"); // Reference to your transactions node
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.row_transaction, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DataAdapter dataAdapter = list.get(position);

        // Set transaction details
        holder.transactionDate.setText(dataAdapter.getDate());
        holder.transactionCategory.setText(dataAdapter.getCategory());
        holder.accountLbl.setText(dataAdapter.getAccount());

        // Set the amount text and color based on transaction type
        String amount = dataAdapter.getAmount();
        String transactionType = dataAdapter.getType(); // Assuming you have a method getType()

        if ("Income".equals(transactionType)) {
            holder.transactionAmount.setTextColor(Color.GREEN); // Set color for income
            holder.transactionAmount.setText(amount);
        } else if ("Expense".equals(transactionType)) {
            holder.transactionAmount.setTextColor(Color.RED); // Set color for expense
            holder.transactionAmount.setText("-" + amount); // Add a minus sign for expenses
        } else {
            holder.transactionAmount.setText(amount); // Default case, if type is unknown
            holder.transactionAmount.setTextColor(Color.BLACK); // Default color
        }

        switch (dataAdapter.getAccount()) {
            case "Cash":
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCash)); // Change background color
                break;
            case "Cheque":
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCheque)); // Change background color
                break;
            case "NetBanking":
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNetBanking)); // Change background color
                break;
            default:
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOther)); // Change background color for other accounts
                break;
        }

        // Long-click listener for deletion confirmation
        holder.itemView.setOnLongClickListener(view -> {
            // Create an AlertDialog for deletion confirmation
            AlertDialog deleteDialog = new AlertDialog.Builder(context).create();
            deleteDialog.setTitle("Delete Transaction");
            deleteDialog.setMessage("Are you sure you want to delete this transaction?");
            deleteDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog, which) -> deleteTransaction(dataAdapter));
            deleteDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", (dialog, which) -> deleteDialog.dismiss());
            deleteDialog.show();
            return true; // Return true to indicate the click was handled
        });
    }

    private void deleteTransaction(DataAdapter dataAdapter) {
        // Retrieve the transaction properties from the DataAdapter
        String date = dataAdapter.getDate();
        String amount = dataAdapter.getAmount();
        String category = dataAdapter.getCategory();
        String account = dataAdapter.getAccount();
        String type = dataAdapter.getType();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Transactions").child(userId);

            // Retrieve all transactions
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        DataAdapter transaction = snapshot.getValue(DataAdapter.class);

                        if (transaction != null &&
                                transaction.getDate().equals(date) &&
                                transaction.getAmount().equals(amount) &&
                                transaction.getCategory().equals(category) &&
                                transaction.getAccount().equals(account) &&
                                transaction.getType().equals(type)) {

                            // Delete the transaction from Firebase
                            snapshot.getRef().removeValue();
                            Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show();
                            break; // Exit the loop after deletion
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseError", error.getMessage());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView transactionDate, transactionAmount, transactionCategory, accountLbl;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionDate = itemView.findViewById(R.id.transactionDate);
            transactionAmount = itemView.findViewById(R.id.transactionAmount);
            transactionCategory = itemView.findViewById(R.id.transactionCategory);
            accountLbl = itemView.findViewById(R.id.accountLbl);
        }
    }
}
