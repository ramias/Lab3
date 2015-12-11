package lab.lab3a;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Animation fallingLeafAnimation;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastShakeUpdate = 0;
    private double lastVelocity = 0;
    private static final int SHAKE_LIMIT = 11, TIME_TRESHHOLD = 1000;
    private ImageView leaf[], shank[], pupil, dryPupil, dryLeaf[];
    private int lastShankPosition = 0;
    private boolean isFlowerDead, hasFallen, tiltingRight;
    private int avgTiltCounter = 0, avgShakeCounter = 0;
    private double tiltSumX = 0;
    private double velocitySum = 0;
    private boolean hasShaken;
    private double prevX;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fallingLeafAnimation = AnimationUtils.loadAnimation(this, R.anim.falling_leaf);
        pupil = (ImageView) findViewById(R.id.pupil);
        dryPupil = (ImageView) findViewById(R.id.drypupil);
        shank = new ImageView[10];
        leaf = new ImageView[5];
        dryLeaf = new ImageView[5];
        isFlowerDead = false;
        initializeImageViews();

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);


    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, Menu.NONE, "New Flower!");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 0) {
            recreate();
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        double tiltAvg, avgVelocity, filteredVelocity, currentVelocity;
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double x = sensorEvent.values[0];
            double y = sensorEvent.values[1];
            double z = sensorEvent.values[2];

            if (avgTiltCounter == 20) {
                avgTiltCounter = 1;
                tiltSumX = 0;
            }
            // filteredvalue(n)= F*filteredvalue(n-1)+(1-F)* sensorvalue(n)
            x = (0.97 * prevX) + (0.03 * x);
            prevX = x;

            tiltSumX += x;
            tiltAvg = tiltSumX / avgTiltCounter++;


            if (avgShakeCounter == 30) {
                avgShakeCounter = 0;
                velocitySum = 0;
            }

            currentVelocity = (float) Math.sqrt(x * x + y * y + z * z);
            velocitySum += currentVelocity;
            avgVelocity = velocitySum / ++avgShakeCounter;

            double delta = currentVelocity - lastVelocity;
            filteredVelocity = avgVelocity * 0.85 + delta * 0.15;
            lastVelocity = currentVelocity;

            if (tiltAvg < 0 && !tiltingRight) {
                tiltingRight = true;
                for (ImageView i : shank) i.setScaleX(1);
            } else if (tiltAvg >= 0 && tiltingRight) {
                tiltingRight = false;
                for (ImageView i : shank) i.setScaleX(-1);
            }

            int newShankPosition = (int) Math.abs(Math.round(tiltAvg));
            if (newShankPosition < 0){
                newShankPosition = 0;
            }
            if (lastShankPosition != newShankPosition && newShankPosition < 10) {
                shank[lastShankPosition].setVisibility(View.INVISIBLE);
                if (newShankPosition == 9) { // 9 = dead flower
                    isFlowerDead = true;
                    dryPupil.setVisibility(View.VISIBLE);
                    pupil.setVisibility(View.INVISIBLE);
                    if (!hasFallen) {
                        for (int i = 0; i < leaf.length; ++i) {
                            leaf[i].setVisibility(View.INVISIBLE);
                            dryLeaf[i].setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    isFlowerDead = false;
                    dryPupil.setVisibility(View.INVISIBLE);
                    pupil.setVisibility(View.VISIBLE);
                    if (!hasFallen) {
                        for (int i = 0; i < leaf.length; ++i) {
                            leaf[i].setVisibility(View.VISIBLE);
                            dryLeaf[i].setVisibility(View.INVISIBLE);
                        }
                    }
                }
                shank[newShankPosition].setVisibility(View.VISIBLE);
                lastShankPosition = newShankPosition;
            }


            if (Math.abs(filteredVelocity) >= SHAKE_LIMIT && !hasFallen) {
                if (!hasShaken) {
                    hasShaken = true;
                    lastShakeUpdate = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - lastShakeUpdate >= TIME_TRESHHOLD) {
                    shakeLeafs();
                    hasShaken = false;
                    lastShakeUpdate = System.currentTimeMillis();
                }
            } else {
                if (System.currentTimeMillis() - lastShakeUpdate > TIME_TRESHHOLD / 3) {
                    hasShaken = false;
                    lastShakeUpdate = System.currentTimeMillis();
                }
            }
        }
    }

    private void shakeLeafs() {
        if (!isFlowerDead && !hasFallen) {
            hasFallen = true;
            for (ImageView i : leaf) i.startAnimation(fallingLeafAnimation);
        } else if (isFlowerDead && !hasFallen) {
            hasFallen = true;
            for (ImageView i : dryLeaf) i.startAnimation(fallingLeafAnimation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void initializeImageViews() {
        shank = new ImageView[10];
        leaf = new ImageView[5];
        dryLeaf = new ImageView[5];
        isFlowerDead = false;

        shank[0] = (ImageView) findViewById(R.id.shank1);
        shank[1] = (ImageView) findViewById(R.id.shank2);
        shank[2] = (ImageView) findViewById(R.id.shank3);
        shank[3] = (ImageView) findViewById(R.id.shank4);
        shank[4] = (ImageView) findViewById(R.id.shank5);
        shank[5] = (ImageView) findViewById(R.id.shank6);
        shank[6] = (ImageView) findViewById(R.id.shank7);
        shank[7] = (ImageView) findViewById(R.id.shank8);
        shank[8] = (ImageView) findViewById(R.id.shank9);
        shank[9] = (ImageView) findViewById(R.id.dryshank);

        leaf[0] = (ImageView) findViewById(R.id.leaf1);
        leaf[1] = (ImageView) findViewById(R.id.leaf2);
        leaf[2] = (ImageView) findViewById(R.id.leaf3);
        leaf[3] = (ImageView) findViewById(R.id.leaf4);
        leaf[4] = (ImageView) findViewById(R.id.leaf5);

        dryLeaf[0] = (ImageView) findViewById(R.id.dryleaf1);
        dryLeaf[1] = (ImageView) findViewById(R.id.dryleaf2);
        dryLeaf[2] = (ImageView) findViewById(R.id.dryleaf3);
        dryLeaf[3] = (ImageView) findViewById(R.id.dryleaf4);
        dryLeaf[4] = (ImageView) findViewById(R.id.dryleaf5);

        // positions
        dryLeaf[0].setX(40);
        dryLeaf[0].setY(55);

        dryLeaf[1].setX(-25);
        dryLeaf[1].setY(65);

        dryLeaf[2].setX(-100);
        dryLeaf[2].setY(75);

        dryLeaf[3].setX(70);
        dryLeaf[3].setY(15);

        dryLeaf[4].setX(40);
        dryLeaf[4].setY(0);
        // ------------------------------
        leaf[0].setX(40);
        leaf[0].setY(55);

        leaf[1].setX(-25);
        leaf[1].setY(65);

        leaf[2].setX(-100);
        leaf[2].setY(75);

        leaf[3].setX(70);
        leaf[3].setY(15);

        leaf[4].setX(40);
        leaf[4].setY(0);
    }
}
