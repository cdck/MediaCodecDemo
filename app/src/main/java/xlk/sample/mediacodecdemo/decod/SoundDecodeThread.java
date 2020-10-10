package xlk.sample.mediacodecdemo.decod;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Created by xlk on 2020/9/29.
 * @desc 视频文件音频解码
 */
public class SoundDecodeThread extends Thread {
    private final String TAG = "SoundDecodeThread-->";
    private final String filePath;
    private MediaCodec mediaCodec;
    private AudioPlayer audioPlayer;
    private AtomicBoolean quit = new AtomicBoolean(false);
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private MediaExtractor mediaExtractor;

    public SoundDecodeThread(String filePath) {
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
            Log.d(TAG, "initMediaCodec :  获取音频format信息 --> " + format);
            //判断是否是音频信道
            if (mimeType != null && mimeType.startsWith("audio/")) {
                //切换到音频信道
                mediaExtractor.selectTrack(i);
                mediaCodec = MediaCodec.createDecoderByType(mimeType);
                mediaCodec.configure(format, null, null, 0);
                audioPlayer = new AudioPlayer(
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
                mediaCodec.start();
                break;
            }
        }
    }

    private void startDecoding() {
        boolean bIsEos = false;
        long startMs = System.currentTimeMillis();
        while (!quit.get()) {
            if (!bIsEos) {
                int inIndex = mediaCodec.dequeueInputBuffer(0);
                if (inIndex >= 0) {
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inIndex);
                    // 读取一帧数据至buffer中
                    int nSampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                    if (nSampleSize < 0) {
                        mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        bIsEos = true;
                    } else {
                        mediaCodec.queueInputBuffer(inIndex, 0, nSampleSize, mediaExtractor.getSampleTime(), 0);
                        // 继续下一取样
                        mediaExtractor.advance();
                    }

                }
            }
            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            if (outIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outIndex);
                while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                //清空缓存
                outputBuffer.clear();
                //播放解码后的数据
                audioPlayer.play(outData, 0, bufferInfo.size);
                mediaCodec.releaseOutputBuffer(outIndex, true);
            }
            //所有解码的帧均已渲染，我们现在可以停止播放
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
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
            if (audioPlayer != null) {
                audioPlayer.release();
                audioPlayer = null;
            }
            Log.d(TAG, "release end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void quit() {
        quit.set(true);
    }
}
