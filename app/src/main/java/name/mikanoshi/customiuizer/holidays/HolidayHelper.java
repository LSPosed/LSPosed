package name.mikanoshi.customiuizer.holidays;

import android.app.Activity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.github.jinatonic.confetti.ConfettiManager;
import com.github.jinatonic.confetti.ConfettoGenerator;
import com.github.matteobattilana.weather.PrecipType;
import com.github.matteobattilana.weather.WeatherView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import io.github.lsposed.manager.R;
import name.mikanoshi.customiuizer.utils.GravitySensor;
import name.mikanoshi.customiuizer.utils.Helpers;

public class HolidayHelper {

    private static WeakReference<WeatherView> weatherView;
    private static WeakReference<GravitySensor> angleListener;

    public static void setWeatherGenerator(ConfettoGenerator generator) {
        try {
            ConfettiManager manager = weatherView.get().getConfettiManager();
            Field confettoGenerator = ConfettiManager.class.getDeclaredField("confettoGenerator");
            confettoGenerator.setAccessible(true);
            confettoGenerator.set(manager, generator);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void setup(Activity activity) {
        Helpers.detectHoliday();

        WeatherView view = activity.findViewById(R.id.weather_view);
        ImageView header = activity.findViewById(R.id.holiday_header);

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        weatherView = new WeakReference<>(view);
        GravitySensor listener = null;
        if (Helpers.currentHoliday == Helpers.Holidays.NEWYEAR) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            view.setPrecipType(PrecipType.SNOW);
            view.setSpeed(50);
            view.setEmissionRate(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 8 : 4);
            view.setFadeOutPercent(0.75f);
            view.setAngle(0);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
            lp.height = activity.getResources().getDisplayMetrics().heightPixels / (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 2 : 3);
            view.setLayoutParams(lp);
            setWeatherGenerator(new SnowGenerator(activity));
            view.resetWeather();
            view.setVisibility(View.VISIBLE);
            view.getConfettiManager().setRotationalVelocity(0, 45);

            listener = new GravitySensor(activity, view);
            listener.setOrientation(rotation);
            listener.setSpeed(50);
            listener.start();

            header.setImageResource(R.drawable.newyear_header);
            header.setVisibility(View.VISIBLE);
        } else if (Helpers.currentHoliday == Helpers.Holidays.LUNARNEWYEAR) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            view.setPrecipType(PrecipType.SNOW);
            view.setSpeed(35);
            view.setEmissionRate(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 4 : 2);
            view.setFadeOutPercent(0.75f);
            view.setAngle(0);
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
            lp.height = activity.getResources().getDisplayMetrics().heightPixels / (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 3 : 4);
            view.setLayoutParams(lp);
            setWeatherGenerator(new FlowerGenerator(activity));
            view.resetWeather();
            view.setVisibility(View.VISIBLE);
            view.getConfettiManager().setRotationalVelocity(0, 45);

            listener = new GravitySensor(activity, view);
            listener.setOrientation(rotation);
            listener.setSpeed(35);
            listener.start();

            header.setImageResource(R.drawable.lunar_newyear_header);
            header.setVisibility(View.VISIBLE);
        } else {
            ((ViewGroup) view.getParent()).removeView(view);
            ((ViewGroup) header.getParent()).removeView(header);
        }
        angleListener = new WeakReference<>(listener);
    }

    public static void onPause() {
        GravitySensor listener = angleListener.get();
        if (listener != null) listener.onPause();
        WeatherView view = weatherView.get();
        if (view != null) view.getConfettiManager().terminate();
    }

    public static void onResume() {
        GravitySensor listener = angleListener.get();
        if (listener != null) listener.onResume();
        WeatherView view = weatherView.get();
        if (view != null) view.getConfettiManager().animate();
    }

    public static void onDestroy() {
        GravitySensor listener = angleListener.get();
        if (listener != null) listener.stop();
    }

}
