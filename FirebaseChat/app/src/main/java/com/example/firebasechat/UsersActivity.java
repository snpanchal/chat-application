package com.example.firebasechat;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView usersList;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private FirebaseRecyclerAdapter<User, UsersViewHolder> fbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        toolbar = findViewById(R.id.users_app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key));

        usersList = findViewById(R.id.users_list);
        usersList.hasFixedSize();
        usersList.setLayoutManager(new LinearLayoutManager(this));

        Query usersQuery = usersRef.orderByKey();
        FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(usersQuery, User.class)
                .build();

        fbAdapter = new FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder usersViewHolder, int position, @NonNull User user) {
                usersViewHolder.setDisplayName(user.getName());
                usersViewHolder.setUserStatus(user.getStatus());
                usersViewHolder.setUserImage(user.getThumbImageLink());

                final String userId = getRef(position).getKey();

                usersViewHolder.userView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra(getString(R.string.user_id), userId);
                        startActivity(profileIntent);
                    }
                });
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_single_layout, parent, false);
                return new UsersViewHolder(view);
            }
        };

        usersList.setAdapter(fbAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        fbAdapter.startListening();

        usersRef.child(auth.getCurrentUser().getUid()).child(getString(R.string.online_key)).setValue(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        fbAdapter.stopListening();

        usersRef.child(auth.getCurrentUser().getUid()).child(getString(R.string.online_key)).setValue(false);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View userView;

        public UsersViewHolder(View itemView) {
            super(itemView);
            userView = itemView;
        }

        public void setDisplayName(String name) {
            TextView tvUsername = userView.findViewById(R.id.user_single_name);
            tvUsername.setText(name);
        }

        public void setUserStatus(String status) {
            TextView tvStatus = userView.findViewById(R.id.user_single_status);
            tvStatus.setText(status);
        }

        public void setUserImage(String thumbImageLink) {
            CircleImageView userImageView = userView.findViewById(R.id.user_image);
            Picasso.get().load(thumbImageLink).placeholder(R.drawable.default_user_image).into(userImageView);
        }
    }
}
