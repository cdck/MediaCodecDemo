package xlk.sample.mediacodecdemo.record;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import xlk.sample.mediacodecdemo.R;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.io.File;

/**
 * @author Administrator
 */
public class RecordActivity extends AppCompatActivity {
    final int REQUEST_MEDIA_PROJECTION = 1;
    private MediaProjectionManager mpm;
    private MediaProjection mediaProjection;
    private DisplayMetrics dm;
    private WindowManager window;
    private ScreenRecorder recorder;
    private String saveFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        dm = new DisplayMetrics();
        window = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getMetrics(dm);
        //目录：/storage/emulated/0/Android/data/xlk.sample.mediacodecdemo/files/录屏文件/mediaCodecDemo.mp4
        saveFilePath = getExternalFilesDir("录屏文件").getAbsolutePath() + "/mediaCodecDemo.mp4";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK) {
            mediaProjection = mpm.getMediaProjection(resultCode, data);
            recorder = new ScreenRecorder(dm.widthPixels, dm.heightPixels,
                    dm.densityDpi, mediaProjection, saveFilePath);
            recorder.start();
        }
    }

    public void startScreen(View view) {
        if (recorder == null) {
            mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }
    }

    public void stopScreen(View view) {
        if (recorder != null) {
            recorder.quit();
            recorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recorder != null) {
            recorder.quit();
            recorder = null;
        }
    }
}
