package name.mikanoshi.customiuizer.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

import com.github.matteobattilana.weather.WeatherView;

public final class GravitySensor implements SensorEventListener {
    private final SensorManager sensorManager;
    private float[] magneticValues;
    private float[] accelerometerValues;
    private int orientation;
    private int speed;
    private boolean started;
    private final Context context;
    private final WeatherView weatherView;

    public GravitySensor(Context context, WeatherView weatherView) {
        super();
        this.context = context;
        this.weatherView = weatherView;
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void setOrientation(int orient) {
        this.orientation = orient;
    }

    public void setSpeed(int spd) {
        this.speed = spd;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        switch (event.sensor.getType()) {
            case 1:
                this.accelerometerValues = event.values;
                break;
            case 2:
                this.magneticValues = event.values;
                break;
        }
        if (this.magneticValues == null || this.accelerometerValues == null) return;

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrix(rotationMatrix, null, this.accelerometerValues, this.magneticValues);
        float[] remappedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix);
        float[] orientationAngles = new float[3];
        SensorManager.getOrientation(remappedRotationMatrix, orientationAngles);
        //double pitch = Math.toDegrees((double)orientationAngles[1]);
        double roll = Math.toDegrees(orientationAngles[2]) + Math.random() * 20 - 10;
        if (this.orientation == Surface.ROTATION_90) roll += 90;
        else if (this.orientation == Surface.ROTATION_270) roll -= 90;
        else if (this.orientation == Surface.ROTATION_180) roll += roll > 0 ? 180 : -180;
        if (roll > 90) roll -= 180;
        else if (roll < -90) roll += 180;
        this.weatherView.setAngle((int) roll);
        this.weatherView.setSpeed(this.speed + (int) Math.round(Math.random() * 20 - 10));
    }

    private void registerListener() {
        this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(1), 2);
        this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(2), 2);
    }

    private void unregisterListener() {
        this.sensorManager.unregisterListener(this);
    }

    public final void start() {
        this.started = true;
        this.registerListener();
    }

    public final void stop() {
        this.started = false;
        this.unregisterListener();
    }

    public final void onResume() {
        if (this.started) {
            this.registerListener();
        }
    }

    public final void onPause() {
        this.unregisterListener();
    }

    public final Context getContext() {
        return this.context;
    }

}
