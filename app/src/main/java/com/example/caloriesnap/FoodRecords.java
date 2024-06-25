package com.example.caloriesnap;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.caloriesnap.databinding.ActivityFoodRecordsBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class FoodRecords extends AppCompatActivity {

    private ActivityFoodRecordsBinding binding;
    private ArrayList<FoodRecord> foodRecords;
    private FoodRecordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodRecordsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        foodRecords = new ArrayList<>();
        adapter = new FoodRecordAdapter();
        binding.foodRecordsListView.setAdapter(adapter);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);

        if (email != null) {
            fetchFoodRecords(email);
        } else {
            Toast.makeText(this, "Email not found. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchFoodRecords(String email) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "http://192.168.43.152:80/FYP/get_food_records.php?email=" + email;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String foodName = jsonObject.getString("food_name");
                                String foodCalorie = jsonObject.getString("food_calorie");
                                String foodImageBase64 = jsonObject.getString("food_image");
                                String dateTime = jsonObject.getString("date_time");
                                byte[] foodImageBytes = Base64.decode(foodImageBase64, Base64.DEFAULT);
                                Bitmap foodImage = BitmapFactory.decodeByteArray(foodImageBytes, 0, foodImageBytes.length);
                                foodRecords.add(new FoodRecord(foodName, foodCalorie, foodImage, dateTime));
                            }
                            adapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(FoodRecords.this, "Error parsing data.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("FoodRecordsActivity", "Error: " + error.toString());
                        Toast.makeText(FoodRecords.this, "Failed to fetch data. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(stringRequest);
    }


    private class FoodRecord {
        String name;
        String calorie;
        Bitmap image;
        String dateTime;

        FoodRecord(String name, String calorie, Bitmap image, String dateTime) {
            this.name = name;
            this.calorie = calorie;
            this.image = image;
            this.dateTime = dateTime;
        }
    }


    private class FoodRecordAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return foodRecords.size();
        }

        @Override
        public Object getItem(int position) {
            return foodRecords.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(FoodRecords.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            FoodRecord record = foodRecords.get(position);

            // Create RelativeLayout
            RelativeLayout layout = new RelativeLayout(FoodRecords.this);
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setPadding(16, 16, 16, 16);

            // Create ImageView
            ImageView imageView = new ImageView(FoodRecords.this);
            imageView.setId(View.generateViewId());
            RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(150, 150);
            imageParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(record.image);

            // Create TextViews
            TextView text1 = new TextView(FoodRecords.this);
            text1.setId(View.generateViewId());
            text1.setText(record.name);
            RelativeLayout.LayoutParams text1Params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            text1Params.addRule(RelativeLayout.RIGHT_OF, imageView.getId());
            text1Params.addRule(RelativeLayout.ALIGN_TOP, imageView.getId());
            text1Params.setMargins(16, 0, 0, 0);

            TextView text2 = new TextView(FoodRecords.this);
            text2.setText(record.calorie + " calories");
            RelativeLayout.LayoutParams text2Params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            text2Params.addRule(RelativeLayout.BELOW, text1.getId());
            text2Params.addRule(RelativeLayout.ALIGN_START, text1.getId());
            text2Params.setMargins(16, 8, 0, 0);

            // Create DateTime TextView
            TextView text3 = new TextView(FoodRecords.this);
            text3.setText(record.dateTime);
            RelativeLayout.LayoutParams text3Params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            text3Params.addRule(RelativeLayout.ALIGN_PARENT_END); // Align with the right edge of the parent
            text3Params.addRule(RelativeLayout.ALIGN_BOTTOM, imageView.getId()); // Align with the top of the image
            text3Params.setMargins(0, 0, 16, 0); // Margin to the right
            text3Params.addRule(RelativeLayout.BELOW, text2.getId()); // Below the calorie TextView

            // Add views to layout
            layout.addView(imageView);
            layout.addView(text1, text1Params);
            layout.addView(text2, text2Params);
            layout.addView(text3, text3Params);

            return layout;
        }


    }
}
