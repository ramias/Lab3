package lab.lab3b;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accText = (TextView) findViewById(R.id.AccText);
        maxAccText = (TextView) findViewById(R.id.MaxAccText);
        minAccText = (TextView) findViewById(R.id.MinAccText);
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        uploadButton = (Button) findViewById(R.id.UploadButton);
        stateMsg = (TextView) findViewById(R.id.StateMsg);

        updateFromPreferences();
    }

    public void onPause() {
        super.onPause();
        stopListening();
        if (uploadFileTask != null)
            uploadFileTask.cancel(true);
        uploadFileTask = null;
    }

    public void onResume() {
        super.onResume();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        uploadButton.setEnabled(false);
        stateMsg.setText("Ready");
    }

    private void startListening() {

        // ? Set a timer/check file size to prevent file from draining memory

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometer,
                sensorFrequency);
        // Alt: SensorManager.SENSOR_DELAY_FASTEST, SENSOR_DELAY_NORMAL,
        // SENSOR_DELAY_UI

        try {
            FileOutputStream fout = openFileOutput(filename,
                    Context.MODE_PRIVATE);
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    fout)));
			/*
			 * To write to external SD card (example) try { File root =
			 * Environment.getExternalStorageDirectory(); if (root.canWrite()) {
			 * File data = new File(root, "accdata.txt"); writer = new
			 * BufferedWriter(new FileWriter(data)); } } catch (IOException e)
			 */

            updateTimer = new Timer("updateUI");
            TimerTask updateUITask = new TimerTask() {
                @Override
                public void run() {
                    updateUI();
                }
            };
            updateTimer.scheduleAtFixedRate(updateUITask, 0, 200);
        } catch (IOException ioe) {
            Log.e("startListening", ioe.toString());
            showToast("Error opening file, " + ioe.toString());
        }
    }

    private void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
            updateTimer.cancel();
        }

        if (writer != null) {
            try {
                writer.close();
                Log.i("stopListening()", "writer closed");
            } catch (Exception e) {
                Log.e("onPause", e.toString());
                System.err.println("Error closing writer");
                e.printStackTrace(System.err);
            }
        }
    }

    public void onUploadCompleted(String result) {
        stateMsg.setText(result);
        uploadButton.setEnabled(true);
    }

    /*
     * Button listener call backs
     */
    public void onStartClicked(View view) {
        this.startListening();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        uploadButton.setEnabled(false);
        stateMsg.setText("Reading accelerometer data");
    }

    public void onStopClicked(View v) {
        stopListening();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        uploadButton.setEnabled(true);
        stateMsg.setText("Reading stopped");
    }

    public void onUploadClicked(View v) {
        uploadButton.setEnabled(false);
        uploadFileTask = new UploadFileTask(MainActivity.this, serverIP,
                serverPort, filename);
        uploadFileTask.execute();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        uploadButton.setEnabled(true);
        stateMsg.setText("Uploading data");
    }

    /*
     * Options menu code (for preferences)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_PREFERENCES, Menu.NONE, "Preferences");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case (MENU_PREFERENCES): {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, SHOW_PREFERENCES);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SHOW_PREFERENCES) {
            if (resultCode == Activity.RESULT_OK) {
                updateFromPreferences();
            }
        }
    }

    private void updateFromPreferences() {
        Context context = getApplicationContext();
        SharedPreferences preferenses = PreferenceManager
                .getDefaultSharedPreferences(context);
        // startDelay = preferenses.getBoolean(
        //      SettingsActivity.PREF_START_DELAY, true);
        sensorFrequency = Integer.parseInt(preferenses.getString(
                SettingsActivity.PREF_SENSOR_FREQ, "3"));
        serverPort = Integer.parseInt(preferenses.getString(
                SettingsActivity.PREF_SERVER_PORT, "0"));
        serverIP = preferenses.getString(SettingsActivity.PREF_SERVER_IP, "0");
        filename = preferenses.getString(SettingsActivity.PREF_FILENAME, "0");
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String currentG = String.format(Locale.getDefault(),
                        "[%6.3f, %6.3f, %6.3f]", x, y, z);
                accText.setText(currentG);
                accText.invalidate();
                String maxG = String.format(Locale.getDefault(),
                        "[%6.3f, %6.3f, %6.3f]", maxX, maxY, maxZ);
                maxAccText.setText(maxG);
                maxAccText.invalidate();
                String minG = String.format(Locale.getDefault(),
                        "[%6.3f, %6.3f, %6.3f]", minX, minY, minZ);
                minAccText.setText(minG);
                minAccText.invalidate();
            }
        });
    }

    private TextView accText, maxAccText, minAccText, stateMsg;
    private Button startButton, stopButton, uploadButton;
    private SensorManager sensorManager;
    private double x, y, z, maxX = -1000.0F, maxY = -1000.0F, maxZ = -1000.0F,
            minX = 1000.0F, minY = 1000.0F, minZ = 1000.0F;
    private Timer updateTimer = null;
    private PrintWriter writer;
    private UploadFileTask uploadFileTask = null;

    // private boolean startDelay;
    private int sensorFrequency, serverPort;
    private String serverIP, filename;

    private static final int MENU_PREFERENCES = Menu.FIRST;
    private static final int SHOW_PREFERENCES = 1;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {

        // private double calibration = SensorManager.STANDARD_GRAVITY;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            // NB! Need to store max/min when display switches orientation.
            if (x > maxX)
                maxX = x;
            if (y > maxY)
                maxY = y;
            if (z > maxZ)
                maxZ = z;
            if (x < minX)
                minX = x;
            if (y < minY)
                minY = y;
            if (z < minZ)
                minZ = z;

            // Store time stamp
            String data = x + ";" + y + ";" + z + ";" + event.timestamp
                    + "\r\n";
            try {
                writer.println(data);
                Log.i("doInBackground", data);
            } catch (Exception e) {
                Log.e("StopClickListener", e.toString());
                System.err.println("Error writing in onSensorChanged");
                e.printStackTrace(System.err);
            }
        }
    };

    private void showToast(CharSequence msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

}