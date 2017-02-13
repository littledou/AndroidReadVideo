package hongibn.dou.androidreadvideo;

import android.util.Log;

/**
 * 日志
 * Created by mac on 16/6/14.
 */
public class DLog {

    public static boolean mSwitch = true;

    public static final int V = 1;
    public static final int D = 2;
    public static final int I = 3;
    public static final int W = 4;
    public static final int E = 5;

    public static void v() {
        v(null);
    }

    public static void d() {
        d(null);
    }

    public static void i() {
        i(null);
    }

    public static void w() {
        w(null);
    }

    public static void e() {
        e(null);
    }

    public static void v(Object message) {
        v(null, message);
    }

    public static void d(Object message) {
        d(null, message);
    }

    public static void i(Object message) {
        i(null, message);
    }

    public static void w(Object message) {
        w(null, message);
    }

    public static void e(Object message) {
        e(null, message);
    }

    public static void v(String tag, Object message) {
        llog(V, tag, message);
    }

    public static void d(String tag, Object message) {
        llog(D, tag, message);
    }

    public static void i(String tag, Object message) {
        llog(I, tag, message);
    }

    public static void w(String tag, Object message) {
        llog(W, tag, message);
    }

    public static void e(String tag, Object message) {
        llog(E, tag, message);
    }

    /**
     * 执行打印方法
     *
     * @param type
     * @param tagStr
     * @param obj
     */
    public static void llog(int type, String tagStr, Object obj) {
        String msg;
        if (!mSwitch) {
            return;
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int index = 5;
        String className = stackTrace[index].getFileName();
        String methodName = stackTrace[index].getMethodName();
        int lineNumber = stackTrace[index].getLineNumber();

        String tag = (tagStr == null ? "DLog" : tagStr);
        methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ (").append(className).append(":").append(lineNumber).append(")#").append(methodName).append(" ] ");

        if (obj == null) {
            msg = "Log with null Object";
        } else {
            msg = obj.toString();
        }
        if (msg != null) {
            stringBuilder.append(msg);
        }

        String logStr = stringBuilder.toString();

        switch (type) {
            case V:
                Log.v(tag, logStr);
                break;
            case D:
                Log.d(tag, logStr);
                break;
            case I:
                Log.i(tag, logStr);
                break;
            case W:
                Log.w(tag, logStr);
                break;
            case E:
                Log.e(tag, logStr);
                break;
        }
    }


}
