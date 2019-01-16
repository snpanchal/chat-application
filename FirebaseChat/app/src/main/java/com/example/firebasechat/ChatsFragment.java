package com.example.firebasechat;


import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.firebase.database.ChildEventListener;
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
public class ChatsFragment extends Fragment {

    private RecyclerView conversationsList;
    private View mainView;

    private FirebaseAuth auth;
    private DatabaseReference conversationsRef;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;

    private String currentUserId;

    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_chats, container, false);

        conversationsList = mainView.findViewById(R.id.conv_list);

        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();
        conversationsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.chats_key));
        usersRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key));
        usersRef.keepSynced(true);
        messagesRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.messages_key)).child(currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        conversationsList.setHasFixedSize(true);
        conversationsList.setLayoutManager(layoutManager);

        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("chatsfragment", "1");
        Query conversationQuery = conversationsRef.orderByChild(getString(R.string.timestamp_key));
        FirebaseRecyclerOptions<Conversation> options = new FirebaseRecyclerOptions.Builder<Conversation>()
                .setQuery(conversationQuery, Conversation.class)
                .build();
        Log.i("chatsfragment", "2");
        FirebaseRecyclerAdapter<Conversation, ConversationViewHolder> convAdapter =
                new FirebaseRecyclerAdapter<Conversation, ConversationViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ConversationViewHolder holder, int position, @NonNull final Conversation conversation) {
                final String userId = getRef(position).getKey();
                Query lastMsgQuery = messagesRef.child(userId).limitToLast(1);
                lastMsgQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        String lastMsg = dataSnapshot.child(getString(R.string.message_key)).getValue().toString();
                        Log.i("chatsfragment", "last msg: " + lastMsg);
                        holder.setMessage(lastMsg, conversation.isSeen());
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
                Log.i("chatsfragment", "3");
                usersRef.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String username = dataSnapshot.child(getString(R.string.name_key)).getValue().toString();
                        String thumbImageLink = dataSnapshot.child(getString(R.string.thumbnail_key)).getValue().toString();

                        if (dataSnapshot.hasChild(getString(R.string.online_key))) {
                            boolean online = Boolean.parseBoolean(dataSnapshot.child(getString(R.string.online_key)).getValue().toString());
                            holder.setUserOnline(online);
                        }

                        holder.setName(username);
                        holder.setUserImage(thumbImageLink);

                        holder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra(getString(R.string.user_id), userId);
                                chatIntent.putExtra(getString(R.string.username), username);
                                startActivity(chatIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_single_layout, parent, false);
                return new ConversationViewHolder(view);
            }
        };

        conversationsList.setAdapter(convAdapter);
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {

        View view;

        public ConversationViewHolder(View itemView) {
            super(itemView);
            view = itemView;
        }

        public void setMessage(String message, boolean seen) {
            TextView tvMessage = view.findViewById(R.id.user_single_status);
            tvMessage.setText(message);

            if (seen) {
                tvMessage.setTypeface(tvMessage.getTypeface(), Typeface.BOLD);
            }
            else {
                tvMessage.setTypeface(tvMessage.getTypeface(), Typeface.NORMAL);
            }
        }

        public void setName(String name) {
            TextView tvName = view.findViewById(R.id.user_single_name);
            tvName.setText(name);
        }

        public void setUserImage(String thumbImage) {
            CircleImageView userImageView = view.findViewById(R.id.user_image);
            Picasso.get().load(thumbImage).placeholder(R.drawable.default_user_image).into(userImageView);
        }

        public void setUserOnline(boolean online) {
            ImageView userOnlineView = (ImageView) view.findViewById(R.id.users_single_online);

            if (online) {
                userOnlineView.setVisibility(View.VISIBLE);
            }
            else {
                userOnlineView.setVisibility(View.INVISIBLE);
            }
        }
    }
}
