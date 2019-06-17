package com.example.aocr;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
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

public class LoginActivity extends AppCompatActivity {
    //todo add userid key and convert input number to string and login
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_USERID = "userid";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_EMPTY = "";
    private EditText etPhoneNumber;
    private EditText etPassword;
    private String phone;
    private String password;
    private ProgressDialog pDialog;
    private String login_url = "http://10.0.2.2:8080/AOCR/login.php";
    private SessionHandler session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionHandler(getApplicationContext());

        if(session.isLoggedIn()){
            loadMainActivity();
        }
        setContentView(R.layout.activity_login);

        etPhoneNumber = findViewById(R.id.etLoginPhoneNumber);
        etPassword = findViewById(R.id.etLoginPassword);

        Button register = findViewById(R.id.btnLoginRegister);
        Button login = findViewById(R.id.btnLogin);

        //Launch Registration screen when Register Button is clicked
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadRegisterActivity();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Retrieve the data entered in the edit texts
                phone = etPhoneNumber.getText().toString().toLowerCase().trim();
                password = etPassword.getText().toString();
                if (validateInputs()) {
                    login();
                }
            }
        });
    }

    /**
     * Launch Dashboard Activity on Successful Login
     */
    private void loadMainActivity() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }

    /**
     * Display Progress bar while Logging in
     */

    private void displayLoader() {
        pDialog = new ProgressDialog(LoginActivity.this);
        pDialog.setMessage("Logging In.. Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();

    }

    private void login() {
        displayLoader();
        JSONObject request = null;
        try {
            request = new JSONObject("{}");
            //Populate the request parameters
            request.put(KEY_PHONE, phone);
            request.put(KEY_PASSWORD, password);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i("tagconvertstr", "["+request+"]");

        JsonObjectRequest jsArrayRequest = new JsonObjectRequest
                (Request.Method.POST, login_url, request, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        pDialog.dismiss();
                        try {
                            //Check if user got logged in successfully or errors
                            switch(response.getInt(KEY_STATUS)) {
                                case 0: //logged in successfully
                                    session.loginUser(response.getInt(KEY_USERID), response.getString(KEY_FULLNAME));
                                    System.out.println(response.getString(KEY_MESSAGE));
                                    loadMainActivity();
                                    break;
                                case 1: //wrong password
                                    etPassword.setError("Please check password");
                                    etPassword.requestFocus();
                                    break;
                                case 2: //Invalid phone number or bad format
                                    etPhoneNumber.setError("Number invalid or not registered : 2");
                                    etPhoneNumber.requestFocus();
                                    break;
                                case 4: //Invited User, redirect to registration page
                                    Toast.makeText(getApplicationContext(), "Please register first", Toast.LENGTH_SHORT).show();
                                    loadRegisterActivity();
                                    break;
                                default:
                                    Toast.makeText(getApplicationContext(), response.getString(KEY_MESSAGE), Toast.LENGTH_SHORT).show();
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
        if(KEY_EMPTY.equals(phone)){
            etPhoneNumber.setError("Phone Number cannot be empty");
            etPhoneNumber.requestFocus();
            return false;
        }
        if(KEY_EMPTY.equals(password)){
            etPassword.setError("Password cannot be empty");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void loadRegisterActivity() {
        Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(i);
        finish();
    }
}