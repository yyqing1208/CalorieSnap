package com.example.caloriesnap

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.caloriesnap.databinding.ActivityDetailBinding
import com.example.caloriesnap.ml.ModelRegulerizer
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.io.ByteArrayOutputStream
import android.util.Base64

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var bitmap: Bitmap
    private lateinit var resultFood: String
    private lateinit var resultCalorie: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        binding.etGrams.setText("100")

        val fileName = "labels.txt"
        val caloriesName = "calorie.txt"

        val inputString = application.assets.open(fileName).bufferedReader().use { it.readText() }
        val caloriesString = application.assets.open(caloriesName).bufferedReader().use { it.readText() }

        val foodList = inputString.split("\n")
        val caloriesList = caloriesString.split("\n")

        val imageUri: Uri? = intent.data
        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

        loadImage(imageUri)

        binding.btnPredicted.setOnClickListener {
            binding.progbar.visibility = View.VISIBLE
            binding.btnPredicted.visibility = View.GONE
            binding.btnSelectTake.visibility = View.VISIBLE

            val model = ModelRegulerizer.newInstance(this)

            val imageProcessor: ImageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build()
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = tensorImage.buffer
            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val max = getMax(outputFeature0.floatArray)

            resultFood = foodList[max]
            resultCalorie = caloriesList[max]

            binding.foodName.text = resultFood
            binding.foodDesc.text = "Your calories intake from $resultFood (as per 100g food / portion)"

            if (resultCalorie.isNotEmpty()) {
                binding.calorieChart.visibility = View.VISIBLE
                calorieChart(resultCalorie)
            } else {
                binding.calorieChart.visibility = View.GONE
            }

            binding.progbar.visibility = View.GONE

            val etGrams = binding.etGrams
            binding.etGrams.visibility = View.VISIBLE

            binding.btnSave.visibility = View.VISIBLE

            etGrams.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val grams = s.toString().toFloatOrNull() ?: return
                    updateCalories(grams, resultCalorie)
                }
            })
        }

        binding.btnSelectTake.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnSave.setOnClickListener {
            val etGrams = binding.etGrams
            val gramsText = etGrams.text.toString()
            if (gramsText.isBlank()) {
                Toast.makeText(this, "Please enter grams", Toast.LENGTH_SHORT).show()
            } else {
                val grams = gramsText.toFloatOrNull()
                if (grams != null && grams > 0) {
                    saveDataToServer(resultFood, resultCalorie, grams)
                } else {
                    Toast.makeText(this, "Please enter grams greater than 0", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadImage(image: Uri?) {
        Log.d("DetailActivity", "Loading image: $image")
        if (image != null) {
            binding.foodPhoto.setImageURI(image)
        } else {
            binding.foodName.text = "No Pictures"
        }
    }

    private fun getMax(arr: FloatArray): Int {
        var ind = 0
        var min = 0.0f

        for (i in arr.indices) {
            if (arr[i] > min) {
                ind = i
                min = arr[i]
            }
        }
        return ind
    }

    private fun calorieChart(data: String) {
        val visitors: ArrayList<PieEntry> = ArrayList()
        visitors.add(PieEntry(data.toFloat(), ""))
        val pieDataSet = PieDataSet(visitors, "")
        pieDataSet.color = Color.rgb(157, 190, 185)
        pieDataSet.valueTextSize = 12f
        val pieData = PieData(pieDataSet)

        binding.apply {
            calorieChart.data = pieData
            calorieChart.description.isEnabled = false
            calorieChart.centerText = "Calories"
            calorieChart.legend.isEnabled = false
            calorieChart.animate()
        }
    }

    private fun updateCalories(grams: Float, originalCalories: String) {
        val caloriesPer100g = originalCalories.toFloatOrNull() ?: return
        val calculatedCalories = (grams / 100) * caloriesPer100g
        binding.foodDesc.text = "Calories for ${String.format("%.2f", grams)} grams of this food: $calculatedCalories kcal"

        val calorieEntry = PieEntry(calculatedCalories, "")
        val pieDataSet = PieDataSet(listOf(calorieEntry), "")
        pieDataSet.color = Color.rgb(157, 190, 185)
        pieDataSet.valueTextSize = 12f
        val pieData = PieData(pieDataSet)
        binding.calorieChart.data = pieData
        binding.calorieChart.legend.isEnabled = false
        binding.calorieChart.invalidate()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveDataToServer(foodName: String, calculatedCalories: String, grams: Float) {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)

        Log.d("DetailActivity", "Saving data: $foodName, $calculatedCalories, $email")

        if (email != null) {
            val requestQueue: RequestQueue = Volley.newRequestQueue(this)
            val url = "http://192.168.43.152:80/FYP/food.php"

            val calculatedCalories = if (grams > 0) {
                (grams / 100) * calculatedCalories.toFloat()
            } else {
                calculatedCalories.toFloat()
            }

            val foodImageBase64 = bitmapToBase64(bitmap)

            val stringRequest = object : StringRequest(Request.Method.POST, url,
                Response.Listener { response ->
                    Log.d("DetailActivity", "Response: $response")
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                },
                Response.ErrorListener { error ->
                    Log.e("DetailActivity", "Error: ${error.toString()}")
                    Toast.makeText(this, "Failed to save data. Please try again.", Toast.LENGTH_SHORT).show()
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["food_name"] = foodName
                    params["food_calorie"] = calculatedCalories.toString()
                    params["email"] = email
                    params["food_image"] = foodImageBase64
                    Log.d("DetailActivity", "Params: $params")
                    return params
                }
            }
            requestQueue.add(stringRequest)
        } else {
            Toast.makeText(this, "Email not found. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
}
