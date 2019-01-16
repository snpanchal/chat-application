package com.example.firebasechat;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by AkshayeJH on 24/07/17.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {


    private List<Messages> messagesList;

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    public MessageAdapter(List<Messages> messagesList) {

        this.messagesList = messagesList;
        auth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child(FirebaseChat.getAppContext().getResources().getString(R.string.users_key));
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_single_layout ,parent, false);

        return new MessageViewHolder(v);

    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public TextView nameText;
        public ImageView messageImage;

        public MessageViewHolder(View view) {
            super(view);

            messageText = view.findViewById(R.id.message_text);
            profileImage = view.findViewById(R.id.message_profile);
            nameText = view.findViewById(R.id.name_text);
            messageImage = view.findViewById(R.id.message_image);
        }
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder viewHolder, int position) {
        String currentUserId = auth.getCurrentUser().getUid();

        Messages messages = messagesList.get(position);

        String fromUser = messages.getFrom();
        userRef.child(fromUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = dataSnapshot.child(FirebaseChat.getAppContext().getResources().getString(R.string.name_key)).getValue().toString();
                String thumbImageLink = dataSnapshot.child(FirebaseChat.getAppContext().getResources().getString(R.string.thumbnail_key)).getValue().toString();

                viewHolder.nameText.setText(username);
                if (!thumbImageLink.equals(FirebaseChat.getAppContext().getString(R.string.default_thumb))) {
                    Picasso.get().load(thumbImageLink).into(viewHolder.profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        if (messages.getType().equals("text")) {
            viewHolder.messageText.setText(messages.getMessage());
            viewHolder.messageImage.setVisibility(View.INVISIBLE);
        }
        else {
            viewHolder.messageText.setVisibility(View.INVISIBLE);
            Picasso.get().load(messages.getMessage()).into(viewHolder.messageImage);
        }
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }






}