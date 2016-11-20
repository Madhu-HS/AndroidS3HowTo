package com.tl.joe.example;

import android.content.Intent;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;
import java.io.IOException;

import java.util.Date;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;



public class MenuActivity extends AppCompatActivity {
    File img;
    String imgFName;
    private static final String TAG = "imgInsert";
    public static final String PREFS_NAME = "MyPrefsFile";

    AmazonS3 s3;
    TransferUtility transferUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_activty);

        
        final Button bPic = (Button) findViewById(R.id.goToPic);
        
        final Button viewPic = (Button) findViewById(R.id.viewImg);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "YOUR_ID", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );

        s3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3, getApplicationContext());

        

        bPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }

        });

        viewPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToViewPic();
            }

        });
    }
    

    public void goToViewPic(){

        Intent intent = new Intent(this, ImgViewActivity.class);
        startActivity(intent);
    }
   
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;


    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

                Log.e("ERROR", ex.getMessage(), ex);

            }

            if (photoFile != null) {
                img = photoFile;
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            upload();
        }
    }


    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        Log.i(TAG, "Creating image file");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        imgFName = imageFileName;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        //img = image;
        Log.i(TAG, "Image created and returned");
        return image;
    }
   
   
    public void upload() {

        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy_hh:mm:ss");
        String DateToStr = format.format(curDate);

        String fName = "Joe";
        String lName = "Fuerst";

        String imgName = fName + lName + DateToStr;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("curImg", imgName);
        editor.commit();
        if (img == null){
            Log.i("ERROR", "The file is empty");
        }
        else {
            TransferObserver observer = transferUtility.upload(
                    "496demobucket",     // The bucket to upload to
                    imgName,    // The key for the uploaded object
                    img        // The file where the data to upload exists
            );
            observer.setTransferListener(new TransferListener() {
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                   
                    Log.i(TAG, "progress changed");
                }

                public void onStateChanged(int id, TransferState state) {
                    Log.i(TAG, "state changed");
                }

                public void onError(int id, Exception ex) {
                    Log.e("ERROR", ex.getMessage(), ex);
                }
            });

        }
    }
}
