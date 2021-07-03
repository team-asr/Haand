package asr.carinae.eta.haand;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsMessage;
import android.text.Html;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainInterface
        extends Activity
        implements LocationListener, SensorEventListener, View.OnClickListener, TextToSpeech.OnInitListener, DataListener, SmsReceivedListener {

    static final private int REQUEST_CODE = 0x2324242;
    static final private int LOCATION_REQUEST_CODE = 0x2324242;

    static private LocationManager locMgr = null;
    static private Sensor magnetometer = null, accelerometer = null;
    static private SensorManager senMgr = null;
    static private float orientation[] = null;

    static private GeoApiContext mapApiContext = null;
    static private TextToSpeech voice = null;

    static private SmsReceiver smsHandler = new SmsReceiver();

    private ArrayList<String> results = null;
    private float mGravity[] = null, mGeomagnetic[] = null;

    private Button speak = null;
    private Location location = null;

    static private MainInterface mainInterface = null;

    static private HaandDriver haandDriver = null;

    protected void enableBluetooth() {
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth, 0);
    }

    static protected MainInterface getInstance() {
        return mainInterface;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_interface);
        speak = (Button) this.findViewById(R.id.speak);
        speak.setOnClickListener(this);
        locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String permissions[] = {"android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.RECORD_AUDIO", "android.permission.VIBRATE", "android.permission.ACCESS_COARSE_LOCATION"};
            ActivityCompat.requestPermissions(this, permissions, 1);
            return;
        }
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        senMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = senMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = senMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senMgr.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        senMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mapApiContext = new GeoApiContext().setApiKey("AIzaSyDBz6ut7L30slLe9Gi5jgm2jR-euQy-xuc");
        location = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        voice = new TextToSpeech(this, this);

        this.getApplicationContext().registerReceiver(smsHandler,
                new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        smsHandler.addSMSReceivedListener(this);
        this.mainInterface = this;

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) voice.setLanguage(Locale.getDefault());
        //this.speak("Welcome, ghishanga");
        this.haandDriver = HaandDriver.getHaandDriver();
        this.haandDriver.addDataListener(this);
        this.speak("Welcome, ghishanga");

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //System.out.println(event.values[0]);
        /*if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9], I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                //System.out.println("Heading: " + orientation[0]);
            }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private LocationManager getLocationManager() {
        return this.locMgr;
    }

    private void vibrate(VibrationConstants option) {
        System.out.println("Vrrrrrrmmmmmmmmmmmmmmmmmm :-)");
        switch (option) {
            case SMS_VIBRATION:
            default:
                return;
        }
    }

    private boolean isConnected() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) return true;
        else return false;
    }

    private void recognize() {
        if (isConnected()) {
            Intent speech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            this.startActivityForResult(speech, REQUEST_CODE);
        }
    }


    private String getCurrentLocation() {
        double lat = 0d, lon = 0d;
        String currrentLocation = "";
        if (location == null)
            return "";
        try {
            lat = location.getLatitude();
            lon = location.getLongitude();
            System.out.println("Latitude: " + lat + ", Longitude: " + lon);
            GeocodingResult[] results = GeocodingApi.reverseGeocode(mapApiContext, new LatLng(lat, lon)).await();
            currrentLocation = results[0].formattedAddress;
            for (int a = 0; a < results.length; a++)
                System.out.println(results[a].formattedAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currrentLocation;
    }

    private String getOwnersName() {
        return "ghishahngah";
    }

    private void navigation(String destination) {
        System.out.println("Destination: " + destination);
        DirectionsApiRequest request = DirectionsApi.newRequest(mapApiContext);
        this.speak("Okay " + this.getOwnersName() + ". Please wait while I figure out how to get to " + destination + ".");
        try {
            request.mode(TravelMode.WALKING);
            request.mode(TravelMode.WALKING);
            request.origin(new LatLng(this.location.getLatitude(), this.location.getLongitude()));
            request.destination(destination);
            DirectionsResult results = request.await();
            DirectionsRoute routes[] = results.routes;
            if (routes.length < 1) {
                this.speak("There doesnt seem to be routes available to " + destination + ". Please try again later.");
                return;
            }
            DirectionsLeg legs[] = routes[0].legs;
            if (legs.length < 1) {
                this.speak("There doesnt seem to be legs available to " + destination + ". Please try again later.");
                return;
            }
            DirectionsStep steps[] = legs[0].steps;
            this.walk(steps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void walk(DirectionsStep steps[]) {
        String instructions = "";
        for (DirectionsStep step : steps) {
            instructions = Html.fromHtml(step.htmlInstructions).toString();
            System.out.println("First waypoint: " + step.startLocation + ", end waypoint: " + step.endLocation + ", current position: " + this.getCurrentLocation());
            this.getLocationManager().addProximityAlert(step.endLocation.lat, step.endLocation.lng, 5,
                    -1, this.createPendingResult(MainInterface.LOCATION_REQUEST_CODE, new WalkIntent(steps), PendingIntent.FLAG_UPDATE_CURRENT));
            System.out.println(instructions);
            this.speak(instructions);
            return;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            for (String s : results)
                System.out.print(s + " ");
            System.out.println("");
            this.processSpeechRequest(results.get(0).toLowerCase().trim());
        } else if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            WalkIntent walkData = (WalkIntent) data;
            DirectionsStep[] oldSteps = walkData.getSteps();
            DirectionsStep[] steps = Arrays.copyOfRange(oldSteps, 1, oldSteps.length);
            this.walk(steps);
        }
    }

    private void processSpeechRequest(String request) {
        if (request.contains("messages"))
            this.readNewSms();
        else if (request.contains("time")) {
            SimpleDateFormat format = (SimpleDateFormat) DateFormat.getDateInstance();
            format.applyPattern("h m a");
            this.speak("The time is now " + format.format(new Date()));
        } else if (request.contains("where") || request.contains("location"))
            this.speak("You are currently at " + this.getCurrentLocation());
        else if (request.contains("date")) {
            SimpleDateFormat format = (SimpleDateFormat) DateFormat.getDateInstance();
            format.applyPattern("E M d y");
            this.speak("Today is " + format.format(new Date()));
        } else if (request.contains("take") || request.contains("navigate")) {
            //take me to ndumberi
            String destination = request.substring(request.indexOf("to ") + 2).trim();
            this.navigation(destination.substring(0, 1).toUpperCase() + destination.substring(1));
        } else
            System.out.println("I have no idea what you said");
    }

    private void speak(String message) {
        voice.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        while (voice.isSpeaking()) {
        }
    }

    @Override
    public void onDestroy() {
        if (voice != null) voice.stop();
        voice.shutdown();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (v == speak)
            recognize();
    }

    static boolean read = false;

    @Override
    public void dataReceived(String data) {
        //0,0,0,0,0
        boolean keymap[] = new boolean[5];
        String payload[] = data.split(",");
        System.out.println("Received---> " + data + "; Length--->" + payload.length);

        if (payload.length != 5)
            return;
        for (int a = 0; a < payload.length; a++)
            keymap[a] = payload[a].contains("1") ? true : false;

        if (keymap[0]) {
            processSpeechRequest("where");
        } else if (keymap[1]) {
            processSpeechRequest("date");
        } else if (keymap[2])
            processSpeechRequest("take me to Nairobi");
        else if (keymap[3])
            processSpeechRequest("messages");
        data = null;
    }

    static SmsMessage smsMessage = null;

    @Override
    public void smsReceived(SmsMessage smsMessage) {
        this.smsMessage = smsMessage;
        this.vibrate(VibrationConstants.SMS_VIBRATION);
        System.out.println(smsMessage);
    }

    private void readNewSms() {
        String message = "Message from " + smsMessage.getOriginatingAddress() +
                ". " + smsMessage.getMessageBody();
        this.speak(message);
    }
}

enum VibrationConstants {
    SMS_VIBRATION,
    NO_NETWORK_VIBRATION
};