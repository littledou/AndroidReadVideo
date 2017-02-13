package hongibn.dou.androidreadvideo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by  on 2017/2/13 下午5:28.
 */

public class MainActivity extends Activity {
    ImageView image;
    TextView text;

    private String video_path = getExternalStorageDirectory() + "/img/test.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = (ImageView) findViewById(R.id.image_view);
        text = (TextView) findViewById(R.id.textview);

        model();
//        mode2();
    }

//    private void mode2() {
//        // https://github.com/zhantong/Android-VideoToImages
//        MediaExtractor extractor = null;
//        MediaCodec decoder = null;
//        try {
//            File videoFile = new File(video_path);:
//            extractor = new MediaExtractor();
//            extractor.setDataSource(videoFile.toString());
//            int trackIndex = selectTrack(extractor);
//            if (trackIndex < 0) {
//                throw new RuntimeException("No video track found in " + videoFilePath);
//            }
//            extractor.selectTrack(trackIndex);
//            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
//            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
//            decoder = MediaCodec.createDecoderByType(mime);
//            showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
//            if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
//                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
//                Log.i(TAG, "set decode color format to type " + decodeColorFormat);
//            } else {
//                Log.i(TAG, "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
//            }
//            decodeFramesToImage(decoder, extractor, mediaFormat);
//            decoder.stop();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (decoder != null) {
//                decoder.stop();
//                decoder.release();
//                decoder = null;
//            }
//            if (extractor != null) {
//                extractor.release();
//                extractor = null;
//            }
//
//        }
//    }

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
