package com.google.ar.core.examples.java.helloar;

import android.Manifest;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Frame.TrackingState;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloudHitResult;
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.helloar.rendering.PlaneAttachment;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using
 * the ARCore API. The application will display any detected planes and will allow the user to
 * tap on a plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = HelloArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private Config mDefaultConfig;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private GestureDetector mGestureDetector;
    private Snackbar mLoadingMessageSnackbar = null;

    private ObjectRenderer mVirtualObject = new ObjectRenderer();
    private ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();
    private PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private PointCloudRenderer mPointCloud = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private ArrayList<PlaneAttachment> mTouches = new ArrayList<>();
    private ArrayList<PointAttachment> mTouchePoints = new ArrayList<>();
    Button btnShowLocation;
    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    // GPSTracker class
    GPSTracker gps;
    double latitude;
    double longitude;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);

        mSession = new Session(/*context=*/this);

        MementoClass dimension = load();
        if (dimension != null) {
          double x = dimension.getX();
            Log.v("....x",""+x);
            double y = dimension.getY();
            Log.v("....y",""+y);

            double z = dimension.getZ();
            Log.v("....z",""+z);

            double lat = dimension.getLattitude();
            Log.v("....lat",""+lat);

            double lon = dimension.getLongitude();
            Log.v("....lon",""+lon);

//            mTouchePoints.add(new PointAttachment(
//                    ((PointCloudHitResult) hit).getPointCloud(),
//                    mSession.addAnchor(hit.getHitPose())));
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnShowLocation = (Button) findViewById(R.id.button);
        textView=(TextView)findViewById(R.id.distance);
        btnShowLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // create class object
                gps = new GPSTracker(HelloArActivity.this);

                // check if GPS enabled
                if(gps.canGetLocation()){

                     latitude = gps.getLatitude();
                     longitude = gps.getLongitude();

                    // \n is for new line
                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
                            + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

                    Log.v("latitude",""+latitude);
                    Log.v("longitude",""+longitude);

                }else{
                    // can't get location
                    // GPS or Network is not enabled
                    // Ask user to enable GPS/network in settings
                    gps.showSettingsAlert();
                }

            }
        });

        // Create default config, check is supported, create session from that config.
        mDefaultConfig = Config.createDefaultConfig();
        if (!mSession.isSupported(mDefaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();

                return mGestureDetector.onTouchEvent(event);
            }
        });

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
//            showLoadingMessage();
            // Note that order matters - see the note in onPause(), the reverse applies here.
            mSession.resume(mDefaultConfig);
            mSurfaceView.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mSurfaceView.onPause();
        mSession.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this);
        mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

        // Prepare the other rendering objects.
        try {
//            mVirtualObject.createOnGlThread(/*context=*/this, "andy.obj", "andy.png");
            mVirtualObject.createOnGlThread(/*context=*/this, "sphere.obj", "sphere.png");
//            mVirtualObject.createOnGlThread(/*context=*/this, "square.obj", "square.png");
//            mVirtualObject.createOnGlThread(/*context=*/this, "triangle.obj", "triangle.png");
            // giving android robot color as green
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualObjectShadow.createOnGlThread(/*context=*/this,
                    "andy_shadow.obj", "andy_shadow.png");
            mVirtualObjectShadow.setBlendMode(BlendMode.Shadow);
            mVirtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
       /* try {
            mPlaneRenderer.createOnGlThread(*//*context=*//*this, "grid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }*/
        mPointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

       /* try {
            mPlaneRenderer.createOnGlThread(*//*context=*//*this, "grid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }*/
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mSession.setDisplayGeometry(width, height);
    }


    @Override
    public void onDrawFrame(GL10 gl) {

        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && frame.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon.
                  /*  if (hit instanceof PlaneHitResult && ((PlaneHitResult) hit).isHitInPolygon()) {

                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (mTouches.size() >= 2) {
                            mSession.removeAnchors(Arrays.asList(mTouches.get(0).getAnchor()));
                            mTouches.remove(0);
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor will be used in PlaneAttachment to place the 3d model
                        // in the correct position relative both to the world and to the plane.
                        mTouches.add(new PlaneAttachment(
                                ((PlaneHitResult) hit).getPlane(),
                                mSession.addAnchor(hit.getHitPose())));

                        String x = String.valueOf(((PlaneHitResult) hit).getPlane().getExtentX());
                        String z = String.valueOf(((PlaneHitResult) hit).getPlane().getExtentZ());
                        MementoClass memento = new MementoClass(x, z);
                        save(memento);

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;

                    }*/
                    // Check if any point was hit

                    if (hit instanceof PointCloudHitResult) {

                        // Get camera matrix and draw.
                       /* if (mTouchePoints.size() >= 2) {
                            mSession.removeAnchors(Arrays.asList(mTouchePoints.get(0).getAnchor()));
                            mTouchePoints.remove(0);
                        }*/

                        mTouchePoints.add(new PointAttachment(
                                ((PointCloudHitResult) hit).getPointCloud(),
                                mSession.addAnchor(hit.getHitPose())));

                        double disFrmCamera = hit.getDistance();
                        Log.v("bbb",""+disFrmCamera);


                        //Position, decimal degrees
                        double lat =latitude;
                        double lon = longitude;

                        //Earth’s radius, sphere
                        double R=6378137;

                        //offsets in meters
                        double dn = disFrmCamera;
                        double de = disFrmCamera;

                        //Coordinate offsets in radians
                        double dLat = dn/R;
                        double dLon = de/(R*Math.cos(Math.PI*lat/180));

                        //OffsetPosition, decimal degrees
                        double latO = lat + dLat * 180/Math.PI;
                        double lonO = lon + dLon * 180/Math.PI;
                        Log.v("latO",""+latO);
                        Log.v("lonO",""+lonO);


//                        String x = String.valueOf(((PlaneHitResult) hit).getPlane().getExtentX());
//                        String z = String.valueOf(((PlaneHitResult) hit).getPlane().getExtentZ());

                        double x = mTouchePoints.get(0).getAnchor().getPose().tx();
                        Log.v("----x",""+x);

                        double y = mTouchePoints.get(0).getAnchor().getPose().ty();
                        Log.v("----y",""+y);

                        double z = mTouchePoints.get(0).getAnchor().getPose().tz();
                        Log.v("----z",""+z);

                        double objLat =latO;
                        Log.v("----objLat",""+objLat);

                        double objLon =lonO;
                        Log.v("----objLon",""+objLon);

                        List<Coordinates> coordinates = new ArrayList<>();
// Example of valid ship coordinates off the coast of California :-)
                        coordinates.add(new Coordinates(36.385913, -127.441406));

                        MementoClass memento = new MementoClass(x,y,z,objLat,objLon);
                        save(memento);

//                        new_latitude  = latitude  + (dy / r_earth) * (180 / pi);
//                        new_longitude = longitude + (dx / r_earth) * (180 / pi) / cos(latitude * pi/180);


                      /*  double x1 = mTouchePoints.get(mTouchePoints.size() - 1).getAnchor().getPose().tx();
                        double y1 = mTouchePoints.get(mTouchePoints.size() - 1).getAnchor().getPose().ty();
                        double z1 = mTouchePoints.get(mTouchePoints.size() - 1).getAnchor().getPose().tz();
                        double a1 = x1;
                        double b1 = y1;
                        double c1 = z1;
                        double x2 = mTouchePoints.get(mTouchePoints.size() - 2).getAnchor().getPose().tx();
                        double y2 = mTouchePoints.get(mTouchePoints.size() - 2).getAnchor().getPose().ty();
                        double z2 = mTouchePoints.get(mTouchePoints.size() - 2).getAnchor().getPose().tz();
                        double distance = Math.sqrt((x2 - a1)*(x2 - a1) + (y2 - b1) *(y2 - b1) + (z2 - c1) * (z2 - c1));
//                        tv.setText((int) distance);
                        Log.v("aaa",""+distance);
                        textView.setText(""+distance);*/


                    }
                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (frame.getTrackingState() == TrackingState.NOT_TRACKING) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            mSession.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            frame.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            mPointCloud.update(frame.getPointCloud());
            mPointCloud.draw(frame.getPointCloudPose(), viewmtx, projmtx);

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mLoadingMessageSnackbar != null) {
                for (Plane plane : mSession.getAllPlanes()) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            plane.getTrackingState() == Plane.TrackingState.TRACKING) {
//                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes.
//            mPlaneRenderer.drawPlanes(mSession.getAllPlanes(), frame.getPose(), projmtx);

            // Visualize anchors created by touch.
            // size of the virtual object is depending on scalefactor
            float scaleFactor = 1.0f;
            for (PlaneAttachment planeAttachment : mTouches) {
                if (!planeAttachment.isTracking()) {
                    continue;
                }
                // Get the current combined pose of an Anchor and Plane in world space. The Anchor
                // and Plane poses are updated during calls to session.update() as ARCore refines
                // its estimate of the world.
                planeAttachment.getPose().toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.
                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
                mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);

            }


            float scaleFactor_ = 0.01f;// for sphere
//            float scaleFactor_ = 0.001f;// for pyramid
//            float scaleFactor_ = 0.01f;// for triangle
//            float scaleFactor_ = 0.1f;// for square and triangle
            for (PointAttachment pointAttachment : mTouchePoints) {
//                if (!pointAttachment.equals(pointAttachment)) {
//                    continue;
//                }
                // Get the current combined pose of an Anchor and Plane in world space. The Anchor
                // and Plane poses are updated during calls to session.update() as ARCore refines
                // its estimate of the world.
                pointAttachment.getPose().toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.
                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor_);
                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor_);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
                mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);

            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

  /*  private void showLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingMessageSnackbar = Snackbar.make(
                        HelloArActivity.this.findViewById(android.R.id.content),
                        "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE);
                mLoadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
                mLoadingMessageSnackbar.show();
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingMessageSnackbar.dismiss();
                mLoadingMessageSnackbar = null;
            }
        });
    }*/

    public void save(MementoClass dimension) {
        String fileName = "data.txt";

        try {
            File root = new File(getApplicationContext().getApplicationInfo().dataDir);

            File writefile = new File(root, fileName);
            FileWriter writer = new FileWriter(writefile, true);
            writer.append(dimension.getX() + "\n" + dimension.getY() +
                    "\n"+ dimension.getZ() + "\n"+ dimension.getLattitude() + "\n"+ dimension.getLongitude()+"\n");
            writer.flush();
            writer.close();
            Toast.makeText(this, "Data has been written to Storage File", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();

        }

        Toast.makeText(getApplicationContext(), "saved successfuly", Toast.LENGTH_SHORT).show();
    }

    public MementoClass load() {
        //1. open the file
        File root = new File(getApplicationContext().getApplicationInfo().dataDir);
        File reader = new File(root, "data.txt");

        //2. reads the file content
        StringBuilder text = new StringBuilder();
        String x = null;
        String y = null;
        String z = null;
        String lat = null;
        String lon = null;
        double x1 = 0;
        double y1 = 0;
        double z1 = 0;
        double lat1 = 0;
        double lon1 = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(reader));
            String line;
            int lineno = 1;
            while ((line = br.readLine()) != null) {

                if (lineno == 1) {
                    x = new StringBuilder(line).toString();
                     x1 = Double.parseDouble(x);
                } else if (lineno == 2) {
                    y = new StringBuilder(line).toString();
                     y1 = Double.parseDouble(y);

                } else if (lineno == 3) {
                    z = new StringBuilder(line).toString();
                     z1 = Double.parseDouble(z);

                } else if (lineno == 4) {
                    lat = new StringBuilder(line).toString();
                     lat1 = Double.parseDouble(lat);

                } else if(lineno==5) {
                    lon = new StringBuilder(line).toString();
                    lon1 = Double.parseDouble(lon);}
                else {

                }
                lineno++;
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

        //3. creates memento
        MementoClass memento = new MementoClass(x1, y1, z1, lat1, lon1);

        //4. retruns memento
        return memento;

    }
}

