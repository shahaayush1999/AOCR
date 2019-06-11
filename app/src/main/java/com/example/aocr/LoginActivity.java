package com.example.aocr;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
    
    Button buttonLoginUsingPassword;
    Button buttonGetPasswordByText;
    EditText loginName;
    EditText loginPassword;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        buttonLoginUsingPassword = (Button) findViewById(R.id.buttonLoginUsingPassword);
        buttonGetPasswordByText = (Button) findViewById(R.id.buttonGetPasswordByText);
        loginName = (EditText) findViewById(R.id.editTextLoginName);
        loginPassword = (EditText) findViewById(R.id.editTextLoginPassword);

        buttonLoginUsingPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPassword();
            }
        });

        buttonGetPasswordByText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOTP();
            }
        });
    }
    //todo modify login xml with appropriate status strings for login page
    public void verifyPassword() {};
    public void sendOTP() {};
    public void passwordVerifiedLaunchMainActivity() {};
    public void passwordNotVerified() {};
}
