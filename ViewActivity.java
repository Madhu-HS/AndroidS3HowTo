package com.tl.joe.toplist;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.widget.ImageView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImgViewActivity extends AppCompatActivity {

    String imgFName;
    File iFile = null;

    private static final String TAG = "image view";
    public static final String PREFS_NAME = "MyPrefsFile";
    AmazonS3 s3;
    TransferUtility transferUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_view);

        final ImageView imgView = (ImageView) findViewById(R.id.imgView);
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "YOUR_ID", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );

        s3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3, getApplicationContext());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String img = prefs.getString("curImg", null);



        try {
            iFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Log.e("ERROR", ex.getMessage(), ex);

        }
        
        TransferObserver observer = transferUtility.download(
                "496demobucket",     /* The bucket to download from */
                img,    /* The key for the object to download */
                iFile        /* The file to download the object to */
        );
        observer.setTransferListener(new TransferListener() {
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                // update progress bar
                //progressBar.setProgress(bytesCurrent);
                Log.i(TAG, "progress changed");
            }

            public void onStateChanged(int id, TransferState state) {
                Bitmap myBitmap = BitmapFactory.decodeFile(iFile.getAbsolutePath());
                Bitmap fin = rotateBitmap(myBitmap, 90);
                imgView.setImageBitmap(fin);
            }

            public void onError(int id, Exception ex) {
                Log.e("ERROR", ex.getMessage(), ex);
            }
        });

    }

    String mCurrentPhotoPath;

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

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
}