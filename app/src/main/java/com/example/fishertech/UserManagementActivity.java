package com.example.fishertech;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private EditText etSearch;
    private ImageView btnBack;
    private UserAdapter adapter;
    private ArrayList<UserModel> userList;
    private ArrayList<UserModel> filteredList;
    private ArrayList<String> userKeys;
    private ArrayList<String> filteredKeys;
    private DatabaseReference mRefUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        initViews();
        setupRecycler();
        loadUsers();
        setupSearch();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        recyclerUsers = findViewById(R.id.recyclerUsers);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);

        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        userKeys = new ArrayList<>();
        filteredKeys = new ArrayList<>();

        // Siguraduhing tugma ito sa path ng iyong JSON export
        mRefUsers = FirebaseDatabase.getInstance().getReference("FisherTech").child("Users");
    }

    private void setupRecycler() {
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        // Ipinapasa ang filteredList at filteredKeys sa adapter
        adapter = new UserAdapter(this, filteredList, filteredKeys);
        recyclerUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        mRefUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                userKeys.clear();
                filteredList.clear();
                filteredKeys.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    UserModel user = data.getValue(UserModel.class);
                    if (user != null) {
                        // Huwag ipakita ang admin sa listahan para hindi ma-delete ang sarili
                        if (!"admin".equals(user.getRole())) {
                            userList.add(user);
                            userKeys.add(data.getKey());

                            filteredList.add(user);
                            filteredKeys.add(data.getKey());
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterUsers(String text) {
        filteredList.clear();
        filteredKeys.clear();

        for (int i = 0; i < userList.size(); i++) {
            UserModel user = userList.get(i);
            if (user.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(user);
                filteredKeys.add(userKeys.get(i));
            }
        }
        adapter.notifyDataSetChanged();
    }
}