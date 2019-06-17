package com.example.aocr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.content.res.AssetManager;
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
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
    private String datapath = "";
    private String currentPhotoPath;
    private String OCRResult, name, email, phone, pinCode, position, website, companyName, city;
    private static final String TAG = "MainActivity.java";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize session for user using shared pref
        session = new SessionHandler(getApplicationContext());
        user = session.getUserDetails();

        //Buttons
        buttonOpenContacts = (Button) findViewById(R.id.buttonOpenContacts);
        buttonImageFromGallery = (Button) findViewById(R.id.buttonOpenGallery);
        buttonImageFromCamera = (Button) findViewById(R.id.buttonOpenCamera);
        buttonPostData = (Button) findViewById(R.id.buttonPostData);
        logoutBtn = (Button) findViewById(R.id.btnLogout);
        //Image
        displayImage = (ImageView) findViewById(R.id.imageView);
        //Display field
        displayStatus = (TextView) findViewById(R.id.statusBar);
        //Editable Display fields
        displayName = (EditText) findViewById(R.id.editTextName);
        displayPhone = (EditText) findViewById(R.id.editTextPhone);
        displayEmail = (EditText) findViewById(R.id.editTextEmail);
        displayCompanyName = (EditText) findViewById(R.id.editTextCompanyName);
        displayPosition = (EditText) findViewById(R.id.editTextPosition);
        displayCity = (EditText) findViewById(R.id.editTextCity);
        displayPinCode = (EditText) findViewById(R.id.editTextPinCode);
        displayWebsite = (EditText) findViewById(R.id.editTextWebsite); 
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
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

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
    //todo make all data lowercase before storing in database
    //todo make cleaner function regex replaceall for extract* functions where params = (str, "<stuff to be removed from string>" and returns new string with removed unnecessary spaces and punctuation and replace " *\- *" with " \- "
    //todo make dialog to make user choose between click/pick image and replace both openCamera and openGallery button
    public String extractName(String str) {
        String returnString = "";
        System.out.println("Getting the Name");
        boolean matchFound = false;
        final String NAME_REGEX = "([A-Z]([A-Z]{2,}|\\.) +)([A-Z][A-Z]{2,}-?)";
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);
        if (m.find()) {
            matchFound = true;
            returnString = m.group();
        } else {
            final String NAME_REGEX2 = "([A-Z]([a-zA-Z]{2,}|\\.) +)([A-Z][a-zA-Z]{2,}-?)+$";
            p = Pattern.compile(NAME_REGEX2, Pattern.MULTILINE);
            m = p.matcher(str);
            if (m.find()) {
                matchFound = true;
                returnString = m.group();
            }
        }
        if (matchFound) {
            returnString = convertStringFirstLetterCap(returnString);
        } else {
            returnString = "";
        }
        System.out.println(returnString);
        return returnString;
    }

    public static String convertStringFirstLetterCap(String str) {
        // Create a char array of given String
        char ch[] = str.toCharArray();
        for (int i = 0; i < str.length(); i++) {
            // If first character of a word is found
            if (i == 0 && ch[i] != ' ' ||
                    ch[i] != ' ' && ch[i - 1] == ' ') {
                // If it is in lower-case
                if (ch[i] >= 'a' && ch[i] <= 'z') {
                    // Convert into Upper-case
                    ch[i] = (char) (ch[i] - 'a' + 'A');
                }
            }
            // If apart from first character
            // Any one is in Upper-case
            else if (ch[i] >= 'A' && ch[i] <= 'Z')
                // Convert into Lower-Case
                ch[i] = (char) (ch[i] + 'a' - 'A');
        }
        return new String(ch);
    }

    public String extractEmail(String str) {
        String returnString;
        System.out.println("Getting the email");
        final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            returnString = m.group();
        } else
            returnString = "";
        System.out.println(returnString);
        return returnString;
    }

    public String extractPhone(String str) {
        String returnString = "", tempstring = "";
        System.out.println("Getting Phone Number");
        boolean approachOneFail = true;
        //Approach 1: Using google phone number utility
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Iterable<PhoneNumberMatch> numberMatches = phoneNumberUtil.findNumbers(str, Locale.US.getCountry());
        ArrayList<String> numberList = new ArrayList<>();
        for (PhoneNumberMatch number : numberMatches) {
            String s = number.rawString();
            numberList.add(s);
        }
        if (!numberList.isEmpty()) {
            try {
                final String REGEX = "\\+91";
                Pattern p = Pattern.compile(REGEX, Pattern.MULTILINE);
                int matchFoundIndia = 0;
                for (String i : numberList) {
                    Matcher m = p.matcher(i);   // get a matcher object
                    if (m.find()) {
                        matchFoundIndia = 1;
                        returnString = i;
                        break;
                    }
                }
                if (matchFoundIndia == 0) {
                    returnString = numberList.get(0);
                }
                returnString = returnString.replaceAll("[^+\\d]", ""); //to remove brackets, spaces and hyphens
                System.out.println(returnString);
                approachOneFail = false;
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else
            returnString = "";

        //Approach 2 = Regex method to get phone number, search by removing all spaces from original string
        if (approachOneFail) {
            final String PHONE_REGEX = "\\+?\\d[\\d -]{8,12}\\d"; //Alternative regex (?:^|\D)(\d{3})[)\-. ]*?(\d{3})[\-. ]*?(\d{4})(?:$|\D)
            Pattern p = Pattern.compile(PHONE_REGEX, Pattern.MULTILINE);
            tempstring = str;
            tempstring = tempstring.replaceAll(" ", "");
            Matcher m = p.matcher(tempstring);   // get a matcher object
            if (m.find()) {
                returnString = m.group();
                returnString = returnString.replaceAll("[()-]", ""); //to remove brackets and hyphens
                System.out.println(returnString);
            } else
                returnString = "";
        }
        return returnString;
    }

    public String extractPinCode(String str) {
        String returnString = "";
        System.out.println("Getting PinCode");
        final String PINCODE_REGEX = " [1-9][0-9]{2} ?[0-9]{3}";
        Pattern p = Pattern.compile(PINCODE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            returnString = m.group();
            System.out.println(returnString);
        } else
            returnString = "";
        return returnString;
    }

    public String extractJobTitle(String str) {
        String returnString = "";
        String[] jobTitlesRegex = {"C[A-Z]O", "Founder", "Consult", "Human", "Manager", "Director", "President", "Director", "Chairman", "Senior", "Head", "Chief", "Officer", "Owner", "General", "Deputy", "Assistant", "Leader", "Staff"};
        System.out.println("Getting the Job Title");
        for (String TITLE_REGEX : jobTitlesRegex) {
            Pattern p = Pattern.compile("^.*" + TITLE_REGEX + "([A_Za-z]* ){0,2}$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(str);   // get a matcher object
            if (m.find()) {
                returnString = convertStringFirstLetterCap(m.group());
                System.out.println(returnString);
                break;
            } else
                returnString = "";
        }
        return returnString;
    }

    //todo extract company name from email string. Run validation for gmail/yahoo/outlook/msn etc
    public String extractCompanyName(String str) {
        return "";
    }

    //todo extract city from 1.Pincode, 2.Set of cities
    public String extractCity(String Str) {
        return "";
    }

    //todo doesn't work, try to recheck
    public String extractWebsite(String str) {
        String returnString;
        System.out.println("Getting the website");
        final String WEBSITE_REGEX = "/^(?:(?:ht|f)tp(?:s?)\\:\\/\\/|~\\/|\\/)?(?:\\w+:\\w+@)?((?:(?:[-\\w\\d{1-3}]+\\.)+(?:com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|edu|co\\.uk|ac\\.uk|it|fr|tv|museum|asia|local|travel|[a-z]{2}))|((\\b25[0-5]\\b|\\b[2][0-4][0-9]\\b|\\b[0-1]?[0-9]?[0-9]\\b)(\\.(\\b25[0-5]\\b|\\b[2][0-4][0-9]\\b|\\b[0-1]?[0-9]?[0-9]\\b)){3}))(?::[\\d]{1,5})?(?:(?:(?:\\/(?:[-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?(?:(?:\\?(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(?:&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(?:#(?:[-\\w~!$ |\\/.,*:;=]|%[a-f\\d]{2})*)?$/i;";
        Pattern p = Pattern.compile(WEBSITE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            returnString = m.group();
        } else
            returnString = "";
        System.out.println(returnString);
        return returnString;
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/eng.traineddata";
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
        OCRimage = scaleBitmap(OCRimage);
        mTess.setImage(OCRimage);
        OCRResult = mTess.getUTF8Text();
        System.out.println("Printing raw OCR output\n" + OCRResult + "\nRaw OCR output ends here\n");
        extractDataFromEditText();
        setFields(name, email, phone, companyName, position, city, pinCode, website);
    }

    //todo use company name from the value returned from email and website
    private void extractDataFromEditText() {
        name = extractName(OCRResult);
        email = extractEmail(OCRResult);
        phone = extractPhone(OCRResult);
        pinCode = extractPinCode(OCRResult);
        city = extractCity(OCRResult);
        position = extractJobTitle(OCRResult);
        companyName = extractCompanyName(OCRResult);
        website = extractWebsite(OCRResult);
    }

    private Bitmap scaleBitmap(Bitmap yourBitmap) {
        float scale, width = yourBitmap.getWidth(), height = yourBitmap.getHeight();
        scale = (float) Math.sqrt(720*1280 / (width * height));
        Bitmap resized = Bitmap.createScaledBitmap(yourBitmap, (int) (width * scale), (int) (height * scale), true);
        return resized;
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

    private void setStatus(String status) {
        displayStatus.setText(status);
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

    public boolean isConnectedToServer(String url, int timeout) {
        try{
            URL myUrl = new URL(url);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return true;
        } catch (Exception e) {
            // Handle your exceptions
            return false;
        }
    }
}