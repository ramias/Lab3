package lab.lab3a;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private double samplesX[];
    private Animation fallingLeafAnimation;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0, lastShakeUpdate = 0;
    private double lastVelocity = 0;
    private double oldX, oldY, oldZ;
    private static final int SHAKE_LIMIT = 1, TIME_TRESHHOLD = 1000;
    private ImageView leaf[], shank[], pupil, dryPupil, dryLeaf[];
    private int lastShankPosition = 0;
    private boolean isFlowerDead, hasFallen, tiltingRight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fallingLeafAnimation = AnimationUtils.loadAnimation(this, R.anim.falling_leaf);
        samplesX = new double[20];
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == 0) {
            recreate(); // Kan vara en bugg här, outOfMemory Exception
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int i = 0, k=0;
    private double sum = 0, avg = 0;
    private double xShakeOld = 0;
    private float vel = 0, currVel = 0, velOld = 0, sumVel=0, velAvg=0;
    private boolean hasShaken;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double x = sensorEvent.values[0];
            double y = sensorEvent.values[1];
            double z = sensorEvent.values[2];

            if (i == 20) {
                i = 0;
                avg = 0;
                sum = 0;
            }
            sum += x;
            avg = sum / ++i;

            // filteredvalue(n)= F*filteredvalue(n-1)+(1-F)* sensorvalue(n)
            double xBend = (0.98 * avg) + (0.02 * x);
            double xShake = (0.5 * avg) + (0.5 * x);

//            long currentTime = System.currentTimeMillis();
//            long deltaTime = (currentTime - lastUpdate);
//            double velocity = (Math.abs(x + y + z - oldX - oldY - oldZ) / deltaTime) * 10000;
//            velocity = 0.15 * lastVelocity + 0.95 * velocity;

            if (k == 30) {
                k = 0;
                velAvg = 0;
                sumVel = 0;
            }
            sumVel += currVel;
            velAvg = sumVel / ++k;
            velOld = currVel;
            currVel = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = currVel - velOld;
            vel = velAvg * 0.9f + delta;


            if (true) { // Gränsvärde för hur ofta en förändring ska ta effekt. Nu var 50 ms

//                lastUpdate = currentTime;
                if (xBend < 0 && !tiltingRight) {
                    tiltingRight = true;
                    for (ImageView i : shank) i.setScaleX(1);
                } else if (xBend >= 0 && tiltingRight) {
                    tiltingRight = false;
                    for (ImageView i : shank) i.setScaleX(-1);
                }

                Log.i("xx", "Ute x: " + xBend);
                if (xBend > 9)
                    xBend = 9;
                if (xBend < -9)
                    xBend = -9;
                int newShankPosition = (int) Math.abs(Math.round(xBend)); //Tog bort Math.round
                if (lastShankPosition != newShankPosition && newShankPosition < 10) {
                    shank[lastShankPosition].setVisibility(View.INVISIBLE);
                    if (newShankPosition == 9) {

                        isFlowerDead = true;
                        shank[9].setVisibility(View.VISIBLE);
                        dryPupil.setVisibility(View.VISIBLE);
                        pupil.setVisibility(View.INVISIBLE);
                        if (!hasFallen) {
                            for (ImageView i : leaf) i.setVisibility(View.INVISIBLE);
                            for (ImageView i : dryLeaf) i.setVisibility(View.VISIBLE);
                        }
                        lastShankPosition = newShankPosition;
                        return;
                    } else {
                        isFlowerDead = false;
                        shank[9].setVisibility(View.INVISIBLE);
                        dryPupil.setVisibility(View.INVISIBLE);
                        pupil.setVisibility(View.VISIBLE);
                        if (!hasFallen) {
                            for (ImageView i : leaf) i.setVisibility(View.VISIBLE);
                            for (ImageView i : dryLeaf) i.setVisibility(View.INVISIBLE);
                        }
                    }
                    shank[newShankPosition].setVisibility(View.VISIBLE);
                    lastShankPosition = newShankPosition;
                }
            }

            Log.i("vel", " vel " + Math.abs(vel));
            if (Math.abs(vel) >= 12) {
                if (!hasShaken) {
                    hasShaken = true;
                    lastShakeUpdate = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - lastShakeUpdate >= TIME_TRESHHOLD) {
                    Log.i("aa", "ANIMATE: ");
                    shakeLeafs();
                    hasShaken = false;
                    lastShakeUpdate = System.currentTimeMillis();
                }
            }else{
                if (hasShaken) {
                    hasShaken = false;
                    lastShakeUpdate = System.currentTimeMillis();
                }
                if(System.currentTimeMillis() - lastShakeUpdate > TIME_TRESHHOLD/2){
                    hasShaken = false;
                    lastShakeUpdate = System.currentTimeMillis();
                }
            }
            xShakeOld = xShake;
            oldX = xBend;
            oldY = y;
            oldZ = z;
        }
    }


    private class Timer extends Thread {

        @Override
        public void run() {

        }

    }

    private void shakeLeafs() {
        // Skakar bort löven
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

        //  shank[0].setScaleX(-1); // spegelvända en bild
    }
}
