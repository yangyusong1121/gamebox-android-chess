package com.dawoo.ipc.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dawoo.ipc.event.Events;
import com.dawoo.ipc.event.bean.GameApiEvent;
import com.dawoo.ipc.utl.FastJsonUtils;
import com.dawoo.ipc.utl.GetBytesWithHeadInfo;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP服务端
 */
public class IPCServerService extends Service {
    private static final String TAG = "IPCServerService ";

    private ConnectedThread mConnectedThread;
    private Thread mConnectThread;

    public static int PORT = 8644;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
        RxBus.get().register(this);
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand()");
        return START_STICKY_COMPATIBILITY;
    }

    /**
     * 开启 serverSocket
     */
    private void startServer() {
        if (mConnectThread != null && mConnectThread.isAlive()) {
            mConnectThread.interrupt();
        }
        mConnectThread = new Thread(new TcpServer());
        mConnectThread.start();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy()");
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
        if (mConnectThread != null && mConnectThread.isAlive()) {
            mConnectThread.interrupt();
        }
        RxBus.get().unregister(this);
        super.onDestroy();
    }



    /**
     * 刷新游戏api
     */
    @Subscribe(tags = {@Tag(Events.EVENT_REFRSH_API)})
    public void refreshGameApi(String s) {
        GameApiEvent gameApiEvent = new GameApiEvent();
        gameApiEvent.setGameApi(s);
        gameApiEvent.setType(Events.EVENT_REFRSH_API);
        String json = FastJsonUtils.toJSONString(gameApiEvent);
        Log.e(TAG, json + "");
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendBytes(GetBytesWithHeadInfo.getByteArry(json));
            }
        }).start();
    }

    /**
     * 传输数据
     */
    public synchronized void sendBytes(byte[] bytes) {
        if (null == mConnectedThread || mConnectedThread.isInterrupted()) {
            return;
        }
        mConnectedThread.write(bytes);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class TcpServer implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                Log.e(TAG, "建立链接失败, 端口:" + PORT);
                e.printStackTrace();
                return; // 链接建立失败直接返回
            }
            Socket socket = null;
            while (socket == null) {
                try {
                    socket = serverSocket.accept();
                    Log.e(TAG, "接收数据");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (mConnectedThread != null) {
                mConnectedThread.cancel();
            }
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();
        }
    }


    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(Socket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, " socket not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * 读
         */
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            int headLength;
            byte[] headBytes_ = new byte[GetBytesWithHeadInfo.HEADLENGTH];//包头的缓冲
            try {
                while ((headLength = mmInStream.read(headBytes_)) != -1) {
                    int contentLength = Integer.parseInt(new String(headBytes_, 0, headLength).trim());
                    byte[] contentBytes_ = new byte[contentLength];
                    int temp = mmInStream.read(contentBytes_);
                    while (temp < contentLength) {
                        temp += mmInStream.read(contentBytes_, temp, contentLength - temp);
                    }
                    // 解析    byte[] contentBytes_
                    String json = new String(contentBytes_, 0, contentLength);
                    Log.i(TAG, json);
                }
            } catch (IOException e) {
                Log.e(TAG, " 远程 socket 异常", e);
                e.printStackTrace();
            }
        }

        /**
         * 写
         */
        public void write(byte[] buffer) {
            try {
                if (this != null && this.isAlive()) {
                    mmOutStream.write(buffer);
                    mmOutStream.flush();
                } else {
                    Log.e(TAG, "向客户端写 -- 异常  线程死亡");
                }
            } catch (IOException e) {
                Log.e(TAG, "向客户端写 -- 异常", e);

            }
        }

        public void cancel() {
            try {
                if (this.isAlive()) {
                    this.interrupt();
                }
                mmInStream.close();
                mmSocket.close();
                mmOutStream.close();
            } catch (IOException e) {
                Log.e(TAG, " socket 关闭异常", e);
            }
        }
    }


}
