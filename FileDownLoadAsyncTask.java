package com.example.f_dload;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by AmbroseWord on 2016/05/04.
 */
///////////////////////////////////////////////////////////////////////////////////////////////////
//ファイルDownLoad処理のための、別タスク処理
public class FileDownLoadAsyncTask extends AsyncTask<String, Integer, String> implements DialogInterface.OnCancelListener {

    Context causecontext;        //呼び出し元のContext
    ProgressDialog dialog;      //DownLoad状況表示バー用Dialog

    FileDownLoadAsyncTaskResult asynctask_result;   //ダウンロード結果を格納

    int file_num ;          //  file_num：DownLoadファイル数
    //DownLoadするファイルの総バイト数の確認＆ファイルの存在確認
    int byte_count_sum;         //DownLoadするファイルの総Byte数
    int[] files_byte_count;     //DownLoadするファイルの個々のByte数
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //重要：doInBackground 以外は UI スレッド上で実行されるので、UI に関わる処理が、そのまま行える。
    //ここでは、呼び出し元からダウンロードの成否を確認ように
    public FileDownLoadAsyncTask(Context context, FileDownLoadAsyncTaskResult result){
        causecontext = context;                     //呼び出し元のContext取得
        asynctask_result = result;                  //処理の成否確認用のオブジェクト設定
    }

    public FileDownLoadAsyncTask() {
    }

    @Override
    //////////////////////////////////////////////////////////////////////////////////////////////
    //コンストラクタの、次に実行されるので、ダイアログを表示
    //終了後、別タスクとして、「doInBackground」が実行される。
    protected void onPreExecute(){
        dialog = new ProgressDialog(causecontext);                       //ダイアログ生成
        dialog.setTitle("DownLoad");                                //ダイアログ：タイトル
        dialog.setMessage("Loading...");                            //ダイアログ：メッセージ？
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);   //ダイアログ：水平バー
        dialog.setCancelable(true);                                   //ダイアログ：キャンセル可能
        dialog.setOnCancelListener(this);                           //ダイアログ：キャンセル時はこのClass内で処理
        dialog.setMax(100);                                         //ダイアログ：水平最大値100
        dialog.setProgress(0);                                      //ダイアログ：水平最小値0
        dialog.show();                                              //ダイアログ表示
    }
    @Override
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //「onPreExecute」の次に実行される。「doInBackground」以降は別タスクとして非同期で実行される
    //  時々「isCancelled」で処理の終了確認を入れる
    //  進捗をアップデートする際は、publishProgressを呼び出す
    //  publishProgressを呼び出すと、onProgressUpdateが呼ばれる
    //  その際の、引数は、AsyncTaskをextendsした時の第一引数
    protected String doInBackground(String... params) {
        //①引数が揃っているか確認。不足時はエラー終了（キャンセル）
        //引数はString配列。添え字[0]がSave先ストレージPath、[1]～がDownLoad元のURLを格納
        file_num = params.length;
        if(file_num < 2){
            this.cancel(true);  //引数が「Save先」「DownLoad元」が揃っていない場合、終了処理
        }else{
            //②全DownLoadファイルの有無をチェック＋ファイルサイズ取得
            //  不足ファイルがあればエラー終了（キャンセル）
            //DownLoadするファイルの総バイト数の確認＆ファイルの存在確認
            //byte_count_sum    ：DownLoadするファイルの総バイト数
            //byte_count        ：個々のファイルチェック結果（ファイルのバイト数）
            files_byte_count = new int[params.length];              //個々のﾌｧｲﾙサイズ
            byte_count_sum = 0;                                         //全体のDownLoadサイズ
            for (int i=1;i<file_num;i++){                              //DownLoadﾌｧｲﾙの存在チェックループ
                files_byte_count[i] = chk_download_file(params[i]);     //DownLoadﾌｧｲﾙの存在＆ファイルサイズチェック
                if(files_byte_count[i]<0){
                    this.cancel(true);  //ファイルチェックの結果が失敗した場合、負の数値を返すので、終了処理
                }else{
                    //ファイルチェックの結果が成功した場合、ファイルの総バイト数にカウント
                    byte_count_sum += files_byte_count[i];
                }
            }
        }
        //③保存先フォルダが存在しない場合、作成
        File dir = new File(params[0]);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        //④DownLoad処理
        //指定された全ファイルのDownLoad処理
        int byte_count_all = 0;                                     //DownLoadしたﾊﾞｲﾄ数
        for (int i=1;i<file_num;i++) {                     //DownLoad用ループ（ﾌｧｲﾙ単位）
            int byte_count = 0;                                     //DownLoadした途中のﾊﾞｲﾄ数
            InputStream inputstream = null;                 //DownLoadﾌｧｲﾙのInputStream
            BufferedInputStream buff_inputstream = null;    //DownLoadﾌｧｲﾙのBufferedInputStream
            FileOutputStream file_outputStream = null;      //ｽﾄﾚｰｼﾞへのFileOutputStream
            HttpURLConnection url_connection = null;    //DownLoad用HttpURLConnection
            try{
                byte[] buffer = new byte[1024];              //DownLoad用バッファ
                String[] dir_paths = params[i].split("/");       //DownLoadFile名の
                URL url = new URL(params[i]);               //DownLoadﾌｧｲﾙ用URL
                //DownLoad用URLから、HttpURLConnectionを生成
                url_connection = (HttpURLConnection)url.openConnection();
                url_connection.connect();                                       //DownLoadﾌｧｲﾙへ接続
                inputstream = url_connection.getInputStream();                  //DownLoad用InputStream生成
                //Max512ﾊﾞｲﾄのBufferedInputStreamにInputStreamを接続
                buff_inputstream = new BufferedInputStream(inputstream, 1024);
                //ストレージ上にFileOutputStreamとして出力先ﾌｧｲﾙを生成
                file_outputStream = new FileOutputStream(params[0]+dir_paths[dir_paths.length-1]);
                int len;
                //ファイル転送ループ
                while((len = buff_inputstream.read(buffer)) != -1){ //DownLoad用InputStreamからBufferedInputStreamにRead
                    file_outputStream.write(buffer, 0, len);        //BufferedInputStreamからFileOutputStreamにWrite
                    byte_count += len;
                    publishProgress((int) (100 * (byte_count_all+byte_count) / byte_count_sum));    //定期的に呼ぶ処理（ダイアログ更新）
                    //重要：適当なタイミングで「isCancelled()」の判定を行う
                    //  「戻る」ボタンが押された場合、「isCancelled()」の判定が生じる
                    if(isCancelled()){
                        buff_inputstream.close();
                        file_outputStream.flush();
                        file_outputStream.close();
                        this.cancel(true);
                        break;
                    }
                }
                byte_count_all += files_byte_count[i];                        //DownLoad済みバイト数更新
                publishProgress((int) (100 * byte_count_all / byte_count_sum));    //定期的に呼ぶ処理（ダイアログ更新）
                //1ﾌｧｲﾙDownLoad終了後に一旦、接続を切る
                file_outputStream.close();
                buff_inputstream.close();
                file_outputStream.flush();

                //ﾌｧｲﾙｻｲｽﾞが小さい場合、ダイアログが瞬間で消えるので、0.5sec間隔を取る
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.cancel(true);
                }
            } catch (IOException e) {
                try{
                    buff_inputstream.close();
                    file_outputStream.flush();
                    file_outputStream.close();
                    url_connection.disconnect();
                    this.cancel(true);
                }catch (IOException e1){}
                e.printStackTrace();
                this.cancel(true);
            }
        }
        String stringret = new String(""+params[0]);    //ダウンロード元URLを確認のため返す
        return stringret;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //doInBackground 内で publishProgressを呼ぶと「onProgressUpdate」が呼ばれる
    //引数はAsyncTaskをextendsした時の第２引数（＝「publishProgress」の引数）
    protected void onProgressUpdate(Integer... values){
        dialog.setProgress(values[0]);                      //ダイアログの更新処理
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    //「doInBackground」終了時に呼ばれる。
    //引数は「doInBackground」の戻り値＝AsyncTaskをextendsした時の第３引数
    protected void onPostExecute(String sresult){
        asynctask_result.setResult(1);              //ダウンロード成功した場合「1」をセット
        dialog.dismiss();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //「doInBackground」キャンセル時に呼ばれる。（「this.cancel(true)」の場合）
    protected void onCancelled(){
        asynctask_result.setResult(-1);        //ダウンロードキャンセルした場合「-1」をセット
        dialog.dismiss();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //「ProgressDialog」キャンセル時に呼ばれる。
    public void onCancel(DialogInterface dialog) {
        this.cancel(true);      //「onCancelled」を呼ぶために「this.cancel(true)」
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //以下、通信処理の事前確認
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //URLで指定したファイルの存在をチェック
    //  引数：URLでファイル指定
    //  戻り値：存在する場合、ファイルサイズ
    //          存在しない場合、-1
    int chk_download_file(String urlstring){
        int ret=0;
        URL url;
        HttpURLConnection url_connection = null;
        try {
            url = new URL(urlstring);                               //DownLoadするファイル名からURL生成
            URLConnection conn = url.openConnection();              //接続用URLConnectionを作成
            url_connection = (HttpURLConnection)conn;                     //URLConnectionからHttpURLConnectionを生成
            url_connection.connect();                                     //接続開始
            int response = url_connection.getResponseCode();              //接続結果を取得
            if(response == HttpURLConnection.HTTP_OK){              //接続がOKかチェック
                ret = url_connection.getContentLength();                  //接続先のサイズを戻り値として取得
            }else{
                ret = -1;                                           //正常に接続できない場合、-1を返す
            }
            url_connection.disconnect();                                      //接続を切断する
        } catch (java.io.IOException e) {
            e.printStackTrace();                                    //例外発生時
            if(url_connection!=null){                                     //例外発生時、接続確立していた場合
                url_connection.disconnect();                               //接続を切断する
            }
            return -1;                                             //接続時例外発生なら-1を返す
        }
        return ret;                                                 //接続成功した場合、ファイルサイズを返す
    }

}
