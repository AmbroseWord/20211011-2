package com.example.f_dload;

/**
 * Created by AmbroseWord on 2016/05/30.
 */
//////////////////////////////////////////////////////////////////////////////////////////////////
//ファイル種別＆ファイル拡張子
public final class FileClassification {
    public static final int GraphicFile=1;  //画像ファイル
    public static final int SoundFile=2;    //音声ファイル
    public static final int TextFile=3;    //音声ファイル
    public static final int OtherFile=255;  //その他ファイル
    public static final String[] Graphic_Extension ={"gif","jpg","png","bmp"};  //画像ファイル拡張を
    public static final String[] Sound_Extension ={"mp3","wav"};                //音声ファイル拡張子
    public static final String[] Text_Extension ={"txt"};                //テキスト拡張子
}
