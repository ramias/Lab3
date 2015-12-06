package lab.lab3a;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{


    private Animation fallingLeafAnimation;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    private ImageView img[], shank[], pupil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fallingLeafAnimation = AnimationUtils.loadAnimation(this, R.anim.falling_leaf);

        pupil = (ImageView) findViewById(R.id.pupil);
        shank = new ImageView[10];
        img = new ImageView[5];

        shank[0] =(ImageView) findViewById(R.id.shank1);
        img[0] = (ImageView) findViewById(R.id.leaf1);
        img[1] = (ImageView) findViewById(R.id.leaf2);
        img[2] = (ImageView) findViewById(R.id.leaf3);
        img[3] = (ImageView) findViewById(R.id.leaf4);
        img[4] = (ImageView) findViewById(R.id.leaf5);

        // positions
        img[0].setX(80);
        img[0].setY(70);
        img[1].setX(-35);
        img[1].setY(80);
        img[2].setX(-70);
        img[2].setY(100);
        img[3].setX(70);
        img[3].setY(25);
        img[4].setX(25);
        img[4].setY(20);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    for(ImageView i:img) i.startAnimation(fallingLeafAnimation);
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
            Log.i("aa","X: "+x);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
