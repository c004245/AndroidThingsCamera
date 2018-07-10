package hyunwook.co.kr.raspberrypicamerakotlin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.TextureView
import hyunwook.co.kr.raspberrypicamerakotlin.camera.CameraJava
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var mBackgroundHandler : Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Check Camera Permission.
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //Reboot Raspberry pi, allow Permission android things.
            Log.d(TAG, "No permission...")
            return
        }

        //기존 블랙아이와 동일하기 SurfaceTexture 이용. (광각 + 카메라 반전)
        texture.surfaceTextureListener = textureListener


 /*       capture.setOnClickListener(View.OnClickListener {
            view -> capture();
            true
        })*/

    }

    /*fun capture() {
        var buffer : ByteBuffer = image
    }*/



    //SurfaceTexture interface.
    val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
            //텍스쳐 카메라 버퍼가 바뀔 때..
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
            //텍스쳐 뷰가 업데이트 될 때
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
            //텍스쳐 뷰가 Destroyed 가 될 때
            return false
        }

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            //카메라 오픈 텍스쳐뷰가 이용가능한상태가 되면 (초기)
            CameraJava.Instance.openCamera(this@MainActivity, texture, mBackgroundHandler)
        }
    }
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
