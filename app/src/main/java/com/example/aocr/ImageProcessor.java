package com.example.aocr;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageProcessor {

    private Bitmap OCRimage;
    private String name;
    private String pincode;
    private String email;
    private String phone;
    private String position;
    private String company;
    private String city;
    private String website;
    private String OCRResult;
    private static TessBaseAPI mTess;
    private static String datapath = "";

    public static void initialise(Context myContext) {
        String language = "eng";
        datapath = myContext.getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"), myContext);
        mTess.init(datapath, language);
    }

    public ImageProcessor(Bitmap image) {
        OCRimage = scaleBitmap(image);
        mTess.setImage(OCRimage);
        OCRResult = mTess.getUTF8Text();
        System.out.println("Printing raw OCR output\n" + OCRResult + "\nRaw OCR output ends here\n");

        name = extractName();
        phone = extractPhone();
        email = extractEmail();
        company = extractCompanyName();
        position = extractPosition();
        pincode = extractPinCode();
        website = extractWebsite();
        city = extractCity();
    }

    public String getName() {
        return name;
    }
    public String getPhone() {
        return phone;
    }
    public String getEmail() {
        return email;
    }
    public String getCompanyName() {
        return company;
    }
    public String getPosition() {
        return position;
    }
    public String getPinCode() {
        return pincode;
    }
    public String getCity() {
        return city;
    }
    public String getWebsite() {
        return website;
    }

    //todo make all data lowercase before storing in database and names camel case
    //todo make cleaner function regex replaceall for extract* functions where params = (str, "<stuff to be removed from string>" and returns new string with removed unnecessary spaces and punctuation and replace " *\- *" with " \- "
    private String extractName() {
        String returnString = "";
        System.out.println("Getting the Name");
        boolean matchFound = false;
        final String NAME_REGEX = "([A-Z]([A-Z]{2,}|\\.) +)([A-Z][A-Z]{2,}-?)";
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(OCRResult);
        if (m.find()) {
            matchFound = true;
            returnString = m.group();
        } else {
            final String NAME_REGEX2 = "([A-Z]([a-zA-Z]{2,}|\\.) +)([A-Z][a-zA-Z]{2,}-?)+$";
            p = Pattern.compile(NAME_REGEX2, Pattern.MULTILINE);
            m = p.matcher(OCRResult);
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

    private static String convertStringFirstLetterCap(String str) {
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

    private String extractEmail() {
        String returnString;
        System.out.println("Getting the email");
        final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(OCRResult);   // get a matcher object
        if (m.find()) {
            returnString = m.group();
        } else
            returnString = "";
        System.out.println(returnString);
        return returnString;
    }

    private String extractPhone() {
        String returnString = "", tempstring = "";
        System.out.println("Getting Phone Number");
        boolean approachOneFail = true;
        //Approach 1: Using google phone number utility
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Iterable<PhoneNumberMatch> numberMatches = phoneNumberUtil.findNumbers(OCRResult, Locale.US.getCountry());
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
            tempstring = OCRResult;
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

    private String extractPinCode() {
        String returnString = "";
        System.out.println("Getting PinCode");
        final String PINCODE_REGEX = " [1-9][0-9]{2} ?[0-9]{3}";
        Pattern p = Pattern.compile(PINCODE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(OCRResult);   // get a matcher object
        if (m.find()) {
            returnString = m.group();
            System.out.println(returnString);
        } else
            returnString = "";
        return returnString;
    }

    private String extractPosition() {
        String returnString = "";
        String[] PositionRegex = {"C[A-Z]O", "Founder", "Consult", "Human", "Manager", "Director", "President", "Director", "Chairman", "Senior", "Head", "Chief", "Officer", "Owner", "General", "Deputy", "Assistant", "Leader", "Staff"};
        System.out.println("Getting the Job Title");
        for (String TITLE_REGEX : PositionRegex) {
            Pattern p = Pattern.compile("^.*" + TITLE_REGEX + "([A_Za-z]* ){0,2}$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(OCRResult);   // get a matcher object
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
    private String extractCompanyName() {
        return "";
    }

    //todo extract city from 1.Pincode, 2.Set of cities
    private String extractCity() {
        return "";
    }

    //todo doesn't work, try to recheck
    private String extractWebsite() {
        String returnString;
        System.out.println("Getting the website");
        final String WEBSITE_REGEX = "/^(?:(?:ht|f)tp(?:s?)\\:\\/\\/|~\\/|\\/)?(?:\\w+:\\w+@)?((?:(?:[-\\w\\d{1-3}]+\\.)+(?:com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|edu|co\\.uk|ac\\.uk|it|fr|tv|museum|asia|local|travel|[a-z]{2}))|((\\b25[0-5]\\b|\\b[2][0-4][0-9]\\b|\\b[0-1]?[0-9]?[0-9]\\b)(\\.(\\b25[0-5]\\b|\\b[2][0-4][0-9]\\b|\\b[0-1]?[0-9]?[0-9]\\b)){3}))(?::[\\d]{1,5})?(?:(?:(?:\\/(?:[-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?(?:(?:\\?(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(?:&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(?:#(?:[-\\w~!$ |\\/.,*:;=]|%[a-f\\d]{2})*)?$/i;";
        Pattern p = Pattern.compile(WEBSITE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(OCRResult);   // get a matcher object
        if (m.find()) {
            returnString = m.group();
        } else
            returnString = "";
        System.out.println(returnString);
        return returnString;
    }

    private Bitmap scaleBitmap(Bitmap yourBitmap) {
        float scale, width = yourBitmap.getWidth(), height = yourBitmap.getHeight();
        scale = (float) Math.sqrt(720*1280 / (width * height));
        Bitmap resized = Bitmap.createScaledBitmap(yourBitmap, (int) (width * scale), (int) (height * scale), true);
        return resized;
    }

    private static void checkFile(File dir, Context myContext) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(myContext);
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles(myContext);
            }
        }
    }

    private static void copyFiles(Context myContext) {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = myContext.getAssets();

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
}
