package com.example.firebasechat;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

enum FriendshipStatus {
    NOT_FRIENDS, REQUEST_SENT, FRIENDS, REQUEST_RECEIVED
}

public class ProfileActivity extends AppCompatActivity {
    
    public static final String SENT_STATUS = "sent";
    public static final String RECEIVED_STATUS = "received";

    private ImageView profileImage;
    private TextView tvUsername, tvStatus, tvFriendsCount;
    private Button requestButton, declineButton;
    private ProgressDialog progress;

    private DatabaseReference rootRef;
    private DatabaseReference profileUserRef;
    private DatabaseReference friendRequestsRef;
    private DatabaseReference friendsRef;
    private DatabaseReference notificationsRef;
    private FirebaseUser currentUser;

    private FriendshipStatus currentFriendshipState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String userId = getIntent().getStringExtra(getString(R.string.user_id));
        Log.i("ProfileActivity", "User id: " + userId);

        rootRef = FirebaseDatabase.getInstance().getReference();
        profileUserRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key)).child(userId);
        friendRequestsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.friend_requests_key));
        friendsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.friends_key));
        notificationsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.notifications_key));
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        profileImage = findViewById(R.id.profile_image);
        tvUsername = findViewById(R.id.profile_display_name);
        tvStatus = findViewById(R.id.profile_status);
        tvFriendsCount = findViewById(R.id.profile_total_friends);
        requestButton = findViewById(R.id.profile_request_button);
        declineButton = findViewById(R.id.profile_decline_request_button);

        currentFriendshipState = FriendshipStatus.NOT_FRIENDS;

        progress = new ProgressDialog(this);
        progress.setTitle("Loading User Data");
        progress.setMessage("Please wait while we load the user data.");
        progress.setCanceledOnTouchOutside(false);
        progress.show();

        // Retrieve data from Firebase
        profileUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String displayName = dataSnapshot.child(getString(R.string.name_key)).getValue(String.class);
                String status = dataSnapshot.child(getString(R.string.status_key)).getValue(String.class);
                String imageLink = dataSnapshot.child(getString(R.string.image_link_key)).getValue(String.class);

                tvUsername.setText(displayName);
                tvStatus.setText(status);

                Picasso.get().load(imageLink).placeholder(R.drawable.default_user_image).into(profileImage);

                friendRequestsRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(userId)) {
                            String requestType = dataSnapshot.child(userId).child(getString(R.string.friend_requests_key)).getValue(String.class);

                            switch (requestType) {
                                case RECEIVED_STATUS:
                                    currentFriendshipState = FriendshipStatus.REQUEST_RECEIVED;
                                    requestButton.setText(R.string.accept_friend_request);
                                    declineButton.setVisibility(View.VISIBLE);
                                    declineButton.setEnabled(true);
                                    break;
                                case SENT_STATUS:
                                    currentFriendshipState = FriendshipStatus.REQUEST_SENT;
                                    requestButton.setText(R.string.cancel_friend_request);
                                    declineButton.setVisibility(View.INVISIBLE);
                                    declineButton.setEnabled(false);
                                    break;
                            }

                            progress.dismiss();
                        }
                        else {
                            friendsRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(userId)) {
                                        currentFriendshipState = FriendshipStatus.FRIENDS;
                                        requestButton.setText(R.string.unfriend_person);

                                        declineButton.setVisibility(View.INVISIBLE);
                                        declineButton.setEnabled(false);
                                    }

                                    progress.dismiss();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    progress.dismiss();
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestButton.setEnabled(false);

                switch (currentFriendshipState) {
                    case NOT_FRIENDS:
                        DatabaseReference newNotificationRef = rootRef.child(getString(R.string.notifications_key)).child(userId).push();
                        String newNotificationId = newNotificationRef.getKey();

                        final HashMap<String, String> notificationData = new HashMap<>();
                        notificationData.put("from", currentUser.getUid());
                        notificationData.put("type", "request");

                        final Map requestMap = new HashMap();
                        requestMap.put(getString(R.string.friend_requests_key) + "/" + currentUser.getUid() + "/" + userId + "/request_type", SENT_STATUS);
                        requestMap.put(getString(R.string.friend_requests_key) + "/" + userId + "/" + currentUser.getUid() + "/request_type", RECEIVED_STATUS);
                        requestMap.put(getString(R.string.notifications_key) + "/" + userId + "/" + newNotificationId, notificationData);
                        rootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Toast.makeText(ProfileActivity.this, "Error in sending request", Toast.LENGTH_SHORT).show();
                                }
                                requestButton.setEnabled(true);
                                currentFriendshipState = FriendshipStatus.REQUEST_SENT;
                                requestButton.setText(R.string.cancel_friend_request);
                            }
                        });
                        break;
                    case REQUEST_SENT:
                        friendRequestsRef.child(currentUser.getUid()).child(userId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                friendRequestsRef.child(userId).child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        requestButton.setEnabled(true);
                                        currentFriendshipState = FriendshipStatus.NOT_FRIENDS;
                                        requestButton.setText(R.string.send_friend_request);

                                        declineButton.setVisibility(View.INVISIBLE);
                                        declineButton.setEnabled(false);
                                    }
                                });
                            }
                        });
                        break;
                    case REQUEST_RECEIVED:
                        final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                        Map friendsMap = new HashMap();
                        friendsMap.put(getString(R.string.friends_key) + "/" + currentUser.getUid() + "/" + userId + "/date", currentDate);
                        friendsMap.put(getString(R.string.friends_key) + "/" + userId + "/" + currentUser.getUid() + "/date", currentDate);
                        friendsMap.put(getString(R.string.friend_requests_key) + "/" + currentUser.getUid() + "/" + userId, null);
                        friendsMap.put(getString(R.string.friend_requests_key) + "/" + userId + "/" + currentUser.getUid(), null);

                        rootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    String error = databaseError.getMessage();
                                    Toast.makeText(ProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    requestButton.setEnabled(true);
                                    currentFriendshipState = FriendshipStatus.FRIENDS;
                                    requestButton.setText(R.string.unfriend_person);

                                    declineButton.setVisibility(View.INVISIBLE);
                                    declineButton.setEnabled(false);
                                }
                            }
                        });
                        break;
                    case FRIENDS:
                        Map unfriendMap = new HashMap();
                        unfriendMap.put(getString(R.string.friends_key) + "/" + currentUser.getUid() + "/" + userId, null);
                        unfriendMap.put(getString(R.string.friends_key) + "/" + userId + "/" + currentUser.getUid(), null);

                        rootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    String error = databaseError.getMessage();
                                    Toast.makeText(ProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    currentFriendshipState = FriendshipStatus.NOT_FRIENDS;
                                    requestButton.setText(R.string.send_friend_request);

                                    declineButton.setVisibility(View.INVISIBLE);
                                    declineButton.setEnabled(false);
                                }

                                requestButton.setEnabled(true);
                            }
                        });
                        break;
                }
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentFriendshipState) {
                    case REQUEST_RECEIVED:
                        Map declineMap = new HashMap();
                        declineMap.put(getString(R.string.friend_requests_key) + "/" + currentUser.getUid() + "/" + userId, null);
                        declineMap.put(getString(R.string.friend_requests_key) + "/" + userId + "/" + currentUser.getUid(), null);

                        rootRef.updateChildren(declineMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    String error = databaseError.getMessage();
                                    Toast.makeText(ProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    currentFriendshipState = FriendshipStatus.NOT_FRIENDS;
                                    requestButton.setText(R.string.send_friend_request);

                                    declineButton.setVisibility(View.INVISIBLE);
                                    declineButton.setEnabled(false);
                                }

                                requestButton.setEnabled(true);
                            }
                        });
                        break;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        rootRef.child(getString(R.string.users_key)).child(currentUser.getUid()).child(getString(R.string.online_key)).setValue(true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        rootRef.child(getString(R.string.users_key)).child(currentUser.getUid()).child(getString(R.string.online_key)).setValue(false);
    }
}
