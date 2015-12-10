package lab.lab3b;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    public TextView pulseText, stateMsg;
    private Button startButton, stopButton, uploadButton;
    public static final int REQUEST_ENABLE_BT = 42;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice pulseDevice = null;
    private UploadFileTask uploadFileTask = null;
    private int serverPort;
    private String serverIP;
    private File file;
    private Bluetooth bluetooth;
    private BufferedWriter writer;
    private Timer updateTimer = null;
    private String pulse;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pulseText = (TextView) findViewById(R.id.pulseText);
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        uploadButton = (Button) findViewById(R.id.UploadButton);
        stateMsg = (TextView) findViewById(R.id.StateMsg);
        serverPort = 50000;
        serverIP = "130.229.137.26";
        file = new File(Environment.getExternalStorageDirectory(), "Lab3B.txt");

        //Find bluetooth adaopter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("This device do not support Bluetooth");
            this.finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initBluetooth();
    }

    public void onPause() {
        super.onPause();
        stopReading();
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

    private void initBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            getPulseDevice();
            bluetooth = new Bluetooth(this, pulseDevice, writer);
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
                    Log.i("name", name);
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

    public void onUploadCompleted(String result) {
        stateMsg.setText(result);
        uploadButton.setEnabled(true);
    }


    protected void displayData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pulseText.setText(pulse);
                pulseText.invalidate();
            }
        });
    }

    private void startReading() {
        Log.i("file", "" + file.canWrite());
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            if (file.canWrite()) {
                writer = new BufferedWriter(new FileWriter(file));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateTimer = new Timer("updateUI");
        TimerTask updateUITask = new TimerTask() {
            @Override
            public void run() {
                displayData();
            }
        };
        updateTimer.scheduleAtFixedRate(updateUITask, 0, 1000);
        Log.i("file", "path: " + file.getAbsolutePath());

        bluetooth = new Bluetooth(this, pulseDevice,writer);
        bluetooth.start();

    }


    public void onStartClicked(View view) {
        if (pulseDevice != null) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            uploadButton.setEnabled(false);
            stateMsg.setText("Reading data");
            startReading();
        } else {
            showToast("No pulse sensor found");
        }
    }

    private void stopReading() {
        if (bluetooth != null)
            bluetooth.cancel();
        updateTimer.cancel();
    }

    public void onStopClicked(View v) {
        if (pulseDevice != null) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            uploadButton.setEnabled(true);
            stateMsg.setText("Reading stopped");
            stopReading();
        } else {
            showToast("No pulse sensor found");
        }

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


    private void showToast(CharSequence msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void updateResult(String pulse) {
        this.pulse = pulse;
    }
}