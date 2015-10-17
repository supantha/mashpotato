package com.login.seedit.seedit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by supantha on 16/10/15.
 */
public class UploadVideoPage extends AppCompatActivity {

    TransferUtility transferUtility;
    Button cancelButton;
    ProgressBar progressBar;
    final String selected_id[]={""};
    String filePath;
    String privacy="";
    String title="";
    String labels="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_video_page);
        initTransfer();
        uploadVideo(getIntent().getStringExtra("path"));
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        cancelButton=(Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transferUtility.cancelAllWithType(TransferType.UPLOAD);
            }
        });
        String[] suggest1={"abc","abcd","bcde","abdf","abdg"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (getApplicationContext(),android.R.layout.select_dialog_item,suggest1);
        final AutoCompleteTextView autoCompleteTextView=(AutoCompleteTextView) findViewById(R.id.autocomplete);
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                //String[] suggest={"abng","abkj","acio","fdds","aeqw"};
                final String[] suggest = {" "," "," "," "," "};
                final String[] user_id = new String[5];
                //new Thread(new Runnable() {
                class Threading implements Runnable{
                    URL url = null;

                    @Override
                    public void run() {
                        try {
                            String input = URLEncoder.encode(s.toString(), "UTF-8");
                            String urlString = "http://sagarg.housing.com:3000/user/suggest?input=" + input;
                            try {
                                url = new URL(urlString);
                            } catch (MalformedURLException me) {
                                Log.e("Malformed URL", me.getMessage());
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e("Unsupported Exception", e.getMessage());
                        }
                        try {
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            InputStream in = urlConnection.getInputStream();
                            InputStreamReader isw = new InputStreamReader(in);
                            BufferedReader br = new BufferedReader(isw);
                            String jsonString = br.readLine();
                            try {
                                JSONArray array = new JSONArray(jsonString);
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    suggest[i] = object.getString("handle");
                                    user_id[i] = object.getString("user_id");
                                    //Toast.makeText(getApplicationContext(),suggest[i],Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException je) {
                                Log.e("JSON error", je.getMessage());
                            }
                            /*ArrayAdapter<String> adapter = new ArrayAdapter<String>
                                    (getApplicationContext(),android.R.layout.select_dialog_item,suggest);
                            autoCompleteTextView.setAdapter(adapter);*/
                        } catch (IOException e) {
                            Log.e("IO error", e.getMessage());
                        }
                    }
                }
                //String suggest2[]={"test1","test2","test3","test4","test5"};
                Threading t1=new Threading();
                Thread t=new Thread(t1);
                t.start();
                while(t.isAlive()){}
                ArrayAdapter<String> adapter = new ArrayAdapter<String>
                        (getApplicationContext(), android.R.layout.select_dialog_item, suggest);
                autoCompleteTextView.setAdapter(adapter);
                final int pos[] = {1};
                autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        pos[0] = position;
                    }
                });
                selected_id[0] = user_id[pos[0]-1];
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        final EditText titleBox=(EditText) findViewById(R.id.title);
        final EditText labelsBox=(EditText) findViewById(R.id.labels);
        Button plantButton=(Button) findViewById(R.id.Plant);
        plantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup radioGroup=(RadioGroup) findViewById(R.id.radio);
                int privacyType=radioGroup.getCheckedRadioButtonId();
                RadioButton privacySelected=(RadioButton) findViewById(privacyType);
                if(privacySelected.getText()!=null)
                    privacy=privacySelected.getText().toString();
                if(titleBox.getText()!=null)
                    title=titleBox.getText().toString();
                if(labelsBox.getText()!=null)
                    labels=labelsBox.getText().toString();
                makePostCall(selected_id,filePath,privacy,title,labels);
            }
        });
    }

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
        filePath=videoPath;
        File videoFile = new File(videoPath);
        String title = new String("Filename: " + videoPath);
        //textView.setText("Uploading Started");

        final TransferObserver observer = transferUtility.upload(
                "videoseed",     /* The bucket to upload to */
                title,    /* The key for the uploaded object */
                videoFile      /* The file where the data to upload exists */
        );

        Log.v("Observer", observer.toString());
        Toast.makeText(getApplicationContext(), observer.getState().toString(), Toast.LENGTH_SHORT).show();

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                //dosomething
                Toast.makeText(getApplicationContext(), observer.getState().toString(), Toast.LENGTH_SHORT).show();
                Log.e("Observer", observer.getState().toString());

            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                //textView.setText(Integer.toString(percentage));
                Toast.makeText(getApplicationContext(), Integer.toString(percentage), Toast.LENGTH_SHORT).show();
                progressBar.setProgress(percentage);


            }

            @Override
            public void onError(int id, Exception ex) {
                Toast.makeText(getApplicationContext(), "Failed to upload", Toast.LENGTH_SHORT).show();
                Log.e("Uploading", ex.getMessage());
            }
        });

    }

    public void makePostCall(String id[],String Path,String priv,String tit,String lab) {
        Boolean pub = true;
        if (priv.equals("Private")) {
            pub = false;
        }
        String labels[] = lab.split(",");
        String ids = "";
        for (int i = 0; i < id.length; i++)
            ids = id + ",";
        final JSONObject obj = new JSONObject();
        try {
            obj.put("tag_ids", id[0]);
            obj.put("link", Path);
            obj.put("is_public", pub);
            obj.put("title", tit);
            obj.put("labels", lab);
        } catch (JSONException je) {
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                String urlString = "http://sagarg.housing.com:3000/seed/register";
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setUseCaches(false);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.connect();
                    OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                    out.write(obj.toString());
                    out.close();
                    int HttpResult = urlConnection.getResponseCode();
                } catch (MalformedURLException me) {
                    Log.e("Malformed URL", me.getMessage());
                } catch (ProtocolException pe) {
                    Log.e("Protocol Exception", pe.getMessage());
                } catch (IOException e) {
                    Log.e("IOException", e.getMessage());
                }
            }
        }).start();
    }

}
