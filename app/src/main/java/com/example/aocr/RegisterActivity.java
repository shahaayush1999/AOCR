package com.example.aocr;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {
    private static final String KEY_USERID = "userid";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_EMPTY = "";

    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_COMPANY = "company";
    private static final String KEY_POSITION = "position";
    private static final String KEY_CITY = "city";
    private static final String KEY_PINCODE = "pincode";
    private static final String KEY_WEBSITE = "website";
    private static final String KEY_PASSWORD = "password";

    private EditText etFullName;
    private EditText etNumber;
    private EditText etEmail;
    private EditText etCompany;
    private EditText etPosition;
    private EditText etCity;
    private EditText etPinCode;
    private EditText etWebsite;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button Login;
    private Button Register;

    private String fullName;
    private String number;
    private String email;
    private String company;
    private String position;
    private String city;
    private String pincode;
    private String website;
    private String password;
    private String confirmPassword;

    private ProgressDialog pDialog;
    private String register_url = "http://10.0.2.2:8080/AOCR2/register.php";
    private SessionHandler session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionHandler(getApplicationContext());
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.etFullName);
        etNumber = findViewById(R.id.etNumber);
        etEmail = findViewById(R.id.etEmail);
        etCompany = findViewById(R.id.etCompanyName);
        etPosition = findViewById(R.id.etPosition);
        etCity = findViewById(R.id.etCity);
        etPinCode = findViewById(R.id.etPinCode);
        etWebsite = findViewById(R.id.etWebsite);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        Login = findViewById(R.id.btnRegisterLogin);
        Register = findViewById(R.id.btnRegister);

        //todo make every entry lowercase before storing in database
        Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gatherDataFromEditText();
                if (validateInputs()) {
                    registerUser();
                }
            }
        });

        //Launch Login screen when Login Button is clicked
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    /**
     * Display Progress bar while registering
     */
    private void displayLoader() {
        pDialog = new ProgressDialog(RegisterActivity.this);
        pDialog.setMessage("Signing Up.. Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();

    }

    /**
     * Launch Dashboard Activity on Successful Sign Up
     */
    private void loadMainactivity() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }

    private void registerUser() {
        displayLoader();
        JSONObject request = new JSONObject();
        try {
            //Populate the request parameters
            request.put(KEY_FULLNAME, fullName);
            request.put(KEY_NUMBER, number);
            request.put(KEY_EMAIL, email);
            request.put(KEY_COMPANY, company);
            request.put(KEY_POSITION, position);
            request.put(KEY_CITY, city);
            request.put(KEY_PINCODE, pincode);
            request.put(KEY_WEBSITE, website);
            request.put(KEY_PASSWORD, password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //todo fix the "cannot convert jsonarray to jsonobject error HERE"
        JsonObjectRequest jsArrayRequest = new JsonObjectRequest
                (Request.Method.POST, register_url, request, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        pDialog.dismiss();
                        try {
                            if (response.getInt(KEY_STATUS) == 0 || response.getInt(KEY_STATUS) == 1) {
                                //Status 0 = Uninvited profile created, Status 1 = Invited profile created
                                //Set the user session
                                session.loginUser(response.getInt(KEY_USERID), response.getString(KEY_FULLNAME));
                                System.out.println(response.getString(KEY_MESSAGE));
                                loadMainactivity();

                            } else if(response.getInt(KEY_STATUS) == 2) {
                                //Display error message if user is already existing AND registered
                                etNumber.setError("Number Already registered! Please Log in");
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        response.getString(KEY_MESSAGE), Toast.LENGTH_SHORT).show();

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pDialog.dismiss();

                        //Display error message whenever an error occurs
                        Toast.makeText(getApplicationContext(),
                                error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsArrayRequest);
    }

    /**
     * Validates inputs and shows error if any
     * @return
     */
    private boolean validateInputs() {
        if (KEY_EMPTY.equals(fullName)) {
            etFullName.setError("Full Name cannot be empty");
            etFullName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(number)) {
            etFullName.setError("Full Name cannot be empty");
            etFullName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(company)) {
            etFullName.setError("Full Name cannot be empty");
            etFullName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(position)) {
            etFullName.setError("Full Name cannot be empty");
            etFullName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(city)) {
            etFullName.setError("Full Name cannot be empty");
            etFullName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(password)) {
            etPassword.setError("Password cannot be empty");
            etPassword.requestFocus();
            return false;
        }
        if (KEY_EMPTY.equals(confirmPassword)) {
            etConfirmPassword.setError("Confirm Password cannot be empty");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (!(password.equals(confirmPassword))) {
            etConfirmPassword.setError("Password and Confirm Password does not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    public void gatherDataFromEditText() {
        fullName = etFullName.getText().toString().toLowerCase();
        number = etNumber.getText().toString();
        email = etEmail.getText().toString().toLowerCase();
        company = etCompany.getText().toString().toLowerCase();
        position = etPosition.getText().toString().toLowerCase();
        city = etCity.getText().toString();
        pincode = etPinCode.getText().toString();
        website = etWebsite.getText().toString().toLowerCase();
        password = etPassword.getText().toString();
        confirmPassword = etConfirmPassword.getText().toString();
    }
}