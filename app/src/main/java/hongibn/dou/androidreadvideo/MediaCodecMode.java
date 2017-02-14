package hongibn.dou.androidreadvideo;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by mac on 2017/2/14 上午10:42.
 */

public class MediaCodecMode implements Runnable {

    private String video_path = getExternalStorageDirectory() + "/img/test.mp4";
    private String OUTPUT_DIR = getExternalStorageDirectory() + "/img/output";
    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

    private static final long DEFAULT_TIMEOUT_US = 10000;
    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

    private String outputFormat = "NV21";
    // "I420" "NV21" "JPEG"

    private Thread childThread;

    public void decode() {
        if (childThread == null) {
            childThread = new Thread(this, "decode");
            childThread.start();
        }
    }

    @Override
    public void run() {

        try {
            startDecode();
        } catch (Throwable t) {
        }

    }

    private void startDecode() {
        // https://github.com/zhantong/Android-VideoToImages
        MediaExtractor extractor = null;
        MediaCodec decoder = null;
        try {
            File videoFile = new File(video_path);
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFile.toString());
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + video_path);
            }
            extractor.selectTrack(trackIndex);
            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);


            boolean support = false;
            for (int c : decoder.getCodecInfo().getCapabilitiesForType(mime).colorFormats) {
                DLog.d("Support Color Format " + c);
                if (decodeColorFormat == c) {
                    support = true;
                    break;
                }
            }

            if (support) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
                DLog.i("set decode color format to type " + decodeColorFormat);
            } else {
                DLog.i("unable to set decode color format, color format type " + decodeColorFormat + " not supported");
            }
            decodeFramesToImage(decoder, extractor, mediaFormat);
            decoder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }

        }
    }


    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                DLog.d("Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }
        return -1;
    }


    private Callback callback;

    public interface Callback {
        void onFinishDecode();

        void onDecodeFrame(int index);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }


    private void decodeFramesToImage(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        decoder.configure(mediaFormat, null, null, 0);
        decoder.start();
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int outputFrameCount = 0;
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            int outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
                boolean doRender = (info.size != 0);
                if (doRender) {
                    outputFrameCount++;
                    if (callback != null) {
                        callback.onDecodeFrame(outputFrameCount);
                    }
                    Image image = decoder.getOutputImage(outputBufferId);
                    //YUV_420_888
                    DLog.d("image format: " + image.getFormat() + " out " + outputFormat);

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] arr = new byte[buffer.remaining()];
                    buffer.get(arr);

                    String fileName;
                    switch (outputFormat) {
                        case "I420":
                            fileName = OUTPUT_DIR + String.format("frame_%05d_I420_%dx%d.yuv", outputFrameCount, width, height);
                            dumpFile(fileName, getDataFromImage(image, COLOR_FormatI420));
                            break;
                        case "NV21":
                            fileName = OUTPUT_DIR + String.format("frame_%05d_NV21_%dx%d.yuv", outputFrameCount, width, height);
                            dumpFile(fileName, getDataFromImage(image, COLOR_FormatNV21));
                            break;
                        case "JPEG":
                            fileName = OUTPUT_DIR + String.format("frame_%05d.jpg", outputFrameCount);
                            compressToJpeg(fileName, image);
                            break;
                    }
                    image.close();
                    decoder.releaseOutputBuffer(outputBufferId, true);
                }
            }
        }
        if (callback != null) {
            callback.onFinishDecode();
        }
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        DLog.v("get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            DLog.v("pixelStride " + pixelStride);
            DLog.v("rowStride " + rowStride);
            DLog.v("width " + width);
            DLog.v("height " + height);
            DLog.v("buffer size " + buffer.remaining());
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            DLog.d("Finished reading data from plane " + i);
        }
        return data;
    }

    private static void dumpFile(String fileName, byte[] data) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        try {
            outStream.write(data);
            outStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("failed writing data to file " + fileName, ioe);
        }
    }

    private void compressToJpeg(String fileName, Image image) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        Rect rect = image.getCropRect();
        YuvImage yuvImage = new YuvImage(getDataFromImage(image, COLOR_FormatNV21), ImageFormat.NV21, rect.width(), rect.height(), null);
        yuvImage.compressToJpeg(rect, 100, outStream);
    }
}
