package com.example.batchapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuPrincipal extends AppCompatActivity implements SensorEventListener {
    List<Datos> camp;
    ArrayList<OverlayItem> puntos = new ArrayList<OverlayItem>();
    public static int MILISEGUNDOS_ESPERA = 5000;
    private float lastX, lastY, lastZ;
    Chronometer crono;
    boolean hay=true;
    private long last_update = 0, last_movement = 0;
    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private long mShakeTime = 0;
    boolean mostrar = false;
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
    public Vibrator v;
    private float vibrateThreshold = 0;

    private TextView currentX, currentY, currentZ, maxX, maxY, maxZ;
    private MapView myOpenMapView;
    private MapController myMapController;
    private GeoPoint posicionActual;
    LocationManager locationManager;
    double longitudeNetwork, latitudeNetwork;
    double longitude, latitude;
    FirebaseAuth mAuth;
    private RadioGroup radioGroup;
    private RadioButton radioButton;
    Drawable icono;
    private int numeroIcono=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        toggleNetworkUpdates();
       // requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (tengoPermisoEscritura()) {
            cargarMapas();
        }
        DatabaseReference  databasePostsReference = FirebaseDatabase.getInstance().getReference("bachApp");
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                  GeoPoint geopunto = new GeoPoint((double)postSnapshot.child("latitudeNetwork").getValue(),(double)postSnapshot.child("longitudeNetwork").getValue());
                  Log.d("entro",postSnapshot+"");
                  String titulo = postSnapshot.child("titulo").getValue().toString();
                  String mensaje = postSnapshot.child("mensaje").getValue().toString();
                  int i = Integer.valueOf(postSnapshot.child("icono").getValue().toString());
                  OverlayItem marcador = new OverlayItem(titulo, mensaje, geopunto);
                  marcador.setMarker(obtenerIconoFirebase(i));
                  puntos.add(marcador);
                  refrescaPuntos();
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        databasePostsReference.addListenerForSingleValueEvent(eventListener);
        FirebaseMessaging.getInstance().subscribeToTopic("camping")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = getString(R.string.msg_subscribed);
                        if (!task.isSuccessful()) {
                            msg = getString(R.string.msg_subscribe_failed);
                        }

                       // Toast.makeText(MenuPrincipal.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

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




    public Drawable obtenerIconoFirebase(int i){
        Drawable icon = ContextCompat.getDrawable(this,R.drawable.center);
        switch(i){
            case 2131230843:
                icon =  ContextCompat.getDrawable(this, R.drawable.email);

                break;
            case 2131230768:
                icon =  ContextCompat.getDrawable(this, R.drawable.camping);

                break;
            case 2131230867:
                icon =  ContextCompat.getDrawable(this, R.drawable.fish);

                break;
            case 2131230813:
                icon =  ContextCompat.getDrawable(this, R.drawable.bonfire);

                break;
            case 2131230800:
                icon =  ContextCompat.getDrawable(this, R.drawable.alarm);

                break;


        }
        return icon;
    }
    public void addListenerOnButton() {

        radioGroup = findViewById(R.id.opciones_marcador);
        // get selected radio button from radioGroup
                int selectedId = radioGroup.getCheckedRadioButtonId();
                // find the radiobutton by returned id
                radioButton =  findViewById(selectedId);

        switch(radioButton.getId()){
            case 2131230843:
                icono =  ContextCompat.getDrawable(this, R.drawable.email);
                numeroIcono = 2131230843;
                break;
            case 2131230768:
                icono =  ContextCompat.getDrawable(this, R.drawable.camping);
                numeroIcono = 2131230768;
                break;
            case 2131230867:
                icono =  ContextCompat.getDrawable(this, R.drawable.fish);
                numeroIcono = 2131230867;
                break;
            case 2131230813:
                icono =  ContextCompat.getDrawable(this, R.drawable.bonfire);
                numeroIcono = 2131230813;
                break;
            case 2131230800:
                icono =  ContextCompat.getDrawable(this, R.drawable.alarm);
                numeroIcono = 2131230800;
                break;


        }



    }
    public void crearDialogo(View view){
/*        addListenerOnButton();*/
        Context context = view.getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
       /* final EditText inputTitulo = new EditText(this);
        final EditText inputMensaje = new EditText(this);
        inputTitulo.setInputType(InputType.TYPE_CLASS_TEXT);
        inputMensaje.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTitulo.setHint("Titulo");
        inputMensaje.setHint("Mensaje");
        layout.addView(inputTitulo);
        layout.addView(inputMensaje);*/
        new AlertDialog.Builder(this)
                .setTitle("¿ESO FUE UN BACHE?")
                .setView(layout)
                .setPositiveButton("Afirmativo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(),inputTitulo.getText().toString()+" "+ inputMensaje.getText().toString(), Toast.LENGTH_SHORT).show();
                        onClickBoton("BACHE","");

                        //Marca(latitude,longitude,inputTitulo.getText().toString(),inputMensaje.getText().toString());
                    }
                })
                .setNegativeButton("Negativo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
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
    public void guardar(double latitud, double longitud,String titulo, String mensaje){
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId = user.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("bachApp");
        Datos datos = new Datos(longitud,latitud,titulo,mensaje,userId,numeroIcono);

        Map<String, Object> datosValues = datos.toMap();
        myRef.push().setValue(datosValues);
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    public void toggleNetworkUpdates() {
        if (!checkLocation())
            return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        locationManager.removeUpdates(locationListenerNetwork);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 20 * 1000, 10, locationListenerNetwork);
        Toast.makeText(this, "Network provider started running", Toast.LENGTH_LONG).show();

    }

    private final LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeNetwork = location.getLongitude();
            latitudeNetwork = location.getLatitude();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MenuPrincipal.this, "Network Provider update", Toast.LENGTH_SHORT).show();
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

    //Metodo para dibujar marcador de firebase
    public void Marca (double lat, double lon, String titulo, String mensaje)  {
        puntos.clear();
        GeoPoint nuevo = new GeoPoint(lat, lon);
        myMapController.setCenter(nuevo);
        OverlayItem marcador = new OverlayItem(titulo, mensaje, nuevo);
        marcador.setMarker(ResourcesCompat.getDrawable(getResources(), R.drawable.center, null));
        puntos.add(marcador);
        //guardar(latitude,longitude,"madera","Se encontro madera en esta posicion");
    }

    public void onClickBoton (String titulo, String mensaje)  {
        // do something when the button is clicked
        //puntos.clear();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        GeoPoint nuevo = new GeoPoint(latitude, longitude);
        //OverlayItem nue = new OverlayItem("Madera","se encontro madera en esta posicion",nuevo);
        myMapController.setCenter(nuevo);

        OverlayItem marcador = new OverlayItem("Tu: "+titulo, mensaje, nuevo);
        marcador.setMarker(ContextCompat.getDrawable(this, R.drawable.barrier));
        puntos.add(marcador);
        refrescaPuntos();
        String tituloConUser =user.getEmail()+" : "+titulo;
        guardar(latitude,longitude,tituloConUser, mensaje);
        Log.d("marcadores",""+puntos);
        //puntos.add(new OverlayItem("Madera", "Se encontro madera en esta posicion", nuevo));
        //puntos.add(0,new OverlayItem("Madera", "Se encontro madera en esta posicion", nuevo));
        //Toast.makeText(getApplicationContext(),"madera "+latitude+" "+longitude,Toast.LENGTH_SHORT).show();
    }


    private void cargarMapas() {
        GeoPoint actual = new GeoPoint(latitudeNetwork, longitudeNetwork);

        myOpenMapView = (MapView) findViewById(R.id.openmapview);
        myOpenMapView.setBuiltInZoomControls(true);
        myOpenMapView.setClickable(true);
        myMapController = (MapController) myOpenMapView.getController();
        myMapController.setCenter(actual);
        myMapController.setZoom(6);

        myOpenMapView.setMultiTouchControls(true);

        ///////////////////////////////////
        //Centrar en la posición actual
        final MyLocationNewOverlay myLocationoverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getApplicationContext()), myOpenMapView);
        myOpenMapView.getOverlays().add(myLocationoverlay); //No añadir si no quieres una marca
        myLocationoverlay.enableMyLocation();
        myLocationoverlay.runOnFirstFix(new Runnable() {
            public void run() {
                myMapController.animateTo(myLocationoverlay.getMyLocation());
            }
        });

        /////////////////////////////////////////
        // Añadir un punto en el mapa
        //puntos.add(new OverlayItem("Posicion Actual", "Estas aqui", actual));
        refrescaPuntos();

        /////////////////////////////////////////
        // Detectar cambios de ubicación mediante un listener (OSMUpdateLocation)
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        OSMUpdateLocation detectaPosicion = new OSMUpdateLocation(this);
        if (tengoPermisoUbicacion()) {
            Location ultimaPosicionConocida = null;
            for (String provider : locationManager.getProviders(true)) {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    ultimaPosicionConocida = locationManager.getLastKnownLocation(provider);
                if (ultimaPosicionConocida != null) {
                    actualizaPosicionActual(ultimaPosicionConocida);
                }
                //Pedir nuevas ubicaciones
                locationManager.requestLocationUpdates(provider, 0, 0, detectaPosicion);
                break;
            }
        } else {
            // No tengo permiso de ubicación
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent();
            intent.setClass(this, this.getClass());
            startActivity(intent);
            finish();
        } else {
            // El usuario no ha dado permiso
        }
    }

    public boolean tengoPermisoEscritura() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean tengoPermisoUbicacion() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                return false;
            }
        } else {
            return true;
        }
    }

    public void actualizaPosicionActual(Location location) {
        posicionActual = new GeoPoint(location.getLatitude(), location.getLongitude());
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        myMapController.setCenter(posicionActual);
        if (puntos.size() > 1)
           // puntos.remove(1);
        //OverlayItem marcador = new OverlayItem("Estás aquí", "Posicion actual", posicionActual);
        //marcador.setMarker(ResourcesCompat.getDrawable(getResources(), R.drawable.center, null));
        //puntos.add(marcador);
        refrescaPuntos();
    }

    private void refrescaPuntos() {
        myOpenMapView.getOverlays().clear();
        ItemizedIconOverlay.OnItemGestureListener<OverlayItem> tap = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemLongPress(int arg0, OverlayItem arg1) {
                return false;
            }

            @Override

            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                return true;
            }
        };

        ItemizedOverlayWithFocus<OverlayItem> capa = new ItemizedOverlayWithFocus<>(this, puntos, tap);
        capa.setFocusItemsOnTap(true);
        myOpenMapView.getOverlays().add(capa);
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

    }





    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
/*        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));*/
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

    public void displayCleanValues() {
      /*  currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");*/
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
                    //Toast.makeText(getApplicationContext(), "Micro-sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==4){
                    double ML = (Math.log10(22)) + ((3*Math.log10(20)) - 2.92);
                   // Toast.makeText(getApplicationContext(), "Micro-sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==6){
                    double ML = (Math.log10(35)) + ((3*Math.log10(29)) - 2.92);
                   // Toast.makeText(getApplicationContext(), "Micro-sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==8){
                    double ML = (Math.log10(60)) + ((3*Math.log10(55)) - 2.92);
                    //Toast.makeText(getApplicationContext(), "Sismo menor, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==10){
                    double ML = (Math.log10(130)) + ((3*Math.log10(127)) - 2.92);

                  //  Toast.makeText(getApplicationContext(), "Sismo, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==12){
                    double ML = (Math.log10(180)) + ((3*Math.log10(180)) - 2.92);
                    //Toast.makeText(getApplicationContext(), "Terremoto, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==14){
                    double ML = (Math.log10(315)) + ((3*Math.log10(315)) - 2.92);
                   // Toast.makeText(getApplicationContext(), "Terremoto Mayor, Magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                    crearDialogo(findViewById(android.R.id.content));
                }
                if(contador==17){
                    double ML = (Math.log10(600)) + ((3*Math.log10(600)) - 2.92);
                   // Toast.makeText(getApplicationContext(), "Gran Terremoto, magnitud: "+ML, Toast.LENGTH_SHORT).show();
                    //Datos usuario = new Datos(longitudeNetwork,latitudeNetwork,contador,userId,x,y,z,ML);
                    //myRef.setValue(usuario);
                    crearDialogo(findViewById(android.R.id.content));
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}