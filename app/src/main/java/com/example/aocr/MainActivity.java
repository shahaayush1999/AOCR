package com.example.aocr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private SessionHandler session;
    private User user;

    ImageView displayImage;
    Button buttonImageFromGallery;
    Button buttonOpenContacts;
    Button buttonImageFromCamera;
    Button buttonPostData;
    Button logoutBtn;

    TextView displayStatus;
    EditText displayEmail;
    EditText displayPhone;
    EditText displayName;
    EditText displayCompanyName;
    EditText displayPosition;
    EditText displayCity;
    EditText displayPinCode;
    EditText displayWebsite;

    ProgressBar displayProgress;

    private static final int PICK_IMAGE = 100;
    static final int REQUEST_TAKE_PHOTO = 1;
    private Bitmap image;
    private TessBaseAPI mTess;
    private String currentPhotoPath;
    private String name, email, phone, pinCode, position, website, companyName, city;
    private static final String TAG = "MainActivity.java";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize session for user using shared pref
        session = new SessionHandler(getApplicationContext());
        user = session.getUserDetails();

        //Buttons
        buttonOpenContacts = findViewById(R.id.buttonOpenContacts);
        buttonImageFromGallery = findViewById(R.id.buttonOpenGallery);
        buttonImageFromCamera = findViewById(R.id.buttonOpenCamera);
        buttonPostData = findViewById(R.id.buttonPostData);
        logoutBtn = findViewById(R.id.btnLogout);
        //Image
        displayImage = findViewById(R.id.imageView);
        //Display field
        displayStatus = findViewById(R.id.statusBar);
        //Editable Display fields
        displayName = findViewById(R.id.editTextName);
        displayPhone = findViewById(R.id.editTextPhone);
        displayEmail = findViewById(R.id.editTextEmail);
        displayCompanyName = findViewById(R.id.editTextCompanyName);
        displayPosition = findViewById(R.id.editTextPosition);
        displayCity = findViewById(R.id.editTextCity);
        displayPinCode = findViewById(R.id.editTextPinCode);
        displayWebsite = findViewById(R.id.editTextWebsite); 
        //Progress Bar
        displayProgress = (ProgressBar) findViewById(R.id.progressBar);
        //set progress bar to be invisible
        displayProgress.setVisibility(View.GONE);

        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image0);

        //Clean apps past data
        cleanAppFiles();

        //initialize Tesseract API
        ImageProcessor.initialise(getApplicationContext());

        //import image from gallery
        buttonImageFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        //import image from Camera
        buttonImageFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        buttonPostData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PostDataAsyncTask().execute();
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.logoutUser();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        //Add the extracted info from Business Card to the phone's contacts...
        buttonOpenContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToContacts();
            }
        });
    }

    //todo add batch mode
    //todo make dialog to make user choose between click/pick image and replace both openCamera and openGallery button

    private void addToContacts() {

        // Creates a new Intent to insert a contact
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        // Sets the MIME type to match the Contacts Provider
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        //Checks if we have the name, email and phone number...
        if (displayName.getText().length() > 0 && (displayPhone.getText().length() > 0 || displayEmail.getText().length() > 0)) {
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
        } else {
            Toast.makeText(getApplicationContext(), R.string.toastNoInfoToAddToContact, Toast.LENGTH_LONG).show();
        }


    }

    private void openGallery() {
        Intent gallery =
                new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                // ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri imageUri;
            switch (requestCode) {
                case PICK_IMAGE:
                    imageUri = data.getData();
                    displayImage.setImageURI(imageUri);
                    try {
                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        imageLoadedRunOCR();
                    } catch (Exception e) {
                        e.printStackTrace();
                        displayStatus.setText(R.string.statusImageFailedToLoad);
                    }
                    break;
                case REQUEST_TAKE_PHOTO:
                    imageUri = Uri.parse(currentPhotoPath);
                    displayImage.setImageURI(imageUri);
                    try {
                        image = BitmapFactory.decodeFile(currentPhotoPath);
                        imageLoadedRunOCR();
                    } catch (Exception e) {
                        e.printStackTrace();
                        displayStatus.setText(R.string.statusImageFailedToLoad);
                    }
                    break;
            }

        }
    }

    private void imageLoadedRunOCR() {
        displayStatus.setText(R.string.statusImageLoadedRunOCR);
        clearFields();
        new ProcessImageAsyncTask().execute(image); //Can add array of image over here to pass to background image processing
    }

    //supports multiple images
    private class ProcessImageAsyncTask extends AsyncTask<Bitmap, Integer, Long> {
        protected void onPreExecute() {
            displayProgress.setVisibility(View.VISIBLE);
        }

        protected Long doInBackground(Bitmap... images) {
            int count = images.length;
            long totalImagesProcessed = 0;
            for (Bitmap i : images) {
                processImage(i);
                totalImagesProcessed += 1;
                publishProgress((int) ((float) totalImagesProcessed / count * 100));
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return totalImagesProcessed;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Long result) {
            displayProgress.setVisibility(View.GONE);
            displayStatus.setText(R.string.statusOCRResults);
        }
    }

    public void processImage(Bitmap OCRimage) {
        ImageProcessor image = new ImageProcessor(OCRimage);
        name = image.getName();
        email = image.getEmail();
        phone = image.getPhone();
        pinCode = image.getPinCode();
        city = image.getCity();
        position = image.getPosition();
        companyName = image.getCompanyName();
        website = image.getWebsite();
        setFields(name, email, phone, companyName, position, city, pinCode, website);
    }

    //todo check why clean files doesn't work
    private void cleanAppFiles() {
        File dir = new File("Android/data/com.example.aocr/files/Pictures");
        try {
            for (File child : dir.listFiles())
                child.delete();
            System.out.println("Successfully deleted all app files");
        } catch (NullPointerException e) {
            e.getMessage();
        }
    }

    private void setFields(String name, String email, String phone, String company, String position, String city, String pinCode, String website) {
        displayName.setText(name);
        displayEmail.setText(email);
        displayPhone.setText(phone);
        displayCompanyName.setText(company);
        displayPosition.setText(position);
        displayCity.setText(city);
        displayPinCode.setText(pinCode);
        displayWebsite.setText(website);
    }

    private void clearFields() {
        setFields("","","","", "","","", "");
    }

    public class PostDataAsyncTask extends AsyncTask<String, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            displayStatus.setText(R.string.statusAttemptingPostData);
            displayProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                postText();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String lengthOfFile) {
            displayStatus.setText(R.string.statusPostDataSuccessful);
            displayProgress.setVisibility(View.GONE);
        }
    }

    // this will post our text data
    //todo send userID in POSTDATA fn in mainactivity to backend
    private void postText(){
        try{
            // url where the data will be posted
            String postReceiverUrl = "http://10.0.2.2:8080/AOCR/insert.php";
            Log.v(TAG, "postURL: " + postReceiverUrl);

            // HttpClient
            HttpClient httpClient = new DefaultHttpClient();

            // post header
            HttpPost httpPost = new HttpPost(postReceiverUrl);

            //extract data from edittext to post data
            extractDataFromEditText();

            // add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("fullname", name));
            nameValuePairs.add(new BasicNameValuePair("number", phone));
            nameValuePairs.add(new BasicNameValuePair("email", email));
            nameValuePairs.add(new BasicNameValuePair("company", companyName));
            nameValuePairs.add(new BasicNameValuePair("position", position));
            nameValuePairs.add(new BasicNameValuePair("city", city));
            nameValuePairs.add(new BasicNameValuePair("pincode", pinCode));
            nameValuePairs.add(new BasicNameValuePair("website", website));
            nameValuePairs.add(new BasicNameValuePair("addedbyuserid", user.getUserID().toString()));


            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // execute HTTP post request
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();

            if (resEntity != null) {
                final String responseStr = EntityUtils.toString(resEntity).trim();
                Log.v(TAG, "Response: " +  responseStr);

                //run on UI thread to show toast
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, responseStr, Toast.LENGTH_LONG).show();
                    }
                });

            }

        } catch (Exception e) {
            displayStatus.setText(R.string.statusPostDataFail);
            e.printStackTrace();
        }
    }

    public void extractDataFromEditText() {
        name = displayName.getText().toString().toLowerCase();
        phone = displayPhone.getText().toString();
        email = displayEmail.getText().toString().toLowerCase();
        companyName = displayCompanyName.getText().toString().toLowerCase();
        position = displayPosition.getText().toString().toLowerCase();
        city = displayCity.getText().toString();
        pinCode = displayPinCode.getText().toString();
        website = displayWebsite.getText().toString().toLowerCase();
    }
}