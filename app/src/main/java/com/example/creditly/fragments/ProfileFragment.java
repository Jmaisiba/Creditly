package com.example.creditly.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.creditly.MainActivity;
import com.example.creditly.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.util.HashMap;

import static android.app.Activity.RESULT_OK;



public class ProfileFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    StorageReference storageReference;

    String storagePath = "Users_Profile_Cover_Imgs/";

    ImageView avatarIv, coverIv;
    TextView nameTv,emailTv,phoneTv;
    FloatingActionButton fab;


    ProgressDialog pg;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_REQUEST_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_REQUEST_CODE = 400;


    String cameraPermissions[];
    String storagePermissions[];

    //uri of picked image
    Uri image_uri;

    //check if user is uploading a cover or profile photo
    String profileOrCoverPhoto;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_profile, container, false);

        //initializing firebase
        firebaseAuth= FirebaseAuth.getInstance();
        user= firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference= FirebaseStorage.getInstance().getReference();

        //initializing array of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //Views initialization
        avatarIv = view.findViewById(R.id.avatarIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        coverIv = view.findViewById(R.id.coverIv);
        fab = view.findViewById(R.id.fab);

        pg = new ProgressDialog(getActivity());

        //Retrieving current user details using email
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //check until data is acquire
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    String name = ""+ds.child("name").getValue();
                    String email = ""+ds.child("email").getValue();
                    String phone = ""+ds.child("phone").getValue();
                    String image= ""+ds.child("image").getValue();
                    String cover= ""+ds.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        Picasso.get().load(image).into(avatarIv);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);
                    }

                    try {
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception e){

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();


            }
        });

        return view;
    }

    private boolean checkStoragePermission(){
        //Check if storage permission is enabled
        //return true if enabled and vice versa
       boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
       == (PackageManager.PERMISSION_GRANTED);
       return result;
    }

    private void requestStoragePermission(){

        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //Check if storage permission is enabled
        //return true if enabled and vice versa
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        //request runtime storage permission
       requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {
        //options dialog
        String options[] = {"Edit Profile Picture","Edit Cover Photo","Edit Name", "Edit Phone"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set Title
        builder.setTitle("Choose Action");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item clicked
                if (which == 0) {
                    //edit profile photo
                    pg.setMessage("Updating Profile Picture");
                    profileOrCoverPhoto = "image";
                    showImagePicDialog();
                }
                else  if(which==1){
                    //edit cover image
                    pg.setMessage("Updating Cover Photo");
                    profileOrCoverPhoto = "cover";
                    showImagePicDialog();
                }
                else  if(which==2){
                    //edit name
                    pg.setMessage("Updating Name");
                    showNamePhoneUpdateDialog("name");
                }
                else  if(which==3){
                    //edit phone
                    pg.setMessage("Updating Phone");
                    showNamePhoneUpdateDialog("phone");

                }
            }
        });
        builder.create().show();
    }

    private void showNamePhoneUpdateDialog(String key) {

        //custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+ key);//e.g. Update name / Update phone

        //dialog layout
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);

        //adding editText
        EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+ key);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add buttoms
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //input text from editText
                String value= editText.getText().toString().trim();
                //check for input
                if (!TextUtils.isEmpty(value)){

                    pg.show();
                    HashMap<String, Object>result= new HashMap<>();
                    result.put(key, value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    pg.dismiss();
                                    Toast.makeText(getActivity(), "Updated", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pg.dismiss();
                            Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    Toast.makeText(getActivity(), "Please Enter "+ key, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });
        builder.create().show();
    }

    private void showImagePicDialog() {


        String options[] = {"Camera","Gallery"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set Title
        builder.setTitle("Image from");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item clicked
                if (which == 0) {
                   //Camera clicked
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else {
                        pickFromCamera();
                    }
                }
                else  if(which==1){
                   //Gallery clicked
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else {
                        pickFromGallery();
                    }
                }
            }
        });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){

            case CAMERA_REQUEST_CODE: {

                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickFromCamera();
                    }
                    else{
                        Toast.makeText(getActivity(), "Please Enable Camera Permission and Storage Permission", Toast.LENGTH_SHORT).show();
                    }

                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length>0){

                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (writeStorageAccepted){
                    pickFromGallery();
                }else {
                    Toast.makeText(getActivity(), "Please Enable Storage Permission", Toast.LENGTH_SHORT).show();

                }
                }

            }
            break;
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_REQUEST_CODE){
                //image from gallery
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_REQUEST_CODE){
                //image from camera
                uploadProfileCoverPhoto(image_uri);

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //same function to upload profile and cover images
    //string variable is used to check whether image is a cover or profile photo
    private void uploadProfileCoverPhoto(Uri uri) {

        pg.show();

        String filePathAndName = storagePath+ ""+ profileOrCoverPhoto+""+ user.getUid();

        StorageReference storageReference2 = storageReference.child(filePathAndName);
        storageReference2.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri>uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isSuccessful());
                Uri downloadUri = uriTask.getResult();

                //check is image is uploaded and url recieved
                if (uriTask.isSuccessful()){
                    //image is uploaded


                    HashMap<String, Object> results = new HashMap<>();
                    results.put(profileOrCoverPhoto,downloadUri.toString());

                    databaseReference.child(user.getUid()).updateChildren(results).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            pg.dismiss();
                            Toast.makeText(getActivity(), "Image Updated...", Toast.LENGTH_SHORT).show();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pg.dismiss();
                            Toast.makeText(getActivity(), "error updating image...", Toast.LENGTH_SHORT).show();


                        }
                    });
                }
                else {
                    //error
                    pg.dismiss();
                    Toast.makeText(getActivity(), "An error occured", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
           pg.dismiss();
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFromCamera() {
        ContentValues values= new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_REQUEST_CODE);
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_REQUEST_CODE);
    }


    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user!=null){
            //user remains signed in

            // mProfileTv.setText(user.getEmail());
        }
        else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }


    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id==R.id.action_logout){

            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}