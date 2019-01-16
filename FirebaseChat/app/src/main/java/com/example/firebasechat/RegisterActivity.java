package com.example.firebasechat;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference dbRoot;

    private TextInputLayout usernameInput, emailInput, passwordInput;
    private Button createAccountButton;
    private Toolbar toolbar;
    private ProgressDialog regProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        toolbar = findViewById(R.id.register_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        regProgress = new ProgressDialog(this);

        // Firebase
        auth = FirebaseAuth.getInstance(); // authorization
        dbRoot = FirebaseDatabase.getInstance().getReference();

        // Registration fields
        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.register_email_input);
        passwordInput = findViewById(R.id.register_password_input);
        createAccountButton = findViewById(R.id.create_account_button);
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getEditText().getText().toString();
                String email = emailInput.getEditText().getText().toString();
                String password = passwordInput.getEditText().getText().toString();

                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                    regProgress.setTitle("Registering User");
                    regProgress.setTitle("Please wait while we create your account.");
                    regProgress.setCanceledOnTouchOutside(false);
                    regProgress.show();

                    registerUser(username, email, password);
                }
            }
        });
    }

    private void registerUser(final String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser currentUser = auth.getCurrentUser();
                    String uid = currentUser.getUid();
                    final DatabaseReference userRef = dbRoot.child(getString(R.string.users_key)).child(uid);

                    FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            String deviceTokenId = instanceIdResult.getId();

                            // Adding user details to Firebase database
                            HashMap<String, String> userDetails = new HashMap<>();
                            userDetails.put(getString(R.string.device_token_key), deviceTokenId);
                            userDetails.put(getString(R.string.name_key), username);
                            userDetails.put(getString(R.string.status_key), "Hi there, I am using Firebase Chat.");
                            userDetails.put(getString(R.string.image_link_key), "default");
                            userDetails.put(getString(R.string.thumbnail_key), "default");
                            userRef.setValue(userDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        regProgress.dismiss(); // dismiss dialog if successful
                                        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(mainIntent);
                                        finish();
                                    }
                                }
                            });

                        }
                    });
                }
                else {
                    regProgress.hide();
                    // If sign in fails, display a message to the user
                    Toast.makeText(RegisterActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
