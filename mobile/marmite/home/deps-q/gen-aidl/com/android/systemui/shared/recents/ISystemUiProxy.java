/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: G:\\Android-sdk\\build-tools\\37.0.0\\aidl.exe -oC:\\work\\aidlq\\out -IC:\\work\\aidlq\\src -IC:\\work\\aidlq\\stub C:\\work\\aidlq\\src\\com\\android\\systemui\\shared\\recents\\ISystemUiProxy.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.android.systemui.shared.recents;
/** Temporary callbacks into SystemUI. */
public interface ISystemUiProxy extends android.os.IInterface
{
  /** Default implementation for ISystemUiProxy. */
  public static class Default implements com.android.systemui.shared.recents.ISystemUiProxy
  {
    /**
     * Proxies SurfaceControl.screenshotToBuffer().
     * @Removed
     * GraphicBufferCompat screenshot(in Rect sourceCrop, int width, int height, int minLayer,
     *             int maxLayer, boolean useIdentityTransform, int rotation) = 0;
     */
    /** Begins screen pinning on the provided {@param taskId}. */
    @Override public void startScreenPinning(int taskId) throws android.os.RemoteException
    {
    }
    /** Notifies SystemUI that split screen has been invoked. */
    @Override public void onSplitScreenInvoked() throws android.os.RemoteException
    {
    }
    /** Notifies SystemUI that Overview is shown. */
    @Override public void onOverviewShown(boolean fromHome) throws android.os.RemoteException
    {
    }
    /** Get the secondary split screen app's rectangle when not minimized. */
    @Override public android.graphics.Rect getNonMinimizedSplitScreenSecondaryBounds() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Control the {@param alpha} of the back button in the navigation bar and {@param animate} if
     * needed from current value
     * @deprecated
     */
    @Deprecated
    @Override public void setBackButtonAlpha(float alpha, boolean animate) throws android.os.RemoteException
    {
    }
    /**
     * Control the {@param alpha} of the option nav bar button (back-button in 2 button mode
     * and home bar in no-button mode) and {@param animate} if needed from current value
     */
    @Override public void setNavBarButtonAlpha(float alpha, boolean animate) throws android.os.RemoteException
    {
    }
    /**
     * Proxies motion events from the homescreen UI to the status bar. Only called when
     * swipe down is detected on WORKSPACE. The sender guarantees the following order of events on
     * the tracking pointer.
     * 
     * Normal gesture: DOWN, MOVE/POINTER_DOWN/POINTER_UP)*, UP or CANCLE
     */
    @Override public void onStatusBarMotionEvent(android.view.MotionEvent event) throws android.os.RemoteException
    {
    }
    /** Proxies the assistant gesture's progress started from navigation bar. */
    @Override public void onAssistantProgress(float progress) throws android.os.RemoteException
    {
    }
    /**
     * Proxies the assistant gesture fling velocity (in pixels per millisecond) upon completion.
     * Velocity is 0 for drag gestures.
     */
    @Override public void onAssistantGestureCompletion(float velocity) throws android.os.RemoteException
    {
    }
    /** Start the assistant. */
    @Override public void startAssistant(android.os.Bundle bundle) throws android.os.RemoteException
    {
    }
    /** Creates a new gesture monitor */
    @Override public android.os.Bundle monitorGestureInput(java.lang.String name, int displayId) throws android.os.RemoteException
    {
      return null;
    }
    /** Notifies that the accessibility button in the system's navigation area has been clicked */
    @Override public void notifyAccessibilityButtonClicked(int displayId) throws android.os.RemoteException
    {
    }
    /** Notifies that the accessibility button in the system's navigation area has been long clicked */
    @Override public void notifyAccessibilityButtonLongClicked() throws android.os.RemoteException
    {
    }
    /** Ends the system screen pinning. */
    @Override public void stopScreenPinning() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.android.systemui.shared.recents.ISystemUiProxy
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.android.systemui.shared.recents.ISystemUiProxy interface,
     * generating a proxy if needed.
     */
    public static com.android.systemui.shared.recents.ISystemUiProxy asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.android.systemui.shared.recents.ISystemUiProxy))) {
        return ((com.android.systemui.shared.recents.ISystemUiProxy)iin);
      }
      return new com.android.systemui.shared.recents.ISystemUiProxy.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(DESCRIPTOR);
      }
      switch (code)
      {
        case TRANSACTION_startScreenPinning:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.startScreenPinning(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onSplitScreenInvoked:
        {
          this.onSplitScreenInvoked();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onOverviewShown:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.onOverviewShown(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getNonMinimizedSplitScreenSecondaryBounds:
        {
          android.graphics.Rect _result = this.getNonMinimizedSplitScreenSecondaryBounds();
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setBackButtonAlpha:
        {
          float _arg0;
          _arg0 = data.readFloat();
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          this.setBackButtonAlpha(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setNavBarButtonAlpha:
        {
          float _arg0;
          _arg0 = data.readFloat();
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          this.setNavBarButtonAlpha(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onStatusBarMotionEvent:
        {
          android.view.MotionEvent _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.view.MotionEvent.CREATOR);
          this.onStatusBarMotionEvent(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAssistantProgress:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.onAssistantProgress(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAssistantGestureCompletion:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.onAssistantGestureCompletion(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_startAssistant:
        {
          android.os.Bundle _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.os.Bundle.CREATOR);
          this.startAssistant(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_monitorGestureInput:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          android.os.Bundle _result = this.monitorGestureInput(_arg0, _arg1);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_notifyAccessibilityButtonClicked:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.notifyAccessibilityButtonClicked(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_notifyAccessibilityButtonLongClicked:
        {
          this.notifyAccessibilityButtonLongClicked();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopScreenPinning:
        {
          this.stopScreenPinning();
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static final class Proxy implements com.android.systemui.shared.recents.ISystemUiProxy
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
      public final java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
       * Proxies SurfaceControl.screenshotToBuffer().
       * @Removed
       * GraphicBufferCompat screenshot(in Rect sourceCrop, int width, int height, int minLayer,
       *             int maxLayer, boolean useIdentityTransform, int rotation) = 0;
       */
      /** Begins screen pinning on the provided {@param taskId}. */
      @Override public void startScreenPinning(int taskId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(taskId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startScreenPinning, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Notifies SystemUI that split screen has been invoked. */
      @Override public void onSplitScreenInvoked() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSplitScreenInvoked, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Notifies SystemUI that Overview is shown. */
      @Override public void onOverviewShown(boolean fromHome) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((fromHome)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onOverviewShown, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Get the secondary split screen app's rectangle when not minimized. */
      @Override public android.graphics.Rect getNonMinimizedSplitScreenSecondaryBounds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.graphics.Rect _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNonMinimizedSplitScreenSecondaryBounds, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, android.graphics.Rect.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Control the {@param alpha} of the back button in the navigation bar and {@param animate} if
       * needed from current value
       * @deprecated
       */
      @Deprecated
      @Override public void setBackButtonAlpha(float alpha, boolean animate) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(alpha);
          _data.writeInt(((animate)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_setBackButtonAlpha, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Control the {@param alpha} of the option nav bar button (back-button in 2 button mode
       * and home bar in no-button mode) and {@param animate} if needed from current value
       */
      @Override public void setNavBarButtonAlpha(float alpha, boolean animate) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(alpha);
          _data.writeInt(((animate)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_setNavBarButtonAlpha, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Proxies motion events from the homescreen UI to the status bar. Only called when
       * swipe down is detected on WORKSPACE. The sender guarantees the following order of events on
       * the tracking pointer.
       * 
       * Normal gesture: DOWN, MOVE/POINTER_DOWN/POINTER_UP)*, UP or CANCLE
       */
      @Override public void onStatusBarMotionEvent(android.view.MotionEvent event) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, event, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onStatusBarMotionEvent, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Proxies the assistant gesture's progress started from navigation bar. */
      @Override public void onAssistantProgress(float progress) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(progress);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAssistantProgress, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Proxies the assistant gesture fling velocity (in pixels per millisecond) upon completion.
       * Velocity is 0 for drag gestures.
       */
      @Override public void onAssistantGestureCompletion(float velocity) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(velocity);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAssistantGestureCompletion, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Start the assistant. */
      @Override public void startAssistant(android.os.Bundle bundle) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, bundle, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startAssistant, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Creates a new gesture monitor */
      @Override public android.os.Bundle monitorGestureInput(java.lang.String name, int displayId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.Bundle _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(name);
          _data.writeInt(displayId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_monitorGestureInput, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, android.os.Bundle.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Notifies that the accessibility button in the system's navigation area has been clicked */
      @Override public void notifyAccessibilityButtonClicked(int displayId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(displayId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_notifyAccessibilityButtonClicked, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Notifies that the accessibility button in the system's navigation area has been long clicked */
      @Override public void notifyAccessibilityButtonLongClicked() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_notifyAccessibilityButtonLongClicked, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Ends the system screen pinning. */
      @Override public void stopScreenPinning() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopScreenPinning, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_startScreenPinning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onSplitScreenInvoked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onOverviewShown = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getNonMinimizedSplitScreenSecondaryBounds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setBackButtonAlpha = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setNavBarButtonAlpha = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_onStatusBarMotionEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onAssistantProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onAssistantGestureCompletion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_startAssistant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_monitorGestureInput = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_notifyAccessibilityButtonClicked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_notifyAccessibilityButtonLongClicked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_stopScreenPinning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.android.systemui.shared.recents.ISystemUiProxy";
  /**
   * Proxies SurfaceControl.screenshotToBuffer().
   * @Removed
   * GraphicBufferCompat screenshot(in Rect sourceCrop, int width, int height, int minLayer,
   *             int maxLayer, boolean useIdentityTransform, int rotation) = 0;
   */
  /** Begins screen pinning on the provided {@param taskId}. */
  public void startScreenPinning(int taskId) throws android.os.RemoteException;
  /** Notifies SystemUI that split screen has been invoked. */
  public void onSplitScreenInvoked() throws android.os.RemoteException;
  /** Notifies SystemUI that Overview is shown. */
  public void onOverviewShown(boolean fromHome) throws android.os.RemoteException;
  /** Get the secondary split screen app's rectangle when not minimized. */
  public android.graphics.Rect getNonMinimizedSplitScreenSecondaryBounds() throws android.os.RemoteException;
  /**
   * Control the {@param alpha} of the back button in the navigation bar and {@param animate} if
   * needed from current value
   * @deprecated
   */
  @Deprecated
  public void setBackButtonAlpha(float alpha, boolean animate) throws android.os.RemoteException;
  /**
   * Control the {@param alpha} of the option nav bar button (back-button in 2 button mode
   * and home bar in no-button mode) and {@param animate} if needed from current value
   */
  public void setNavBarButtonAlpha(float alpha, boolean animate) throws android.os.RemoteException;
  /**
   * Proxies motion events from the homescreen UI to the status bar. Only called when
   * swipe down is detected on WORKSPACE. The sender guarantees the following order of events on
   * the tracking pointer.
   * 
   * Normal gesture: DOWN, MOVE/POINTER_DOWN/POINTER_UP)*, UP or CANCLE
   */
  public void onStatusBarMotionEvent(android.view.MotionEvent event) throws android.os.RemoteException;
  /** Proxies the assistant gesture's progress started from navigation bar. */
  public void onAssistantProgress(float progress) throws android.os.RemoteException;
  /**
   * Proxies the assistant gesture fling velocity (in pixels per millisecond) upon completion.
   * Velocity is 0 for drag gestures.
   */
  public void onAssistantGestureCompletion(float velocity) throws android.os.RemoteException;
  /** Start the assistant. */
  public void startAssistant(android.os.Bundle bundle) throws android.os.RemoteException;
  /** Creates a new gesture monitor */
  public android.os.Bundle monitorGestureInput(java.lang.String name, int displayId) throws android.os.RemoteException;
  /** Notifies that the accessibility button in the system's navigation area has been clicked */
  public void notifyAccessibilityButtonClicked(int displayId) throws android.os.RemoteException;
  /** Notifies that the accessibility button in the system's navigation area has been long clicked */
  public void notifyAccessibilityButtonLongClicked() throws android.os.RemoteException;
  /** Ends the system screen pinning. */
  public void stopScreenPinning() throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}
