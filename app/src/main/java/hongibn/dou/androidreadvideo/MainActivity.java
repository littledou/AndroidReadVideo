package hongibn.dou.androidreadvideo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by  on 2017/2/13 下午5:28.
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    ImageView image;
    TextView text;

    private String video_path = getExternalStorageDirectory() + "/img/test.mp4";
    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        image = (ImageView) findViewById(R.id.image_view);
//        text = (TextView) findViewById(R.id.textview);

        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);

    }


    private PlayerThread mPlayer = null;


    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mPlayer == null) {
            mPlayer = new PlayerThread(holder.getSurface());
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.interrupt();
        }
    }

    private class PlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        public PlayerThread(Surface surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(video_path);

                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")) {
                        extractor.selectTrack(i);
                        decoder = MediaCodec.createDecoderByType(mime);
                        decoder.configure(format, surface, null, 0);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            decoder.start();

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            DLog.d("InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 10000);


                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        DLog.d("INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        DLog.d("New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        DLog.d("dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        DLog.d("We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    DLog.d("OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            decoder.stop();
            decoder.release();
            extractor.release();
        }
    }


    //不好用
    @Deprecated
    private void model() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(video_path);
                DLog.d("path = " + video_path);
                if (file.exists()) {

                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(video_path);
                        int time = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        DLog.d("Time:" + time);
                        int count_time = 1;
                        int count_frame = 0;
                        while (count_time < time) {
                            DLog.d("get image");
                            count_time += 200;
                            count_frame++;
                            final Bitmap bitmap = retriever.getFrameAtTime(
                                    count_time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

                            final int count = count_frame;
                            DLog.d("draw " + count);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    image.setImageBitmap(bitmap);
                                    text.setText("当前帧数 ：" + count);
                                }
                            });

                            String path = "/sdcard/img/output/" + count + ".jpg";
                            FileOutputStream fos;
                            try {
                                fos = new FileOutputStream(path);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                                fos.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            retriever.release();
                        } catch (RuntimeException ex) {
                        }
                    }

                } else {
                    DLog.d("文件不存在");
                }

            }
        }).start();
    }
}
