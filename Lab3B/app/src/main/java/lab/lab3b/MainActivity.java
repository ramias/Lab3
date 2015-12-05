package lab.lab3b;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private TextView accText, stateMsg;
    private Button startButton, stopButton, uploadButton;
    private SensorManager sensorManager;
    private double x, y, z;
    private Timer updateTimer = null;
    private BufferedWriter writer;
    private UploadFileTask uploadFileTask = null;

    // private boolean startDelay;
    private int sensorFrequency, serverPort;
    private String serverIP, filename;
    private File file;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accText = (TextView) findViewById(R.id.AccText);
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        uploadButton = (Button) findViewById(R.id.UploadButton);
        stateMsg = (TextView) findViewById(R.id.StateMsg);

        sensorFrequency = 1;
        serverPort = 50000;
        serverIP = "192.168.1.14";
        filename = "Lab3File.txt";
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
                SensorManager.SENSOR_DELAY_UI);
        // Alt: SensorManager.SENSOR_DELAY_FASTEST, SENSOR_DELAY_NORMAL,
        // SENSOR_DELAY_UI

        try {

            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()) {
                file = new File(root, filename);
                writer = new BufferedWriter(new FileWriter(file));
            }


            updateTimer = new Timer("updateUI");
            TimerTask updateUITask = new TimerTask() {
                @Override
                public void run() {
                    updateUI();
                }
            };
            updateTimer.scheduleAtFixedRate(updateUITask, 0, 1000);
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
                serverPort, file);
        uploadFileTask.execute();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        uploadButton.setEnabled(true);
        stateMsg.setText("Uploading data");
    }


    private void updateUI() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String currentG = String.format(Locale.getDefault(),
                        "[%6.3f, %6.3f, %6.3f]", x, y, z);
                accText.setText(currentG);
                accText.invalidate();
            }
        });
    }



    private final SensorEventListener sensorEventListener = new SensorEventListener() {

        // private double calibration = SensorManager.STANDARD_GRAVITY;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            // Store time stamp
            String data = x + ";" + y + ";" + z + ";" + event.timestamp
                    + "\r\n";
            try {
                writer.write(data);
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