package xlk.sample.mediacodecdemo.decod;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Created by xlk on 2020/9/29.
 * @desc 视频文件画面解码
 */
public class VideoDecodeThread extends Thread {
    private final String TAG = "VideoDecodeThread-->";
    private final Surface surface;
    private final String filePath;
    private MediaExtractor mediaExtractor;
    private AtomicBoolean quit = new AtomicBoolean(false);
    private MediaCodec mediaCodec;

    public VideoDecodeThread(Surface surface, String filePath) {
        this.surface = surface;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        super.run();
        try {
            initMediaCodec();
            if (mediaCodec != null) {
                startDecoding();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    private void initMediaCodec() throws IOException {
        mediaExtractor = new MediaExtractor();
        Log.d(TAG, "initMediaCodec filePath=" + filePath);
        try {
//            mediaExtractor.setDataSource(filePath);
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            FileDescriptor fd = fis.getFD();
            mediaExtractor.setDataSource(fd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mimeType = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "initMediaCodec :  获取视频format信息 --> " + format);
            //判断是否是视频信道
            if (mimeType != null && mimeType.startsWith("video/")) {
                //切换到视频信道
                mediaExtractor.selectTrack(i);
                //创建解码器
                mediaCodec = MediaCodec.createDecoderByType(mimeType);
                mediaCodec.configure(format, surface, null, 0);
                mediaCodec.start();
                break;
            }
        }
    }

    private void startDecoding() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long startMs = System.currentTimeMillis();
        boolean bIsEos = false;
        while (!quit.get()) {
            if (!bIsEos) {
                int index = mediaCodec.dequeueInputBuffer(0);
                if (index >= 0) {
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                    //读取一帧数据至buffer中
                    int i = mediaExtractor.readSampleData(inputBuffer, 0);
                    if (i < 0) {
                        mediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        bIsEos = true;
                    } else {
                        mediaCodec.queueInputBuffer(index, 0, i, mediaExtractor.getSampleTime(), 0);
                        //继续下一取样
                        mediaExtractor.advance();
                    }
                }
            }
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            if (index >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                //防止视频播放过快
                while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mediaCodec.releaseOutputBuffer(index, true);
            }
            //所有解码的帧均已渲染，我们现在可以停止播放
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                Log.i(TAG, "startDecoding :  所有解码的帧均已渲染，我们现在可以停止播放 --> ");
                quit();
            }
        }
    }

    private void release() {
        try {
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
            if (mediaExtractor != null) {
                mediaExtractor.release();
                mediaExtractor = null;
            }
            Log.i(TAG, "release end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void quit() {
        quit.set(true);
    }

    private void abc(MediaFormat format) {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        String decoderForFormat = mediaCodecList.findDecoderForFormat(format);
        Log.i(TAG, "abc :  decoderForFormat --> " + decoderForFormat);
        MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            String[] supportedTypes = codecInfo.getSupportedTypes();
            String name = codecInfo.getName();
            boolean encoder = codecInfo.isEncoder();
            Log.i(TAG, "abc :  codecInfo --> encoder=" + encoder + ", name=" + name + ", supportedTypes=" + Arrays.toString(supportedTypes));
        }
    }
}
