package com.example.treasure_and_battle.ui;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;

import android.content.Context;
import android.content.SharedPreferences;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.model.event.EventConfig;
import com.example.treasure_and_battle.utils.GeoUtils;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView mMapView;
    private AMap mAMap;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private MyLocationStyle myLocationStyle;
    private boolean isFirstLocate = true;
    private Circle mPerceptionCircle;
    private static final int PERCEPTION_RADIUS = 20;
    private Button btnDebug;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Runnable mGenerateEventRunnable;
    private Runnable mCheckExpireRunnable;
    private Runnable mBattleCountdownRunnable;
    private Runnable mDebugCountdownRunnable;

    private EventManager mEventManager;

    private View mEventPopup;
    private View mDebugPopup;
    private TextView tvEventName, tvEventDesc, tvEventReward, tvEventRisk;
    private Button btnClosePopup;
    private boolean isPopupShowing = false;
    private boolean isDebugPopupShowing = false;

    private ConstraintLayout mMapRoot;
    private SharedPreferences mPrefs;

    private FrameLayout mIconOverlay;
    private List<ImageView> mEventIcons = new ArrayList<>();

    private FrameLayout mCountdownOverlay;
    private TextView tvCountdown;
    private LatLng mBattleTriggerPosition;
    private boolean mIsProcessingNeutral;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MapsInitializer.updatePrivacyShow(requireContext(), true, true);
        MapsInitializer.updatePrivacyAgree(requireContext(), true);
        AMapLocationClient.updatePrivacyShow(requireContext(), true, true);
        AMapLocationClient.updatePrivacyAgree(requireContext(), true);

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mMapView = view.findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
        mAMap = mMapView.getMap();
        mMapRoot = view.findViewById(R.id.map_root);
        mPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE);

        mEventManager = EventManager.getInstance(requireContext());
        mEventManager.bindAMap(mAMap);

        initDebugButton();

        initIconOverlay();
        initCountdownOverlay();
        initEventPopup(inflater);
        initDebugPopup();
        initMapClick();

        initMapSetting();
        requestLocationPermission();
        initTimedTasks();

        return view;
    }

    private void initDebugButton() {
        btnDebug = new Button(requireContext());
        btnDebug.setId(View.generateViewId());
        btnDebug.setText("调试功能");
        btnDebug.setTextSize(14);
        btnDebug.setTextColor(0xFFFFFFFF);
        btnDebug.setBackgroundColor(0xFF607D8B);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        btnDebug.setPadding(pad, pad, pad, pad);
        btnDebug.setElevation(8 * getResources().getDisplayMetrics().density);
        btnDebug.setVisibility(View.GONE);

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        int topMargin = (int) (80 * getResources().getDisplayMetrics().density);
        int endMargin = (int) (16 * getResources().getDisplayMetrics().density);
        params.setMargins(0, topMargin, endMargin, 0);

        mMapRoot.addView(btnDebug, params);

        btnDebug.setOnClickListener(v -> showDebugPopup());
    }

    private void initDebugPopup() {
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        int margin6 = (int) (6 * getResources().getDisplayMetrics().density);

        LinearLayout popupContent = new LinearLayout(requireContext());
        popupContent.setOrientation(LinearLayout.VERTICAL);
        popupContent.setBackgroundResource(R.drawable.bg_event_popup);
        popupContent.setElevation(8 * getResources().getDisplayMetrics().density);
        popupContent.setPadding(pad, pad, pad, pad);
        popupContent.setClickable(true);
        popupContent.setFocusable(true);

        TextView title = new TextView(requireContext());
        title.setText("调试功能");
        title.setTextSize(18);
        title.setTextColor(0xFF333333);
        title.setPadding(0, 0, 0, pad);
        popupContent.addView(title);

        addDebugSectionLabel(popupContent, "随机大类（3秒后生成）");
        String[] labels = {"战斗事件大类", "中性事件大类", "增益事件大类"};
        String[] types = {"BATTLE", "NEUTRAL", "BENEFIT"};
        int[] colors = {0xFFE53935, 0xFFFFC107, 0xFF4CAF50};

        for (int i = 0; i < labels.length; i++) {
            final String eventType = types[i];
            Button btn = makeDebugButton(labels[i], colors[i]);
            btn.setOnClickListener(b -> {
                hideDebugPopup();
                startDebugCountdown(eventType);
            });
            popupContent.addView(btn);
        }

        Button btnClear = makeDebugButton("清除自身位置事件", 0xFF607D8B);
        btnClear.setOnClickListener(b -> {
            hideDebugPopup();
            LatLng pos = mEventManager.getCurrentLatLng();
            if (pos != null) {
                int removed = mEventManager.removeEventsAtPosition(pos, 100);
                refreshEventIcons();
                Toast.makeText(getContext(), "已清除附近事件：" + removed + " 个", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "当前无定位，无法清除", Toast.LENGTH_SHORT).show();
            }
        });
        popupContent.addView(btnClear);

        addDebugSectionLabel(popupContent, "具体小事件（立即生成）");

        EventConfig.EventItem[] allItems = mEventManager.getEventItems();
        for (EventConfig.EventItem item : allItems) {
            String type = item.getType();
            EventConfig.EventSubItem[] subs = item.getSubEvents();
            if (subs == null || subs.length == 0) {
                if ("UNKNOWN".equals(type)) {
                    Button btn = makeDebugButton("未知事件", 0xFF757575);
                    btn.setOnClickListener(b -> {
                        hideDebugPopup();
                        generateSpecificEvent(item, null);
                    });
                    popupContent.addView(btn);
                }
                continue;
            }
            int typeColor;
            switch (type) {
                case "BATTLE":  typeColor = 0xFFFF5252; break;
                case "BENEFIT": typeColor = 0xFF4CAF50; break;
                case "NEUTRAL": typeColor = 0xFFFFC107; break;
                default:        typeColor = 0xFF888888; break;
            }
            for (EventConfig.EventSubItem sub : subs) {
                Button btn = makeDebugButton(type + "：" + sub.getName(), typeColor);
                final EventConfig.EventItem finalItem = item;
                final EventConfig.EventSubItem finalSub = sub;
                btn.setOnClickListener(b -> {
                    hideDebugPopup();
                    generateSpecificEvent(finalItem, finalSub);
                });
                popupContent.addView(btn);
            }
        }

        View sep1 = new View(requireContext());
        sep1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * getResources().getDisplayMetrics().density)));
        sep1.setBackgroundColor(0x30000000);
        ((LinearLayout.LayoutParams) sep1.getLayoutParams()).setMargins(0, margin6 * 2, 0, margin6);
        popupContent.addView(sep1);

        Button btnBattlePage = makeDebugButton("进入战斗页面", 0xFF4CAF50);
        btnBattlePage.setOnClickListener(b -> {
            hideDebugPopup();
            startActivity(new Intent(getActivity(), BattleActivity.class));
        });
        popupContent.addView(btnBattlePage);

        Button btnTradePage = makeDebugButton("进入商店页面", 0xFF4CAF50);
        btnTradePage.setOnClickListener(b -> {
            hideDebugPopup();
            startActivity(new Intent(getActivity(), TradeActivity.class));
        });
        popupContent.addView(btnTradePage);

        Button btnCloseDbg = new Button(requireContext());
        btnCloseDbg.setText("关闭");
        btnCloseDbg.setTextSize(13);
        btnCloseDbg.setTextColor(0xFF2196F3);
        btnCloseDbg.setBackgroundResource(R.drawable.bg_event_popup);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.END;
        closeParams.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        btnCloseDbg.setLayoutParams(closeParams);
        btnCloseDbg.setOnClickListener(b -> hideDebugPopup());
        popupContent.addView(btnCloseDbg);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().heightPixels * 0.75)));
        scrollView.addView(popupContent);

        FrameLayout overlay = new FrameLayout(requireContext());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(0x60000000);
        overlay.setClickable(true);
        overlay.setOnClickListener(v -> hideDebugPopup());

        FrameLayout.LayoutParams popupParams = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.82),
                FrameLayout.LayoutParams.WRAP_CONTENT);
        popupParams.gravity = Gravity.CENTER;
        overlay.addView(scrollView, popupParams);
        overlay.setVisibility(View.GONE);
        mDebugPopup = overlay;
        mMapView.addView(mDebugPopup);
    }

    private void addDebugSectionLabel(LinearLayout parent, String text) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextSize(12);
        label.setTextColor(0xFF999999);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (5 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin * 2, 0, margin);
        label.setLayoutParams(params);
        parent.addView(label);
    }

    private Button makeDebugButton(String text, int bgColor) {
        Button btn = new Button(requireContext());
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(bgColor);
        int btnPad = (int) (10 * getResources().getDisplayMetrics().density);
        btn.setPadding(btnPad, btnPad, btnPad, btnPad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, margin);
        btn.setLayoutParams(params);
        return btn;
    }

    private void showDebugPopup() {
        mDebugPopup.setVisibility(View.VISIBLE);
        isDebugPopupShowing = true;
    }

    private void hideDebugPopup() {
        if (isDebugPopupShowing) {
            mDebugPopup.setVisibility(View.GONE);
            isDebugPopupShowing = false;
        }
    }

    private void startDebugCountdown(String eventType) {
        mDebugCountdownRunnable = new Runnable() {
            int countdown = 3;
            @Override
            public void run() {
                if (countdown > 0) {
                    btnDebug.setText("生成中..." + countdown);
                    countdown--;
                    mMainHandler.postDelayed(this, 1000);
                } else {
                    btnDebug.setText("调试功能");
                    generateEventAtPosition(eventType);
                }
            }
        };
        mMainHandler.post(mDebugCountdownRunnable);
    }

    private void generateEventAtPosition(String eventType) {
        EventConfig.EventItem[] items = mEventManager.getEventItems();
        EventConfig.EventItem target = null;
        for (EventConfig.EventItem item : items) {
            if (item.getType().equals(eventType)) {
                target = item;
                break;
            }
        }
        if (target == null) return;

        EventConfig.EventSubItem sub = mEventManager.pickRandomSubEvent(target);
        if (sub == null) return;

        Circle circle = mAMap.addCircle(new CircleOptions()
                .center(mEventManager.getCurrentLatLng())
                .radius(target.getRadius())
                .strokeColor(target.getStrokeColorInt())
                .strokeWidth(4)
                .fillColor(target.getFillColorInt()));

        EventManager.EventCircle ec = new EventManager.EventCircle(circle, mEventManager.getCurrentLatLng(), target);
        ec.selectedSubEvent = sub;
        mEventManager.addDebugEvent(ec);
        refreshEventIcons();

        Toast.makeText(getContext(), "已在当前位置生成：" + sub.getName(), Toast.LENGTH_SHORT).show();
    }

    private void generateSpecificEvent(EventConfig.EventItem target, EventConfig.EventSubItem sub) {
        if (target == null) return;
        LatLng pos = mEventManager.getCurrentLatLng();
        if (pos == null) {
            Toast.makeText(getContext(), "当前无定位，无法生成", Toast.LENGTH_SHORT).show();
            return;
        }

        Circle circle = mAMap.addCircle(new CircleOptions()
                .center(pos)
                .radius(target.getRadius())
                .strokeColor(target.getStrokeColorInt())
                .strokeWidth(4)
                .fillColor(target.getFillColorInt()));

        EventManager.EventCircle ec = new EventManager.EventCircle(circle, pos, target);
        ec.selectedSubEvent = sub;
        mEventManager.addDebugEvent(ec);
        refreshEventIcons();

        String name = sub != null ? sub.getName() : "未知事件";
        Toast.makeText(getContext(), "已在当前位置生成：" + name, Toast.LENGTH_SHORT).show();
    }

    private void initMapSetting() {
        mAMap.showBuildings(false);
        mAMap.getUiSettings().setTiltGesturesEnabled(false);
        mAMap.showMapText(mPrefs.getBoolean("showMapPoi", false));
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(18f));
        mAMap.getUiSettings().setZoomGesturesEnabled(true);
        mAMap.getUiSettings().setScrollGesturesEnabled(true);
        mAMap.getUiSettings().setRotateGesturesEnabled(true);
        mAMap.getUiSettings().setZoomControlsEnabled(false);
        mAMap.getUiSettings().setMyLocationButtonEnabled(true);

        mAMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                refreshEventIcons();
            }

            @Override
            public void onCameraChangeFinish(CameraPosition position) {
                refreshEventIcons();
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            startLocation();
        }
    }

    private void initTimedTasks() {
        mGenerateEventRunnable = () -> {
            if (isDetached()) return;
            int count = mEventManager.generateRandomEvents();
            if (count > 0) {
                Toast.makeText(getContext(), "生成事件：" + count, Toast.LENGTH_SHORT).show();
                refreshEventIcons();
            }
            mMainHandler.postDelayed(mGenerateEventRunnable, mEventManager.getGlobalConfig().getGenerateInterval());
        };

        mCheckExpireRunnable = () -> {
            if (isDetached()) return;
            int count = mEventManager.checkExpiredEvents();
            if (count > 0) {
                refreshEventIcons();
            }
            mMainHandler.postDelayed(mCheckExpireRunnable, 1000);
        };
    }

    private void startTimedTasks() {
        mMainHandler.removeCallbacks(mGenerateEventRunnable);
        mMainHandler.removeCallbacks(mCheckExpireRunnable);
        mMainHandler.postDelayed(mGenerateEventRunnable, 1000);
        mMainHandler.post(mCheckExpireRunnable);
    }

    private void startBattleCountdown() {
        mCountdownOverlay.setVisibility(View.VISIBLE);
        mBattleCountdownRunnable = new Runnable() {
            int countdown = 5;
            @Override
            public void run() {
                if (countdown > 0) {
                    tvCountdown.setText("即将进入战斗\n" + countdown);
                    countdown--;
                    mMainHandler.postDelayed(this, 1000);
                } else {
                    mCountdownOverlay.setVisibility(View.GONE);
                    mBattleTriggerPosition = null;
                    Intent intent = new Intent(getActivity(), BattleActivity.class);
                    startActivity(intent);
                }
            }
        };
        mMainHandler.post(mBattleCountdownRunnable);
    }

    private void handleBenefitAction(EventConfig.EventSubItem sub) {
        if (sub == null) return;
        String key = sub.getKey();
        if ("recovery".equals(key)) {
            Toast.makeText(getContext(), "回复生命+80！", Toast.LENGTH_SHORT).show();
        } else if ("training".equals(key)) {
            Toast.makeText(getContext(), "力量+5！", Toast.LENGTH_SHORT).show();
        } else if ("treasure".equals(key)) {
            Toast.makeText(getContext(), "开启宝藏！获得金币、装备、材料和宝石", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleNeutralAction(EventConfig.EventSubItem sub) {
        if (sub == null) return;
        String key = sub.getKey();
        if ("merchant".equals(key)) {
            Toast.makeText(getContext(), "与商人交易！", Toast.LENGTH_SHORT).show();
        } else if ("exploration".equals(key)) {
            Toast.makeText(getContext(), "深入探险！", Toast.LENGTH_SHORT).show();
        } else if ("traveler".equals(key)) {
            Toast.makeText(getContext(), "帮助了迷路的旅人！获得补给品×3、金币×200，幸运值提升1小时", Toast.LENGTH_SHORT).show();
        } else if ("scholar".equals(key)) {
            Toast.makeText(getContext(), "消耗金币×500，天赋点/技能点已重置", Toast.LENGTH_SHORT).show();
        } else if ("statue_blessing".equals(key)) {
            Toast.makeText(getContext(), "接受雕像祝福！下3场战斗获得随机Buff", Toast.LENGTH_SHORT).show();
        } else if ("monster_camp".equals(key)) {
            Toast.makeText(getContext(), "偷袭成功！必定先手进入战斗", Toast.LENGTH_SHORT).show();
        } else if ("cave_treasure".equals(key)) {
            Toast.makeText(getContext(), "开启宝箱！小心怪物偷袭...", Toast.LENGTH_SHORT).show();
        } else if ("equipment_reforge".equals(key)) {
            Toast.makeText(getContext(), "装备词条重炼完成！", Toast.LENGTH_SHORT).show();
        }
    }

    private void openNeutralEventPage(EventConfig.EventSubItem sub) {
        if (sub == null) return;
        Intent intent = new Intent(getActivity(), NeutralEventActivity.class);
        intent.putExtra("event_key", sub.getKey());
        intent.putExtra("event_name", sub.getName());
        intent.putExtra("event_desc", sub.getDesc());
        intent.putExtra("event_reward", sub.getReward());
        intent.putExtra("event_risk", sub.getRisk());
        startActivity(intent);
    }

    private void startLocation() {
        try {
            mLocationClient = new AMapLocationClient(getContext());
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setOnceLocation(false);
            mLocationOption.setInterval(500);
            mLocationOption.setNeedAddress(true);
            mLocationOption.setWifiScan(true);
            mLocationOption.setLocationCacheEnable(false);
            mLocationClient.setLocationOption(mLocationOption);

            myLocationStyle = new MyLocationStyle();
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            myLocationStyle.radiusFillColor(0x00000000);
            myLocationStyle.strokeColor(0x00000000);
            mAMap.setMyLocationStyle(myLocationStyle);

            mAMap.setOnMyLocationChangeListener(location -> {
                if (location == null) return;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mEventManager.updateCurrentLatLng(latLng);
                drawPerceptionCircle(latLng);

                boolean inRange = mEventManager.checkEventInTriggerRange();
                boolean countdownRunning = mBattleCountdownRunnable != null;

                if (inRange && !countdownRunning && !mIsProcessingNeutral) {
                    EventConfig.EventItem triggered = mEventManager.getTriggeredEvent();
                    if (triggered == null) {
                        if (isFirstLocate) {
                            mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
                            isFirstLocate = false;
                            startTimedTasks();
                        }
                        return;
                    }
                    String type = triggered.getType();
                    EventConfig.EventSubItem sub = mEventManager.getTriggeredSubEvent();

                    if ("UNKNOWN".equals(type)) {
                        sub = mEventManager.resolveUnknownEvent();
                        if (sub != null) {
                            Toast.makeText(getContext(), "揭开神秘面纱！原来是：" + sub.getName(), Toast.LENGTH_LONG).show();
                            type = determineSubEventCategory(sub);
                        }
                    }

                    if ("BATTLE".equals(type) || (sub != null && sub.getKey().startsWith("battle_"))) {
                        mEventManager.removeTriggeredEvents();
                        refreshEventIcons();
                        mBattleTriggerPosition = latLng;
                        Toast.makeText(getContext(), "即将进入战斗...", Toast.LENGTH_SHORT).show();
                        startBattleCountdown();
                    } else if ("BENEFIT".equals(type) || "recovery".equals(sub != null ? sub.getKey() : "") || "training".equals(sub != null ? sub.getKey() : "") || "treasure".equals(sub != null ? sub.getKey() : "")) {
                        handleBenefitAction(sub);
                        mEventManager.removeTriggeredEvents();
                        refreshEventIcons();
                        Toast.makeText(getContext(), "已触发增益事件", Toast.LENGTH_SHORT).show();
                    } else if ("NEUTRAL".equals(type)) {
                        mIsProcessingNeutral = true;
                        mEventManager.removeTriggeredEvents();
                        refreshEventIcons();
                        openNeutralEventPage(sub);
                    }
                } else if (!inRange && countdownRunning && mBattleTriggerPosition != null) {
                    double distFromTrigger = GeoUtils.calculateDistance(latLng, mBattleTriggerPosition);
                    if (distFromTrigger > 20) {
                        mMainHandler.removeCallbacks(mBattleCountdownRunnable);
                        mBattleCountdownRunnable = null;
                        mBattleTriggerPosition = null;
                        if (mCountdownOverlay != null) {
                            mCountdownOverlay.setVisibility(View.GONE);
                        }
                        Toast.makeText(getContext(), "已离开战斗范围", Toast.LENGTH_SHORT).show();
                    }
                }

                if (isFirstLocate) {
                    mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
                    isFirstLocate = false;
                    startTimedTasks();
                }
            });

            mAMap.setMyLocationEnabled(true);
            mLocationClient.startLocation();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawPerceptionCircle(LatLng center) {
        if (mPerceptionCircle != null) mPerceptionCircle.remove();
        mPerceptionCircle = mAMap.addCircle(new CircleOptions()
                .center(center)
                .radius(PERCEPTION_RADIUS)
                .strokeColor(0xFF90CAF9)
                .strokeWidth(2)
                .fillColor(0x3090CAF9));
    }

    private void initMapClick() {
        mAMap.setOnMapClickListener(latLng -> {
            hideEventPopup();

            for (EventManager.EventCircle eventCircle : mEventManager.getEventCircleList()) {
                double d = GeoUtils.calculateDistance(latLng, eventCircle.position);
                if (d <= eventCircle.config.getRadius()) {
                    showEventPopup(eventCircle);
                    break;
                }
            }
        });
    }

    private void initIconOverlay() {
        mIconOverlay = new FrameLayout(requireContext());
        mIconOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        mIconOverlay.setClickable(false);
        mIconOverlay.setFocusable(false);
        mMapView.addView(mIconOverlay);
    }

    private void initCountdownOverlay() {
        mCountdownOverlay = new FrameLayout(requireContext());
        mCountdownOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        mCountdownOverlay.setBackgroundColor(0x80000000);
        mCountdownOverlay.setClickable(true);
        mCountdownOverlay.setFocusable(true);
        mCountdownOverlay.setVisibility(View.GONE);

        tvCountdown = new TextView(requireContext());
        tvCountdown.setTextSize(28);
        tvCountdown.setTextColor(0xFFFFFFFF);
        tvCountdown.setGravity(Gravity.CENTER);
        tvCountdown.setBackgroundResource(R.drawable.bg_event_popup);
        int pad = (int) (30 * getResources().getDisplayMetrics().density);
        tvCountdown.setPadding(pad, pad, pad, pad);

        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = Gravity.CENTER;
        mCountdownOverlay.addView(tvCountdown, tvParams);
        mMapView.addView(mCountdownOverlay);
    }

    private void refreshEventIcons() {
        mIconOverlay.removeAllViews();
        mEventIcons.clear();

        List<EventManager.EventCircle> circles = mEventManager.getEventCircleList();
        if (circles.isEmpty()) return;

        int iconSize = (int) (18 * getResources().getDisplayMetrics().density);

        for (EventManager.EventCircle ec : circles) {
            int resId = getEventIconRes(ec.config.getType());
            if (resId == 0) continue;

            ImageView iv = new ImageView(requireContext());
            iv.setImageResource(resId);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(iconSize, iconSize);

            android.graphics.Point point = mAMap.getProjection().toScreenLocation(ec.position);
            params.leftMargin = point.x - iconSize / 2;
            params.topMargin = point.y - iconSize / 2;

            mIconOverlay.addView(iv, params);
            mEventIcons.add(iv);
        }
    }

    private int getEventIconRes(String type) {
        switch (type) {
            case "BATTLE":   return R.drawable.ic_event_battle;
            case "BENEFIT":  return R.drawable.ic_event_benefit;
            case "NEUTRAL":  return R.drawable.ic_event_neutral;
            case "UNKNOWN":  return R.drawable.ic_event_unknown;
            default:         return 0;
        }
    }

    private void initEventPopup(LayoutInflater inflater) {
        View popupContent = inflater.inflate(R.layout.layout_event_popup, null);
        tvEventName = popupContent.findViewById(R.id.tv_event_name);
        tvEventDesc = popupContent.findViewById(R.id.tv_event_desc);
        tvEventReward = popupContent.findViewById(R.id.tv_event_reward);
        tvEventRisk = popupContent.findViewById(R.id.tv_event_risk);
        btnClosePopup = popupContent.findViewById(R.id.btn_close_popup);

        popupContent.setClickable(true);
        popupContent.setFocusable(true);
        btnClosePopup.setOnClickListener(v -> hideEventPopup());

        FrameLayout overlay = new FrameLayout(requireContext());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(0x60000000);
        overlay.setClickable(true);
        overlay.setOnClickListener(v -> hideEventPopup());

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        FrameLayout.LayoutParams popupParams = new FrameLayout.LayoutParams(
                (int) (screenW * 0.66),
                (int) (screenH * 0.66));
        popupParams.gravity = Gravity.CENTER;
        overlay.addView(popupContent, popupParams);
        overlay.setVisibility(View.GONE);
        mEventPopup = overlay;
        mMapView.addView(mEventPopup);
    }

    private void showEventPopup(EventManager.EventCircle eventCircle) {
        EventConfig.EventItem item = eventCircle.config;
        EventConfig.EventSubItem sub = eventCircle.selectedSubEvent;

        if ("UNKNOWN".equals(item.getType())) {
            tvEventName.setText("未知事件");
            tvEventDesc.setText("描述：???");
            tvEventReward.setText("奖励：???");
            tvEventRisk.setText("风险：???");
        } else if (sub != null) {
            tvEventName.setText(sub.getName());
            tvEventDesc.setText("描述：" + sub.getDesc());
            tvEventReward.setText("奖励：" + sub.getReward());
            tvEventRisk.setText("风险：" + sub.getRisk());
        } else {
            tvEventName.setText(item.getType());
            tvEventDesc.setText("描述：暂无信息");
            tvEventReward.setText("奖励：暂无信息");
            tvEventRisk.setText("风险：暂无信息");
        }

        mEventPopup.setVisibility(View.VISIBLE);
        isPopupShowing = true;
    }

    private String determineSubEventCategory(EventConfig.EventSubItem sub) {
        if (sub == null) return "BATTLE";
        String key = sub.getKey();
        if (key.startsWith("battle_")) return "BATTLE";
        if ("recovery".equals(key) || "training".equals(key) || "treasure".equals(key)) return "BENEFIT";
        if ("merchant".equals(key) || "exploration".equals(key) || "traveler".equals(key)
                || "scholar".equals(key) || "statue_blessing".equals(key)
                || "monster_camp".equals(key) || "cave_treasure".equals(key)
                || "equipment_reforge".equals(key)) return "NEUTRAL";
        return "BATTLE";
    }

    private void hideEventPopup() {
        if (isPopupShowing) {
            mEventPopup.setVisibility(View.GONE);
            isPopupShowing = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        mIsProcessingNeutral = false;
        refreshDebugButton();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshDebugButton();
        }
    }

    private void refreshDebugButton() {
        boolean debugMode = mPrefs.getBoolean("debugMode", false);
        btnDebug.setVisibility(debugMode ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        mMainHandler.removeCallbacks(mGenerateEventRunnable);
        mMainHandler.removeCallbacks(mCheckExpireRunnable);
        if (mBattleCountdownRunnable != null) {
            mMainHandler.removeCallbacks(mBattleCountdownRunnable);
            mBattleCountdownRunnable = null;
            mBattleTriggerPosition = null;
        }
        if (mDebugCountdownRunnable != null) {
            mMainHandler.removeCallbacks(mDebugCountdownRunnable);
            mDebugCountdownRunnable = null;
            btnDebug.setText("调试功能");
        }
        if (mCountdownOverlay != null) {
            mCountdownOverlay.setVisibility(View.GONE);
        }
        hideEventPopup();
        hideDebugPopup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mLocationClient != null) mLocationClient.onDestroy();
        if (mPerceptionCircle != null) mPerceptionCircle.remove();
        mEventManager.clearAllEvents();
        mIconOverlay.removeAllViews();
        mEventIcons.clear();
        mMainHandler.removeCallbacksAndMessages(null);
        hideEventPopup();
        hideDebugPopup();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Handler(Looper.getMainLooper()).postDelayed(this::startLocation, 300);
        }
    }
}
