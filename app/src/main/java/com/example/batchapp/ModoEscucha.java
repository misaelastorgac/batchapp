package com.example.batchapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ModoEscucha extends AppCompatActivity implements SensorEventListener {
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListner;
    boolean mostrar = false;
    public static int MILISEGUNDOS_ESPERA = 5000;
    private float lastX, lastY, lastZ;
    Chronometer crono;
    boolean hay=true;
    private long last_update = 0, last_movement = 0;
    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private long mShakeTime = 0;

    //private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance ();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    int contador=0;
    private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private float vibrateThreshold = 0;

    private TextView currentX, currentY, currentZ, maxX, maxY, maxZ;

    public Vibrator v;

    LocationManager locationManager;
    double longitudeBest, latitudeBest;
    double longitudeGPS, latitudeGPS;
    double longitudeNetwork, latitudeNetwork;
    TextView longitudeValueBest, latitudeValueBest;
    TextView longitudeValueGPS, latitudeValueGPS;
    TextView longitudeValueNetwork, latitudeValueNetwork;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListner);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_escucha);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        initializeViews();
        mAuth = FirebaseAuth.getInstance();
        mAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser()==null)
                {
                    startActivity(new Intent(ModoEscucha.this, MainActivity.class));
                }
            }
        };
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }

        //initialize vibration
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        Sensor mSensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (!checkLocation())
            return;
        //Button button = (Button) view;
        if (mostrar==true) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            locationManager.removeUpdates(locationListenerNetwork);
            //button.setText("continuar");
        }
        else {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 20 * 1000, 10, locationListenerNetwork);
            Toast.makeText(this, "Network provider started running", Toast.LENGTH_LONG).show();
            //button.setText(R.string.pause);
        }

    }
    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Su ubicación esta desactivada.\npor favor active su ubicación " +
                        "usa esta app")
                .setPositiveButton("Configuración de ubicación", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    public void toggleNetworkUpdates(View view) {
        if (!checkLocation())
            return;
        //Button button = (Button) view;
        if (mostrar==true) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            locationManager.removeUpdates(locationListenerNetwork);
            //button.setText("continuar");
        }
        else {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 20 * 1000, 10, locationListenerNetwork);
            Toast.makeText(this, "Network provider started running", Toast.LENGTH_LONG).show();
            //button.setText(R.string.pause);
        }
    }
    private final LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeNetwork = location.getLongitude();
            latitudeNetwork = location.getLatitude();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //longitudeValueNetwork.setText(longitudeNetwork + "");
                    //latitudeValueNetwork.setText(latitudeNetwork + "");
                    Toast.makeText(ModoEscucha.this, "Network Provider update", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {

        }
        @Override
        public void onProviderDisabled(String s) {

        }
    };

    public void initializeViews() {
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);


    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        //contador+=1;
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        contador=0;
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //@Override
    //public void onSensorChanged(SensorEvent event) {

        // clean current values
    //    displayCleanValues();
        // display the current x,y,z accelerometer values
     //   displayCurrentValues();
        // display the max x,y,z accelerometer values
     //   displayMaxValues();

        // get the change of the x,y,z values of the accelerometer
     //   deltaX = Math.abs(lastX - event.values[0]);
     //   deltaY = Math.abs(lastY - event.values[1]);
     //   deltaZ = Math.abs(lastZ - event.values[2]);

        // if the change is below 2, it is just plain noise
     //   if (deltaX < 2)
     //       deltaX = 0;
     //   if (deltaY < 2)
     //       deltaY = 0;

    //}
    public void esperarYCerrar(int milisegundos) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // acciones que se ejecutan tras los milisegundos
                contador=0;
            }
        }, milisegundos);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long current_time = event.timestamp;
    // clean current values
        displayCleanValues();
    // display the current x,y,z accelerometer values
       displayCurrentValues();
    // display the max x,y,z accelerometer values
       displayMaxValues();

    // get the change of the x,y,z values of the accelerometer
       deltaX = Math.abs(lastX - event.values[0]);
       deltaY = Math.abs(lastY - event.values[1]);
       deltaZ = Math.abs(lastZ - event.values[2]);

        if (lastX == 0 && lastY == 0 && lastZ == 0) {
            last_update = current_time;
            last_movement = current_time;
        }
    // if the change is below 2, it is just plain noise
       if (deltaX < 2)
          deltaX = 0;
       if (deltaY < 2)
           deltaY = 0;
        long time_difference = current_time - last_update;
        float movement = Math.abs((deltaX + deltaY + deltaZ) - (lastX - lastY - lastZ))/ current_time;

        detectShake(event,movement);



        //long time_difference = current_time - last_update;
        //Toast.makeText(getApplicationContext(), "Diferencia de " + time_difference, Toast.LENGTH_SHORT).show();

    }
    private void detectShake(SensorEvent event,float aceleracion) {
        long now = System.currentTimeMillis();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId = user.getUid();
        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //DatabaseReference myRef = database.getReference("message");
        //mDatabase.child("users").child(userId).setValue(user);
        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("DatosTerremoto");

        //myRef.setValue("Hello, World!");





        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];



            // gForce will be close to 1 when there is no movement
            double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            double d = gForce*(20+((0.5*aceleracion)*400));
            //double ML = (Math.log10(d)) + ((3*Math.log10(20)) - 2.92);
            //double magnitud=(ML-2)*10;
            //Toast.makeText(getApplicationContext(), "Movimiento Final, "+ gForce , Toast.LENGTH_SHORT).show();
            if (gForce > SHAKE_THRESHOLD) {
                //soundAcc.start();
                //long segundos = Math.round(now/1000.0);
                //DecimalFormat formato1 = new DecimalFormat("#.##");
                //formato1.format(segundos);
// aquí el proceso que quieres medir cuanto tarda
                //long t2 = System.currentTimeMillis();
                //now.setTime(now.getTime() + 5000);
                //System.out.println ("Ha tardado " + (t2-t1) + "milisegundos");
                //BigDecimal bigDecimal = new BigDecimal(segundos).setScale(2, RoundingMode.UP);
                float segundos = mShakeTime/100;
                double vf = gForce + (aceleracion*20);
                //double d = gForce*(20+((0.5*aceleracion)*400));
                contador+=1;
                hay=true;
                if(contador==2){
                    double ML = (Math.log10(10)) + ((3*Math.log10(10)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Micro-sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();


                }
                if(contador==4){
                    double ML = (Math.log10(22)) + ((3*Math.log10(20)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Micro-sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                }
                if(contador==6){
                    double ML = (Math.log10(35)) + ((3*Math.log10(29)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Micro-sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                }
                if(contador==8){
                    double ML = (Math.log10(60)) + ((3*Math.log10(55)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Sismo menor, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                }
                if(contador==10){
                    double ML = (Math.log10(130)) + ((3*Math.log10(127)) - 2.92);

                    Toast.makeText(getApplicationContext(), "Sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                }
                if(contador==12){
                    double ML = (Math.log10(180)) + ((3*Math.log10(180)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Terremoto, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                }
                if(contador==14){
                    double ML = (Math.log10(315)) + ((3*Math.log10(315)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Terremoto Mayor, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                }
                if(contador==17){
                    double ML = (Math.log10(600)) + ((3*Math.log10(600)) - 2.92);
                    Toast.makeText(getApplicationContext(), "Gran Terremoto, magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                }
                if(contador==20){
                    contador=0;
                }
                //Toast.makeText(getApplicationContext(), "Movimiento Final, "+ gForce + ", Movimiento Inicial,"+movement, Toast.LENGTH_SHORT).show();
                //Toast.makeText(getApplicationContext(), "Se Sacudio "+contador+" veces", Toast.LENGTH_SHORT).show();
            }else{
                hay=false;
            }

        }else{
            if(((now - mShakeTime) < SHAKE_WAIT_TIME_MS)&&hay==false){
                //esperarYCerrar(MILISEGUNDOS_ESPERA);
            }

        }
    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }

    // display the max x,y,z accelerometer values
    public void displayMaxValues() {
        if (deltaX > deltaXMax) {
            deltaXMax = deltaX;

        }
        if (deltaY > deltaYMax) {
            deltaYMax = deltaY;

        }
        if (deltaZ > deltaZMax) {
            deltaZMax = deltaZ;

        }
    }
}