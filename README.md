## 在Android设备中按帧读取视频

1. Mode1 使用MediaMetadataRetriever
    Bitmap bitmap = retriever.getFrameAtTime(
        time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    使用MediaMetadataRetriever获取到的帧并不对，取得的应该是附近的关键帧，说明MediaMetadataRetriever解码能力不够
2. Mode2 Android: MediaCodec硬件解码
    
