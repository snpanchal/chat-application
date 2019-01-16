package com.example.firebasechat;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    private Toolbar toolbar;
    private ViewPager viewPager;
    private SectionsPagerAdapter pagerAdapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key)).child(auth.getCurrentUser().getUid());
        }

        toolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Firebase Chat");

        viewPager = findViewById(R.id.main_tab_pager);
        tabLayout = findViewById(R.id.main_tabs);

        pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed-in (FirebaseUser should be non-null)
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            sendToStart();
        }
        else {
            userRef.child(getString(R.string.online_key)).setValue(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (auth.getCurrentUser() != null) {
            userRef.child(getString(R.string.online_key)).setValue(false);
            userRef.child(getString(R.string.last_seen_key)).setValue(ServerValue.TIMESTAMP);
        }

    }

    private void sendToStart() {
        Intent startIntent = new Intent(this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.main_logout_button:
                auth.signOut();
                sendToStart();
                break;
            case R.id.main_settings_button:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.main_all_users_button:
                Intent usersIntent = new Intent(this, UsersActivity.class);
                startActivity(usersIntent);
                break;
        }

        return true;
    }
}
