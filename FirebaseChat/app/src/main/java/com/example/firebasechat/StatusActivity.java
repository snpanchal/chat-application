package com.example.firebasechat;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    private Toolbar toolbar;
    private TextInputLayout statusInput;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        toolbar = findViewById(R.id.status_app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Your Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserUid = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key)).child(currentUserUid);

        statusInput = findViewById(R.id.status_input);
        saveButton = findViewById(R.id.status_save_button);

        String currentStatus = getIntent().getStringExtra(getString(R.string.current_status));
        statusInput.getEditText().setText(currentStatus);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Progress
                final ProgressDialog progress = new ProgressDialog(StatusActivity.this);
                progress.setTitle("Saving Changes");
                progress.setMessage("Please wait while we save changes.");
                progress.show();

                String status = statusInput.getEditText().getText().toString();
                if (!TextUtils.isEmpty(status)) {
                    userRef.child(getString(R.string.status_key)).setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                progress.dismiss();
                            }
                            else {
                                Toast.makeText(StatusActivity.this, "Error saving changes", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        userRef.child(getString(R.string.online_key)).setValue(true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        userRef.child(getString(R.string.online_key)).setValue(false);
    }
}
