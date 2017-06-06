package xyz.facex.imitaterunning;

import android.animation.TypeEvaluator;

/**
 * Created by zhu on 17-6-1.
 */

public class LngLatEvaluator implements TypeEvaluator {
    @Override
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        LngLat startLnglat = (LngLat) startValue;
        LngLat endLnglat = (LngLat) endValue;
        double lng = startLnglat.getLng() + fraction * (endLnglat.getLng() - startLnglat.getLng());
        double lat = startLnglat.getLat() + fraction * (endLnglat.getLat() - startLnglat.getLat());
        return new LngLat(lng,lat);
    }
}
