package com.example.firebasechat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class LoginActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout loginEmail, loginPassword;
    private Button loginButton;
    private ProgressDialog loginProgress;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key));

        toolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Login");

        loginProgress = new ProgressDialog(this);

        loginEmail = findViewById(R.id.login_email_input);
        loginPassword = findViewById(R.id.login_password_input);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = loginEmail.getEditText().getText().toString();
                String password = loginPassword.getEditText().getText().toString();

                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                    loginProgress.setTitle("Logging In");
                    loginProgress.setMessage("Please wait while we log you in.");
                    loginProgress.setCanceledOnTouchOutside(false);
                    loginProgress.show();

                    loginUser(email, password);
                }
            }
        });
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    loginProgress.dismiss();

                    FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            String deviceTokenId = instanceIdResult.getToken();
                            String currentUserId = auth.getCurrentUser().getUid();
                            usersRef.child(currentUserId).child(getString(R.string.device_token_key)).setValue(deviceTokenId).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(mainIntent);
                                    finish();
                                }
                            });
                        }
                    });

                }
                else {
                    loginProgress.hide();
                    // If sign in fails, display a message to the user.
                    Toast.makeText(LoginActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
