package com.example.mashpotato.mashpotato;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    static final int CAPTURE_VIDEO_ACTIVITY=1;
    String mCurrentVideoPath;
    Button cancelButton;
    TransferUtility transferUtility;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initTransfer();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapIntent=new Intent(MainActivity.this,MapsActivity.class);
                MainActivity.this.startActivity(mapIntent);
            }
        });

        textView=(TextView) findViewById(R.id.upload_bar);
        Button videoButton= (Button) findViewById(R.id.videobutton);
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
        cancelButton=(Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transferUtility.cancelAllWithType(TransferType.UPLOAD);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_VIDEO_ACTIVITY && resultCode == RESULT_OK) {
            try {
                //Uri videoUri=MediaStore.Video.Media.getContentUri(mCurrentVideoPath);
                Uri selectedVideoUri = data.getData();
                String selectedPath = getPath(selectedVideoUri);
                //Toast.makeText(getApplicationContext(),"SELECT_VIDEO Path: " + selectedPath,Toast.LENGTH_SHORT).show();
                uploadVideo(selectedPath);
                //Intent intent=new Intent(MainActivity.this,UploadVideoPage.class);
                //intent.putExtra("path",selectedPath);
                //MainActivity.this.startActivity(intent);

            } catch (Exception e){
                System.out.println("Error Encountered");
            }
        }
    }

    private String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION};
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

    public void initTransfer()
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

    }

    private void uploadVideo(String videoPath)
    {
            File videoFile = new File(videoPath);
            String title = new String("Filename: " + videoPath);
            textView.setText("Uploading Started");

            final TransferObserver observer = transferUtility.upload(
                    "videoseed",     /* The bucket to upload to */
                    title,    /* The key for the uploaded object */
                    videoFile      /* The file where the data to upload exists */
            );

        Log.v("Observer", observer.toString());
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

    }



    @Override
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
}
