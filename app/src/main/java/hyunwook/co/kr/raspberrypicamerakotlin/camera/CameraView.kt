package hyunwook.co.kr.camerakotlinproject.Camera

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast

/**
 * Created by hyunwook on 2018-04-19.
 * 카메라 관련 처리 코틀린
 */
class CameraView {
    private var context: Context? = null //nullable ?
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private var textureView: TextureView? = null

    private var captureRequest: CaptureRequest? = null //캡쳐
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null

    private var mBackgroundHandler : Handler? = null //

    private var mShutterCallback : ShutterCallback? = null
    companion object {
        private val TAG = CameraView::class.java.simpleName
        val Instance = CameraView()
    }

    //카메라 오픈
    fun openCamera(context: Context, textureView: TextureView, backgroundHandler: Handler?) {
        this.context = context
        this.textureView = textureView
        this.mBackgroundHandler = backgroundHandler

        //카메라 서비스
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        Log.d(TAG, "camera open..")

        try {
            this.cameraId = manager.cameraIdList[0] //카메라 아이디
            val characteristics = manager.getCameraCharacteristics(cameraId)
            Log.d(TAG, "characteristics -->" + characteristics)

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!! //nullable 이면 오류 발생 : !!

            //영상사이즈
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[2]

            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Camera access exception", e)
        }
        Log.d(TAG,"OPENCAMERA X")
    }

    //카메라 닫기
    fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null
    }

 /** 카메라 디바이스 상태변화를 얻는다
  * 비동기로 카메라 디바이스의 접속을 검출한다
  * CameraDevice.StateCallback 인터페이스
  */


    private val stateCallback = object : CameraDevice.StateCallback() {
        // onOpened 카메라 디바이스와 접속이 완료
        override fun onOpened(p0: CameraDevice?) {
            Log.d(TAG, "onOpened")
            cameraDevice = p0
            createCameraPreview()
        }
        // onDisconnected	카메라 디바이스와 접속이 끊어졌다
        override fun onDisconnected(p0: CameraDevice?) {
            cameraDevice?.close()
        }
        //    onError	회복 불능 상태
        override fun onError(p0: CameraDevice?, p1: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    //카메라 화면 생성
    private fun createCameraPreview() {
        try {
            val texture = textureView?.surfaceTexture!! //null 허용안함

            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)

            val surface = Surface(texture)

            //카메라 미리보기에 창에 적합한 요청
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)

            //CaptureRequestBuild 취득
            cameraDevice?.createCaptureSession(arrayListOf(surface), object : CameraCaptureSession.StateCallback() {

                // Preview 준비 실패
                override fun onConfigureFailed(p0: CameraCaptureSession?) {
                    Toast.makeText(context!!, "Configuration change", Toast.LENGTH_SHORT).show()
                }
                // Preview 준비가 완료
                override fun onConfigured(p0: CameraCaptureSession?) {
                    if (cameraDevice == null) {
                        return
                    }

                    cameraCaptureSessions = p0
                    updatePreview()
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Camera access exception", e)
        }
    }

    //화면 준비완료 판단
    private fun updatePreview() {
        if (cameraDevice == null) {
            Log.d(TAG, "updatePreview error, return")
        }

        //CONTROL_MODE = auto-exposure, auto-while-balance, auto-focus  3A 모드 자동설정
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions?.setRepeatingRequest(captureRequestBuilder?.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "updatePreview error.....");
        }
    }

    private var mOnImageAvailableListener : PictureDoneCallback = PictureDoneCallback()

    private class PictureDoneCallback: ImageReader.OnImageAvailableListener {
        var mDelegate: PictureCallback? = null

        override fun onImageAvailable(reader: ImageReader) {
            if (mDelegate != null) {
                mDelegate!!.onPictureTaken(reader.acquireNextImage())
            }

        }


    }

    public fun takePicture(shutter: ShutterCallback, picCallback: PictureCallback) {
        mShutterCallback = shutter
        mOnImageAvailableListener.mDelegate = picCallback
//        lockFocus()

    }

    public interface ShutterCallback {
        fun onShutter()
    }

    public interface PictureCallback {
        fun onPictureTaken(image: Image)
    }
}