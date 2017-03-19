package com.example.administrator.maptest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.baidu.navisdk.adapter.PackageUtil.getSdcardDir;

public class MainActivity extends AppCompatActivity {
    private TextureMapView mTextureMapView;
    private LocationClient mLocationClient;
    private BaiduMap mBaiduMap;
    private Boolean isFirstLocate=true;
    private Spinner mSpinner;
    private ArrayAdapter<String> mAdapter;
    private List<String> MyList;
    private List<String> permissionList;
    private Button btnSearch;
    private Boolean isGone=true;
    private PoiSearch mPoiSearch;
    private String address;
    private LatLng mLatLng;
    private PoiNearbySearchOption mPoiNearbySearchOption;
    private final static String authBaseArr[] =
            { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION };
    private String mSDCardPath = null;
    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo";
    private String authinfo=null;
    private boolean hasInitSuccess=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        setContentView(R.layout.activity_main);

        mTextureMapView= (TextureMapView) findViewById(R.id.bmapView);
        mSpinner= (Spinner) findViewById(R.id.spinner);
        mPoiSearch=PoiSearch.newInstance();


        btnSearch= (Button) findViewById(R.id.btn_Search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isGone){
                    mSpinner.setVisibility(View.VISIBLE);
                    isGone=false;
                } else {
                    mSpinner.setVisibility(View.GONE);
                    isGone=true;
                }

            }
        });

        initSpinner();

        mBaiduMap=mTextureMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        mPoiSearch.setOnGetPoiSearchResultListener(searchListener);

        initPermissions();

        if (initDirs()) {
            initNavi();
        }

    }



    private void initSpinner() {
        MyList=new ArrayList<>();
        MyList.add("餐馆");
        MyList.add("学校");
        mAdapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,MyList);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

/*                mPoiSearch.searchNearby(new PoiNearbySearchOption().keyword(mAdapter.getItem(position))
                        .location(mLatLng)
                        .pageCapacity(10)
                        .pageNum(0)
                        .radius(2000));*/

                mBaiduMap.clear();

                mPoiNearbySearchOption=new PoiNearbySearchOption().location(mLatLng)
                        .keyword(mAdapter.getItem(position))
                        .pageNum(0)
                        .radius(5000)
                        .sortType(PoiSortType.distance_from_near_to_far);
                mPoiSearch.searchNearby(mPoiNearbySearchOption);


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }

    //PoiSearch POI检索
    OnGetPoiSearchResultListener searchListener=new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if(poiResult == null){
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, poiResult.getAllPoi().get(0).location.toString(), Toast.LENGTH_SHORT).show();
/*
                    for(int i=0;i<poiResult.getAllPoi().size();i++) {
                    LatLng ll=new LatLng(poiResult.getAllPoi().get(i).location.latitude,poiResult.getAllPoi().get(i).location.longitude);
                    MapStatus.Builder builder=new MapStatus.Builder();
                    builder.target(ll).zoom(18.0f);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }*/


    //
                int Map_Num= poiResult.getAllPoi().size();
                for(int i=0;i<Map_Num;i++){
                    LatLng latLng=new LatLng(poiResult.getAllPoi().get(i).location.latitude,poiResult.getAllPoi().get(i).location.longitude);
                    MarkerOptions markerOptions=new MarkerOptions();
                    BitmapDescriptor bitmapDescriptor= BitmapDescriptorFactory.fromResource(R.drawable.maker);
                    markerOptions.icon(bitmapDescriptor);
                    markerOptions.position(latLng);
                    mBaiduMap.addOverlay(markerOptions);
                    MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLngZoom(latLng,18f);

                }


            }


        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
    };



    private void initPermissions() {
        permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!permissionList.isEmpty()){
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1001);
        } else {
            requestLocation();
        }


    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(3000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1001:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result !=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意所有权限才能使用程序", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    requestLocation();
                    initNavi();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                }

        }


    }


    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }


    private boolean hasBasePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initNavi() {
        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (!hasBasePhoneAuth()) {

                this.requestPermissions(authBaseArr, 1002);
                return;

            }
        }
        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME,
                new BaiduNaviManager.NaviInitListener() {
                    @Override
                    public void onAuthResult(int i, String s) {
                        if (i ==0) {
                            authinfo = "key校验成功!";
                        } else {
                            authinfo = "key校验失败, " + s;
                        }
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void initStart() {
                        Toast.makeText(MainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void initSuccess() {
                        Toast.makeText(MainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                        hasInitSuccess = true;
                        initSetting();
                    }

                    @Override
                    public void initFailed() {
                        Toast.makeText(MainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
                    }
                }, null /*mTTSCallback*/);
    }

    private void initSetting() {
            // BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
            BNaviSettingManager
                    .setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
            BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
            // BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
            BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mTextureMapView.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mPoiSearch.destroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mTextureMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mTextureMapView.onPause();
    }


    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            StringBuilder current=new StringBuilder();
            current.append("纬度:").append(bdLocation.getLatitude()).append("\n");
            current.append("经度:").append(bdLocation.getLongitude()).append("\n");
            current.append("定位方式:");
            if(bdLocation.getLocType() == BDLocation.TypeGpsLocation){
                current.append("GPS");
            } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation){
                current.append("网络");
            }
            address=bdLocation.getCity();
            current.append(address);

            Log.d("MyLocation",current.toString());

            if(bdLocation.getLocType()== BDLocation.TypeNetWorkLocation
                    || bdLocation.getLocType()== BDLocation.TypeGpsLocation){
                navigateTo(bdLocation);
            }


        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }

    private void navigateTo(BDLocation bdLocation) {
        if(isFirstLocate){
            mLatLng=new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
            MapStatus.Builder builder=new MapStatus.Builder();
            builder.target(mLatLng).zoom(18.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            isFirstLocate=false;
        }

        MyLocationData.Builder locationBuiler=new MyLocationData.Builder();
        locationBuiler.latitude(bdLocation.getLatitude());
        locationBuiler.longitude(bdLocation.getLongitude());
        MyLocationData myLocationData=locationBuiler.build();
        mBaiduMap.setMyLocationData(myLocationData);
    }





}
