package name.mikanoshi.customiuizer.holidays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.Surface;
import android.view.WindowManager;

import com.github.jinatonic.confetti.confetto.Confetto;
import com.github.matteobattilana.weather.confetti.ConfettoInfo;

import java.util.Random;

import org.lsposed.manager.R;

@SuppressWarnings("FieldCanBeLocal")
public class FlowerParticle extends Confetto {
    private final ConfettoInfo confettoInfo;
    private final Bitmap petal;
    private float petalScale;
    private final int[] petals = new int[]{R.drawable.confetti1, R.drawable.confetti1, R.drawable.confetti2, R.drawable.confetti2, R.drawable.confetti3, R.drawable.confetti3, R.drawable.petal};

    FlowerParticle(Context context, ConfettoInfo confettoInfo) {
        super();
        this.confettoInfo = confettoInfo;
        petalScale = 0.6f - (float) Math.random() * 0.15f;
        petal = BitmapFactory.decodeResource(context.getResources(), petals[new Random().nextInt(petals.length)]);

        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) petalScale *= 1.5;
    }

    public int getHeight() {
        return 0;
    }

    public int getWidth() {
        return 0;
    }

    public void reset() {
        super.reset();
    }

    protected void configurePaint(Paint paint) {
        super.configurePaint(paint);
        paint.setColor(-1);
        paint.setAntiAlias(true);
    }

    protected void drawInternal(Canvas canvas, Matrix matrix, Paint paint, float x, float y, float rotation, float percentageAnimated) {
        switch (confettoInfo.getPrecipType()) {
            case CLEAR:
                break;
            case SNOW:
                matrix.postScale(petalScale, petalScale);
                matrix.postRotate(rotation, petal.getWidth() / 2f, petal.getHeight() / 2f);
                matrix.postTranslate(x, y);
                canvas.drawBitmap(petal, matrix, paint);
                break;
        }
    }

    public final ConfettoInfo getConfettoInfo() {
        return this.confettoInfo;
    }
}
