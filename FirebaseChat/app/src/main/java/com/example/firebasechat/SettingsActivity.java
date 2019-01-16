package com.example.firebasechat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    public static final int GALLERY_CODE = 1000;

    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private StorageReference profileImgStorage; // Firebase cloud storage

    private CircleImageView displayImage;
    private TextView tvUsername, tvStatus;
    private Button statusButton, imgButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        displayImage = findViewById(R.id.settings_image);
        tvUsername = findViewById(R.id.settings_display_name);
        tvStatus = findViewById(R.id.settings_status);
        statusButton = findViewById(R.id.change_status_button);
        imgButton = findViewById(R.id.change_img_button);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserUid = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users_key)).child(currentUserUid);
        profileImgStorage = FirebaseStorage.getInstance().getReference();

        userRef.keepSynced(true); // enable offline capabilities

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child(getString(R.string.name_key)).getValue().toString();
                final String imageLink = dataSnapshot.child(getString(R.string.image_link_key)).getValue().toString();
                String status = dataSnapshot.child(getString(R.string.status_key)).getValue().toString();
                String thumbImageLink = dataSnapshot.child(getString(R.string.thumbnail_key)).getValue().toString();

                tvUsername.setText(name);
                tvStatus.setText(status);

                if (!imageLink.equals("default")) {
//                    Picasso.get().load(imageLink).placeholder(R.drawable.default_user_image).into(displayImage);
                    Picasso.get().load(imageLink).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.default_user_image).into(displayImage, new Callback() {
                        @Override
                        public void onSuccess() {}

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(imageLink).placeholder(R.drawable.default_user_image).into(displayImage);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentStatus = tvStatus.getText().toString(); // get current status of user
                Intent statusIntent = new Intent(SettingsActivity.this, StatusActivity.class);
                statusIntent.putExtra(getString(R.string.current_status), currentStatus);
                startActivity(statusIntent);
            }
        });

        imgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_CODE);

//                CropImage.activity()
//                        .setGuidelines(CropImageView.Guidelines.ON)
//                        .start(SettingsActivity.this);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
                // Crop image
                Uri imageUri = data.getData();
                CropImage.activity(imageUri)
                        .setAspectRatio(1, 1)
                        .start(this);
            }

            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    final ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle("Uploading Image");
                    progressDialog.setMessage("Please wait while we upload and process the image.");
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();

                    Uri resultUri = result.getUri();
                    String currentUserId = currentUser.getUid();

                    File thumbnailPath = new File(resultUri.getPath());
                    Bitmap thumbnailBitmap = new Compressor(this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumbnailPath);

                    ByteArrayOutputStream bitmapOutputStream = new ByteArrayOutputStream();
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmapOutputStream);
                    final byte[] thumbnailBytes = bitmapOutputStream.toByteArray();

                    final StorageReference filePath = profileImgStorage.child(getString(R.string.profile_images_folder)).child(currentUserId + ".jpg");
                    final StorageReference thumbnailsRef = profileImgStorage.child(getString(R.string.profile_images_folder)).child(getString(R.string.thumbnails_folder)).child(currentUserId + ".jpg");

                    filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                    final String downloadUrl = task.getResult().toString();

                                    UploadTask thumbnailUploadTask = thumbnailsRef.putBytes(thumbnailBytes);
                                    thumbnailUploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbnailTask) {
                                            if (thumbnailTask.isSuccessful()) {
                                                thumbnailsRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Uri> thumbnailUrlTask) {
                                                        String thumbnailDownloadUrl = thumbnailUrlTask.getResult().toString();
                                                        Map<String, Object> imageUrlUpdates = new HashMap<>();
                                                        imageUrlUpdates.put(getString(R.string.image_link_key), downloadUrl);
                                                        imageUrlUpdates.put(getString(R.string.thumbnail_key), thumbnailDownloadUrl);

                                                        userRef.updateChildren(imageUrlUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(SettingsActivity.this, "Success Uploading", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                                    }
                                                });

                                            }
                                            else {
                                                Toast.makeText(SettingsActivity.this, "Error in uploading thumbail", Toast.LENGTH_SHORT).show();
                                                progressDialog.dismiss();
                                            }
                                        }
                                    });


                                    }
                                });
                            }
                            else {
                                Toast.makeText(SettingsActivity.this, "Error in uploading", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }
                    });
                }
                else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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

    public static String generateRandomStr() {
        Random generator = new Random();
        StringBuilder sbRandom = new StringBuilder();
        int length = generator.nextInt(10);
        char tempChar;
        // Generate random string by generating random chars
        for (int i = 0; i < length; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            sbRandom.append(tempChar);
        }
        return sbRandom.toString();
    }
}
