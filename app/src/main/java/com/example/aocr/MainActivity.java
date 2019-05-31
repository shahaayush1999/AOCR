package com.example.aocr;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.MediaStore;
import android.net.Uri;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";

    ImageView displayImage;
    Button runOCR;
    Button imageFromGallery;
    Button openContacts;
    Button imageFromCamera;
    TextView displayText;
    EditText displayEmail;
    EditText displayPhone;
    EditText displayName;
    ProgressBar displayProgress;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String mCurrentPhotoPath;
    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Buttons
        runOCR = (Button) findViewById(R.id.textView);
        openContacts = (Button) findViewById(R.id.textView6);
        imageFromGallery = (Button) findViewById(R.id.textView7);
        imageFromCamera = (Button) findViewById(R.id.button);
        //Image
        displayImage = (ImageView) findViewById(R.id.imageView);
        //Display field
        displayText = (TextView) findViewById(R.id.textView2);
        //Editable Display fields
        displayName = (EditText) findViewById(R.id.textView3);
        displayPhone = (EditText) findViewById(R.id.textView4);
        displayEmail = (EditText) findViewById(R.id.textView5);
        //Progress Bar
        displayProgress = (ProgressBar) findViewById(R.id.progressBar2);
        //set progress bar to be invisible
        displayProgress.setVisibility(View.GONE);

        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image0);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        //import image from gallery
        imageFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        //import image from Camera
        imageFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        //run the OCR on the test_image...
        runOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayProgress.setVisibility(View.VISIBLE);
                new ProcessImageTask().execute(image); //Can add array of image over here to pass to background image processing
            }
        });

        //Add the extracted info from Business Card to the phone's contacts...
        openContacts.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                addToContacts();
            }
        });
    }

    public void extractName(String str) {
        System.out.println("Getting the Name");
        final String NAME_REGEX = "^([A-Z]([a-z]*|\\.) *){1,2}([A-Z][a-z]+-?)+$"; //og regex
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m =  p.matcher(str);
        if(m.find()){
            System.out.println(m.group());
            displayName.setText(m.group()); //change this to use new edittexts, create new ones on the fly
        }
    }

    public void extractEmail(String str) {
        System.out.println("Getting the email");
        final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if(m.find()){
            System.out.println(m.group());
            displayEmail.setText(m.group()); //change this to use new edittexts, create new ones on the fly
        }
    }

    public void extractPhone(String str){
        System.out.println("Getting Phone Number");
        final String PHONE_REGEX="\\+?\\d[\\d -]{8,12}\\d"; //original regex (?:^|\D)(\d{3})[)\-. ]*?(\d{3})[\-. ]*?(\d{4})(?:$|\D)
        Pattern p = Pattern.compile(PHONE_REGEX, Pattern.MULTILINE);
        //comment next two lines if anything breaks
        str = str.replaceAll(" ", "");
        Matcher m = p.matcher(str);   // get a matcher object
        if(m.find()){
            str = m.group();
            str = str.replaceAll("[()-]", ""); //to remove brackets and hyphens
            System.out.println(str);
            displayPhone.setText(str); //change this to use new edittexts, create new ones on the fly
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToContacts(){

        // Creates a new Intent to insert a contact
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        // Sets the MIME type to match the Contacts Provider
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        //Checks if we have the name, email and phone number...
        if(displayName.getText().length() > 0 && ( displayPhone.getText().length() > 0 || displayEmail.getText().length() > 0 )){
            //Adds the name...
            intent.putExtra(ContactsContract.Intents.Insert.NAME, displayName.getText().toString());

            //Adds the email...
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, displayEmail.getText().toString());
            //Adds the email as Work Email
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);

            //Adds the phone number...
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, displayPhone.getText().toString());
            //Adds the phone number as Work Phone
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);

            //starting the activity...
            startActivity(intent);
        }else{
            Toast.makeText(getApplicationContext(), R.string.toastNoInfoToAddToContact, Toast.LENGTH_LONG).show();
        }


    }

    private void openGallery() {
        Intent gallery =
                new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    //doesnt work, fix/replace
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i(TAG, "IOException");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    //doesnt work, fix/replace
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            displayImage.setImageURI(imageUri);
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                displayText.setText(R.string.statusImageLoadedRunOCR);
            } catch (Exception e) {
                displayText.setText(R.string.statusImageFailedToLoad);
            }
            finally {
                displayName.setText("");
                displayEmail.setText("");
                displayPhone.setText("");
            }
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
                displayImage.setImageBitmap(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //supports multiple images
    private class ProcessImageTask extends AsyncTask<Bitmap, Integer, Long> {
        protected void onPreExecute(Long result) {
        }

        protected Long doInBackground(Bitmap... images) {
            int count = images.length;
            long totalImagesProcessed = 0;
            for (Bitmap i : images ) {
                processImage(i);
                totalImagesProcessed += 1;
                publishProgress((int) ((float)totalImagesProcessed / count * 100));
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return totalImagesProcessed;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Long result) {
            displayProgress.setVisibility(View.GONE);
            displayText.setText(R.string.statusOCRResults);
        }
    }

    public void processImage(Bitmap OCRimage) {
        String OCRresult;
        mTess.setImage(OCRimage);
        OCRresult = mTess.getUTF8Text();
        System.out.println(OCRresult);
        extractName(OCRresult);
        extractEmail(OCRresult);
        extractPhone(OCRresult);
    }
}