package com.example.firebasechat;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    public static final int GALLERY_PICK = 2200;

    private String chatUserId;
    private List<Messages> messagesList;
    private MessageAdapter messageAdapter;
    private String currentUserId;
    private int currentPage = 1;
    private int itemPos = 0;
    private String lastKey = "";
    private String prevKey = "";

    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private DatabaseReference messageRef;
    private StorageReference imageStorage;
    private String currentUserRef;
    private String chatUserRef;

    private Toolbar chatToolbar;
    private TextView titleView;
    private TextView lastSeenView;
    private CircleImageView profileImage;
    private ImageButton addButton;
    private ImageButton sendButton;
    private EditText messageView;
    private RecyclerView messagesListView;
    private SwipeRefreshLayout refreshLayout;
    private LinearLayoutManager linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatToolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(chatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();
        imageStorage = FirebaseStorage.getInstance().getReference();

        chatUserId = getIntent().getStringExtra(getString(R.string.user_id));
        String username = getIntent().getStringExtra(getString(R.string.username_key));
        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList);

        currentUserRef = getString(R.string.messages_key) + "/" + currentUserId + "/" + chatUserId;
        chatUserRef = getString(R.string.messages_key) + "/" + chatUserId + "/" + currentUserId;

        getSupportActionBar().setTitle(username);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View actionBarView = inflater.inflate(R.layout.chat_toolbar, null);
        actionBar.setCustomView(actionBarView);

        titleView = findViewById(R.id.chat_title);
        lastSeenView = findViewById(R.id.chat_last_seen);
        profileImage = findViewById(R.id.chat_image);
        addButton = findViewById(R.id.chat_add_btn);
        sendButton = findViewById(R.id.chat_send_btn);
        messageView = findViewById(R.id.chat_message_view);
        messagesListView = findViewById(R.id.messages_list);
        refreshLayout = findViewById(R.id.message_swipe);

        linearLayout = new LinearLayoutManager(this);
        messagesListView.setHasFixedSize(true);
        messagesListView.setLayoutManager(linearLayout);
        messagesListView.setAdapter(messageAdapter);
        loadMessages();

        titleView.setText(username);
        rootRef.child(getString(R.string.users_key)).child(chatUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("online", "onDataChange: " + dataSnapshot.child(getString(R.string.online_key)));
                boolean online = Boolean.parseBoolean(dataSnapshot.child(getString(R.string.online_key)).getValue().toString());
                String imageLink = dataSnapshot.child(getString(R.string.image_link_key)).getValue().toString();

                if (online) {
                    lastSeenView.setText(R.string.online);
                }
                else {
                    GetTimeAgo gta = new GetTimeAgo();
                    String lastSeen = dataSnapshot.child(getString(R.string.last_seen_key)).getValue().toString();
                    long lastTime = Long.parseLong(lastSeen);
                    String lastTimeSeen = gta.getTimeAgo(lastTime);
                    lastSeenView.setText(lastTimeSeen);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        rootRef.child(getString(R.string.chats_key)).child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(chatUserId)) {
                    Map chatMap = new HashMap();
                    chatMap.put(getString(R.string.seen_key), false);
                    chatMap.put(getString(R.string.timestamp_key), ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put(getString(R.string.chats_key) + "/" + currentUserId + "/" + chatUserId, chatMap);
                    chatUserMap.put(getString(R.string.chats_key) + "/" + chatUserId + "/" + currentUserId, chatMap);

                    rootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.d("Chat Log", databaseError.getMessage());
                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        // Send button onclick listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                itemPos = 0;
                loadMoreMessages();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);
            }
        });
    }

    private void loadMessages() {
        DatabaseReference messageRef = rootRef.child(getString(R.string.messages_key)).child(currentUserId).child(chatUserId);

        Query messageQuery = messageRef.limitToLast(currentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);

                itemPos++;
                if (itemPos == 1) {
                    lastKey = dataSnapshot.getKey();
                    prevKey = dataSnapshot.getKey();
                }

                messageAdapter.notifyDataSetChanged();
                messagesListView.scrollToPosition(messagesList.size() - 1);
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadMoreMessages() {
        DatabaseReference messageRef = rootRef.child(getString(R.string.messages_key)).child(currentUserId).child(chatUserId);

        Query messageQuery = messageRef.orderByKey().endAt(lastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                String messagesKey = dataSnapshot.getKey();

                if (!prevKey.equals(messagesKey)) {
                    messagesList.add(itemPos++, messages);
                }
                else {
                    prevKey = lastKey;
                }

                if (itemPos == 1) {
                    lastKey = messagesKey;
                }

                messageAdapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
                linearLayout.scrollToPositionWithOffset(10, 0);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {
        String message = messageView.getText().toString();
        if (!TextUtils.isEmpty(message)) {

            DatabaseReference userMessagePush = rootRef.child(getString(R.string.messages_key)).child(currentUserId).child(chatUserId).push();
            String pushId = userMessagePush.getKey();

            addMessageToFirebase(message, "text", pushId);
        }
    }

    private void addMessageToFirebase(String message, String type, String pushId) {
        Map messageMap = new HashMap();
        messageMap.put(getString(R.string.message_key), message);
        messageMap.put(getString(R.string.send_key), false);
        messageMap.put(getString(R.string.type_key), type);
        messageMap.put(getString(R.string.time_key), ServerValue.TIMESTAMP);
        messageMap.put(getString(R.string.from_key), currentUserId);

        Map messageUserMap = new HashMap();
        messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
        messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

        messageView.getText().clear(); // clear message field on sending

        rootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.d("Chat_Log", databaseError.getMessage());
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GALLERY_PICK:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = data.getData();

                    DatabaseReference userMessagePush = rootRef.child(getString(R.string.messages_key))
                            .child(currentUserId).child(chatUserId).push();
                    final String pushId = userMessagePush.getKey();
                    final StorageReference filePath = imageStorage.child(getString(R.string.message_image_storage)).child(pushId + ".jpg");
                    filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> downloadUrlTask) {
                                        if (downloadUrlTask.isSuccessful()) {
                                            final String downloadUrl = downloadUrlTask.getResult().toString();

                                            addMessageToFirebase(downloadUrl, "image",pushId);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
        }
    }
}
