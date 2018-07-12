package hyunwook.co.kr.raspberrypicamerakotlin.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kotlin.collections.CollectionsKt;

/**
 * Created by hyunwook on 2018-07-10.
 */

public class CameraJava {

    private Context context;
    private String cameraId;
    private CameraDevice cameraDevice;
    private Size imageDimension;

    private ImageReader imageReader;
    private TextureView textureView;

    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;

    private Handler mBackgroundHandler;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //private ShutterCallback mShutterCallback;
    StreamConfigurationMap map;
    static final String TAG = CameraJava.class.getSimpleName();
    public static final CameraJava Instance = new CameraJava();

    public void openCamera(Context context, TextureView textureView, Handler backgroundHandler) {
        this.context = context;
        this.textureView = textureView;
        this.mBackgroundHandler = backgroundHandler;

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            this.cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            imageDimension = map.getOutputSizes(SurfaceTexture.class)[2];

            Log.d(TAG, "imageDimension -->" + imageDimension.getWidth() + "--"+ imageDimension.getHeight());

            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //카메라 닫기
    public void closeCamera() {
        cameraDevice.close();
        cameraDevice = null;

        imageReader.close();
        imageReader = null;
    }

/*    private PictureDoneCallback mOnImageAvailableListener = new PictureDoneCallback();

    public void takePicture(ShutterCallback shutter, PictureCallback picCallback) {
        Log.d(TAG, "takePicture.,..");
        mShutterCallback = shutter;

        mOnImageAvailableListener.mDelegate = picCallback;
        //lockFocus();
    }*/

    /**
     * 카메라 디바이스 상태변화 얻기.
     * 비동기로 카메라 디바이스 접속 검출
     * CameraDevice.StateCallback Interface.
     */

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice p0) {
            cameraDevice = p0;
            createCameraPreview();
        }


        @Override
        public void onDisconnected(@NonNull CameraDevice p0) {
            cameraDevice.close();

        }

        @Override
        public void onError(@NonNull CameraDevice p0, int p1) {
            cameraDevice.close();
            cameraDevice = null;

        }
    };

    //카메라 화면 생성
    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();

            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);

            //카메라 미리보기에 요청
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            //CaptureRequestBuild 취득
            cameraDevice.createCaptureSession(CollectionsKt.arrayListOf(surface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigureFailed(CameraCaptureSession p0) {
                    Toast.makeText(context, "Configuration change", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onConfigured(CameraCaptureSession p0) {
                    if (cameraDevice == null) {
                        return;
                    }

                    cameraCaptureSession = p0;
                    updatePreview();
                }

            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //화면 준비완료
    private void updatePreview() {
        if (cameraDevice == null) {
            Log.d(TAG, "updatePreview error..");
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture() {
        if (null == cameraDevice) {
            return;
        }

        try {
            Size[] jpegSize = null;

            if (map != null) {
                jpegSize = map.getOutputSizes(ImageFormat.JPEG);
            }

            int width = 320;
            int height = 240;

            if (jpegSize != null && 0 < jpegSize.length) {
                width = jpegSize[0].getWidth();
                height = jpegSize[0].getHeight();

            }
//160 //120

            Log.d(TAG, "width --" + width + "--" + height);
            ImageReader reader = ImageReader.newInstance(160, 120, ImageFormat.JPEG, 1);
            final List<Surface> outputSurfaces = new ArrayList<Surface>(2);

            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(0));

            Log.d(TAG, "reader -->" + reader);
            final File file = new File(Environment.getExternalStorageDirectory() + "pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = nu    ll;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        Log.d(TAG, "bytes -->" + bytes);
                        save(bytes);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                            reader.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }


            };

            HandlerThread thread = new HandlerThread("CameraPicture");
            thread.start();

            final Handler backgroundHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);

            Log.d(TAG, "backgroundHandler -->" + backgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    Toast.makeText(context, "Saved: " + file, Toast.LENGTH_LONG).show();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG, "camera createCaptureSession ->" + outputSurfaces);
                    try {
                        session.capture(captureBuilder.build(), captureListener, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d(TAG, "Fail configure--" + session);
                }
            }, backgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Size smallerPreviewSize = chooseVideoSize(map.getOutputSizes(SurfaceTexture.class));

    protected Size chooseVideoSize(Size[] choices) {
        List<Size> smallEnough = new ArrayList<>();

        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                smallEnough.add(size);
            }
        }
        if (smallEnough.size() > 0) {
            return Collections.max(smallEnough, new CompareSizeByArea());
        }

        return choices[choices.length - 1];
    }

    public class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }


    }
}
