package com.faceunity.fulivedemo;

import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.faceunity.FURenderer;
import com.faceunity.fulivedemo.entity.BeautyParameterModel;
import com.faceunity.fulivedemo.ui.BeautyControlView;
import com.faceunity.utils.Constant;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import tech.shutu.jni.YuvUtils;

/**
 * 美颜界面
 * Created by tujh on 2018/1/31.
 */

public class FUBeautyActivity extends FUBaseUIActivity
        implements FURenderer.OnFUDebugListener,
        FURenderer.OnTrackingStatusChangedListener {
    public final static String TAG = FUBeautyActivity.class.getSimpleName();

    private byte[] mFuNV21Byte;
    private byte[] mFuNV21Byte2;

    private BeautyControlView mBeautyControlView;
    private FURenderer mFURenderer;

    protected TXLivePushConfig mLivePushConfig;
    protected TXLivePusher mLivePusher;
    protected boolean pushStreaming;

    @Override
    protected void onCreate() {

        mHeightCheckBox.setVisibility(View.VISIBLE);
        mHeightImg.setVisibility(View.VISIBLE);
        mHeightCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mHeightImg.setImageResource(isChecked ? R.drawable.performance_checked : R.drawable.performance_normal);
                mBeautyControlView.setHeightPerformance(isChecked);
            }
        });
        //初始化FU相关 authpack 为证书文件
        mFURenderer = new FURenderer
                .Builder(this)
                .maxFaces(4)
                .inputTextureType(FURenderer.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE)
                .createEGLContext(false)
                .needReadBackImage(false)
                .defaultEffect(null)
                .setOnFUDebugListener(this)
                .setOnTrackingStatusChangedListener(this)
                .build();

        mBottomViewStub.setLayoutResource(R.layout.layout_fu_beauty);
        mBottomViewStub.inflate();

        mBeautyControlView = (BeautyControlView) findViewById(R.id.fu_beauty_control);
        mBeautyControlView.setOnFUControlListener(mFURenderer);
        mBeautyControlView.setOnBottomAnimatorChangeListener(new BeautyControlView.OnBottomAnimatorChangeListener() {
            @Override
            public void onBottomAnimatorChangeListener(float showRate) {
                mTakePicBtn.setDrawWidth((int) (getResources().getDimensionPixelSize(R.dimen.x166) * (1 - showRate * 0.265)));
            }
        });
        mBeautyControlView.setOnDescriptionShowListener(new BeautyControlView.OnDescriptionShowListener() {
            @Override
            public void onDescriptionShowListener(String str) {
                showDescription(str, 1000);
            }
        });
        mGLSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeautyControlView.hideBottomLayoutAnimator();
            }
        });

        mInputTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.fu_base_input_type_double:
                        isDoubleInputType = true;
                        break;
                    case R.id.fu_base_input_type_single:
                        isDoubleInputType = false;
                        break;
                }
                mFURenderer.changeInputType();
            }
        });

        mHeightCheckBox.setChecked(BeautyParameterModel.isHeightPerformance);
        mSelectDataBtn.setVisibility(View.VISIBLE);
        mSelectDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txLivePush();
//                Intent intent = new Intent(FUBeautyActivity.this, SelectDataActivity.class);
//                intent.putExtra("SelectData", TAG);
//                startActivity(intent);
            }
        });

        initLivePush();
    }

    private void txLivePush() {
        String rtmpUrl = "rtmp://livepush.17kuxiu.com/live/30310_77777002870?bizid=30310&txSecret=938d2b0f730f74b83c91539efe645768&txTime=5C4D24A1";
        if (pushStreaming) {
            Toast.makeText(FUBeautyActivity.this, "停止推流", Toast.LENGTH_LONG).show();
            pushStreaming = false;
            mLivePusher.stopPusher();
        } else {
            Toast.makeText(FUBeautyActivity.this, "开始推流", Toast.LENGTH_LONG).show();
            pushStreaming = true;
            mLivePusher.startPusher(rtmpUrl);
        }
    }

    private void initLivePush() {
        YuvUtils.allocateMemo(1280 * 720 * 3 / 2, 0,
                1280 * 720 * 3 / 2);

        mLivePusher = new TXLivePusher(this);
        mLivePushConfig = new TXLivePushConfig();
        mLivePushConfig.enableScreenCaptureAutoRotate(true);
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
        mLivePushConfig.setPauseImg(BitmapFactory.decodeResource(getResources(), R.mipmap.pause_publish));
        //设置推流暂停时,后台播放暂停图片的方式  暂停时间，暂停帧数
        mLivePushConfig.setPauseImg(300, 5);

        int customModeType = 0;
        customModeType |= TXLiveConstants.CUSTOM_MODE_VIDEO_CAPTURE;
        //目前竖屏只支持推 360x640、540x960、720x1280 这3种分辨率。
//        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_640_360);
//        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_960_540);
//        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_1280_720);

        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
//        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);
//        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280);
        //开启回声消除。连麦时必须开启，非连麦正常推流时不要开启。默认关闭
        mLivePushConfig.enableAEC(false);
        mLivePushConfig.setAutoAdjustBitrate(false);
        mLivePushConfig.setVideoBitrate(1200);
        mLivePushConfig.setVideoEncodeGop(3);
        mLivePushConfig.setVideoFPS(15);
        mLivePushConfig.setCustomModeType(customModeType);
        //必须使用硬编
        mLivePushConfig.setHardwareAcceleration(TXLiveConstants.ENCODE_VIDEO_SOFTWARE);

        mLivePusher.setConfig(mLivePushConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        txLivePush();
        if (mBeautyControlView != null){
            mBeautyControlView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pushStreaming = false;
        mLivePusher.stopCameraPreview(false);
    }

    @Override
    protected void onSensorChanged(int rotation) {
        mFURenderer.setTrackOrientation(rotation);
    }

    @Override
    public void onCameraChange(int currentCameraType, int cameraOrientation) {
        mFURenderer.onCameraChange(currentCameraType, cameraOrientation);
    }

    @Override
    public void onFpsChange(final double fps, final double renderTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDebugText.setText(String.format("resolution:\n\t%dX%d\nfps:%d\nrender time:\n\t\t\t\t\t%dms", mCameraRenderer.getCameraWidth(), mCameraRenderer.getCameraHeight(), (int) fps, (int) renderTime));
            }
        });
    }

    private ExecutorService executorService;

    @Override
    public void onTrackingStatusChanged(final int status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIsTrackingText.setVisibility(status > 0 ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mFURenderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public int onDrawFrame(byte[] cameraNV21Byte, int cameraTextureId, final int cameraWidth,final int cameraHeight, float[] mtx, long timeStamp) {
        final int fuTextureId;
        if (mFuNV21Byte == null) {
            mFuNV21Byte = new byte[cameraNV21Byte.length];
        }
        if (isDoubleInputType) {
            fuTextureId = mFURenderer.onDrawFrame(cameraNV21Byte, cameraTextureId, cameraWidth, cameraHeight, mFuNV21Byte, cameraWidth, cameraHeight);
//            fuTextureId = mFURenderer.onDrawFrame(cameraNV21Byte, cameraTextureId, cameraWidth, cameraHeight);
        } else {
            //System.arraycopy(cameraNV21Byte, 0, mFuNV21Byte, 0, cameraNV21Byte.length);
            fuTextureId = mFURenderer.onDrawFrame(cameraNV21Byte, cameraWidth, cameraHeight, mFuNV21Byte, cameraWidth, cameraHeight);
//            fuTextureId = mFURenderer.onDrawFrame(mFuNV21Byte, cameraWidth, cameraHeight);
        }

        if (pushStreaming) {
            if (executorService == null) executorService = Executors.newCachedThreadPool();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    long t = System.currentTimeMillis();
                    if (mFuNV21Byte2 == null) {
                        mFuNV21Byte2 = new byte[mFuNV21Byte.length];
                    }
                    byte[] buffer = nv21ToI420(mFuNV21Byte, cameraWidth, cameraHeight);
                    if(mFURenderer.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT){
                        YuvUtils.scaleAndRotateYV12ToI420(buffer, mFuNV21Byte2,
                                1280, 720, 270,  1280, 720);
                    } else {
                        YuvUtils.scaleAndRotateYV12ToI420(buffer, mFuNV21Byte2,
                                1280, 720, 90,  1280, 720);
                    }
                    Log.e("Time end", "" + (System.currentTimeMillis() - t));
                    int result = mLivePusher.sendCustomVideoData(mFuNV21Byte2, TXLivePusher.YUV_420P, cameraWidth, cameraHeight);
//                  int result = mLivePusher.sendCustomVideoTexture(fuTextureId, cameraHeight, cameraWidth);
                }
            });
        }

        sendRecordingData(fuTextureId, mtx, timeStamp / Constant.NANO_IN_ONE_MILLI_SECOND);
        checkPic(fuTextureId, mtx, cameraHeight, cameraWidth);
        return fuTextureId;
    }

    /**
     * nv21转I420
     *
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static byte[] nv21ToI420(byte[] data, int width, int height) {
        byte[] ret = new byte[data.length];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);
        ByteBuffer bufferU = ByteBuffer.wrap(ret, total, total / 4);
        ByteBuffer bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4);

        bufferY.put(data, 0, total);
        for (int i = total; i < data.length; i += 2) {
            bufferV.put(data[i]);
            bufferU.put(data[i + 1]);
        }
        return ret;
    }

    @Override
    public void onSurfaceDestroy() {
        //通知FU销毁
        mFURenderer.onSurfaceDestroyed();
        YuvUtils.releaseMemo();
    }
}
