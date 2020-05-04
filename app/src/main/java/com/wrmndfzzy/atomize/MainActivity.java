package com.wrmndfzzy.atomize;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.support.design.widget.Snackbar;

import java.io.File;
import java.io.IOException;

import com.wrmndfzzy.pngquant.LibPngQuant;

public class MainActivity extends AppCompatActivity {

    private TextView imgPath;
    private ImageView preView;
    private static final int SELECT_PICTURE = 1;
    private static String selectedImagePath;
    private static String gone = "image does not exist";
    private boolean imgSelected = false;
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    File extFolder = new File(Environment.getExternalStorageDirectory() + "/Atomize");

    private Switch deleteSwitch;

    private FloatingActionButton atomButton;

    File input;
    File output;
    private EditText fnEdit;
    public String imageName;

    private ProgressBar quantProgress;

    private String wrongFileType="wrongfiletype";

    Button fnDialogCancel;
    Button fnDialogConfirm;

    public static Activity mA;

    private String sizeBefore, sizeAfter;

    public static Activity getInstance(){
        return mA;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mA = this;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);
                //  If the activity has never started before...
                if (isFirstStart) {
                    //  Launch app intro
                    Intent i = new Intent(MainActivity.this, com.wrmndfzzy.atomize.intro.IntroActivity.class);
                    startActivity(i);
                }
            }
        });
        // Start the thread
        t.start();

        setContentView(R.layout.activity_main);

        imgPath = (TextView) findViewById(R.id.imgPath);
        preView = (ImageView) findViewById(R.id.imgPreview);


        deleteSwitch = (Switch) findViewById(R.id.deleteSwitch);

        atomButton = (FloatingActionButton) findViewById(R.id.atomize);

        quantProgress = (ProgressBar) findViewById(R.id.progBar);

        preView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent();
                intent.setType("image/png");
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
                    Snackbar.make(findViewById(android.R.id.content), "Read permissions are required to run this app.", Snackbar.LENGTH_LONG).show();
                    /*Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory( Intent.CATEGORY_HOME );
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    MainActivity.this.finish();*/
                }
                else{
                    Snackbar.make(findViewById(android.R.id.content), "Read permissions granted!", Snackbar.LENGTH_LONG).show();
                }
                break;
            }

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length <= 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(findViewById(android.R.id.content), "Write permissions are required to run this app.", Snackbar.LENGTH_LONG).show();
                    /*Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory( Intent.CATEGORY_HOME );
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    MainActivity.this.finish();*/
                }
                else{
                    Snackbar.make(findViewById(android.R.id.content), "Write permissions granted!", Snackbar.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    //Thanks: http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String invSel = "Invalid selection.";
        String invFile = "Invalid file type.";
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(this, selectedImageUri);
                if (selectedImagePath != null) {
                    String imageType = handleImageType(selectedImagePath);
                    String selectedImageLocation = "Selected Image Path: " + selectedImagePath;
                    if (imageType.equals(gone)) {
                        Snackbar.make(findViewById(android.R.id.content), "Selected image has either been\n" +
                                "deleted or already Atomized.", Snackbar.LENGTH_LONG).show();
                        imgPath.setText(invSel);
                        preView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.atom_watermark));
                        imgSelected = false;
                    } else if (imageType.equals(wrongFileType)) {
                        Snackbar.make(findViewById(android.R.id.content), "Please select a valid PNG file.", Snackbar.LENGTH_LONG).show();
                        imgPath.setText(invFile);
                        preView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.atom_watermark));
                        imgSelected = false;
                    } else {
                        imgSelected = true;
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            Log.d("imgSelected", String.valueOf(bitmap));
                            sizeBefore = getPrettyImageFileSize(selectedImageUri);
                            preView.setImageBitmap(bitmap);
                            imgPath.setText(String.format("%s\n%s", selectedImageLocation, sizeBefore));
                            atomButton.setImageDrawable(getDrawable(R.drawable.check));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    Snackbar.make(findViewById(android.R.id.content), "Invalid File Path.", Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private String getPrettyImageFileSize(Uri uri) {
        String prettyImgFileSize = "";
        AssetFileDescriptor afd = null;
        try {
            afd = getContentResolver().openAssetFileDescriptor(uri, "r");
            if (afd != null) {
                prettyImgFileSize = android.text.format.Formatter.formatShortFileSize(this, afd.getLength());
                Log.v("getPrettyImageFileSize", prettyImgFileSize);
                afd.close();
            }
        } catch (IOException e) {
            Log.e("getPrettyImageFileSize", e.getMessage());
        }
        return prettyImgFileSize;
    }

    // listen for atomize once picture is chosen
    public void atomize(View v) {
        if(imgSelected){
            if(!extFolder.exists() && !extFolder.isDirectory()){
                try{
                    if(!extFolder.mkdirs())
                        Log.e("extFolder", "directory cannot be create");
                }
                catch(Exception e){
                    //fileProbDialog();
                }
            }
            input = new File(selectedImagePath);
            fileNameDialog();
        } else if (output != null) {
            this.shareAtomizedImage(output);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Please select an image.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void shareAtomizedImage(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivity(Intent.createChooser(intent, "Share atomized image"));
    }

    public void execQuantTask(){
        output = new File(extFolder + "/" + imageName);
        new AsyncTask<Object, Object, Void>() {
            @Override
            protected Void doInBackground(Object... params) {
                quantize();
                return null;
            }
            @Override
            protected void onPreExecute(){
                Snackbar.make(findViewById(android.R.id.content), "Atomizing...", Snackbar.LENGTH_SHORT).show();
                quantProgress.setVisibility(View.VISIBLE);
                preView.setEnabled(false);
                deleteSwitch.setEnabled(false);
                atomButton.setEnabled(false);
                deleteSwitch.setAlpha(0.4f);
                atomButton.setAlpha(0.4f);
            }
            @Override
            protected void onPostExecute(Void v){
                Log.d("quantize", "quantize done");
                sizeAfter = getPrettyImageFileSize(Uri.fromFile(output));
                String noImgText = String.format("%s\n%s\n%s",
                        getString(R.string.atomize_successful),
                        String.format(getString(R.string.fmt_imgfilesize_before_after), sizeBefore, sizeAfter),
                        getString(R.string.tap_to_select_another_image));
                Snackbar.make(findViewById(android.R.id.content), "Done! Saved in /sdcard/Atomize.", Snackbar.LENGTH_SHORT).show();
                quantProgress.setVisibility(View.INVISIBLE);
                preView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.atom_watermark));
                imgPath.setText(noImgText);
                selectedImagePath = "";
                imgSelected = false;
                preView.setEnabled(true);
                deleteSwitch.setEnabled(true);
                atomButton.setEnabled(true);
                deleteSwitch.setAlpha(1.0f);
                atomButton.setAlpha(1.0f);

                // replace btn icon
                atomButton.setImageDrawable(getDrawable(R.drawable.share));
            }
        }.execute();
    }

    public void quantize() {
        //If output already exists, delete it. If we can't delete
        //it, interrupt the thread and log the error.
        if (output.exists()) {
            if (!output.delete()) {
                Log.e("output", "exists, but cannot be deleted");
                Thread.currentThread().interrupt();
            }
        }
        new LibPngQuant().pngQuantFile(input, output);
        if (deleteSwitch.isChecked()) {
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

    public String handleImageType(String path) {

        String type = getFileType(path);
        String pngType ="png";

        if (pngType.equals(type)) {
            return pngType;
        }else if (gone.equals(type)) {
            return gone;
        } else {
            return wrongFileType;
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

    protected void fileNameDialog(){
        final Dialog fnDialog = new Dialog(MainActivity.this);
        fnDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        fnDialog.setTitle("File Name");
        fnDialog.setContentView(R.layout.filename_dialog);
        fnEdit = (EditText) fnDialog.findViewById(R.id.fnDialogTextBox);
        imageName = input.getName();
        fnEdit.setText(imageName);
        fnDialogCancel = (Button) fnDialog.findViewById(R.id.fnDialogCancel);
        fnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fnDialog.dismiss();
            }
        });
        fnDialogConfirm = (Button) fnDialog.findViewById(R.id.fnDialogConfirm);
        fnDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((fnEdit.getText().toString().indexOf(".png") == (fnEdit.getText().toString().length()-4)) && fnEdit.getText().toString().length() != 3){
                    imageName = fnEdit.getText().toString();
                }
                else if(fnEdit.getText().toString().length() == 3){
                    imageName = fnEdit.getText().toString() + ".png";
                }
                else if(fnEdit.getText().toString().length() < 1){
                    imageName = input.getName();
                }
                else{
                    imageName = fnEdit.getText().toString() + ".png";
                }
                Log.d("imageName", imageName);
                fnDialog.dismiss();
                execQuantTask();
            }
        });
        fnDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_aboutlink:
                Intent i = new Intent(this, AboutActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
