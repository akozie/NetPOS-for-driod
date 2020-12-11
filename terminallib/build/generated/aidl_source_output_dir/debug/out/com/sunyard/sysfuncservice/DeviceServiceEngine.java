/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.sunyard.sysfuncservice;
public interface DeviceServiceEngine extends android.os.IInterface
{
  /** Default implementation for DeviceServiceEngine. */
  public static class Default implements com.sunyard.sysfuncservice.DeviceServiceEngine
  {
    //静默安装指定的App
    //appFilePath 应用文件路径  runActivityName 安装完毕之后需要运行的activity   appName 服务界面提示的应用名称（正在安装xxxx应用,请耐心等待）

    @Override public boolean installApp(java.lang.String appFilePath) throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.sunyard.sysfuncservice.DeviceServiceEngine
  {
    private static final java.lang.String DESCRIPTOR = "com.sunyard.sysfuncservice.DeviceServiceEngine";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.sunyard.sysfuncservice.DeviceServiceEngine interface,
     * generating a proxy if needed.
     */
    public static com.sunyard.sysfuncservice.DeviceServiceEngine asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.sunyard.sysfuncservice.DeviceServiceEngine))) {
        return ((com.sunyard.sysfuncservice.DeviceServiceEngine)iin);
      }
      return new com.sunyard.sysfuncservice.DeviceServiceEngine.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_installApp:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          boolean _result = this.installApp(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.sunyard.sysfuncservice.DeviceServiceEngine
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      //静默安装指定的App
      //appFilePath 应用文件路径  runActivityName 安装完毕之后需要运行的activity   appName 服务界面提示的应用名称（正在安装xxxx应用,请耐心等待）

      @Override public boolean installApp(java.lang.String appFilePath) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(appFilePath);
          boolean _status = mRemote.transact(Stub.TRANSACTION_installApp, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().installApp(appFilePath);
          }
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      public static com.sunyard.sysfuncservice.DeviceServiceEngine sDefaultImpl;
    }
    static final int TRANSACTION_installApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(com.sunyard.sysfuncservice.DeviceServiceEngine impl) {
      if (Stub.Proxy.sDefaultImpl == null && impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.sunyard.sysfuncservice.DeviceServiceEngine getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  //静默安装指定的App
  //appFilePath 应用文件路径  runActivityName 安装完毕之后需要运行的activity   appName 服务界面提示的应用名称（正在安装xxxx应用,请耐心等待）

  public boolean installApp(java.lang.String appFilePath) throws android.os.RemoteException;
}
