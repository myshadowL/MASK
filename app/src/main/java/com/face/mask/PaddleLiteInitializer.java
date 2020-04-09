package com.face.mask;

public class PaddleLiteInitializer {
    public static final String JNI_LIB_NAME = "paddle_lite_jni";

    protected static boolean init()
    {
        System.loadLibrary("paddle_lite_jni");
        return true;
    }
}
