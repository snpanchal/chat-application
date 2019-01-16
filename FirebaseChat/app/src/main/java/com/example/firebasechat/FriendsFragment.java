package com.example.firebasechat;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView friendsList;
    private View mainView;

    private DatabaseReference friendsRef;
    private DatabaseReference usersRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseRecyclerAdapter<Friends, FriendsViewHolder> friendsAdapter;


    // Required empty public constructor
    public FriendsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_friends, container, false);
        friendsList = mainView.findViewById(R.id.friends_list);
        friendsList.hasFixedSize();
        friendsList.setLayoutManager(new LinearLayoutManager((this.getActivity())));

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        friendsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.friends_key)).child(currentUser.getUid());
        friendsRef.keepSynced(true);
        usersRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key));
        usersRef.keepSynced(true);


        Query friendsQuery = friendsRef.orderByKey();
        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(friendsQuery, Friends.class)
                .build();

        friendsAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder friendsViewHolder, int position, @NonNull Friends friends) {
                final String listUserId = getRef(position).getKey();

                usersRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String username = dataSnapshot.child(getString(R.string.name_key)).getValue().toString();
                        String status = dataSnapshot.child(getString(R.string.status_key)).getValue().toString();
                        String thumbImageLink = dataSnapshot.child(getString(R.string.thumbnail_key)).getValue().toString();
                        boolean online = Boolean.parseBoolean(dataSnapshot.child(getString(R.string.online_key)).getValue().toString());

                        friendsViewHolder.setName(username);
                        friendsViewHolder.setStatus(status);
                        friendsViewHolder.setUserImage(thumbImageLink);
                        friendsViewHolder.setOnline(online);

                        friendsViewHolder.userView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence[] options = new CharSequence[]{"Open profile", "Send message"};
                                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
                                dialogBuilder.setTitle("Select Options")
                                        .setItems(options, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int optionNum) {
                                                // First option
                                                switch (optionNum) {
                                                    case 0:
                                                        Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                                        profileIntent.putExtra(getString(R.string.user_id), listUserId);
                                                        startActivity(profileIntent);
                                                        break;
                                                    case 1:
                                                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                        chatIntent.putExtra(getString(R.string.user_id), listUserId);
                                                        chatIntent.putExtra(getString(R.string.username_key), username);
                                                        startActivity(chatIntent);
                                                        break;
                                                }
                                            }
                                        });
                                dialogBuilder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_single_layout, parent, false);
                return new FriendsViewHolder(view);
            }
        };
        friendsList.setAdapter(friendsAdapter);

        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        friendsAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        friendsAdapter.stopListening();
    }

    private static class FriendsViewHolder extends RecyclerView.ViewHolder {

        View userView;

        public FriendsViewHolder(View itemView) {
            super(itemView);
            userView = itemView;
        }

        public void setStatus(String status) {
            TextView tvStatus = userView.findViewById(R.id.user_single_status);
            tvStatus.setText(status);
        }

        public void setName(String name) {
            TextView tvName = userView.findViewById(R.id.user_single_name);
            tvName.setText(name);
        }

        public void setUserImage(String thumbImageLink) {
            CircleImageView userImageView = userView.findViewById(R.id.user_image);
            Picasso.get().load(thumbImageLink).placeholder(R.drawable.default_user_image).into(userImageView);
        }

        public void setOnline(boolean online) {
            ImageView onlineIndicator = userView.findViewById(R.id.users_single_online);
            if (online) {
                onlineIndicator.setVisibility(View.VISIBLE);
            }
            else {
                onlineIndicator.setVisibility(View.INVISIBLE);
            }
        }
    }
}
