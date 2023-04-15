package com.parivishti.racetime;

//import android.app.ProgressDialog;
//import android.os.Bundle;
//import android.widget.TextView;
//
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.android.volley.AuthFailureError;
//import com.android.volley.DefaultRetryPolicy;
//import com.android.volley.Request;
//import com.android.volley.RequestQueue;
//import com.android.volley.Response;
//import com.android.volley.RetryPolicy;
//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.StringRequest;
//import com.android.volley.toolbox.Volley;
//import com.journeyapps.barcodescanner.ScanContract;
//import com.journeyapps.barcodescanner.ScanOptions;
//
//import java.util.HashMap;
//import java.util.Map;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    Button btn_scan, btn_start, btn_stop, btn_reset;

    TextView tv_timer;

    private boolean isTimerRunning = false;
    private long startTimeInMillis;
    private long timeLeftInMillis;
    private long endTime;


    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timeLeftInMillis = endTime - System.currentTimeMillis();

            if (timeLeftInMillis < 0) {
                isTimerRunning = false;
                timeLeftInMillis = 0;
                updateTimerText();
            } else {
                updateTimerText();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_scan = findViewById(R.id.btn_scan);
        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        btn_reset = findViewById(R.id.btn_reset);
        tv_timer = findViewById(R.id.tv_timer);
        btn_scan.setOnClickListener(v ->{scanCode();});
        btn_start.setOnClickListener(v -> startTimer());
        btn_stop.setOnClickListener(v -> stopTimer());
        btn_reset.setOnClickListener(v -> resetTimer());
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    private void startTimer() {
        if (!isTimerRunning) {
            startTimeInMillis = System.currentTimeMillis();
            endTime = startTimeInMillis + timeLeftInMillis;

            handler.postDelayed(runnable, 1000);
            isTimerRunning = true;
        }
    }

    private void stopTimer() {
        if (isTimerRunning) {
            handler.removeCallbacks(runnable);
            timeLeftInMillis = endTime - System.currentTimeMillis();
            isTimerRunning = false;
        }
    }

    private void resetTimer() {
        if (isTimerRunning) {
            handler.removeCallbacks(runnable);
            isTimerRunning = false;
        }

        timeLeftInMillis = 0;
        updateTimerText();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        tv_timer.setText(timeLeftFormatted);
    }



    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result ->
    {
        if (result.getContents() != null) {
            String[] arrOfStr = result.getContents().split(":", 2);
            String name = arrOfStr[0];
            String entry_no = arrOfStr[1];
            addItemToSheet(name, entry_no);
        }
    });

    private void addItemToSheet(String name, String entry_no) {
        final ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Adding Item", "please wait...");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbyopFLDEtCAYc8mUu2pmbbBKIsTQ2ZckkWZHMd7iJTvJYSx3VeAn9swTTC8ig55qzep/exec", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "" + response, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
            }
        }) {
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("action", "addItem");
                params.put("name", name);
                params.put("entryNumber", entry_no);
                return params;
            }
        };
        int timeOut = 50000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(timeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(stringRequest);
    }
}
