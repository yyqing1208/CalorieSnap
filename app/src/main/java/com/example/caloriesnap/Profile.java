package com.example.caloriesnap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private TextView emailTextView, usernameTextView;
    private ProgressBar progressBar;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        emailTextView = findViewById(R.id.email_text_view);
        usernameTextView = findViewById(R.id.username_text_view);
        progressBar = findViewById(R.id.progress_bar);
        logoutButton = findViewById(R.id.logout_button);

        fetchProfileDetails();

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
    }

    private void fetchProfileDetails() {
        progressBar.setVisibility(View.VISIBLE);

        // Fetch email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);

        if (email == null) {
            Toast.makeText(Profile.this, "No user is logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Profile.this, Login.class));
            finish();
            return;
        }

        String url = "http://192.168.43.152:80/FYP/profile.php";
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String status = jsonResponse.getString("status");

                    if ("success".equals(status)) {
                        String username = jsonResponse.getString("username");
                        emailTextView.setText(email);
                        usernameTextView.setText(username);
                    } else {
                        String message = jsonResponse.getString("message");
                        Toast.makeText(Profile.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(Profile.this, "JSON parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                String errorMessage = "Request error: ";
                if (error.networkResponse != null) {
                    errorMessage += "Status Code: " + error.networkResponse.statusCode + "\n";
                }
                String errorDetailMessage = error.getMessage();
                if (errorDetailMessage != null) {
                    errorMessage += "Message: " + errorDetailMessage;
                }
                Toast.makeText(Profile.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void logoutUser() {
        // Clear SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(Profile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(Profile.this, Login.class));
        finish();
    }
}
