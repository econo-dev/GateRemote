package com.gal.gateremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This activity displays an image on the screen.
 * The image has three different regions that can be clicked / touched.
 * When a region is touched, the activity changes the view to show a different
 * image.
 */

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    OkHttpClient client = new OkHttpClient();
    Request request;
    boolean isOpen = false;
    int stateFlag = 0;
    Context context;

    Gate gate = new Gate();

    Button btnOpen;

    /**
     * Create the view for the activity.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        gate.setState(Gate.State.OPEN);
        btnOpen = (Button)findViewById(R.id.btn_open);
        ImageView iv = (ImageView) findViewById(R.id.image);
        if (iv != null) {
            iv.setOnTouchListener(this);
        }

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendPostRequest();
//
//                    switch (gate.getState()){
//                        case OPEN:
//                            btnOpen.setText("STOP");
//                            stateFlag = 1;
////                            gate.nextState();
//                            break;
//                        case STOP:
//                            btnOpen.setText(stateFlag==1?"CLOSE":"OPEN");
////                            gate.nextState();
//                            break;
//                        case CLOSE:
//                            btnOpen.setText("STOP");
//                            stateFlag = 2;
////                            gate.nextState();
//                            break;
//                        default:
//                            break;
//                    }
                    setButtonStates();
                    gate.nextState();

//                    backgroundButtonText(getApplicationContext(), "OPEN");
                    setButtonTextDelay(20_000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
//        toast("Touch the screen to discover where the regions are.");
    }

    private void setButtonTextDelay(long delay) {
        final Timer timer = new java.util.Timer();
        timer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        backgroundButtonText(getApplicationContext(), "OPEN");
                        timer.cancel();
                    }
                },
                delay
        );
    }

    public void setButtonStates() {
        switch (gate.getState()){
            case OPEN:
                btnOpen.setText("STOP");
                stateFlag = 1;
//                            gate.nextState();
                break;
            case STOP:
                btnOpen.setText(stateFlag==1?"CLOSE":"OPEN");
//                            gate.nextState();
                break;
            case CLOSE:
                btnOpen.setText("STOP");
                stateFlag = 2;
//                            gate.nextState();
                break;
            default:
                break;
        }
    }
    /**
     * Respond to the user touching the screen.
     * Change images to make things appear and disappear from the screen.
     */
    public boolean onTouchOriginal(View v, MotionEvent ev) {
        boolean handledHere = false;

        final int action = ev.getAction();

        final int evX = (int) ev.getX();
        final int evY = (int) ev.getY();
        int nextImage = -1;            // resource id of the next image to display

        // If we cannot find the imageView, return.
        ImageView imageView = (ImageView) v.findViewById(R.id.image);
        if (imageView == null) return false;

        // When the action is Down, see if we should show the "pressed" image for the default image.
        // We do this when the default image is showing. That condition is detectable by looking at the
        // tag of the view. If it is null or contains the resource number of the default image, display the pressed image.
        Integer tagNum = (Integer) imageView.getTag();
        int currentResource = (tagNum == null) ? R.drawable.p2_ship_default : tagNum.intValue();

        // Now that we know the current resource being displayed we can handle the DOWN and UP events.

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (currentResource == R.drawable.p2_ship_default) {
                    nextImage = R.drawable.p2_ship_pressed;
                    handledHere = true;
       /*
       } else if (currentResource != R.drawable.p2_ship_default) {
         nextImage = R.drawable.p2_ship_default;
         handledHere = true;
       */
                } else handledHere = true;
                break;

            case MotionEvent.ACTION_UP:
                // On the UP, we do the click action.
                // The hidden image (image_areas) has three different hotspots on it.
                // The colors are red, blue, and yellow.
                // Use image_areas to determine which region the user touched.
                int touchColor = getHotspotColor(R.id.image_areas, evX, evY);

                // Compare the touchColor to the expected values. Switch to a different image, depending on what color was touched.
                // Note that we use a Color Tool object to test whether the observed color is close enough to the real color to
                // count as a match. We do this because colors on the screen do not match the map exactly because of scaling and
                // varying pixel density.
                ColorTool ct = new ColorTool();
                int tolerance = 25;
                nextImage = R.drawable.p2_ship_default;
                if (ct.closeMatch(Color.RED, touchColor, tolerance))
                    nextImage = R.drawable.p2_ship_alien;
                else if (ct.closeMatch(Color.BLUE, touchColor, tolerance))
                    nextImage = R.drawable.p2_ship_powered;
                else if (ct.closeMatch(Color.YELLOW, touchColor, tolerance))
                    nextImage = R.drawable.p2_ship_no_star;
                else if (ct.closeMatch(Color.WHITE, touchColor, tolerance))
                    nextImage = R.drawable.p2_ship_default;

                // If the next image is the same as the last image, go back to the default.
                // toast ("Current image: " + currentResource + " next: " + nextImage);
                if (currentResource == nextImage) {
                    nextImage = R.drawable.p2_ship_default;
                }
                handledHere = true;
                break;

            default:
                handledHere = false;
        } // end switch

        if (handledHere) {

            if (nextImage > 0) {
                imageView.setImageResource(nextImage);
                imageView.setTag(nextImage);
            }
        }
        return handledHere;
    }

    /**
     * Resume the activity.
     */

    @Override
    protected void onResume() {
        super.onResume();

//        View v = findViewById(R.id.wglxy_bar);
//        if (v != null) {
//            Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.fade_in);
//            //anim1.setAnimationListener (new StartActivityAfterAnimation (i));
//            v.startAnimation(anim1);
//        }
    }

    /**
     * Handle a click on the Wglxy views at the bottom.
     */

    public void onClickWglxy(View v) {
        Intent viewIntent = new Intent("android.intent.action.VIEW",
                Uri.parse("http://double-star.appspot.com/blahti/ds-download.html"));
        startActivity(viewIntent);

    }


/**
 */
// More methods

    /**
     * Get the color from the hotspot image at point x-y.
     */

    public int getHotspotColor(int hotspotId, int x, int y) {
        ImageView img = (ImageView) findViewById(hotspotId);
        if (img == null) {
            Log.d("ImageAreasActivity", "Hot spot image not found");
            return 0;
        } else {
            img.setDrawingCacheEnabled(true);
            Bitmap hotspots = Bitmap.createBitmap(img.getDrawingCache());
            if (hotspots == null) {
                Log.d("ImageAreasActivity", "Hot spot bitmap was not created");
                return 0;
            } else {
                img.setDrawingCacheEnabled(false);
                return hotspots.getPixel(x, y);
            }
        }
    }

    /**
     * Show a string on the screen via Toast.
     *
     * @param msg String
     * @return void
     */

    public void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    } // end toast

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean handledHere = false;

        final int action = motionEvent.getAction();

        final int evX = (int) motionEvent.getX();
        final int evY = (int) motionEvent.getY();
        int nextImage = -1;            // resource id of the next image to display

        // If we cannot find the imageView, return.
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        if (imageView == null) return false;

        // When the action is Down, see if we should show the "pressed" image for the default image.
        // We do this when the default image is showing. That condition is detectable by looking at the
        // tag of the view. If it is null or contains the resource number of the default image, display the pressed image.
        Integer tagNum = (Integer) imageView.getTag();

        int currentResource = (tagNum == null) ? R.drawable.remote_550 : tagNum;

        // Now that we know the current resource being displayed we can handle the DOWN and UP events.

        switch (action) {
            case MotionEvent.ACTION_DOWN:
//                if (currentResource == R.drawable.remote_left_pressed) {
//                    nextImage = R.drawable.remote_left_pressed;
//                    handledHere = true;
//
//                } else
//                    if (currentResource == R.drawable.remote_right_pressed){
//                    nextImage = R.drawable.remote_550;
//                    handledHere = true;
//                } else
//                    if (currentResource != R.drawable.remote_550) {
//                    nextImage = R.drawable.remote_550;
//                    handledHere = true;
//
//                } else handledHere = true;
//                break;

            case MotionEvent.ACTION_UP:
                // On the UP, we do the click action.
                // The hidden image (image_areas) has three different hotspots on it.
                // The colors are red, blue, and yellow.
                // Use image_areas to determine which region the user touched.
                int touchColor = getHotspotColor(R.id.image_areas, evX, evY);

                // Compare the touchColor to the expected values. Switch to a different image, depending on what color was touched.
                // Note that we use a Color Tool object to test whether the observed color is close enough to the real color to
                // count as a match. We do this because colors on the screen do not match the map exactly because of scaling and
                // varying pixel density.
                ColorTool ct = new ColorTool();
                int tolerance = 55;
                nextImage = R.drawable.remote_550;
                if (ct.closeMatch(Color.RED, touchColor, tolerance))
                    nextImage = R.drawable.remote_left_pressed;
                else if (ct.closeMatch(Color.BLUE, touchColor, tolerance)) {
                    nextImage = R.drawable.remote_right_pressed;
                    try {
                        sendPostRequest();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//                else if (ct.closeMatch(Color.YELLOW, touchColor, tolerance))
//                    nextImage = R.drawable.p2_ship_no_star;
//                else if (ct.closeMatch(Color.WHITE, touchColor, tolerance)) {
//                }

                // If the next image is the same as the last image, go back to the default.
                // toast ("Current image: " + currentResource + " next: " + nextImage);
                if (currentResource == nextImage) {
                    nextImage = R.drawable.remote_550;
                }
                handledHere = true;
                break;

            default:
        } // end switch

        if (handledHere) {

            if (nextImage > 0) {
                imageView.setImageResource(nextImage);
                imageView.setTag(nextImage);
            }
        }
        return handledHere;
    }

    public void sendPostRequest()
            throws IOException {
        String BASE_URL = "https://shelly-1-eu.shelly.cloud/device/relay/control/";

        RequestBody formBody = new FormBody.Builder()
                .add("auth_key", getString(R.string.AUTH_KEY))
                .add("turn", "on")
                .add("channel", "0")
                .add("id", getString(R.string.DEVICE_ID))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("ERROR___", e.getMessage());
//                gate.setOpen(Gate.State.OPEN);
//                Log.e("STATES :", gate.toString());
                call.cancel();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                Log.e("OK___MSG", response.message());
//                Log.e("OK___BODY", response.body().string());

//                gate.setOpen(Gate.State.OPEN);
//                Log.e("STATES :", gate.getState().name());
//                gate.setClose(Gate.State.CLOSE);
//                Log.e("STATES :", gate.getState().name());

                try {
                    String inString = response.body().string();
                    JSONObject responseJson = new JSONObject(inString);
                    boolean isOk = (boolean) responseJson.getBoolean("isok");
                    if (isOk) {

                        backgroundThreadShortToast(getApplicationContext(), "OK");
                    } else {
                        backgroundThreadShortToast(getApplicationContext(), "ERROR");
                    }
                    Log.e("JSON", "is response ok: "+isOk);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                response.close();
            }
        });
    }
    // public static void - original method
    public void backgroundThreadShortToast(final Context context,
                                                  final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    View toastView = View.inflate(context, R.layout.custom_toast, null);
                    Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
                    toast.setView(toastView);
                    toast.setGravity(Gravity.TOP|Gravity.CENTER,0,150);
                    TextView toastTxt = (TextView) toastView.findViewById(R.id.toast_txt);
                    toastTxt.setText(msg);
                    toast.show();
                }
            });

        }
    }

    public void backgroundButtonText(final Context context, final String txt) {
        if (context != null && txt != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    btnOpen.setText(txt);
                }
            });

        }
    }
} // end class

