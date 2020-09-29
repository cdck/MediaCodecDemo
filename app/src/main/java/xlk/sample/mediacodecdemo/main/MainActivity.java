package xlk.sample.mediacodecdemo.main;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import xlk.sample.mediacodecdemo.R;
import xlk.sample.mediacodecdemo.decod.PlayActivity;
import xlk.sample.mediacodecdemo.record.RecordActivity;
import xlk.sample.mediacodecdemo.util.ScreenUtils;
import xlk.sample.mediacodecdemo.util.UriUtil;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity-->";
    private TextView tv_show;
    private final int CHOOSE_FILE_REQUEST_CODE = 1;
    private final String PREFIX_VIDEO = "video/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_show = findViewById(R.id.tv_show);
        XXPermissions.with(this)
                .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean all) {

                    }

                    @Override
                    public void noPermission(List<String> denied, boolean never) {

                    }
                });
    }


    public void jump2Record(View view) {
        startActivity(new Intent(this, RecordActivity.class));
    }

    /**
     * 获取虚拟按键的高度，只有屏幕垂直时才有效
     */
    public void getVirtualHeight(View view) {
        tv_show.setText("当前屏幕方向：" + (ScreenUtils.isVertical(this) ? "竖屏" : "横屏")
                + "\n屏幕宽高：" + ScreenUtils.getScreenWidth(this) + "x" + ScreenUtils.getScreenHeight(this)
                + "\n状态栏高度：" + ScreenUtils.getStatusBarHeight(this)
                + "\n标题栏高度：" + ScreenUtils.getTitleBarHeight(this)
                + "\n是否有虚拟按键：" + ScreenUtils.isNavigationBarShown(this)
                + "\n虚拟按键高度：" + ScreenUtils.getNavigationBarHeight(this)
        );
    }

    public void chooseFile(View view) {
        Intent i = new Intent(ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        i.setType("video/*");
        startActivityForResult(i, CHOOSE_FILE_REQUEST_CODE);
    }

    private boolean isVideoFile(String fileName) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String mimeType = fileNameMap.getContentTypeFor(fileName);
        Log.i(TAG, fileName + " 的文件类型：" + mimeType);
        return mimeType.contains(PREFIX_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            String filePath = UriUtil.getFilePath(this, data.getData());
            Log.i(TAG, "onActivityResult filePath=" + filePath);
            if (filePath != null && !TextUtils.isEmpty(filePath)) {
                String filaName = filePath.substring(filePath.lastIndexOf("/"));
                if (isVideoFile(filaName)) {
                    startActivity(new Intent(MainActivity.this, PlayActivity.class)
                            .putExtra("video_file", filePath));
                }
            }
        }
    }
}
