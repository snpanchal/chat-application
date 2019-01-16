package com.example.firebasechat;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class FirebaseChat extends Application {

    private DatabaseReference userRef;
    private FirebaseAuth auth;

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Picasso offline capabilities
        Picasso.Builder picassoBuilder =  new Picasso.Builder(this);
        picassoBuilder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = picassoBuilder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
        
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            userRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key)).child(auth.getCurrentUser().getUid());
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot != null) {
                        userRef.child(getString(R.string.online_key)).onDisconnect().setValue(false);
                        userRef.child(getString(R.string.last_seen_key)).setValue(ServerValue.TIMESTAMP);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
        }

    }

    public static Context getAppContext() {
        return appContext;
    }
}
