package com.example.treasure_and_battle.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.content.SharedPreferences;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.MyLocationStyle;
import com.example.treasure_and_battle.R;

import java.util.List;

// 地图界面：LBS定位 + 实时地图 + 我的位置
public class MapFragment extends Fragment {

    // 地图核心控件
    private MapView mMapView;
    private AMap mAMap;

    // 定位客户端
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;

    // 定位蓝点样式
    private MyLocationStyle myLocationStyle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 高德地图 SDK 合规隐私政策更新（必须在初始化地图前调用）
        MapsInitializer.updatePrivacyShow(requireContext(), true, true);
        MapsInitializer.updatePrivacyAgree(requireContext(), true);
        AMapLocationClient.updatePrivacyShow(requireContext(), true, true);
        AMapLocationClient.updatePrivacyAgree(requireContext(), true);

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 1. 初始化地图
        mMapView = view.findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState); // 必须调用，生命周期绑定
        mAMap = mMapView.getMap();

        // 2. 初始化地图交互（缩放+拖动+旋转）
        initMapSetting();

        // 3. 申请定位权限 + 开启定位
        requestLocationPermission();

        // TODO 后续在这里添加：事件点图标、宝箱图标、NPC标记
        // TODO 后续在这里添加：地理围栏、点击事件触发

        return view;
    }

    // ====================== 地图基础设置（支持拖动/缩放/旋转） ======================
    private void initMapSetting() {
        // 1. 将地图强制变成2D纯净模式（不显示3D楼块、关闭双指倾斜手势）
        mAMap.showBuildings(false);
        mAMap.getUiSettings().setTiltGesturesEnabled(false);
        
        // 2. 读取设置：是否显示地图自带的地名/POI图标（避免和游戏事件冲突）
        SharedPreferences prefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
        boolean isShowPoi = prefs.getBoolean("showMapPoi", false); // 默认为false更干净
        mAMap.showMapText(isShowPoi);

        // 3. 初始地图缩放级别（数字越大，尺度越小，默认大概只有12，调到17即可清晰看到街道）
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(17f));

        // 启用缩放手势
        mAMap.getUiSettings().setZoomGesturesEnabled(true);
        // 启用滑动手势
        mAMap.getUiSettings().setScrollGesturesEnabled(true);
        // 启用旋转手势 (虽然是2D，但允许玩家平转地图有时候是好看的，如果不喜欢可以改成false)
        mAMap.getUiSettings().setRotateGesturesEnabled(true);
        // 显示缩放按钮
        mAMap.getUiSettings().setZoomControlsEnabled(false); // 通常游戏里为了全屏美观会关掉+和-号，靠手势缩放
        // 显示定位按钮
        mAMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    // ====================== 动态申请定位权限 ======================
    private void requestLocationPermission() {
        // 检查是否已经有定位权限
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 没有权限 → 申请权限
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001); // 1001 是权限申请的请求码，用来回调识别
        } else {
            // 已经有权限 → 直接开启定位
            startLocation();
        }
    }

    // 权限申请的回调方法（必须加，用来接收用户的授权结果）
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 匹配我们之前设置的请求码
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授权了 → 开启定位
                startLocation();
            } else {
                // 用户拒绝了 → 提示
                Toast.makeText(getContext(), "请开启定位权限，才能使用地图功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ====================== 开启定位 + 显示蓝点 ======================
    private void startLocation() {
        try {
            // 初始化定位
            mLocationClient = new AMapLocationClient(getContext());
            mLocationOption = new AMapLocationClientOption();

            // 高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 只定位一次
            mLocationOption.setOnceLocation(true);
            // 获取最近3s内精度最高的一次定位结果
            mLocationOption.setOnceLocationLatest(true);

            mLocationClient.setLocationOption(mLocationOption);

            // 定位监听
            mLocationClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    if (aMapLocation == null || aMapLocation.getErrorCode() != 0) {
                        String errText = "定位失败," + (aMapLocation != null ? aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo() : "null");
                        android.util.Log.e("AmapError", "location Error, ErrCode:"
                                + (aMapLocation != null ? aMapLocation.getErrorCode() + ", errInfo:" + aMapLocation.getErrorInfo() : "null"));
                        
                        // 弹窗显示具体的错误，方便复制
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("定位错误信息")
                                .setMessage(errText)
                                .setPositiveButton("确定", null)
                                .show();
                        
                        return;
                    }

                    // 定位成功 → 显示蓝点
                    showMyLocation(aMapLocation);
                    Toast.makeText(getContext(), "已定位到当前位置", Toast.LENGTH_SHORT).show();
                }
            });

            // 启动定位
            mLocationClient.startLocation();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== 显示定位蓝点 ======================
    private void showMyLocation(AMapLocation location) {
        myLocationStyle = new MyLocationStyle();
        // 连续定位、蓝点不会移动到地图中心点
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        // 显示蓝点
        myLocationStyle.showMyLocation(true);
        
        // 隐藏外围的紫色精度圈（设置边框颜色和填充颜色为透明）
        myLocationStyle.strokeColor(android.graphics.Color.TRANSPARENT);
        myLocationStyle.radiusFillColor(android.graphics.Color.TRANSPARENT);

        mAMap.setMyLocationStyle(myLocationStyle);
        mAMap.setMyLocationEnabled(true);
    }

    // ====================== 地图生命周期（必须写，否则崩溃） ======================
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        // 每次地图页面重新可见时（比如从Settings返回），重新读取开关并应用
        if (mAMap != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
            boolean isShowPoi = prefs.getBoolean("showMapPoi", false);
            mAMap.showMapText(isShowPoi);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}