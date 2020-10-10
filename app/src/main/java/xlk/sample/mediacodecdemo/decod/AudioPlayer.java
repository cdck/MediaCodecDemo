package xlk.sample.mediacodecdemo.decod;

import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * @author Created by xlk on 2020/9/29.
 * @desc
 */
public class AudioPlayer {

    private AudioTrack mAudioTrack;

    /**
     * @param frequency 采样率
     * @param channel   声道
     * @param sampBit   采样精度
     */
    public AudioPlayer(int frequency, int channel, int sampBit) {
        //获得构建对象的最小缓冲区大小
        int minBufSize = AudioTrack.getMinBufferSize(frequency, channel, sampBit);
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                frequency, channel, sampBit, minBufSize, AudioTrack.MODE_STREAM
        );
        mAudioTrack.play();
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    public void play(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        mAudioTrack.write(data, offset, length);
    }
}
