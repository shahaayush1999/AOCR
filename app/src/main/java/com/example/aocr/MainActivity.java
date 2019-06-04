package com.example.aocr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

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
import android.os.Bundle;

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

public class MainActivity extends AppCompatActivity {
    ImageView displayImage;
    Button imageFromGallery;
    Button openContacts;
    Button imageFromCamera;
    TextView displayText;
    EditText displayEmail;
    EditText displayPhone;
    EditText displayName;
    ProgressBar displayProgress;

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE = 100;
    static final int REQUEST_TAKE_PHOTO = 1;
    private Bitmap image;
    private TessBaseAPI mTess;
    private String datapath = "";
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Buttons
        openContacts = (Button) findViewById(R.id.buttonOpenContacts);
        imageFromGallery = (Button) findViewById(R.id.buttonOpenGallery);
        imageFromCamera = (Button) findViewById(R.id.buttonOpenCamera);
        //Image
        displayImage = (ImageView) findViewById(R.id.imageView);
        //Display field
        displayText = (TextView) findViewById(R.id.statusBar);
        //Editable Display fields
        displayName = (EditText) findViewById(R.id.editTextName);
        displayPhone = (EditText) findViewById(R.id.editTextPhone);
        displayEmail = (EditText) findViewById(R.id.editTextEmail);
        //Progress Bar
        displayProgress = (ProgressBar) findViewById(R.id.progressBar);
        //set progress bar to be invisible
        displayProgress.setVisibility(View.GONE);

        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image0);

        //Clean apps past data
        cleanAppFiles();

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
        final String NAME_REGEX = "([A-Z]([a-zA-Z]*|\\.) +){1,2}([A-Z][a-zA-Z]+-?)+$";
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m =  p.matcher(str);
        if(m.find()){
            System.out.println(m.group());
            displayName.setText(m.group());
        }
    }

    public void extractEmail(String str) {
        System.out.println("Getting the email");
        final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if(m.find()){
            System.out.println(m.group());
            displayEmail.setText(m.group());
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
            displayPhone.setText(str);
        }
    }

    public void extractAddress(String str) {

    }

    public void extractPinCode(String str) {
        System.out.println("Getting PinCode");
        final String PINCODE_REGEX="^[1-9][0-9]{5}$";
        Pattern p = Pattern.compile(PINCODE_REGEX, Pattern.MULTILINE);
        //comment next two lines if anything breaks
        str = str.replaceAll(" ", "");
        Matcher m = p.matcher(str);   // get a matcher object
        if(m.find()){
            str = m.group();
            str = str.replaceAll("[()-]", ""); //to remove brackets and hyphens
            System.out.println(str);
            //displayPincode.setText(str); //change this to use new edittexts, create new ones on the fly
        }
    }

    public void extractJobTitle(String str) {

    }

    public void extractCompanyName(String str) {

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
                case PICK_IMAGE: //the ^ operator is exclusive or operator
                    imageUri = data.getData();
                    displayImage.setImageURI(imageUri);
                    try {
                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        displayText.setText(R.string.statusImageLoadedRunOCR);
                        displayName.setText("");
                        displayEmail.setText("");
                        displayPhone.setText("");
                        displayProgress.setVisibility(View.VISIBLE);
                        new ProcessImageTask().execute(image); //Can add array of image over here to pass to background image processing
                    } catch (Exception e) {
                        e.printStackTrace();
                        displayText.setText(R.string.statusImageFailedToLoad);
                        displayName.setText("");
                        displayEmail.setText("");
                        displayPhone.setText("");
                    } finally {
                    }
                    break;
                case REQUEST_TAKE_PHOTO:
                    imageUri = Uri.parse(currentPhotoPath);
                    displayImage.setImageURI(imageUri);
                    try {
                        image = BitmapFactory.decodeFile(currentPhotoPath);
                        displayText.setText(R.string.statusImageLoadedRunOCR);
                        displayName.setText("");
                        displayEmail.setText("");
                        displayPhone.setText("");
                        displayProgress.setVisibility(View.VISIBLE);
                        new ProcessImageTask().execute(image); //Can add array of image over here to pass to background image processing
                    } catch (Exception e) {
                        e.printStackTrace();
                        displayText.setText(R.string.statusImageFailedToLoad);
                        displayName.setText("");
                        displayEmail.setText("");
                        displayPhone.setText("");
                    } finally {
                    }
                    break;
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
        //extractPinCode(OCRresult);
        //extractAddress(OCRresult);
        //extractCompanyName(OCRresult);
    }

    private void cleanAppFiles() {
        File dir = new File("Android/data/com.example.aocr/files/Pictures");
        try {
            for (File child : dir.listFiles())
                child.delete();
        }
        catch(NullPointerException e) {
            e.getMessage();
        }
    }
}