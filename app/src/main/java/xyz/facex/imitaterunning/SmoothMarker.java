package xyz.facex.imitaterunning;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * 按照指定的经纬度数据和时间，平滑移动
 */
public class SmoothMarker {

    private AMap mAMap;
    //默认总时长为10000ms
    //    private long duration = 10000L;
    /**
     * 平滑移动的速度，单位km/h
     */
    private float speed;

    //每段点的队列，第一个点为起点
    private LinkedList<LatLng> points = new LinkedList<LatLng>();
    //每段距离队列  大小为points.size() - 1
    private LinkedList<Double> eachDistance = new LinkedList<Double>();
    private double totalDistance = 0;
    private double remainDistance = 0; // 剩余距离
    private LatLng endPoint, lastEndPoint;

    //Marker位置
    private Marker marker = null;

    private BitmapDescriptor descriptor;

    private boolean useDefaultDescriptor = false;

    private SmoothMarkerMoveListener moveListener;

    //marker动画
    private ValueAnimator markerAnimator;
    //是否暂停动画
    private boolean animStop = true;


    private LatLng currentLatlng;

    private LatLng nextPos;

    private boolean animCancel = false;

    private double currentPlayDistance;
    private double currentRemainDistance;


    public interface SmoothMarkerMoveListener {
        void move(double remainDistance, double totalDistance);

        void stop();
    }

    public SmoothMarker(AMap mAMap) {
        this.mAMap = mAMap;
    }


    public void setPoint(LatLng point) {
        if (point == null)
            return;
        List<LatLng> list = new ArrayList<LatLng>();
        list.add(point);
        setPoints(list);
    }


    /**
     * 设置平滑移动的经纬度数组
     *
     * @param points
     */
    public void setPoints(List<LatLng> points) {
        this.points.clear();
        for (LatLng latLng : points) {
            this.points.add(latLng);
        }

        if (points.size() > 1) {
            endPoint = points.get(points.size() - 1);
            lastEndPoint = points.get(points.size() - 2);
        }

        eachDistance.clear();
        totalDistance = 0;

        //计算比例
        for (int i = 0; i < points.size() - 1; i++) {
            double distance = AMapUtils.calculateLineDistance(points.get(i), points.get(i + 1));
            eachDistance.add(distance);
            totalDistance += distance;
        }

        remainDistance = totalDistance;

        LatLng markerPoint = this.points.removeFirst();

        if (marker != null) {
            marker.setPosition(markerPoint);
            //判断是否使用正确的图标
            checkMarkerIcon();
        } else {
            if (descriptor == null) {
                useDefaultDescriptor = true;
            }
            marker = mAMap.addMarker(new MarkerOptions().belowMaskLayer(true).position
                    (markerPoint).icon(descriptor).title("").anchor(0.5f, 0.5f));
        }

    }

    /**
     * 判断是否使用的是设置的icon
     */
    private void checkMarkerIcon() {
        if (useDefaultDescriptor) {
            if (descriptor == null) {
                useDefaultDescriptor = true;
            } else {
                marker.setIcon(descriptor);
                useDefaultDescriptor = false;
            }
        }
    }
    //
    //    /**
    //     * 设置平滑移动的总时间
    //     *
    //     * @param duration 单位: 毫秒
    //     */
    //    public void setTotalDuration(long duration) {
    //        if (duration <= 1000)
    //            duration = 4000;
    //        this.duration = duration;
    //    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * 开始平滑移动
     */
    public void startSmoothMove() {
        if (points.size() < 1) {
            return;
        }
        animStop = false;
        startRun();
    }

    private void startRun() {
        if (points.size() < 1) {
            setEndRotate();
            return;
        }
        final double dis = eachDistance.poll();
        //        final long time = (long) (duration * (dis / totalDistance));
        final long time = (long) (dis / speed * 60 * 60);
        //计算旋转
        LatLng curPos = marker.getPosition();
        nextPos = points.poll();
        Log.d(TAG, "next: " + nextPos.toString());
        float rotate = getRotate(curPos, nextPos);
        marker.setRotateAngle(360 - rotate + mAMap.getCameraPosition().bearing);
        final LngLat curLngLat = new LngLat(curPos.longitude, curPos.latitude);
        LngLat nextLngLat = new LngLat(nextPos.longitude, nextPos.latitude);
        markerAnimator = ValueAnimator.ofObject(new LngLatEvaluator(), curLngLat,
                nextLngLat);
        markerAnimator.setDuration(time);
        markerAnimator.setInterpolator(new LinearInterpolator());
        markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LngLat lngLat = (LngLat) animation.getAnimatedValue();
                currentLatlng = new LatLng(lngLat.getLat(), lngLat.getLng());
                Log.d(TAG, "current: " + curLngLat.toString());
                marker.setPosition(currentLatlng);
                currentPlayDistance = dis * animation.getCurrentPlayTime() / time;
                currentRemainDistance = remainDistance - currentPlayDistance;
                if (moveListener != null) {
                    if (currentRemainDistance < 0)
                        currentRemainDistance = 0;
                    moveListener.move(currentRemainDistance, totalDistance);
                }

            }
        });
        markerAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!animCancel) {
                    remainDistance = remainDistance - dis;
                } else {
                    remainDistance = remainDistance - currentPlayDistance;
                }
                //一段结束，开始下一段
                //如果不是最后一段
                if (!animStop) {
                    startRun();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animCancel = true;
                points.addFirst(nextPos);
                double newDistance = AMapUtils.calculateLineDistance(marker.getPosition(), points
                        .get(0));
                eachDistance.addFirst(newDistance);
            }

        });
        markerAnimator.start();

    }

    /**
     * 设置运行时间过短导致的 终点及角度问题
     */
    private void setEndRotate() {
        if (moveListener != null) {
            moveListener.stop();
        }
        float rotate = getRotate(lastEndPoint, endPoint);
        marker.setRotateAngle(360 - rotate + mAMap.getCameraPosition().bearing);
        marker.setPosition(endPoint);
    }

    /**
     * 根据经纬度计算需要偏转的角度
     */
    private float getRotate(LatLng curPos, LatLng nextPos) {
        double x1 = curPos.latitude;
        double x2 = nextPos.latitude;
        double y1 = curPos.longitude;
        double y2 = nextPos.longitude;

        return (float) (Math.atan2(y2 - y1, x2 - x1) / Math.PI * 180);
    }


    /**
     * 停止平滑移动
     */
    public void stopMove() {
        if (animStop) {
            return;
        }
        animStop = true;
        markerAnimator.cancel();
    }

    /**
     * 暂停动画
     */
    public void pauseMove() {
        if (animStop) {
            return;
        }
        animStop = true;
        markerAnimator.cancel();
    }

    public void changeSpeed(float speed) {
        pauseMove();
        this.speed = speed;
        resumeMove();

    }

    /**
     * 恢复动画
     */
    public void resumeMove() {
        if (!animStop) {
            return;
        }
        animStop = false;
        startRun();
    }


    public Marker getMarker() {
        return marker;
    }

    public LatLng getPosition() {
        if (marker == null)
            return null;
        return marker.getPosition();
    }

    public void destroy() {
        stopMove();

        if (descriptor != null) {
            descriptor.recycle();
        }
        if (marker != null) {
            marker.destroy();
            marker = null;
        }

        points.clear();
        eachDistance.clear();
    }

    public void setDescriptor(BitmapDescriptor descriptor) {
        if (this.descriptor != null) {
            this.descriptor.recycle();
        }
        this.descriptor = descriptor;
        if (marker != null) {
            marker.setIcon(descriptor);
        }
    }

    public void setRotate(float rotate) {
        if (marker != null) {
            marker.setRotateAngle(360 - rotate);

        }
    }

    public void setVisible(boolean b) {
        if (marker != null) {
            marker.setVisible(b);
        }
    }

    public void setMoveListener(SmoothMarkerMoveListener moveListener) {
        this.moveListener = moveListener;
    }
}
