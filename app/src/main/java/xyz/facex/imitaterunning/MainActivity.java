package xyz.facex.imitaterunning;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.SupportMapFragment;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.route.WalkStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: www.facex.xyz
 */
public class MainActivity extends FragmentActivity implements RouteSearch.OnRouteSearchListener, View.OnClickListener {
    private Button mRoute;
    private Button mStart;
    private Button mPause;
    private Button mContinue;
    /**
     * 事先准备的起点和终点
     */
    LatLonPoint startPoint = new LatLonPoint(29.807138, 121.556755);
    LatLonPoint endPoint = new LatLonPoint(29.825699, 121.545456);
    /**
     * 模拟速度
     */
    private double mSpeed = 300;
    /**
     * 路径总距离
     */
    private float mAllDistance;
    /**
     * 路径剩余距离
     */
    private double mDistanceRemain;
    private int mPauseIndex = 0;
    private List<LatLng> mPoints;
    private List<LatLng> mLatLngs;
    private AMap mMap;
    private SmoothMoveMarker mSmoothMoveMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setUpMapIfNeeded();
    }

    private void initView() {
        mRoute = (Button) findViewById(R.id.main_route);
        mStart = (Button) findViewById(R.id.main_start);
        mPause = (Button) findViewById(R.id.main_pause);
        mContinue = (Button) findViewById(R.id.main_continue);
        mRoute.setOnClickListener(this);
        mStart.setOnClickListener(this);
        mPause.setOnClickListener(this);
        mContinue.setOnClickListener(this);
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
        }
    }

    /**
     * 根据起点和重点，调用高德api规划路径
     */
    private void getRoute() {
        RouteSearch routeSearch = new RouteSearch(this);
        routeSearch.setRouteSearchListener(this);
        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(startPoint, endPoint);
        RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WalkDefault);
        routeSearch.calculateWalkRouteAsyn(query);
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {
        mLatLngs = new ArrayList<>();
        mMap.clear();
        if (i == 1000) {
            if (walkRouteResult != null && walkRouteResult.getPaths() != null && walkRouteResult.getPaths().size() > 0) {
                WalkPath walkPath = walkRouteResult.getPaths().get(0);
                mAllDistance = walkPath.getDistance();
                List<WalkStep> walkSteps = walkPath.getSteps();
                for (int j = 0; j < walkSteps.size(); j++) {
                    List<LatLonPoint> latLonPoints = walkSteps.get(j).getPolyline();
                    for (int k = 0; k < latLonPoints.size(); k++) {
                        LatLonPoint latLonPoint = latLonPoints.get(k);
                        mLatLngs.add(new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()));
                    }
                }
                /**
                 * 在地图上显示规划路径
                 */
                addPolylineInPlayGround();
            }
        }
    }

    /**
     * 在地图上显示轨迹
     */
    private void addPolylineInPlayGround() {
        List<LatLng> list = new ArrayList<>(mLatLngs);
        /**
         * 设置路径颜色
         */
        mMap.addPolyline(new PolylineOptions().addAll(list).width(10).color(Color.RED));
        /**
         *  改变地图视口
         */
        LatLngBounds bounds = new LatLngBounds(list.get(0), list.get(list.size() - 2));
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
    }


    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_route:
                getRoute();
                break;
            case R.id.main_start:
                startRun();
                break;
            case R.id.main_pause:
                pauseRun();
                break;
            case R.id.main_continue:
                continueRun();
                break;
        }

    }

    private void continueRun() {
        LatLng drivePoint = mPoints.get(mPauseIndex + 1);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(mPoints, drivePoint);
        mPoints.set(pair.first, drivePoint);
        mPoints = mPoints.subList(pair.first, mPoints.size());
        /**
         * 重新设置剩余的轨迹点
         */
        mSmoothMoveMarker.setPoints(mPoints);
        /**
         * 重新设置滑动时间
         */
        mSmoothMoveMarker.setTotalDuration((int) (mDistanceRemain / mSpeed));
        mSmoothMoveMarker.startSmoothMove();
    }

    private void pauseRun() {
        mSmoothMoveMarker.stopMove();
    }

    private void startRun() {
        mPoints = new ArrayList<>(mLatLngs);
        mSmoothMoveMarker = new SmoothMoveMarker(mMap);
        /**
         * 设置滑动的图标
         */
        mSmoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.ic_navigation_green_a700_24dp));
        LatLng drivePoint = mPoints.get(0);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(mPoints, drivePoint);
        mPoints.set(pair.first, drivePoint);
        List<LatLng> subList = mPoints.subList(pair.first, mPoints.size());
        /**
         * 设置滑动的轨迹左边点
         */
        mSmoothMoveMarker.setPoints(subList);
        /**
         * 设置滑动的总时间
         */
        mSmoothMoveMarker.setTotalDuration((int) (mAllDistance / mSpeed));
        mSmoothMoveMarker.startSmoothMove();
        mSmoothMoveMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
            @Override
            public void move(double v) {
                /**
                 * 最后一次取到的值即为暂停时的数据，getIndex()方法取到的是当前list的下标,v为剩余距离
                 */
                mPauseIndex = mSmoothMoveMarker.getIndex();
                mDistanceRemain = v;
            }
        });
    }
}
