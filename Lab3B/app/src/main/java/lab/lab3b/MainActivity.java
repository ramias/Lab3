package lab.lab3b;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Set;

public class MainActivity extends Activity {
    private TextView pulseText,plethText, stateMsg;
    private Button startButton, stopButton, uploadButton;
    public static final int REQUEST_ENABLE_BT = 42;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice pulseDevice = null;
    private UploadFileTask uploadFileTask = null;
    private int serverPort;
    private String serverIP;
    private File file;
    private Bluetooth bluetooth;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pulseText = (TextView) findViewById(R.id.pulseText);
        plethText = (TextView) findViewById(R.id.plethText);
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        uploadButton = (Button) findViewById(R.id.UploadButton);
        stateMsg = (TextView) findViewById(R.id.StateMsg);
        serverPort = 50000;
        serverIP = "192.168.1.14";
        file = new File(file, "Lab3B.txt");

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
        //dataView.setText(R.string.data);
        initBluetooth();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.cancel();
    }

    public void onPause() {
        super.onPause();
        bluetooth.cancel();
        bluetoothAdapter.cancelDiscovery();
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

    protected void displayData(String result) {
        if (result != null) {
            String values[] = result.split(";");
            pulseText.setText(values[0]);
            pulseText.invalidate();
            plethText.setText(values[1]);
            plethText.invalidate();
        }
    }

    private void initBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            getPulseDevice();
            bluetooth = new Bluetooth(this, pulseDevice, file);
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

    /*
     * Button listener call backs
     */
    public void onStartClicked(View view) {

        if (pulseDevice != null) {
            bluetooth.run();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            uploadButton.setEnabled(false);
            stateMsg.setText("Reading data");
        } else {
            showToast("No pulse sensor found");
        }
    }

    public void onStopClicked(View v) {

        if (pulseDevice != null) {
            bluetooth.cancel();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            uploadButton.setEnabled(true);
            stateMsg.setText("Reading stopped");
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

}