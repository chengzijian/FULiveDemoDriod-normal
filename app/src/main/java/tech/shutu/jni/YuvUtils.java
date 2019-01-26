package tech.shutu.jni;

/**
 * Created by raomengyang on 24/12/2016.
 */
public class YuvUtils {

    public static native void allocateMemo(int src_yuv_length, int src_argb_length, int dst_length);

    public static native void scaleAndRotateYV12ToI420(byte[] src_data, byte[] dst_data, int src_width, int src_height, int rotation, int dst_width, int dst_height);

    public static native void releaseMemo();

    /**
     * so文件默认前缀带lib，在此引用时需要去掉"lib"和后缀".so"
     * */
    static {
        System.loadLibrary("yuv_utils");
        System.loadLibrary("yuv");
    }
}
