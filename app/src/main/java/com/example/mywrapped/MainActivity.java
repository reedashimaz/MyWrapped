package com.example.mywrapped;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mywrapped.R;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String CLIENT_ID = "be94bf52fd094d7781e0f1ff7f9030f2";
    public static final String REDIRECT_URI = "com.example.mywrapped://auth";

    public static final int AUTH_TOKEN_REQUEST_CODE = 0;
    public static final int AUTH_CODE_REQUEST_CODE = 1;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mAccessToken, mAccessCode;
    private Call mCall;

    private TextView track1View, track2View, track3View, track4View, track5View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        track1View = (TextView) findViewById(R.id.track1);
        track2View = (TextView) findViewById(R.id.track2);
        track3View = (TextView) findViewById(R.id.track3);
        track4View = (TextView) findViewById(R.id.track4);
        track5View = (TextView) findViewById(R.id.track5);

        if (mAccessToken == null) {
            getToken();
        } else {
            getTopTracks();
        }
    }

    public void getToken() {
        AuthorizationRequest req = getAuthenticationRequest(AuthorizationResponse.Type.TOKEN);
        AuthorizationClient.openLoginActivity(this, AUTH_TOKEN_REQUEST_CODE, req);
    }

    private AuthorizationRequest getAuthenticationRequest(AuthorizationResponse.Type type) {
        return new AuthorizationRequest.Builder(CLIENT_ID, type, REDIRECT_URI)
                .setScopes(new String[]{"user-top-read", "user-modify-playback-state"}) // Ensures the scope includes access to top tracks
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);

        if (requestCode == AUTH_TOKEN_REQUEST_CODE) {
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                mAccessToken = response.getAccessToken();
                getTopTracks();  // Fetch top tracks once token is received
            } else if (response.getType() == AuthorizationResponse.Type.ERROR) {
                Log.e("Spotify Auth", "Error: " + response.getError());
            }
        }
    }

    private String[] trackURIs = new String[5];

    public void getTopTracks() {
        if (mAccessToken == null) {
            Toast.makeText(this, "Access token is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/tracks?limit=5")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Spotify", "Error fetching top tracks", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("Spotify", "Failed to fetch data: " + response);
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray items = jsonObject.getJSONArray("items");

                    String[] trackURIs = new String[5];

                    for (int i = 0; i < trackURIs.length; i++) {
                        JSONObject trackObject = items.getJSONObject(i);
                        String trackURI = trackObject.getString("uri");
                        trackURIs[i] = trackURI;
                    }

                    runOnUiThread(() -> {
                        try {
                            track1View.setText(items.getJSONObject(0).getString("name"));
                            track2View.setText(items.getJSONObject(1).getString("name"));
                            track3View.setText(items.getJSONObject(2).getString("name"));
                            track4View.setText(items.getJSONObject(3).getString("name"));
                            track5View.setText(items.getJSONObject(4).getString("name"));

                            Button playBtn1 = findViewById(R.id.playTrack1);
                            Button playBtn2 = findViewById(R.id.playTrack2);
                            Button playBtn3 = findViewById(R.id.playTrack3);
                            Button playBtn4 = findViewById(R.id.playTrack4);
                            Button playBtn5 = findViewById(R.id.playTrack5);

                            playBtn1.setOnClickListener(v -> playTrack(trackURIs[0]));
                            playBtn2.setOnClickListener(v -> playTrack(trackURIs[1]));
                            playBtn3.setOnClickListener(v -> playTrack(trackURIs[2]));
                            playBtn4.setOnClickListener(v -> playTrack(trackURIs[3]));
                            playBtn5.setOnClickListener(v -> playTrack(trackURIs[4]));

                        } catch (JSONException e) {
                            Log.e("Spotify", "JSON parsing error", e);
                        }
                    });
                } catch (JSONException e) {
                    Log.e("Spotify", "Response parsing error", e);
                }
            }
        });
    }

    public void playTrack(String trackURI) {
        if (mAccessToken == null) {
            Toast.makeText(this, "Access token missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (trackURI == null) {
            Toast.makeText(this, "Track URI missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String playUrl = "https://api.spotify.com/v1/me/player/play";

        JSONObject payload = new JSONObject();
        try {
            JSONArray uris = new JSONArray();
            uris.put(trackURI);
            payload.put("uris", uris);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(playUrl)
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Spotify", "Error playing track", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to play track", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("Spotify", "Failed to command Spotify: " + response);
                    // Log response body for debugging
                    Log.e("Spotify", "Response body: " + response.body().string());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to command Spotify", Toast.LENGTH_SHORT).show());
                } else {
                    Log.d("Spotify", "Successfully played track");
                    // Log response body for debugging
                    Log.d("Spotify", "Response body: " + response.body().string());
                }
            }
        });
    }
}