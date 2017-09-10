package tour.compus.yuntech.yuntechcompustour;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class compus_map extends AppCompatActivity implements OnMapReadyCallback,GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private static final int REQUEST_PERMISSION = 99; //設定權限是否設定成功的檢查碼
    private ListView lvSnippet;
    private ArrayAdapter<String> adFloor;
    private Toolbar toolbar;
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
    private static final String yuntech_json  = "[{\"college\":\"工程學院\",\"department\":[{\"name\":\"工程一館\",\"code\":\"EM\"},{\"name\":\"工程二館\",\"code\":\"EL\"},{\"name\":\"工程三館\",\"code\":\"ES\"},{\"name\":\"工程四館\",\"code\":\"EC\"},{\"name\":\"工程五館\",\"code\":\"EB\",\"floor\":[{\"floor_num\":\"1F\",\"classroom\":[{\"type\":\"電腦教室\",\"number\":\"EB102\"},{\"type\":\"一般教室\",\"number\":\"EB109\"},{\"type\":\"一般教室\",\"number\":\"EB110\"}]},{\"floor_num\":\"2F\",\"classroom\":[{\"type\":\"實驗室\",\"number\":\"EB201\"},{\"type\":\"一般教室\",\"number\":\"EB202\"}]}]},{\"name\":\"工程六館\",\"code\":\"EN\"}]}]";

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

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle the menu item
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
                .commit();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        MenuItem menuSearchItem = menu.findItem(R.id.my_search);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menuSearchItem.getActionView();

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // 這邊讓icon可以還原到搜尋的icon
        searchView.setIconifiedByDefault(true);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        init_marker();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        mMap.setOnMarkerClickListener(this);
        mMap.setMyLocationEnabled(true); // 右上角的定位功能；這行會出現紅色底線，不過仍可正常編譯執行
        mMap.getUiSettings().setZoomControlsEnabled(true);  // 右下角的放大縮小功能
        mMap.getUiSettings().setCompassEnabled(true);       // 左上角的指南針，要兩指旋轉才會出現
        mMap.getUiSettings().setMapToolbarEnabled(true);    // 右下角的導覽及開啟 Google Map功能
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));     // 放大地圖到 16 倍大
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPS();
                } else {
                    Toast.makeText(compus_map.this, "請允許所有權限避免功能不正常", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    void checkGPS()
    {
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
                lvSnippet = ((ListView) infoWindow.findViewById(R.id.lvFloor));
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
        settings = getSharedPreferences(marker_data,0);
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
                        settings.edit().putBoolean(bicycle_state, temp_bicycle).commit();
                        settings.edit().putBoolean(montor_state, temp_montor).commit();
                        settings.edit().putBoolean(trash_state, temp_trash).commit();
                        settings.edit().putBoolean(toilet_state, temp_toilet).commit();
                        settings.edit().putBoolean(college_state, temp_college).commit();
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
                            mMarkers.add(singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[1])&&(settings.getBoolean(montor_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
                            mMarkers.remove(i);
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_scooter);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[1]);
                            mMarkers.add(singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[2])&&(settings.getBoolean(trash_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
                            mMarkers.remove(i);
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_trash);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[2]);
                            mMarkers.add(singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[3])&&(settings.getBoolean(toilet_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
                            mMarkers.remove(i);
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_toilet);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[3]);
                            mMarkers.add(singleMarker);
                        }
                    }
                    for(int i = 0;i<mMarkers.size();i++)
                    {
                        if(mMarkers.get(i).getTag().equals(marker_type[4])&&(settings.getBoolean(college_state,true))){
                            latlng = mMarkers.get(i).getPosition();
                            String title = mMarkers.get(i).getTitle();
                            String snippet = mMarkers.get(i).getSnippet();
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
                            mMarkers.remove(i);
                            icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_college);
                            singleMarker = mMap.addMarker( new MarkerOptions()
                                    .position(latlng)
                                    .title(title)
                                    .snippet(snippet)
                                    .icon(icon));
                            singleMarker.setTag(marker_type[4]);
                            mMarkers.add(singleMarker);
                        }
                    }
                    //Toast.makeText(view.getContext(),"確定", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        settings = getSharedPreferences(marker_data,0);
        switch(view.getId()) {
            case R.id.cb_bicycle:
                settings.edit().putBoolean(bicycle_state,checked).commit();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_motor:
                settings.edit().putBoolean(montor_state,checked).commit();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_trash:
                settings.edit().putBoolean(trash_state,checked).commit();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_toilet:
                settings.edit().putBoolean(toilet_state,checked).commit();
                //Toast.makeText(this,((CheckBox) view).getText() +"/n"+checked,Toast.LENGTH_SHORT).show();
                break;
            case R.id.cb_college:
                settings.edit().putBoolean(college_state,checked).commit();
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
        mMarkers.add(singleMarker);

        latlng = new LatLng(23.696210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_biycycle);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("腳踏車停車場" + "----------")
                .icon(icon));
        singleMarker.setTag("腳踏車車位");
        mMarkers.add(singleMarker);

        latlng = new LatLng(23.697210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_scooter);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("機車停車場" + "----------")
                .icon(icon));
        singleMarker.setTag("機車車位");
        mMarkers.add(singleMarker);

        latlng = new LatLng(23.698210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_toilet);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("廁所" + "----------")
                .icon(icon));
        singleMarker.setTag("廁所");
        mMarkers.add(singleMarker);

        latlng = new LatLng(23.699210, 120.536921);
        icon = BitmapDescriptorFactory.fromResource(R.mipmap.marker_trash);
        singleMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title("垃圾桶" + "----------")
                .icon(icon));
        singleMarker.setTag("垃圾桶");
        mMarkers.add(singleMarker);

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        String tag = marker.getTag().toString();
        Toast.makeText(compus_map.this, tag , Toast.LENGTH_SHORT).show();
        return false;
    }

}