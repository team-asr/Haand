package asr.carinae.eta.haand;

import android.content.Intent;

import com.google.maps.model.DirectionsStep;

/**
 * Created by moses gichangA on 10/7/2016.
 */
public class WalkIntent extends Intent {
    private DirectionsStep steps[] = null;
    public static final Creator<WalkIntent> CREATOR = null;

    WalkIntent() {
        super();
    }

    WalkIntent(DirectionsStep steps[]) {
        this.steps = steps;
    }

    protected DirectionsStep[] getSteps() {
        return this.steps;
    }
}
