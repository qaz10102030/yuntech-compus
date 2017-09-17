package tour.compus.yuntech.yuntechcompustour;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class compus_map extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        SearchView.OnQueryTextListener,
        SearchView.OnSuggestionListener {

    private GoogleMap mMap;
    private static final int REQUEST_PERMISSION = 99; //設定權限是否設定成功的檢查碼
    private ArrayAdapter adFloor;
    private SearchView searchView;
    private CheckBox cb_bicycle;
    private CheckBox cb_montor;
    private CheckBox cb_trash;
    private CheckBox cb_toilet;
    private CheckBox cb_college;
    private ArrayList<Marker> mMarkers = new ArrayList<>();
    private Marker singleMarker;
    private LatLng latlng;
    private BitmapDescriptor icon;
    private SharedPreferences settings;
    private static final String marker_data = "DATA";
    private static final String bicycle_state = "bicycle_state";
    private static final String montor_state = "montor_state";
    private static final String trash_state = "trash_state";
    private static final String toilet_state = "toilet_state";
    private static final String college_state = "college_state";
    private boolean isComfirmed;
    private static final String[] marker_type = {"腳踏車車位","機車車位","垃圾桶","廁所","學院"};
    private static final int[] marker_icon = {R.mipmap.marker_biycycle,R.mipmap.marker_scooter,R.mipmap.marker_trash,R.mipmap.marker_toilet,R.mipmap.marker_college};
    private static final String yuntech_json  = "[{\"college\":\"工程學院\",\"department\":[{\"name\":\"工程一館\",\"code\":\"EM\"},{\"name\":\"工程二館\",\"code\":\"EL\"},{\"name\":\"工程三館\",\"code\":\"ES\"},{\"name\":\"工程四館\",\"code\":\"EC\"},{\"name\":\"工程五館\",\"code\":\"EB\",\"floor\":[{\"floor_num\":\"1F\",\"classroom\":[{\"type\":\"電腦教室\",\"number\":\"EB102\"},{\"type\":\"一般教室\",\"number\":\"EB109\"},{\"type\":\"一般教室\",\"number\":\"EB110\"}]},{\"floor_num\":\"2F\",\"classroom\":[{\"type\":\"實驗室\",\"number\":\"EB201\"},{\"type\":\"一般教室\",\"number\":\"EB202\"}]}]},{\"name\":\"工程六館\",\"code\":\"EN\"}]}]";

    private HashMap<String, HashMap<String, ArrayList<LatLng>>> mapArea=null;
    private HashMap<String, HashMap<String, List<LatLng>>> buildkind=null;
    private static Boolean isExit = false;
    private static Boolean hasTask = false;
    private List<String> items;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compus_map);

        int location = ActivityCompat.checkSelfPermission(compus_map.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (location != PackageManager.PERMISSION_GRANTED) { //檢查是否有權限
            ActivityCompat.requestPermissions( //如果沒有就跟使用者要求
                    compus_map.this,
                    new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION}, REQUEST_PERMISSION
            );
        }
        checkGPS();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return true;
            }
        });
        toolbar.inflateMenu(R.menu.main_menu);
        setSupportActionBar(toolbar);
        settings = getSharedPreferences(marker_data,0);
        settings.edit()
                .putBoolean(bicycle_state, true)
                .putBoolean(montor_state, true)
                .putBoolean(trash_state, true)
                .putBoolean(toilet_state, true)
                .putBoolean(college_state, true)
                .apply();

        buildkind = CommonMethod.Buildingkind();
        mapArea = CommonMethod.BuildingArea();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        MenuItem menuSearchItem = menu.findItem(R.id.my_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menuSearchItem.getActionView();
        SearchView.SearchAutoComplete mSearchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
        mSearchAutoComplete.setThreshold(1);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint("在找什麼嗎?");
        searchView.setIconifiedByDefault(true);
        searchView.setOnSuggestionListener(this);
        return true;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng yuntech = new LatLng(23.6951701,120.5337975);
        addArea();
        init_marker();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setOnMarkerClickListener(this);
        mMap.setMyLocationEnabled(true); // 右上角的定位功能；這行會出現紅色底線，不過仍可正常編譯執行
        mMap.getUiSettings().setZoomControlsEnabled(true);  // 右下角的放大縮小功能
        mMap.getUiSettings().setCompassEnabled(true);       // 左上角的指南針，要兩指旋轉才會出現
        mMap.getUiSettings().setMapToolbarEnabled(true);    // 右下角的導覽及開啟 Google Map功能
        mMap.moveCamera(CameraUpdateFactory.newLatLng(yuntech));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPS();
                } else {
                    Toast.makeText(compus_map.this, "請允許所有權限避免功能不正常", Toast.LENGTH_SHORT).show();
                }
        }
    }

    void checkGPS() {
        if(!isGPSEnabled(this)) {
            Toast.makeText(compus_map.this, "請開始定位功能避免定位失效", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    public static boolean isGPSEnabled(Context context){
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
        @Override
        public View getInfoContents(Marker marker) {
            // 依指定layout檔，建立地標訊息視窗View物件
            LayoutInflater inflater = LayoutInflater.from(compus_map.this);
            View infoWindow = inflater.inflate(R.layout.my_infowindow, null);
            // 顯示地標title
            TextView title = ((TextView) infoWindow.findViewById(R.id.txtTitle));
            title.setText(marker.getTitle());
            // 顯示地標snippet
            String floorinfo = marker.getSnippet();
            if( floorinfo != null) {
                ListView lvSnippet = ((ListView) infoWindow.findViewById(R.id.lvFloor));
                adFloor = new ArrayAdapter(compus_map.this, android.R.layout.simple_list_item_1);
                lvSnippet.setAdapter(adFloor);
                parsefloor(floorinfo);
            }
            return infoWindow;
        }

        private void parsefloor(String floorinfo) {
            String test = "";
            JSONArray jArray = null;
            JSONObject jObject = null;
            try {
                jArray = new JSONArray(floorinfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < jArray.length(); i++) {
                try {
                    jObject = jArray.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    test = jObject.getJSONArray("department").getJSONObject(4).getJSONArray("floor").toString();
                    JSONArray parsefloor = new JSONArray(test);
                    for(int j = 0;j<parsefloor.length();j++)
                    {
                        String floor_lv_string = "";
                        String floor_num = parsefloor.getJSONObject(j).getString("floor_num");
                        floor_lv_string += floor_num;
                        JSONArray floor_class = new JSONArray(parsefloor.getJSONObject(j).getJSONArray("classroom").toString());
                        for(int k = 0;k<floor_class.length();k++)
                        {
                            String get_class_number = floor_class.getJSONObject(k).getString("number");
                            String get_class_type = floor_class.getJSONObject(k).getString("type");
                            floor_lv_string += "\n" + get_class_number + " " + get_class_type;
                        }
                        adFloor.add(floor_lv_string);
                        adFloor.notifyDataSetChanged();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_marker_switch:
                init_dialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void init_dialog() {
        LayoutInflater inflater = LayoutInflater.from(compus_map.this); //LayoutInflater的目的是將自己設計xml的Layout轉成View
        final View view = inflater.inflate(R.layout.marker_switch_dialog, null); //指定要給View表述的Layout
        cb_bicycle = (CheckBox)view.findViewById(R.id.cb_bicycle);
        cb_montor = (CheckBox)view.findViewById(R.id.cb_motor);
        cb_trash = (CheckBox)view.findViewById(R.id.cb_trash);
        cb_toilet = (CheckBox)view.findViewById(R.id.cb_toilet);
        cb_college = (CheckBox)view.findViewById(R.id.cb_college);
        cb_bicycle.setChecked(settings.getBoolean(bicycle_state,true));
        cb_montor.setChecked(settings.getBoolean(montor_state,true));
        cb_trash.setChecked(settings.getBoolean(trash_state,true));
        cb_toilet.setChecked(settings.getBoolean(toilet_state,true));
        cb_college.setChecked(settings.getBoolean(college_state,true));
        final boolean temp_bicycle = settings.getBoolean(bicycle_state,true);
        final boolean temp_montor = settings.getBoolean(montor_state,true);
        final boolean temp_trash = settings.getBoolean(trash_state,true);
        final boolean temp_toilet = settings.getBoolean(toilet_state,true);
        final boolean temp_college = settings.getBoolean(college_state,true);
        isComfirmed = false;


        new AlertDialog.Builder(compus_map.this) //宣告對話框物件，並顯示
            .setTitle("顯示圖例")
            .setView(view)
            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if(!isComfirmed) {
                        cb_bicycle.setChecked(temp_bicycle);
                        cb_montor.setChecked(temp_montor);
                        cb_trash.setChecked(temp_trash);
                        cb_toilet.setChecked(temp_toilet);
                        cb_college.setChecked(temp_college);
                        settings.edit().putBoolean(bicycle_state, temp_bicycle)
                                .putBoolean(montor_state, temp_montor)
                                .putBoolean(trash_state, temp_trash)
                                .putBoolean(toilet_state, temp_toilet)
                                .putBoolean(college_state, temp_college).apply();
                        Toast.makeText(view.getContext(), "取消", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isComfirmed = true;

                    //先清除全部
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        String temp_tag = mMarkers.get(i).getTag().toString();
                        singleMarker = mMarkers.get(i);
                        singleMarker.remove();
                        singleMarker.setTag(temp_tag);
                    }

                    //在把有選取的標上
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[0]) && (settings.getBoolean(bicycle_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
                            mMarkers.remove(i);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[0]);
                            startDropMarkerAnimation(singleMarker);

                            mMarkers.add(i,singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[1])&&(settings.getBoolean(montor_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_scooter);
                            mMarkers.remove(i);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[1]);
                            startDropMarkerAnimation(singleMarker);

                            mMarkers.add(i,singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[2])&&(settings.getBoolean(trash_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_trash);
                            mMarkers.remove(i);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[2]);
                            startDropMarkerAnimation(singleMarker);

                            mMarkers.add(i,singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[3])&&(settings.getBoolean(toilet_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_toilet);
                            mMarkers.remove(i);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[3]);
                            startDropMarkerAnimation(singleMarker);

                            mMarkers.add(i,singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[4])&&(settings.getBoolean(college_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_college);
                            mMarkers.remove(i);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[4]);
                            startDropMarkerAnimation(singleMarker);

                            mMarkers.add(i,singleMarker);
                        }

                    }
                    //Toast.makeText(view.getContext(),"確定", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.cb_bicycle:
                settings.edit().putBoolean(bicycle_state,checked).apply();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_motor:
                settings.edit().putBoolean(montor_state,checked).apply();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_trash:
                settings.edit().putBoolean(trash_state,checked).apply();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_toilet:
                settings.edit().putBoolean(toilet_state,checked).apply();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_college:
                settings.edit().putBoolean(college_state,checked).apply();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
        }
    }

    void init_marker(){
        latlng = new LatLng(23.695210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_college);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("工程五館" + "----------")
                .snippet(yuntech_json)
                .icon(icon));
        singleMarker.setTag("學院");
        startDropMarkerAnimation(singleMarker);

        mMarkers.add(singleMarker);

        latlng = new LatLng(23.696210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("腳踏車停車場" + "----------")
                .icon(icon));
        singleMarker.setTag("腳踏車車位");
        startDropMarkerAnimation(singleMarker);

        mMarkers.add(singleMarker);

        latlng = new LatLng(23.697210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_scooter);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("機車停車場" + "----------")
                .icon(icon));
        singleMarker.setTag("機車車位");
        startDropMarkerAnimation(singleMarker);

        mMarkers.add(singleMarker);

        latlng = new LatLng(23.698210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_toilet);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("廁所" + "----------")
                .icon(icon));
        singleMarker.setTag("廁所");
        startDropMarkerAnimation(singleMarker);

        mMarkers.add(singleMarker);

        latlng = new LatLng(23.699210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_trash);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("垃圾桶" + "----------")
                .icon(icon));
        singleMarker.setTag("垃圾桶");
        startDropMarkerAnimation(singleMarker);

        mMarkers.add(singleMarker);

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        String tag = marker.getTag().toString();
        Toast.makeText(compus_map.this, tag , Toast.LENGTH_SHORT).show();
        return false;
    }

    private void startDropMarkerAnimation(final Marker marker) {
        final LatLng target = marker.getPosition();
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point targetPoint = proj.toScreenLocation(target);
        final long duration = (long) (25 + (targetPoint.y * 0.6));
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        startPoint.y = 0;
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final Interpolator interpolator = new LinearOutSlowInInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * target.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * target.latitude + (1 - t) * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    // Post again 16ms later == 60 frames per second
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void addArea() {
        PolygonOptions polygonOptions;
        for (String key : mapArea.keySet()) {
            int color = 0;
            switch (key) {
                case "行政區域":
                    color = Color.argb(255, 140, 178, 131);
                    break;
                case "一般區域":
                    color = Color.argb(255, 199, 211, 147);
                    break;
                case "學生宿舍":
                    color = Color.argb(255, 221, 152, 155);
                    break;
                case "管理學院":
                    color = Color.argb(255, 114, 176, 187);
                    break;
                case "工程學院":
                    color = Color.argb(255, 189, 154, 186);
                    break;
                case "人科學院":
                    color = Color.argb(255, 210, 185, 93);
                    break;
                case "設計學院":
                    color = Color.argb(255, 200, 131, 98);
                    break;
            }
            for (String key1 : mapArea.get(key).keySet()) {
                polygonOptions = new PolygonOptions();
                polygonOptions
                        .addAll(mapArea.get(key).get(key1))
                        .strokeWidth(0)
                        .fillColor(color);
                mMap.addPolygon(polygonOptions).setZIndex(1);
            }
        }
        for (String key : buildkind.keySet()) {
            int color = 0;
            if (!key.equals("推薦景點") && !key.equals("休閒區域")) {
                color = Color.argb(255, 237, 229, 210);
            } else if (key.equals("推薦景點")) {
                color = Color.argb(255, 188, 218, 220);
            } else if (key.equals("休閒區域")) {
                color = Color.argb(255, 173, 199, 128);
            }
            for (String key1 : buildkind.get(key).keySet()) {
                polygonOptions = new PolygonOptions();
                polygonOptions
                        .addAll(buildkind.get(key).get(key1))
                        .strokeWidth(1)
                        .strokeColor(Color.BLACK)
                        .fillColor(color);
                mMap.addPolygon(polygonOptions).setZIndex(2);
            }
        }
    }

    Timer timerExit = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            isExit = false;
            hasTask = true;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 判斷是否按下Back
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!searchView.isIconified() && searchView.getQuery() == null) {
                searchView.setIconified(true);
            }else {
                // 是否要退出
                if (!isExit) {
                    isExit = true; //記錄下一次要退出
                    Toast.makeText(this, "再按一次Back退出APP"
                            , Toast.LENGTH_SHORT).show();
                    // 如果超過兩秒則恢復預設值
                    if (!hasTask) {
                        timerExit.schedule(task, 2000);
                    }
                } else {
                    finish(); // 離開程式
                    System.exit(0);
                }
            }
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Toast.makeText(this,"送出_onQueryTextSubmit "+query,Toast.LENGTH_SHORT).show();
        searchView.setIconified(true);
        searchMarker(query);
        return false;
    }

    private void searchMarker(String query) {
        Marker search = null;
        for (int i = 0; i < mMarkers.size(); i++) {
            if(mMarkers.get(i).getTitle().equals(query + "----------"))
                search = mMarkers.get(i);
        }
        if(search != null){
            search.showInfoWindow();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(search.getPosition()));
        }else{
            Toast.makeText(this,"搜尋不到 "+query,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(newText.equals(""))
            searchView.setIconified(true);

        Cursor cursor = TextUtils.isEmpty(newText) ? null : queryData(newText);
        if (searchView.getSuggestionsAdapter() == null) {
            searchView.setSuggestionsAdapter(new SimpleCursorAdapter(this, R.layout.search_item, cursor, new String[]{"name"}, new int[]{R.id.item}));
        } else {
            searchView.getSuggestionsAdapter().changeCursor(cursor);
        }
        return false;
    }

    private Cursor queryData(String key) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(getFilesDir() + "yuntech_data.db", null);
        Cursor cursor = null;
        try {
            String querySql = "select * from tb_department where name like '%" + key + "%'";
            cursor = db.rawQuery(querySql, null);
        } catch (Exception e) {
            e.printStackTrace();

            String createSql = "create table tb_department (_id integer primary key autoincrement,name varchar(100))";
            db.execSQL(createSql);

            String insertSql = "insert into tb_department values (null,?)";
            for (int i = 0; i < Cheeses.sCheeseStrings.length; i++) {
                db.execSQL(insertSql, new String[]{Cheeses.sCheeseStrings[i]});
            }

            String querySql = "select * from tb_department where name like '%" + key + "%'";
            cursor = db.rawQuery(querySql, null);

        }
        return cursor;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        String suggestion = getSuggestion(position);
        searchView.setQuery(suggestion, true); // submit query now
        return true; // replace default search manager behaviour
    }

    private String getSuggestion(int position) {
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(
                position);
        return cursor.getString(cursor.getColumnIndex("name"));
    }

}