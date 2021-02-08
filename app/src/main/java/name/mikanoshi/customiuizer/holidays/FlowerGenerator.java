package name.mikanoshi.customiuizer.holidays;

import android.content.Context;

import com.github.jinatonic.confetti.ConfettoGenerator;
import com.github.jinatonic.confetti.confetto.Confetto;
import com.github.matteobattilana.weather.PrecipType;
import com.github.matteobattilana.weather.confetti.ConfettoInfo;

import java.util.Random;

public class FlowerGenerator implements ConfettoGenerator {
    private final ConfettoInfo confettoInfo;
    private final Context context;

    public FlowerGenerator(Context ctx) {
        super();
        this.context = ctx;
        this.confettoInfo = new ConfettoInfo(PrecipType.SNOW);
    }

    public Confetto generateConfetto(Random random) {
        return new FlowerParticle(this.context, this.confettoInfo);
    }

    public final ConfettoInfo getConfettoInfo() {
        return this.confettoInfo;
    }
}
