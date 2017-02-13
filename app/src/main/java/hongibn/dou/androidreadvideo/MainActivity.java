package hongibn.dou.androidreadvideo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by  on 2017/2/13 下午5:28.
 */

public class MainActivity extends Activity {
    ImageView image;

    private String video_path = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = (ImageView) findViewById(R.id.image_view);

        model();
    }

    private void model() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = "";
                File file = new File(video_path);
                if (file.exists()) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(path);
                    int time = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    DLog.d("Time:" + time);
                    int count_time = 1;
                    int count_frame = 0;
                    while (count_time < time) {
                        count_time += 200;
                        count_frame++;
                        Bitmap bitmap = retriever.getFrameAtTime(count_time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

                        String pathPic = Environment.getExternalStorageDirectory() + "/img/out/" + count_frame + ".jpg";

                        FileOutputStream fos;
                        try {
                            fos = new FileOutputStream(pathPic);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                            DLog.d("out" + count_frame + "：" + count_time);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    DLog.d("文件不存在");
                }

            }
        }).start();
    }
}
