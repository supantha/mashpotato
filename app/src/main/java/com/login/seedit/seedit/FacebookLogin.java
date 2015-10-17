package com.login.seedit.seedit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class FacebookLogin extends AppCompatActivity {
    private CallbackManager callbackManager;
    private TextView info;
    public String login_response;
    //    private LoginButton loginButton;
    private LoginButton fbbutton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.login_page);
        info = (TextView) findViewById(R.id.info);
        fbbutton = (LoginButton) findViewById(R.id.login_button);
        fbbutton.setReadPermissions(Arrays.asList("public_profile, email, user_birthday"));

        fbbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Call private method
                onFblogin();
            }
        });
    }
    private void onFblogin()
    {
        callbackManager = CallbackManager.Factory.create();
        fbbutton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            //LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            public void onSuccess(final LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        // Application code
                        Log.i("LoginActivity", object.toString());
                        try {
                            object.put("access_token", loginResult.getAccessToken().getToken());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        login_response = object.toString();
                        (new com.login.seedit.seedit.DownloadFilesTask()).execute("http://10.1.6.69:3000/user/create_user", login_response);


                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender, birthday");
                request.setParameters(parameters);
                request.executeAsync();
                Intent intent = new Intent(FacebookLogin.this, MapsActivity.class);
                startActivity(intent);
            }


            @Override
            public void onCancel() {
                info.setText("Login attempt cancelled.");
            }

            @Override
            public void onError(FacebookException e) {
                info.setText("Login attempt failed.");
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);

    }
}
