package com.example.f_dload;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by AmbroseWord on 2016/05/26.
 */
public class FDTestView extends View {
    FileList fileList;      //ダウンロードしたファイルのリスト
    Bitmap bmp01 , bmp02;   //ファイルのリスト中の画像
    Bitmap bmp11 , bmp12;   //drawable内の画像（比較対象）

    MediaPlayer bgm_player; //ファイルのリスト中の音楽

    public FDTestView(Context context, FileList input_filelist) {
        super(context);
        fileList = input_filelist;          //ファイルのリストを取得
        bgm_player = new MediaPlayer();     //音声再生のMediaPlayer生成
        try {
            bgm_player.setDataSource(fileList.sound_files.get(0));  //1つ目の音声ファイル読込
            bgm_player.prepare();                                       //再生状態を初期化
            bgm_player.setLooping(true);                                //ループ再生指定
        } catch (IOException e) {
            e.printStackTrace();
        }

        bmp11 = BitmapFactory.decodeResource(getResources(), R.drawable.europe00);  //drawableの画像①
        bmp12 = BitmapFactory.decodeResource(getResources(), R.drawable.s000101);  //drawableの画像②

        FileInputStream bmp_inputStream01 = null;   //ストレージから1つ目の画像ファイル準備
        FileInputStream bmp_inputStream02 = null;   //ストレージから2つ目の画像ファイル準備
        File bmpFile01 = new File(input_filelist.graphic_files.get(0)); //1つ目の画像ファイル指定
        File bmpFile02 = new File(input_filelist.graphic_files.get(1)); //2つ目の画像ファイル指定
        try {
            bmp_inputStream01 = new FileInputStream(bmpFile01);     //1つ目の画像ファイル読込開始
            bmp01 = BitmapFactory.decodeStream(bmp_inputStream01);  //1つ目の画像ファイル読込
            bmp_inputStream01.close();                              //1つ目の画像ファイル読込終了

            bmp_inputStream02 = new FileInputStream(bmpFile02);     //2つ目の画像ファイル読込開始
            bmp02 = BitmapFactory.decodeStream(bmp_inputStream02);  //2つ目の画像ファイル読込
            bmp_inputStream02.close();                              ////2つ目の画像ファイル読込終了
        } catch (IOException e) {
            //以下、画像ファイル読込失敗時の処理
            if (bmp_inputStream01 != null) {
                try {
                    bmp_inputStream01.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (bmp_inputStream02 != null) {
                    try {
                        bmp_inputStream02.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                e.printStackTrace();
            }
        }
        bgm_player.start();     //1つ目の音声ファイル再生
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bmp02, 0, 0, null);   //2つ目の画像ファイル表示
        canvas.drawBitmap(bmp01, 0, 0, null);   //1つ目の画像ファイル表示
        canvas.drawBitmap(bmp12, 0, 640, null); //drawable上の画像①表示（比較対象）
        canvas.drawBitmap(bmp11, 0, 640, null); //drawable上の画像②表示（比較対象）
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //画面タッチ処理
    public boolean onTouchEvent(MotionEvent event) {
        if (bgm_player.isPlaying()) {   //音声再生チェック
            bgm_player.stop();            //音声再生中なら、再生停止
            try {
                bgm_player.prepare();   //停止後、再度再生のため初期化
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            bgm_player.start();         //音声停止中なら、再生
        }
        invalidate();       //画面更新
        return false;
    }

}
