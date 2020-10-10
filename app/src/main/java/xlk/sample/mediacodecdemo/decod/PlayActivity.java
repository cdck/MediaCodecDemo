package xlk.sample.mediacodecdemo.decod;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;
import xlk.sample.mediacodecdemo.R;

/**
 * @author xlk
 */
public class PlayActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private String mVideoFilePath;
    private VideoDecodeThread videoDecodeThread;
    private SoundDecodeThread soundDecodeThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        mVideoFilePath = getIntent().getStringExtra("video_file");
        initView();
    }

    private void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (videoDecodeThread == null) {
            videoDecodeThread = new VideoDecodeThread(holder.getSurface(), mVideoFilePath);
            videoDecodeThread.start();
        }
        if (soundDecodeThread == null) {
            soundDecodeThread = new SoundDecodeThread(mVideoFilePath);
            soundDecodeThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (videoDecodeThread != null) {
            videoDecodeThread.quit();
        }
        if (soundDecodeThread != null) {
            soundDecodeThread.quit();
        }
    }
}
