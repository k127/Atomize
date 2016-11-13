package com.wrmndfzy.atomize;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.wrmndfzzy.pngquant.LibPngQuant;

public class MainActivity extends AppCompatActivity {

    private TextView imgPath;
    private ImageView preView;
    private static final int SELECT_PICTURE = 1;
    public static String selectedImagePath;
    public static String imagePath;
    public static String gone = "image does not exist";
    private boolean imgSelected = false;
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    File extFolder = new File(Environment.getExternalStorageDirectory() + "/Atomize");
    File tmpFolder = new File(Environment.getExternalStorageDirectory() + "/Atomize/tmp");

    private Switch deleteSwitch;

    private Button atomButton;

    private ProgressBar quantProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgPath = (TextView) findViewById(R.id.imgPath);
        final Button select = (Button) findViewById(R.id.select);
        deleteSwitch = (Switch) findViewById(R.id.deleteSwitch);

        atomButton = (Button) findViewById(R.id.atomize);

        quantProgress = (ProgressBar) findViewById(R.id.progBar);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length <= 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Read permissions are required to run this app.", Toast.LENGTH_LONG).show();
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory( Intent.CATEGORY_HOME );
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length <= 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Write permissions are required to run this app.", Toast.LENGTH_LONG).show();
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory( Intent.CATEGORY_HOME );
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                }
            }
        }
    }

    //Thanks: http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {

                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(this, selectedImageUri);
                imagePath = handleImageType(selectedImagePath);
                String selectedImageLocation = "Selected Image Path: " + selectedImagePath;

                if (imagePath.equals(gone)) {
                    Toast.makeText(MainActivity.this, "Selected image has either been\n" +
                            "deleted or already Atomized.", Toast.LENGTH_LONG).show();
                }
                if (imagePath != null) {
                    imgSelected = true;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        Log.d("imgSelected", String.valueOf(bitmap));
                        preView = (ImageView) findViewById(R.id.imgPreview);
                        preView.setImageBitmap(bitmap);
                        imgPath.setText(selectedImageLocation);
                        preView.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select a valid image.", Toast.LENGTH_LONG).show();
                    SystemClock.sleep(1000);
                    Toast.makeText(MainActivity.this, "Valid image types include:\n" +
                            "PNG, JPEG, PJPEG, and BMP", Toast.LENGTH_LONG).show();
                    imgSelected = false;
                }
            }
        }
    }

    // listen for atomize once picture is chosen
    public void atomize(View v) {
        if(imgSelected){
            if(!extFolder.exists() && !extFolder.isDirectory()){
                try{
                    if (!extFolder.mkdirs())
                        Log.e("extFolder", "directory cannot be created");
                }
                catch(Exception e){
                    //fileProbDialog();
                }
            }
            new AsyncTask<Object, Object, Void>() {
                @Override
                protected Void doInBackground(Object... params) {
                    quantize();
                    return null;
                }
                @Override
                protected void onPreExecute(){
                    Toast.makeText(MainActivity.this, "Atomizing...", Toast.LENGTH_SHORT).show();
                    quantProgress.setVisibility(View.VISIBLE);
                    atomButton.setEnabled(false);
                    atomButton.setAlpha(0.4f);
                }
                @Override
                protected void onPostExecute(Void v){
                    Log.d("quantize", "quantize done");
                    String noImgText = "No image selected.";
                    quantProgress.setVisibility(View.INVISIBLE);
                    preView.setVisibility(View.GONE);
                    imgPath.setText(noImgText);
                    Toast.makeText(MainActivity.this, "Done!", Toast.LENGTH_SHORT).show();
                    imagePath = "";
                    selectedImagePath = "";
                    imgSelected = false;
                    atomButton.setEnabled(true);
                    atomButton.setAlpha(1.0f);
                }
            }.execute();
        }
        else{
            Toast.makeText(MainActivity.this, "Please select an image.", Toast.LENGTH_SHORT).show();
        }
    }

    public void quantize() {
        File input = new File(imagePath);
        String imageName = input.getName();
        File output = new File(extFolder + "/" + imageName);
        boolean converted = false;
        //Convert image if necessary, return if failed
        if (!imagePath.equals(selectedImagePath)) {
            if (!convertImage(selectedImagePath, imagePath)) {
                Log.e("output", "exists, but cannot be deleted");
                return;
            }
            converted = true;
        }
        //If output already exists, delete it. If we can't delete
        //it, interrupt the thread and log the error.
        if (output.exists()) {
            if (!output.delete()) {
                Log.e("output", "exists, but cannot be deleted");
                Thread.currentThread().interrupt();
            }
        }
        new LibPngQuant().pngQuantFile(input, output);
        if (deleteSwitch.isChecked() || converted) {
            if (!input.delete())
                Log.e("input", "cannot be deleted");
        }
    }

    // File path methods taken from aFileChooser, thanks to iPaulPro: https://github.com/iPaulPro/aFileChooser
    public static String getPath(final Context context, final Uri uri) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type))
                    return Environment.getExternalStorageDirectory() + "/" + split[1];

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type))
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("video".equals(type))
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                else if ("audio".equals(type))
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme()))
            return getDataColumn(context, uri, null, null);
            // File
        else if ("file".equalsIgnoreCase(uri.getScheme()))
            return uri.getPath();

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    //Checks if an image is a PNG, converts valid formats if not.
    public String handleImageType(String path) {

        String type = getFileType(path);

        if ("png".equals(type)) {
            return path;
        } else if ("jpeg".equals(type) || "jpg".equals(type) || "pjpeg".equals(type) || "bmp".equals(type)) {
            File input = new File(path);
            String imageName = input.getName();
            path = tmpFolder + "/" + imageName + ".png";
            return path;
        } else if (gone.equals(type)) {
            return gone;
        } else {
            return null;
        }

    }

    public static String getFileType(String path) {
        File input = new File(path);
        if (!input.exists())
            return gone;
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        Log.d("getFileType", extension);
        return extension;
    }

    public boolean convertImage(String path, String outPath) {

        if(!tmpFolder.exists() && !tmpFolder.isDirectory()){
            if (!tmpFolder.mkdirs())
                Log.e("tmpFolder", "directory cannot be created");
        }

        try {
            File orig = new File(path);
            Bitmap bmp = BitmapFactory.decodeFile(path);
            FileOutputStream out = new FileOutputStream(outPath);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); //100-best quality
            out.close();
            Log.d("convertImage", "converted " + path + " to " + outPath);
            if (deleteSwitch.isChecked()) {
                if (!orig.delete())
                    Log.e("orig", "cannot be deleted");

                Log.d("switch", "checked");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
