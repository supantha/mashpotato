package com.login.seedit.seedit;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    TextView heading;
    TextView distance;
    TextView location;
    static final int CAPTURE_VIDEO_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);
        heading = (TextView) findViewById(R.id.name);
        distance = (TextView) findViewById(R.id.distance);
        location = (TextView) findViewById(R.id.location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Button videoButton = (Button) findViewById(R.id.videobutton);
        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY);
                }
            }
        });
    }


    int count = 0;


    obj[] locations = new obj[50];


    @Override
    public void onMapReady(GoogleMap map) {
        // Add a marker in Sydney, Australia, and move the camera.

        map.setOnMarkerClickListener(this);

        Intent GPSt = new Intent(this, GPSTracker.class);
        startService(GPSt);

        GPSTracker gps;
        gps = new GPSTracker(MapsActivity.this);
        if (gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            Log.v("Your LOCATION is -" + "Lat:", latitude + "Long: " + longitude);

            LatLng presentLoc = new LatLng(latitude, longitude);
            double zoomLevel = 16.0; //This goes up to 21
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(presentLoc, (float) zoomLevel));
            LatLng NE = map.getProjection().getVisibleRegion().latLngBounds.northeast;
            LatLng SW = map.getProjection().getVisibleRegion().latLngBounds.southwest;
            JSONObject request_query = new JSONObject();
            try {
                request_query.put("user_id", "123");
                request_query.put("loc", "POINT(" + longitude + " " + latitude + ")");
                request_query.put("ne", "POINT(" + NE.longitude + " " + NE.latitude + ")");
                request_query.put("sw", "POINT(" + SW.longitude + " " + SW.latitude + ")");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i("Request Query: ", request_query.toString());
//            (new com.login.seedit.seedit.DownloadFilesTask()).execute("http://sagarg.housing.com:3001/seed/nearby", request_query.toString());
            map.addMarker(new MarkerOptions().position(presentLoc).title("Present Location"));


            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);


            LatLng tmp = new LatLng(20, 60);
            Bitmap icon1 = BitmapFactory.decodeResource(getResources(), R.drawable.pin);
            Bitmap bhalfsize1 = Bitmap.createScaledBitmap(icon1, icon1.getWidth() / 2, icon1.getHeight() / 2, false);
            map.addMarker(new MarkerOptions().position(tmp).icon(BitmapDescriptorFactory.fromBitmap(bhalfsize1)));


            try {
                // JSON here
                String result = executePost("http://sagarg.housing.com:3000/seed/nearby", request_query.toString());
                Log.i("Response_result: ", result);
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);

                    Log.i("Latitude", object.getString("lat"));
                    LatLng loc = new LatLng(Double.parseDouble(object.getString("lat")),
                            Double.parseDouble(object.getString("lng")));


                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.pin);
                    Bitmap bhalfsize = Bitmap.createScaledBitmap(icon, icon.getWidth() / 2, icon.getHeight() / 2, false);
                    map.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.fromBitmap(bhalfsize)));
                    locations[i].setAddress(object.getString("address"));
                    locations[i].setTitle(object.getString("title"));
                    locations[i].setDistance(object.getString("distance"));
                    locations[i].setLatlong(loc);
                    count++;
                    map.setOnMarkerClickListener(this);


                }
                JSONObject lat_lng_data = new JSONObject(result);
                Log.i("Keys: ", lat_lng_data.keys().toString());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Toast.makeText(
                    getApplicationContext(),
                    "Your LOCATION is -\nLat: " + latitude + "\nLong: "
                            + longitude, Toast.LENGTH_LONG).show();
        } else {
            gps.showSettingsAlert();
        }


    }

    public static String executePost(String targetURL, String urlParameters) {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length", "" +
                    Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response
            int status = connection.getResponseCode();
            InputStream is;
            if (status >= HttpStatus.SC_BAD_REQUEST)
                is = connection.getErrorStream();
            else
                is = connection.getInputStream();
//            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {

            e.printStackTrace();
            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        for (int i = 0; i < count; i++) {

            if (locations[i].getLatlong() == marker.getPosition()) ;
            {
                heading.setText(locations[i].getTitle());
                location.setText(locations[i].getAddress());
                distance.setText(locations[i].getDistance());
            }


        }

        return false;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_VIDEO_ACTIVITY && resultCode == RESULT_OK) {
            try {
                //Uri videoUri=MediaStore.Video.Media.getContentUri(mCurrentVideoPath);
                Uri selectedVideoUri = data.getData();
                String selectedPath = getPath(selectedVideoUri);
                //Toast.makeText(getApplicationContext(),"SELECT_VIDEO Path: " + selectedPath,Toast.LENGTH_SHORT).show();
                //uploadVideo(selectedPath);
                Intent intent = new Intent(MapsActivity.this, UploadVideoPage.class);
                intent.putExtra("path", selectedPath);
                MapsActivity.this.startActivity(intent);

            } catch (Exception e) {
                System.out.println("Error Encountered");
            }
        }
    }

    private String getPath(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        cursor.moveToFirst();
        String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
        int fileSize = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
        long duration = TimeUnit.MILLISECONDS.toSeconds(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));


        //some extra potentially useful data to help with filtering if necessary
        System.out.println("size: " + fileSize);
        System.out.println("path: " + filePath);
        System.out.println("duration: " + duration);

        return filePath;
    }
}
   /* public class MainActivity extends AppCompatActivity {

        static final int CAPTURE_VIDEO_ACTIVITY = 1;
        String mCurrentVideoPath;
        TextView textView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            //initTransfer();
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent mapIntent = new Intent(MainActivity.this, MapsActivity.class);
                    MainActivity.this.startActivity(mapIntent);
                }
            });

            Button videoButton = (Button) findViewById(R.id.videobutton);
            videoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        //File videoFile = null;
                        //try {
                        //  videoFile = createVideoFile();
                        //} catch (IOException e) {
                        //  Log.e("Video File", "Video File Storage Error");
                        //}
                        //intent.putExtra(videoFile);
                        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY);
                    }
                }
            });
        }

   /* private File createVideoFile() throws IOException{
        //Create a video file
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = "MP4_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File video = File.createTempFile(
                videoFileName,  // prefix
                ".mp4",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentVideoPath = "file:"+video.getAbsolutePath();
        return video;
    }*/

     /*   @Override
       protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CAPTURE_VIDEO_ACTIVITY && resultCode == RESULT_OK) {
                try {
                    //Uri videoUri=MediaStore.Video.Media.getContentUri(mCurrentVideoPath);
                    Uri selectedVideoUri = data.getData();
                    String selectedPath = getPath(selectedVideoUri);
                    //Toast.makeText(getApplicationContext(),"SELECT_VIDEO Path: " + selectedPath,Toast.LENGTH_SHORT).show();
                    //uploadVideo(selectedPath);
                    Intent intent = new Intent(MainActivity.this, UploadVideoPage.class);
                    intent.putExtra("path", selectedPath);
                    MainActivity.this.startActivity(intent);

                } catch (Exception e) {
                    System.out.println("Error Encountered");
                }
            }
        }

        private String getPath(Uri uri) {
            String[] projection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION};
            Cursor cursor = managedQuery(uri, projection, null, null, null);
            cursor.moveToFirst();
            String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
            int fileSize = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
            long duration = TimeUnit.MILLISECONDS.toSeconds(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));


            //some extra potentially useful data to help with filtering if necessary
            System.out.println("size: " + fileSize);
            System.out.println("path: " + filePath);
            System.out.println("duration: " + duration);

            return filePath;
        }


   /* private void uploadVideo(String videoPath) throws ParseException, IOException {

        URL url=new URL(urlString);
        HttpURLConnection connection=(HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        //HttpClient httpclient = new DefaultHttpClient();
        //HttpPost httppost = new HttpPost();

        FileBody filebodyVideo = new FileBody(new File(videoPath));
        StringBody title = new StringBody("Filename: " + videoPath);
        StringBody description = new StringBody("This is a description of the video");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("videoFile", filebodyVideo);
        builder.addPart("title", title);
        builder.addPart("description", description);
        httppost.setEntity(reqEntity);

        // DEBUG
        System.out.println( "executing request " + httppost.getRequestLine( ) );
        HttpResponse response = httpclient.execute( httppost );
        HttpEntity resEntity = response.getEntity( );

        // DEBUG
        System.out.println( response.getStatusLine( ) );
        if (resEntity != null) {
            System.out.println( EntityUtils.toString(resEntity) );
        } // end if

        if (resEntity != null) {
            resEntity.consumeContent( );
        } // end if

        httpclient.getConnectionManager( ).shutdown( );
    } // end of uploadVideo( )*/

   /* public void initTransfer()
    {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-1:9f0b6279-d95b-4964-ab70-01b51939fe2f", // Identity Pool ID
                Regions.AP_NORTHEAST_1 // Region
        );
        // Create an S3 client
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);

        // Set the region of your S3 bucket
        s3.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));

        transferUtility = new TransferUtility(s3, getApplicationContext());

    }*/

    /*private void uploadVideo(String videoPath)
    {
            File videoFile = new File(videoPath);
            String title = new String("Filename: " + videoPath);
            textView.setText("Uploading Started");

            final TransferObserver observer = transferUtility.upload(
                    "videoseed",     /* The bucket to upload to */
        //  title,    /* The key for the uploaded object */
        //  videoFile      /* The file where the data to upload exists */
        //        );

       /* Log.v("Observer", observer.toString());
        Toast.makeText(getApplicationContext(),observer.getState().toString(),Toast.LENGTH_SHORT).show();

       observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                //dosomething
                Toast.makeText(getApplicationContext(),observer.getState().toString(),Toast.LENGTH_LONG).show();

            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage=(int) (bytesCurrent/bytesTotal*100);
                textView.setText(Integer.toString(percentage));
                Toast.makeText(getApplicationContext(),Integer.toString(percentage),Toast.LENGTH_LONG).show();

            }

            @Override
            public void onError(int id, Exception ex) {
                Toast.makeText(getApplicationContext(),"Failed to upload",Toast.LENGTH_LONG).show();
                Log.e("Uploading",ex.getMessage());
            }
        });

    }*/


     /*   @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }


        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                return true;
            }

            return super.onOptionsItemSelected(item);
        }
    } */


