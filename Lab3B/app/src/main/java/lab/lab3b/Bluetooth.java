package lab.lab3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Rami on 2015-12-04.
 */
public class Bluetooth extends Thread {
    private BluetoothDevice pulseDevice;
    private BluetoothAdapter adapter;
    private String result;
    private MainActivity main;
    private BluetoothSocket socket = null;
    private static final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //changed the 5th byte from 0x08 to 0x02 for Serial Data Format 2
    private static final byte[] FORMAT = {0x02, 0x70, 0x04, 0x02, 0x02, 0x00, (byte) 0x7E, 0x03};
    private static final byte ACK = 0x06;
    private File file;

    private BufferedWriter writer;

    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    protected Bluetooth(MainActivity main, BluetoothDevice pulseDevice, File file) {
        this.main = main;
        this.pulseDevice = pulseDevice;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.file = file;

    }

    @Override
    public void run() {
        try {
            socket = this.pulseDevice.createRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
        } catch (IOException e) {
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e1) {
                Log.i("socket", "closing socket failed");
            }
        }
        adapter.cancelDiscovery();
        try {
            socket.connect();

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            os.write(FORMAT);
            os.flush();
            byte[] reply = new byte[1];
            is.read(reply);

            if (reply[0] == ACK) {
                try {
                    if (file.canWrite()) {
                        writer = new BufferedWriter(new FileWriter(file));
                    }

                    byte[] frame = new byte[4]; // this -obsolete- format specifies
                    // 4 bytes per frame
                    is.read(frame);
                    int pulse = unsignedByteToInt(frame[1]);
                    int pleth = unsignedByteToInt(frame[2]);
                    result = pulse + ";" + pleth + "\r\n";
                    //Write to the public file
                    writer.write(result);
                    //Display the pulse data
                    main.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            main.displayData(result);
                        }
                    });
                } catch (Exception e23) {

                }
            }
        } catch (Exception e) {
            result = e.getMessage();
        } finally {
            try {
                if (socket != null)
                    socket.close();
                if (writer != null)
                    writer.close();
            } catch (Exception e) {
                Log.i("stream", "closing streams failed");
            }
        }
    }

    public void cancel() {
        try {
            if (socket != null)
                socket.close();
            if (writer != null)
                writer.close();
        } catch (IOException e) {
        }
    }


}
