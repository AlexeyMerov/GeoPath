package com.woxapp.task.geopath.map.animation;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.graphics.Bitmap;
import android.util.Property;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MarkerAnimation {

    private static ObjectAnimator mAnimator;
    private static LatLngInterpolator mLatLngInterpolator = new LatLngInterpolator.Spherical();
    private static Bitmap mCarBitmap;

    private static List<LatLng> mTrips = new ArrayList<>();
    private static Marker mMarker;
    private static long mAnimationTime;

    public static void setCarIcon(Bitmap carBitmap) {
        mCarBitmap = carBitmap;
    }

    public static void animateLine(List<LatLng> trips, GoogleMap map) {
        mTrips.addAll(trips);

        mMarker = map.addMarker(new MarkerOptions().position(trips.get(0))
                .icon(BitmapDescriptorFactory.fromBitmap(mCarBitmap)));

        animateMarker();
    }

    public static void stopAnimation() {
        if (mAnimator != null) {
            mMarker.remove();
            mTrips.clear();
            mAnimator.removeAllListeners();
            mAnimator.end();
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    public static void pauseAnimation() {
        if(mAnimator != null) {
            mAnimationTime = mAnimator.getCurrentPlayTime();
            mAnimator.cancel();
            mAnimator.removeAllListeners();

        }
    }

    public static void playAnimation() {
        if (mAnimator != null) {
//            mAnimator.start();
            mAnimator.setCurrentPlayTime(mAnimationTime);
            mAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    //  animDrawable.stop();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    //  animDrawable.stop();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    //  animDrawable.stop();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //  animDrawable.stop();
                    if (mTrips.size() > 1) {
                        mTrips.remove(0);
                        animateMarker();
                    }
                }
            });
            mAnimator.start();

        }
    }

    private static void animateMarker() {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return mLatLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        mAnimator = ObjectAnimator.ofObject(mMarker, property, typeEvaluator, mTrips.get(0));

        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
                //  animDrawable.stop();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //  animDrawable.stop();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                //  animDrawable.stop();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //  animDrawable.stop();
                if (mTrips.size() > 1) {
                    mTrips.remove(0);
                    animateMarker();
                }
            }
        });

        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setDuration(300);
        mAnimator.start();
    }
}