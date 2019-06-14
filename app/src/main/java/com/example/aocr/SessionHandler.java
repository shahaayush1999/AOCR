package com.example.aocr;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

public class SessionHandler {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERID = "userid";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_EXPIRES = "expires";
    private static final String KEY_EMPTY = "";
    private Context mContext;
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPreferences;

    public SessionHandler(Context mContext) {
        this.mContext = mContext;
        mPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.mEditor = mPreferences.edit();
        //todo comment next line if unable to log in or anything breaks
        mEditor.apply();
    }

    /**
     * Logs in the user by saving user details and setting session
     *
     * @param fullName
     * @param userid
     */
    //change Integer to Long in future to scale app for a lot of users
    public void loginUser(Integer userid, String fullName) {
        mEditor.putInt(KEY_USERID, userid);
        mEditor.putString(KEY_FULLNAME, fullName);
        Date date = new Date();
        //Set user session for next 14 days
        long millis = date.getTime() + (14 * 24 * 60 * 60 * 1000);
        mEditor.putLong(KEY_EXPIRES, millis);
        mEditor.commit();
    }

    /**
     * Checks whether user is logged in
     *
     * @return
     */
    public boolean isLoggedIn() {
        Date currentDate = new Date();

        long millis = mPreferences.getLong(KEY_EXPIRES, 0);

        /* If shared preferences does not have a value
         then user is not logged in
         */
        if (millis == 0) {
            return false;
        }
        Date expiryDate = new Date(millis);

        /* Check if session is expired by comparing
        current date and Session expiry date
        */
        return currentDate.before(expiryDate);
    }

    /**
     * Fetches and returns user details
     *
     * @return user details
     */
    public User getUserDetails() {
        //Check if user is logged in first
        if (!isLoggedIn()) {
            return null;
        }
        User user = new User();
        user.setUserID(mPreferences.getString(KEY_USERID, KEY_EMPTY));
        user.setFullName(mPreferences.getString(KEY_FULLNAME, KEY_EMPTY));
        user.setSessionExpiryDate(new Date(mPreferences.getLong(KEY_EXPIRES, 0)));
        return user;
    }

    /**
     * Logs out user by clearing the session
     */
    public void logoutUser(){
        mEditor.clear();
        mEditor.commit();
    }
}
