package com.example.mapmo;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GeoQueryEventListener, GoogleMap.OnMarkerClickListener {
    DBHandler dbHandler = DBHandler.open(this);

    // 처음에만 내위치 찾아가도록 하기위해서
    public boolean start = false;
    // 마크 표시시 나타나는 슬라이딩드로워
    public TextView selectAddressTv;
    public Button addMemoBt;
    //위치 데이터 private로 수정필요
    public static String addMarkerAddress;
    public static String addMarkerLatitude;
    public static String addMarkerLongitude;
    public EditText searchPt;
    private Geocoder geocoderSearch;
    private GoogleMap mMap;
    private Marker currentMarker = null;


    // 김태성 버튼을 스위치로
    public Switch serviceSwitch;
    public TextView serviceText;

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초

    // onRequestPermissionsResult에서 수신된 결과에서 ActivityCompat.requestPermissions를 사용한 퍼미션 요청을 구별하기 위해 사용됩니다.
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;

    //추가
    private static DatabaseReference myLccationRef;
    public static GeoFire geoFire;
    public static List<LatLng> dangerousArea; // GeoFence 할 장소 변수명 나중에 바꾸자


    // 앱을 실행하기 위해 필요한 퍼미션을 정의합니다.
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};  // 외부 저장소

    public Location mCurrentLocatiion;
    public LatLng currentPosition;


    public FusedLocationProviderClient mFusedLocationClient;
    public LocationRequest locationRequest;
    public Location location;


    private View mLayout;  // Snackbar 사용하기 위해서는 View가 필요합니다.
    // (참고로 Toast에서는 Context가 필요했습니다.)

    public MarkerOptions makerOptions;
    public Marker markerSelect;
    public boolean markerCheck = false;

    public Marker markerDB;

    private ImageButton menuBt;

    public static Boolean radiusCheck = false;
    public int radiusDB;
    public Marker searchMarker;

    public Boolean onAR = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN); // 키보드생성시 화면 스크롤위함
        //getSupportActionBar().setElevation(0); //액션바 그림자 제거
        //데이터베이스 open
        final DBHandler dbHandler = DBHandler.open(getBaseContext());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mLayout = findViewById(R.id.layout_main);

        selectAddressTv = (TextView) findViewById(R.id.selectAddressTv);
        selectAddressTv.setVisibility(View.INVISIBLE);
        addMemoBt = (Button) findViewById(R.id.addMemoBt);
        addMemoBt.setVisibility(View.INVISIBLE);
        searchPt = (EditText) findViewById(R.id.searchPt);
        menuBt = (ImageButton)findViewById(R.id.menubtn);


        //김태성 버튼을 스위치로 변경
        serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);

        //메모 생성 (제목, 장소, 현재날짜) ***********************************************************
        addMemoBt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, addMemoActivity.class);
                startActivityForResult(intent, 100);
            }
        });


        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        // onMapReady 호출
        mapFragment.getMapAsync(this);

        //initArea();
        settingGeoFire();



    }
    //버튼이 눌렸을때 여기로옴

    public void mOnClick(View v){

        //팝업 메뉴 객체 만듬

        PopupMenu popup = new PopupMenu(this, v);



        //xml파일에 메뉴 정의한것을 가져오기위해서 전개자 선언

        MenuInflater inflater = popup.getMenuInflater();

        final Menu menu = popup.getMenu();



        //실제 메뉴 정의한것을 가져오는 부분 menu 객체에 넣어줌

        inflater.inflate(R.menu.menu_view, menu);



        //메뉴가 클릭했을때 처리하는 부분

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {



            @Override

            public boolean onMenuItemClick(MenuItem item) {

                // TODO Auto-generated method stub



                //각 메뉴별 아이디를 조사한후 할일을 적어줌

                switch(item.getItemId()){
                    case R.id.totalmemo:
                        Intent intent = new Intent(MainActivity.this, memoListActivity.class);
                        startActivityForResult(intent, 300);
                        break;
                    case R.id.setting:
                        Intent intent2 = new Intent(MainActivity.this, Setting.class);
                        startActivityForResult(intent2, 200);
                        break;
                    case R.id.info:
                        Intent intent3 = new Intent(MainActivity.this, AppInfo.class);
                        startActivity(intent3);
                        break;
                }

                return false;

            }

        });



        popup.show();

    }


    private void openDB() {

        mMap.clear();
        Cursor cursorRadius = dbHandler.select_radius();
        if (cursorRadius!=null && cursorRadius.getCount() == 0)
        {
            dbHandler.insert_radius(1,300);
        }
        else
        {
            cursorRadius.moveToFirst();
            radiusDB = cursorRadius.getInt(1);
        }

        Location locationA = new Location("pointA");
        Location locationB = new Location("pointB");
        if (onAR)
        {
            locationA.setLatitude(mCurrentLocatiion.getLatitude());
            locationA.setLongitude(mCurrentLocatiion.getLongitude());
        }

        dangerousArea = new ArrayList<>();
        dangerousArea.clear();
        Cursor cursor = dbHandler.select_memo();
        cursor.moveToFirst();
        for (int i=0; i<cursor.getCount();i++)
        {
            int id = cursor.getInt(0);
            String title = cursor.getString(1);
            String start = cursor.getString(2);
            String finish = cursor.getString(3);
            String address = cursor.getString(4);
            String lat = cursor.getString(5);
            String lon = cursor.getString(6);
            String phchk = cursor.getString(7);
            int all = cursor.getInt(8);
            LatLng newMemo = new LatLng(Double.parseDouble(lat),Double.parseDouble(lon));
            if (all != 1)
            {
                markerDB = mMap.addMarker(new MarkerOptions().position(newMemo).title(title).snippet(id+"").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_mapmo_marker2)).alpha(1.0f));
            }
            else if (all == 1 )
            {
                markerDB = mMap.addMarker(new MarkerOptions().position(newMemo).title(title).snippet(id+"").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_mapmo_marker2)).alpha(0.6f));
            }

            locationB.setLatitude(Double.parseDouble(lat));
            locationB.setLongitude(Double.parseDouble(lon));
            if (locationA.distanceTo(locationB)>radiusDB && phchk.equals("true") && onAR && all==0)
            {
                dbHandler.update_memo(id, title, start, finish, "false",0);
                sendNotification("잊으신 일 없으신가요?", title+" 영역에서 나왔습니다!");
            }
            //dangerousArea.add(newMemo);
            if (all == 1)
            {
                mMap.addCircle(new CircleOptions().center(newMemo)
                        .radius(radiusDB) //500m
                        .strokeColor(Color.rgb(186,186,185))
                        .fillColor(0x33BAB9B9)
                        .strokeWidth(5.0f)
                );
                GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(newMemo.latitude,newMemo.longitude), (double)radiusDB/1000); //500m
                geoQuery.addGeoQueryEventListener(MainActivity.this);
            }
            else if (all != 1)
            {
                mMap.addCircle(new CircleOptions().center(newMemo)
                        .radius(radiusDB) //500m
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.parseColor("#500084d3"))
                        .strokeWidth(5.0f)
                );
                GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(newMemo.latitude,newMemo.longitude), (double)radiusDB/1000); //500m
                geoQuery.addGeoQueryEventListener(MainActivity.this);
            }
            cursor.moveToNext();
        }


        /*for(LatLng latLng : dangerousArea)
        {
            mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(radiusDB) //500m
                    .strokeColor(Color.rgb(238,135,114))
                    .fillColor(0x22ed8672)
                    .strokeWidth(5.0f)
            );

            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude), (double)radiusDB/1000); //500m
            geoQuery.addGeoQueryEventListener(MainActivity.this);
        }*/
    }



    public void settingGeoFire() {
        getUUID deviceid = new getUUID(MainActivity.this);
        myLccationRef = FirebaseDatabase.getInstance().getReference(deviceid.getDeviceUuid()+"");
        geoFire = new GeoFire(myLccationRef);
    }

    @Override
    // OnMapReadyCallback의 onMapReady 메소드 구현
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");

        mMap = googleMap;



        //마커클릭 리스터 지정
        mMap.setOnMarkerClickListener(this);

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setDefaultLocation();


        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);



        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            startLocationUpdates(); // 3. 위치 업데이트 시작


        }else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        ActivityCompat.requestPermissions( MainActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }



        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        openDB();

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng point) {

                if (searchMarker != null)
                {
                    searchMarker.remove();
                }


                if (markerCheck == true)
                {
                    markerSelect.remove();
                }

                // 현재 위도와 경도에서 화면 포인트를 알려준다
                Point screenPt = mMap.getProjection().toScreenLocation(point);
                // 현재 화면에 찍힌 포인트로 부터 위도와 경도를 알려준다.
                LatLng latLng = mMap.getProjection().fromScreenLocation(screenPt);
                Log.d("맵좌표","좌표: 위도(" + String.valueOf(point.latitude) + "), 경도(" + String.valueOf(point.longitude) + ")");
                Log.d("화면좌표","화면좌표: X(" + String.valueOf(screenPt.x) + "), Y(" + String.valueOf(screenPt.y) + ")");

                LatLng mAddMarker = new LatLng(Double.parseDouble(String.valueOf(point.latitude)),Double.parseDouble( String.valueOf(point.longitude)));

                // 구글 맵에 표시할 마커에 대한 옵션 설정
                makerOptions = new MarkerOptions();
                makerOptions
                        .position(mAddMarker)
                        .title(getCurrentAddress(mAddMarker))
                        .snippet(null)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_mapmo_marker2));

                // 마커를 생성한다.
                markerSelect = mMap.addMarker(makerOptions);
                markerCheck = true;

                //메모 추가에 들어갈 위치 정보
                addMarkerAddress = getCurrentAddress(mAddMarker);
                addMarkerLatitude = String.valueOf(point.latitude);
                addMarkerLongitude = String.valueOf(point.longitude);

                selectAddressTv.setVisibility(View.VISIBLE);
                addMemoBt.setVisibility(View.VISIBLE);
                selectAddressTv.setText(getCurrentAddress(mAddMarker));

            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                if (searchMarker != null)
                {
                    searchMarker.remove();
                }
                selectAddressTv.setVisibility(View.INVISIBLE);
                addMemoBt.setVisibility(View.INVISIBLE);

                if (markerCheck == true)
                {
                    markerSelect.remove();
                }

            }
        });


        geocoderSearch = new Geocoder(this);
        searchPt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    String str=searchPt.getText().toString();
                    List<Address> addressList = null;
                    try {
                        // editText에 입력한 텍스트(주소, 지역, 장소 등)을 지오 코딩을 이용해 변환
                        addressList = geocoderSearch.getFromLocationName(
                                str, // 주소
                                10); // 최대 검색 결과 개수
                    }
                    catch (IOException e) {
                        e.printStackTrace();

                    }
                    try {
                        System.out.println(addressList.get(0).toString());
                        // 콤마를 기준으로 split
                        String []splitStr = addressList.get(0).toString().split(",");
                        String address = splitStr[0].substring(splitStr[0].indexOf("\"") + 1,splitStr[0].length() - 2); // 주소
                        System.out.println(address);

                        String latitude = splitStr[10].substring(splitStr[10].indexOf("=") + 1); // 위도
                        String longitude = splitStr[12].substring(splitStr[12].indexOf("=") + 1); // 경도
                        System.out.println(latitude);
                        System.out.println(longitude);

                        // 좌표(위도, 경도) 생성
                        LatLng point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                        // 마커 생성
                        MarkerOptions mOptionsSearch = new MarkerOptions();
                        mOptionsSearch.title(address);
                        mOptionsSearch.snippet(null);
                        mOptionsSearch.position(point);
                        mOptionsSearch.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_mapmo_marker2));
                        // 마커 추가
                        if(searchMarker != null)
                        {
                            searchMarker.remove();
                        }

                        searchMarker = mMap.addMarker(mOptionsSearch);
                        // 해당 좌표로 화면 줌
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point,15));

                        selectAddressTv.setVisibility(View.VISIBLE);
                        selectAddressTv.setText(address);
                        addMemoBt.setVisibility(View.VISIBLE);

                        addMarkerAddress = address;
                        addMarkerLatitude = String.valueOf(point.latitude);
                        addMarkerLongitude = String.valueOf(point.longitude);
                    }
                    catch (IndexOutOfBoundsException | NullPointerException e)
                    {
                        Toast.makeText(MainActivity.this, "검색어를 다시 입력해주세요.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    // 키보드 내리기
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchPt.getWindowToken(), 0);
                }
                return true;
            }
        });



    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);

                currentPosition = new LatLng(location.getLatitude(), location.getLongitude());




                geoFire.setLocation("me", new GeoLocation(location.getLatitude(),
                        location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        String markerTitle = getCurrentAddress(currentPosition);
                        String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                                + " 경도:" + String.valueOf(location.getLongitude());

                        Log.d(TAG, "onLocationResult : " + markerSnippet);


                        //현재 위치에 마커 생성하고 이동

                        setCurrentLocation(location, markerTitle, null);

                    }
                });

                mCurrentLocatiion = location;

            }

        }

    };



    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        }else {

            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);



            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED   ) {

                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");
                return;
            }


            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
            {
                mMap.setMyLocationEnabled(true);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        if (checkPermission()) {

            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            if (mMap!=null)
                mMap.setMyLocationEnabled(true);

        }
    }


    @Override
    protected void onStop() {

        super.onStop();

        if (mFusedLocationClient != null) {

            Log.d(TAG, "onStop : call stopLocationUpdates");
            //mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    public String getCurrentAddress(LatLng latlng) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }


        if (addresses == null || addresses.size() == 0) {
            //Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }

    }


    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {


        if (currentMarker != null) currentMarker.remove();


        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());


        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        if (start == false)
        {
            mMap.animateCamera(cameraUpdate);
        }
        start = true;
    }


    public void setDefaultLocation() {


        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.555898, 126.969953);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mMap.moveCamera(cameraUpdate);

    }


    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    private boolean checkPermission() {

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);



        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {
            return true;
        }

        return false;

    }



    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                // 퍼미션을 허용했다면 위치 업데이트를 시작합니다.
                startLocationUpdates();
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {


                    // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();

                }else {


                    // "다시 묻지 않음"을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();
                }
            }

        }
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        selectAddressTv.setVisibility(View.INVISIBLE);
        addMemoBt.setVisibility(View.INVISIBLE);
        onAR = true;
        openDB();
        /*Cursor cursor = dbHandler.select_memo();
        cursor.moveToFirst();
        String phchk = cursor.getString(7);
        Log.i("pppppp",phchk);*/

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d(TAG, "onActivityResult : GPS 활성화 되있음");
                        needRequest = true;

                        return;
                    }
                }
                break;

        }
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        //오늘날짜 구해서 문자열 변환
        long todayLong = System.currentTimeMillis();
        Date todayDate = new Date(todayLong);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayString = sdf.format(todayDate);
        todayString = todayString.replaceAll("-","");

        Location locationA = new Location("pointA");
        Location locationB = new Location("pointB");
        locationA.setLatitude(location.latitude);
        locationA.setLongitude(location.longitude);
        Cursor cursor = dbHandler.select_memo();
        cursor.moveToFirst();
        for (int i=0; i<cursor.getCount();i++)
        {
            int select_id = cursor.getInt(0);
            String title = cursor.getString(1);
            String start = cursor.getString(2);
            String finish = cursor.getString(3);
            String address = cursor.getString(4);
            String lat = cursor.getString(5);
            String lon = cursor.getString(6);
            String phchk = cursor.getString(7);
            //LatLng newMemo = new LatLng(Double.parseDouble(lat),Double.parseDouble(lon));
            locationB.setLatitude(Double.parseDouble(lat));
            locationB.setLongitude(Double.parseDouble(lon));

            int startInt = Integer.parseInt(start.replace("-",""));
            int finishInt = Integer.parseInt(start.replace("-",""));

            Log.i("dayCheck", startInt +","+Integer.parseInt(todayString)+","+finishInt);
            if (startInt<= Integer.parseInt(todayString) && Integer.parseInt(todayString) <= finishInt)
            {

                if (locationA.distanceTo(locationB)<=radiusDB && phchk.equals("false") && cursor.getInt(8)==0)
                {

                    sendNotification("MAPMO에서 알려드립니다", title+" 영역에 들어왔습니다!");

                    dbHandler.update_memo(select_id, title, start, finish, "true",0);
                }

            }
            cursor.moveToNext();
        }

    }

    @Override
    public void onKeyExited(String key) {
        long todayLong = System.currentTimeMillis();
        Date todayDate = new Date(todayLong);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayString = sdf.format(todayDate);
        todayString = todayString.replaceAll("-","");

        Location locationA = new Location("pointA");
        Location locationB = new Location("pointB");
        locationA.setLatitude(mCurrentLocatiion.getLatitude());
        locationA.setLongitude(mCurrentLocatiion.getLongitude());
        Cursor cursor = dbHandler.select_memo();
        cursor.moveToFirst();
        for (int i=0; i<cursor.getCount();i++)
        {
            int select_id = cursor.getInt(0);
            String title = cursor.getString(1);
            String start = cursor.getString(2);
            String finish = cursor.getString(3);
            String address = cursor.getString(4);
            String lat = cursor.getString(5);
            String lon = cursor.getString(6);
            String phchk = cursor.getString(7);

            locationB.setLatitude(Double.parseDouble(lat));
            locationB.setLongitude(Double.parseDouble(lon));
            int startInt = Integer.parseInt(start.replace("-",""));
            int finishInt = Integer.parseInt(start.replace("-",""));

            if (startInt<= Integer.parseInt(todayString) && Integer.parseInt(todayString) <= finishInt)
            {

                if (locationA.distanceTo(locationB)>radiusDB && phchk.equals("true") && cursor.getInt(8)==0)
                {
                    sendNotification("잊으신 일 없으신가요?", title+" 영역에서 나왔습니다!");
                    dbHandler.update_memo(select_id, title, start, finish, "false",0);
                }


            }
            cursor.moveToNext();
        }

        //sendNotification("Mapmo", "메모영역에서 나오셨습니다. 잊으신 일 없으신가요? ");

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }
    private void sendNotification(String title, String content) {
        Log.i("sendNotification", "호출 성공");

        String NOTIFICATION_CHANNEL_ID = "com.example.mapmo";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            //config
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);

    }

    public void startService(View view) {
        String input = "test";
        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View view) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.getSnippet() != null)
        {
            Intent intent = new Intent(MainActivity.this, viewMemoActivity.class);
            int select_id = Integer.parseInt(marker.getSnippet());
            intent.putExtra("select_id", select_id);
            startActivityForResult(intent, 200);
        }
        return true;

    }


}