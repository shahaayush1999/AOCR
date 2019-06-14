package com.example.aocr;

import java.util.Date;

//if anything breaks then convert all declarations and functions to public

public class User {
    private String userID;
    private String fullName;
    private Date sessionExpiryDate;

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setSessionExpiryDate(Date sessionExpiryDate) {
        this.sessionExpiryDate = sessionExpiryDate;
    }

    public String getUserID() {
        return userID;
    }

    public String getFullName() {
        return fullName;
    }

    public Date getSessionExpiryDate() {
        return sessionExpiryDate;
    }
}