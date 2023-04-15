package com.parivishti.racetime;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView timerTextView;
    private Button startButton, resetButton, scanButton;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerTextView = findViewById(R.id.tv_timer);
        startButton = findViewById(R.id.btn_start);
        resetButton = findViewById(R.id.btn_reset);
        scanButton = findViewById(R.id.btn_scan);

        startButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        scanButton.setOnClickListener(this);
        resetButton.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                startTime = System.currentTimeMillis();
                customHandler.postDelayed(updateTimerThread, 0);
                startButton.setEnabled(false);
                resetButton.setEnabled(true);
                break;
            case R.id.btn_reset:
                timeSwapBuff = 0L;
                startTime = System.currentTimeMillis();
                customHandler.removeCallbacks(updateTimerThread);
                timerTextView.setText("00:00:00.0");
                startButton.setEnabled(true);
                resetButton.setEnabled(false);
                break;
            case R.id.btn_scan:
                scanCode();
        }
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = System.currentTimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) ((updatedTime / 100)%10);
            int hours = mins / 60 ;
            timerTextView.setText("" + String.format("%02d", hours) + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs)+"."+String.format("%01d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }


    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result ->
    {
        if (result.getContents() != null) {
            String[] arrOfStr = result.getContents().split(":", 2);
            String name = arrOfStr[0];
            String entry_no = arrOfStr[1];
            timeInMilliseconds = System.currentTimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) ((updatedTime / 100)%10);
            int hours = mins / 60 ;
            String time = "" + String.format("%02d", hours) + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs)+"."+String.format("%01d", milliseconds);
            addItemToSheet(name, entry_no, time);
        }
    });

    private void addItemToSheet(String name, String entry_no, String time) {
        final ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Adding Item", "please wait...");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbw2n9YiHWmROWUB6afdkdUSKA0hagA569t_N1fMthwuA-g7YYe88kjmiLhc_rFraSt3/exec", new Response.Listener<String>() {
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
                params.put("timer", time);
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
