package com.example.aocr;

import java.util.Date;

//if anything breaks then convert all declarations and functions to public

public class User {
    private Integer userID;
    private String fullName;
    private Date sessionExpiryDate;

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setSessionExpiryDate(Date sessionExpiryDate) {
        this.sessionExpiryDate = sessionExpiryDate;
    }

    public Integer getUserID() {
        return userID;
    }

    public String getFullName() {
        return fullName;
    }

    public Date getSessionExpiryDate() {
        return sessionExpiryDate;
    }
}