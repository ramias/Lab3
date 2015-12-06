package lab.lab3b;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private TextView pulseText, stateMsg;
    private Button startButton, stopButton, uploadButton;
    private SensorManager sensorManager;
    public static final int REQUEST_ENABLE_BT = 42;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice pulseDevice = null;
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

        pulseText = (TextView) findViewById(R.id.pulseText);
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        uploadButton = (Button) findViewById(R.id.UploadButton);
        stateMsg = (TextView) findViewById(R.id.StateMsg);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("This device do not support Bluetooth");
            this.finish();
        }

        sensorFrequency = 1;
        serverPort = 50000;
        serverIP = "192.168.1.14";
        filename = "Lab3File.txt";
    }

    @Override
    protected void onStart() {
        super.onStart();
        //dataView.setText(R.string.data);
        initBluetooth();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO: stop ongoing BT communication
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

    protected void displayData(CharSequence data) {
        pulseText.setText(data);
    }

    private void initBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            getPulseDevice();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent result) {
        super.onActivityResult(requestCode, resultCode, result);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (bluetoothAdapter.isEnabled()) {
                getPulseDevice();
            } else {
                showToast("Bluetooth is turned off.");
            }
        }
    }


    private void getPulseDevice() {
        pulseDevice = null;
        Set<BluetoothDevice> pairedBTDevices = bluetoothAdapter
                .getBondedDevices();
        if (pairedBTDevices.size() > 0) {
            // the last Nonin device, if any, will be selected...
            for (BluetoothDevice device : pairedBTDevices) {
                String name = device.getName();
                if (name.contains("Nonin")) {
                    pulseDevice = device;
                    showToast("Paired device: " + name);
                    return;
                }
            }
        }
        if (pulseDevice == null) {
            showToast("No paired pulse devices found!\r\n"
                    + "Please pair a pulse BT device with this device.");
        }
    }

    public void onPollButtonClicked(View view) {
        if (pulseDevice != null) {
            new Bluetooth(this, pulseDevice).execute();
        } else {
            showToast("No pulse sensor found");
        }
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
                pulseText.setText(currentG);
                pulseText.invalidate();
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