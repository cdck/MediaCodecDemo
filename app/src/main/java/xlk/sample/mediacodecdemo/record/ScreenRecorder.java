package xlk.sample.mediacodecdemo.record;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Created by xlk on 2020/9/26.
 * @desc 屏幕录制成mp4文件
 */
public class ScreenRecorder extends Thread {

    private final String TAG = "ScreenRecorder-->";
    /**
     * h.264编码
     */
    private final String MIME_TYPE = "video/avc";
    /**
     * 帧率
     */
    private final int FRAME_RATE = 30;
    /**
     * 关键帧间隔  两关键帧之间的其它帧 = 18*2
     */
    private final int I_FRAME_INTERVAL = 2;
    /**
     * 超时值
     */
    private final int TIMEOUT_US = 10 * 1000;
    private final String saveFilePath;
    private int width, height, bitrate, dpi;
    MediaProjection projection;
    private MediaCodec encoder;
    private Surface mSurface;
    private VirtualDisplay display;
    private MediaMuxer mediaMuxer;

    public ScreenRecorder(int w, int h, int dpi, MediaProjection projection, String saveFilePath) {
        Log.i(TAG, "ScreenRecorder w=" + w + ",h=" + h + ",dpi=" + dpi + ",saveFilePath=" + saveFilePath);
        if (w % 2 != 0) {
            w--;
            Log.e(TAG, "宽必须是2的倍数 ");
        }
        if (h % 2 != 0) {
            h--;
            Log.e(TAG, "高必须是2的倍数 ");
        }
        this.width = w;
        this.height = h;
        this.bitrate = width * height * 3;
        this.dpi = dpi;
        this.projection = projection;
        this.saveFilePath = saveFilePath;
    }

    @Override
    public void run() {
        super.run();
        try {
            //初始化编码器
            prepareEncoder();
            File file = new File(saveFilePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            mediaMuxer = new MediaMuxer(saveFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            //创建VirtualDisplay实例,DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC / DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
            display = projection.createVirtualDisplay("MainScreen", width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, null, null);
            // 录制虚拟屏幕
            recordVirtualDisplay();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    /**
     * 初始化编码器
     *
     * @throws IOException MediaCodec.createEncoderByType
     */
    private void prepareEncoder() throws IOException {
        //1.创建编码器
        encoder = MediaCodec.createEncoderByType(MIME_TYPE);
        //2.调整宽高
        MediaCodecInfo codecInfo = encoder.getCodecInfo();
        MediaCodecInfo.CodecCapabilities capabilitiesForType = codecInfo.getCapabilitiesForType(MIME_TYPE);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilitiesForType.getVideoCapabilities();
        Range<Integer> supportedWidths = videoCapabilities.getSupportedWidths();
        Range<Integer> supportedHeights = videoCapabilities.getSupportedHeights();
        // TODO: 2020/9/26 解决宽高不适配的问题 Fix:android.media.MediaCodec$CodecException: Error 0xfffffc0e
        width = supportedWidths.clamp(width);
        height = supportedHeights.clamp(height);
        Log.e(TAG, "prepareEncoder width=" + this.width + ",height=" + height + ",bitrate=" + bitrate);
        //3.初始化MediaFormat
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        // 码率 越高越清晰 仅编码器需要设置
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger("max-bitrate", bitrate);
        // 颜色格式
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // COLOR_FormatSurface这里表明数据将是一个graphicBuffer元数据
        // 将一个Android surface进行mediaCodec编码
        // 帧数 越高越流畅,24以下会卡顿
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        //画面静止时不会发送数据，屏幕内容有变化才会刷新，
        //仅在以“表面输入”模式配置视频编码器时适用。相关值为long，并给出以微秒为单位的时间，
        //设置如果之后没有新帧可用，则先前提交给编码器的帧在 1000000 / FRAME_RATE 微秒后重复（一次）
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / FRAME_RATE);
        // 关键帧间隔时间s
        // IFRAME_INTERVAL是指的帧间隔，它指的是，关键帧的间隔时间。通常情况下，设置成多少问题都不大。
        // 比如设置成10，那就是10秒一个关键帧。但是，如果有需求要做视频的预览，那最好设置成1
        // 因为如果设置成10，会发现，10秒内的预览都是一个截图
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 这一步非常关键，它设置的，是MediaCodec的编码源，也就是说，要告诉Encoder解码哪些流。
        mSurface = encoder.createInputSurface();
        // 开始编码
        encoder.start();
    }

    private AtomicBoolean quit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private byte[] configbyte;

    /**
     * 获取录制信息
     */
    private void recordVirtualDisplay() {
        while (!quit.get()) {
            int index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            while (index >= 0) {
                resetOutputFormat();
                ByteBuffer outputBuffer = encoder.getOutputBuffer(index);
                read2file(outputBuffer);
//                byte[] outData = new byte[bufferInfo.size];
//                outputBuffer.get(outData);
//                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                    //这表示这样标记的缓冲区包含编解码器初始化/编解码器特定的数据，而不是媒体数据。
//                    configbyte = new byte[bufferInfo.size];
//                    configbyte = outData;

//                } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//                    //这表示带有此标记的（编码的）缓存包含关键帧数据
//                    byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
//                    System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
//                    System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);

//                } else {
//                    //这表示普通帧数据

//                }

                //当格式改变的时候需要重新设置格式，第一次开始的时候会返回这个值
//            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                resetOutputFormat();
//            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            } else if (index >= 0) {
//                if (!m_bMuxerStarted) continue;
//                read2file(index);
//            }
                encoder.releaseOutputBuffer(index, false);
                index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            }
        }
    }

    private int m_videoTrackIndex;
    private boolean m_bMuxerStarted;

    private void resetOutputFormat() {
        if (m_bMuxerStarted) {
            return;
        }
        //将MediaCodec的Format设置给MediaMuxer
        MediaFormat newFormat = encoder.getOutputFormat();

        //获取m_videoTrackIndex，这个值是每一帧画面要放置的顺序
        m_videoTrackIndex = mediaMuxer.addTrack(newFormat);
        mediaMuxer.start();
        m_bMuxerStarted = true;
        Log.e(TAG, "resetOutputFormat mediaMuxer start");
    }

    /**
     * 写入文件
     *
     * @param encodeData 录屏数据
     */
    private void read2file(ByteBuffer encodeData) {
        //当m_bufferInfo返回这个标志位时，就说明已经传完数据了
        //将m_bufferInfo.size设为0，准备将其回收
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            bufferInfo.size = 0;
        }
        if (bufferInfo.size == 0) {
            encodeData = null;
        }
        if (encodeData != null) {
            //设置该从哪个位置读取数据
            encodeData.position(bufferInfo.offset);
            //设置该读取多少数据
            encodeData.limit(bufferInfo.offset + bufferInfo.size);
            //将数据写入到文件
            //第一个参数是每一帧画面要放置的顺序
            //第二个参数是要写入的数据
            //第三个参数是BufferInfo，这个数据包含encodeData的offset和size
            Log.v(TAG, "将录制的数据写入文件");
            mediaMuxer.writeSampleData(m_videoTrackIndex, encodeData, bufferInfo);
        }
    }

    public void quit() {
        quit.set(true);
    }

    /**
     * 释放资源
     */
    private void release() {
        try {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
                encoder = null;
            }
            if (display != null) {
                display.release();
            }
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
                mediaMuxer = null;
            }
            Log.i(TAG, "release 结束录制，释放资源...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
