package hyunwook.co.kr.raspberrypicamerakotlin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import hyunwook.co.kr.raspberrypicamerakotlin.camera.CameraJava;
import hyunwook.co.kr.raspberrypicamerakotlin.capture.CaptureView;
import hyunwook.co.kr.raspberrypicamerakotlin.capture.CaptureViewer;

/**
 * Created by hyunwook on 2018-07-10.
 */

public class MainActivity extends AppCompatActivity {

    private Handler mBackgroundHandler;
    TextureView textureView;

    static final String TAG = MainActivity.class.getSimpleName();

    Button captureButton;

    Bitmap viewByte;
    private CaptureViewer captureViewer;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);


        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CaptureView captureView = (CaptureView) findViewById(R.id.captureView);
        captureViewer = captureView;



        textureView = (TextureView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(this.textureListener);

        captureButton = (Button) findViewById(R.id.capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureButton.setEnabled(false);
                Log.d(TAG, "click..");
                CameraJava.Instance.takePicture(shutterCallback, pictureCallback);

            }
        });
    }

    final CameraJava.ShutterCallback shutterCallback = new CameraJava.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.d(TAG, "Shutter Callback for Camera2");
        }
    };

    final CameraJava.PictureCallback pictureCallback = new CameraJava.PictureCallback() {
        @Override
        public void onPictureTaken(Image image) {
            Log.d(TAG, "Taken picture is here.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    captureButton.setEnabled(true);
                }
            });

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);

            Bitmap picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            FileOutputStream out = null;

            Log.d(TAG, "bytes -->" + bytes);
            Log.d(TAG, "picture -->" + picture);
            viewByte = Bitmap.createScaledBitmap(picture, 200, 200, true);

            try {
                out = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/test.png"));
                picture.compress(Bitmap.CompressFormat.JPEG, 95, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    };

    @NotNull
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture p0, int p1, int p2) {

        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture p0) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture p0) {
            return false;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture p0, int p1, int p2) {
            CameraJava.Instance.openCamera(getApplicationContext(), textureView, mBackgroundHandler);
        }
    };
}
