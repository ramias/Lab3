package lab.lab3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Rami on 2015-12-04.
 */
public class Bluetooth extends Thread {
    private BluetoothDevice pulseDevice;
    private BluetoothAdapter adapter;
    private MainActivity main;
    private BluetoothSocket socket = null;
    private static final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final byte[] FORMAT = {0x02, 0x70, 0x04, 0x02, 0x02, 0x00, (byte) 0x78, 0x03};
    private static final byte ACK = 0x06;
    private static final byte RES = 0x01;
    private int pleth,pulse;
    private BufferedWriter writer;
    private boolean runTask;
    private boolean catchNext;


    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    protected Bluetooth(MainActivity main, BluetoothDevice pulseDevice, BufferedWriter writer) {
        this.main = main;
        this.pulseDevice = pulseDevice;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.writer = writer;
    }

    @Override
    public void run() {
        runTask = true;
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
            Log.i("ack", "Ack : " + String.valueOf(reply[0]));
            if (reply[0] == ACK || reply[0] == RES) {
                while (runTask) {
                    try {
                        byte[] frame = new byte[5];
                        is.read(frame);
                        if(catchNext) {
                            pulse += unsignedByteToInt(frame[3]);
                            catchNext = false;
                            main.updateResult(String.valueOf(pulse));
                        }
                        if ((frame[1] & 0x01) > 0) {
                            pulse  = unsignedByteToInt(frame[3]) * 128;
                            catchNext = true;
                        }

                        pleth = unsignedByteToInt(frame[2]);
                        try {
                            if (writer != null)
                                writer.write(pleth + "\n");
                            else
                                Log.i("writer", "IS NULL");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.i("file", "pleth: " + String.valueOf(pleth));
                        Log.i("file", "pulse :" + String.valueOf(pulse));
                        wait(1);
                    } catch (Exception e23) {

                    }
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                if (writer != null)
                    writer.close();

                if (socket != null)
                    socket.close();
            } catch (Exception e4) {
                Log.i("stream", "closing streams failed");
            }

        }
    }

    public void cancel() {
        runTask = false;
        try {
            if (writer != null)
                writer.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
        }
    }


}
