/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: G:\\Android-sdk\\build-tools\\37.0.0\\aidl.exe -oC:\\work\\aidlq\\out -IC:\\work\\aidlq\\src -IC:\\work\\aidlq\\stub C:\\work\\aidlq\\src\\com\\android\\systemui\\shared\\recents\\IOverviewProxy.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.android.systemui.shared.recents;
public interface IOverviewProxy extends android.os.IInterface
{
  /** Default implementation for IOverviewProxy. */
  public static class Default implements com.android.systemui.shared.recents.IOverviewProxy
  {
    @Override public void onActiveNavBarRegionChanges(android.graphics.Region activeRegion) throws android.os.RemoteException
    {
    }
    @Override public void onInitialize(android.os.Bundle params) throws android.os.RemoteException
    {
    }
    /** @deprecated */
    @Deprecated
    @Override public void onBind(com.android.systemui.shared.recents.ISystemUiProxy sysUiProxy) throws android.os.RemoteException
    {
    }
    /**
     * Called once immediately prior to the first onMotionEvent() call, providing a hint to the
     * target the initial source of the subsequent motion events.
     * 
     * @param downHitTarget is one of the {@link NavigationBarCompat.HitTarget}s
     * 
     * @deprecated
     */
    @Deprecated
    @Override public void onPreMotionEvent(int downHitTarget) throws android.os.RemoteException
    {
    }
    /**
     * Proxies motion events from the nav bar in SystemUI to the OverviewProxyService. The sender
     * guarantees the following order of events:
     * 
     * Normal gesture: DOWN, (MOVE/POINTER_DOWN/POINTER_UP)*, UP
     * Quick scrub: DOWN, (MOVE/POINTER_DOWN/POINTER_UP)*, SCRUB_START, SCRUB_PROGRESS*, SCRUB_END
     * 
     * Once quick scrub is sent, then no further motion events will be provided.
     * 
     * @deprecated
     */
    @Deprecated
    @Override public void onMotionEvent(android.view.MotionEvent event) throws android.os.RemoteException
    {
    }
    /**
     * Sent when the user starts to actively scrub the nav bar to switch tasks. Once this event is
     * sent the caller will stop sending any motion events and will no longer preemptively cancel
     * any recents animations started as a part of the motion event handling.
     * 
     * @deprecated
     */
    @Deprecated
    @Override public void onQuickScrubStart() throws android.os.RemoteException
    {
    }
    /**
     * Sent when the user stops actively scrubbing the nav bar to switch tasks.
     * 
     * @deprecated
     */
    @Deprecated
    @Override public void onQuickScrubEnd() throws android.os.RemoteException
    {
    }
    /**
     * Sent for each movement over the nav bar while the user is scrubbing it to switch tasks.
     * 
     * @deprecated
     */
    @Deprecated
    @Override public void onQuickScrubProgress(float progress) throws android.os.RemoteException
    {
    }
    /** Sent when overview button is pressed to toggle show/hide of overview. */
    @Override public void onOverviewToggle() throws android.os.RemoteException
    {
    }
    /** Sent when overview is to be shown. */
    @Override public void onOverviewShown(boolean triggeredFromAltTab) throws android.os.RemoteException
    {
    }
    /** Sent when overview is to be hidden. */
    @Override public void onOverviewHidden(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws android.os.RemoteException
    {
    }
    /**
     * Sent when a user swipes up over the navigation bar to launch overview. Swipe up is determined
     * by passing the touch slop in the direction towards launcher from navigation bar. During and
     * after this event is sent the caller will continue to send motion events. The motion
     * {@param event} passed after the touch slop was exceeded will also be passed after by
     * {@link onMotionEvent}. Since motion events will be sent, motion up or cancel can still be
     * sent to cancel overview regardless the current state of launcher (eg. if overview is already
     * visible, this event will still be sent if user swipes up). When this signal is sent,
     * navigation bar will not handle any gestures such as quick scrub and the home button will
     * cancel (long) press.
     * 
     * @deprecated
     */
    @Deprecated
    @Override public void onQuickStep(android.view.MotionEvent event) throws android.os.RemoteException
    {
    }
    /** Sent when there was an action on one of the onboarding tips view. */
    @Override public void onTip(int actionType, int viewType) throws android.os.RemoteException
    {
    }
    /** Sent when device assistant changes its default assistant whether it is available or not. */
    @Override public void onAssistantAvailable(boolean available) throws android.os.RemoteException
    {
    }
    /** Sent when the assistant changes how visible it is to the user. */
    @Override public void onAssistantVisibilityChanged(float visibility) throws android.os.RemoteException
    {
    }
    /** Sent when back is triggered. */
    @Override public void onBackAction(boolean completed, int downX, int downY, boolean isButton, boolean gestureSwipeLeft) throws android.os.RemoteException
    {
    }
    /** Sent when some system ui state changes. */
    @Override public void onSystemUiStateChanged(int stateFlags) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.android.systemui.shared.recents.IOverviewProxy
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.android.systemui.shared.recents.IOverviewProxy interface,
     * generating a proxy if needed.
     */
    public static com.android.systemui.shared.recents.IOverviewProxy asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.android.systemui.shared.recents.IOverviewProxy))) {
        return ((com.android.systemui.shared.recents.IOverviewProxy)iin);
      }
      return new com.android.systemui.shared.recents.IOverviewProxy.Stub.Proxy(obj);
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
        case TRANSACTION_onActiveNavBarRegionChanges:
        {
          android.graphics.Region _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.graphics.Region.CREATOR);
          this.onActiveNavBarRegionChanges(_arg0);
          break;
        }
        case TRANSACTION_onInitialize:
        {
          android.os.Bundle _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.os.Bundle.CREATOR);
          this.onInitialize(_arg0);
          break;
        }
        case TRANSACTION_onBind:
        {
          com.android.systemui.shared.recents.ISystemUiProxy _arg0;
          _arg0 = com.android.systemui.shared.recents.ISystemUiProxy.Stub.asInterface(data.readStrongBinder());
          this.onBind(_arg0);
          break;
        }
        case TRANSACTION_onPreMotionEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onPreMotionEvent(_arg0);
          break;
        }
        case TRANSACTION_onMotionEvent:
        {
          android.view.MotionEvent _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.view.MotionEvent.CREATOR);
          this.onMotionEvent(_arg0);
          break;
        }
        case TRANSACTION_onQuickScrubStart:
        {
          this.onQuickScrubStart();
          break;
        }
        case TRANSACTION_onQuickScrubEnd:
        {
          this.onQuickScrubEnd();
          break;
        }
        case TRANSACTION_onQuickScrubProgress:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.onQuickScrubProgress(_arg0);
          break;
        }
        case TRANSACTION_onOverviewToggle:
        {
          this.onOverviewToggle();
          break;
        }
        case TRANSACTION_onOverviewShown:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.onOverviewShown(_arg0);
          break;
        }
        case TRANSACTION_onOverviewHidden:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          this.onOverviewHidden(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onQuickStep:
        {
          android.view.MotionEvent _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.view.MotionEvent.CREATOR);
          this.onQuickStep(_arg0);
          break;
        }
        case TRANSACTION_onTip:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.onTip(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAssistantAvailable:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.onAssistantAvailable(_arg0);
          break;
        }
        case TRANSACTION_onAssistantVisibilityChanged:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.onAssistantVisibilityChanged(_arg0);
          break;
        }
        case TRANSACTION_onBackAction:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          boolean _arg3;
          _arg3 = (0!=data.readInt());
          boolean _arg4;
          _arg4 = (0!=data.readInt());
          this.onBackAction(_arg0, _arg1, _arg2, _arg3, _arg4);
          break;
        }
        case TRANSACTION_onSystemUiStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onSystemUiStateChanged(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static final class Proxy implements com.android.systemui.shared.recents.IOverviewProxy
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
      @Override public void onActiveNavBarRegionChanges(android.graphics.Region activeRegion) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, activeRegion, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onActiveNavBarRegionChanges, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onInitialize(android.os.Bundle params) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onInitialize, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** @deprecated */
      @Deprecated
      @Override public void onBind(com.android.systemui.shared.recents.ISystemUiProxy sysUiProxy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(sysUiProxy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onBind, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Called once immediately prior to the first onMotionEvent() call, providing a hint to the
       * target the initial source of the subsequent motion events.
       * 
       * @param downHitTarget is one of the {@link NavigationBarCompat.HitTarget}s
       * 
       * @deprecated
       */
      @Deprecated
      @Override public void onPreMotionEvent(int downHitTarget) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(downHitTarget);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPreMotionEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Proxies motion events from the nav bar in SystemUI to the OverviewProxyService. The sender
       * guarantees the following order of events:
       * 
       * Normal gesture: DOWN, (MOVE/POINTER_DOWN/POINTER_UP)*, UP
       * Quick scrub: DOWN, (MOVE/POINTER_DOWN/POINTER_UP)*, SCRUB_START, SCRUB_PROGRESS*, SCRUB_END
       * 
       * Once quick scrub is sent, then no further motion events will be provided.
       * 
       * @deprecated
       */
      @Deprecated
      @Override public void onMotionEvent(android.view.MotionEvent event) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, event, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onMotionEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Sent when the user starts to actively scrub the nav bar to switch tasks. Once this event is
       * sent the caller will stop sending any motion events and will no longer preemptively cancel
       * any recents animations started as a part of the motion event handling.
       * 
       * @deprecated
       */
      @Deprecated
      @Override public void onQuickScrubStart() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onQuickScrubStart, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Sent when the user stops actively scrubbing the nav bar to switch tasks.
       * 
       * @deprecated
       */
      @Deprecated
      @Override public void onQuickScrubEnd() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onQuickScrubEnd, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Sent for each movement over the nav bar while the user is scrubbing it to switch tasks.
       * 
       * @deprecated
       */
      @Deprecated
      @Override public void onQuickScrubProgress(float progress) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(progress);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onQuickScrubProgress, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when overview button is pressed to toggle show/hide of overview. */
      @Override public void onOverviewToggle() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onOverviewToggle, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when overview is to be shown. */
      @Override public void onOverviewShown(boolean triggeredFromAltTab) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((triggeredFromAltTab)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onOverviewShown, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when overview is to be hidden. */
      @Override public void onOverviewHidden(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((triggeredFromAltTab)?(1):(0)));
          _data.writeInt(((triggeredFromHomeKey)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onOverviewHidden, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Sent when a user swipes up over the navigation bar to launch overview. Swipe up is determined
       * by passing the touch slop in the direction towards launcher from navigation bar. During and
       * after this event is sent the caller will continue to send motion events. The motion
       * {@param event} passed after the touch slop was exceeded will also be passed after by
       * {@link onMotionEvent}. Since motion events will be sent, motion up or cancel can still be
       * sent to cancel overview regardless the current state of launcher (eg. if overview is already
       * visible, this event will still be sent if user swipes up). When this signal is sent,
       * navigation bar will not handle any gestures such as quick scrub and the home button will
       * cancel (long) press.
       * 
       * @deprecated
       */
      @Deprecated
      @Override public void onQuickStep(android.view.MotionEvent event) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, event, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onQuickStep, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when there was an action on one of the onboarding tips view. */
      @Override public void onTip(int actionType, int viewType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(actionType);
          _data.writeInt(viewType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onTip, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when device assistant changes its default assistant whether it is available or not. */
      @Override public void onAssistantAvailable(boolean available) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((available)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAssistantAvailable, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when the assistant changes how visible it is to the user. */
      @Override public void onAssistantVisibilityChanged(float visibility) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(visibility);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAssistantVisibilityChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when back is triggered. */
      @Override public void onBackAction(boolean completed, int downX, int downY, boolean isButton, boolean gestureSwipeLeft) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((completed)?(1):(0)));
          _data.writeInt(downX);
          _data.writeInt(downY);
          _data.writeInt(((isButton)?(1):(0)));
          _data.writeInt(((gestureSwipeLeft)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onBackAction, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Sent when some system ui state changes. */
      @Override public void onSystemUiStateChanged(int stateFlags) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(stateFlags);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSystemUiStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onActiveNavBarRegionChanges = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_onInitialize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onBind = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onPreMotionEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onMotionEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onQuickScrubStart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onQuickScrubEnd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onQuickScrubProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onOverviewToggle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onOverviewShown = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_onOverviewHidden = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onQuickStep = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onTip = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_onAssistantAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_onAssistantVisibilityChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_onBackAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_onSystemUiStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.android.systemui.shared.recents.IOverviewProxy";
  public void onActiveNavBarRegionChanges(android.graphics.Region activeRegion) throws android.os.RemoteException;
  public void onInitialize(android.os.Bundle params) throws android.os.RemoteException;
  /** @deprecated */
  @Deprecated
  public void onBind(com.android.systemui.shared.recents.ISystemUiProxy sysUiProxy) throws android.os.RemoteException;
  /**
   * Called once immediately prior to the first onMotionEvent() call, providing a hint to the
   * target the initial source of the subsequent motion events.
   * 
   * @param downHitTarget is one of the {@link NavigationBarCompat.HitTarget}s
   * 
   * @deprecated
   */
  @Deprecated
  public void onPreMotionEvent(int downHitTarget) throws android.os.RemoteException;
  /**
   * Proxies motion events from the nav bar in SystemUI to the OverviewProxyService. The sender
   * guarantees the following order of events:
   * 
   * Normal gesture: DOWN, (MOVE/POINTER_DOWN/POINTER_UP)*, UP
   * Quick scrub: DOWN, (MOVE/POINTER_DOWN/POINTER_UP)*, SCRUB_START, SCRUB_PROGRESS*, SCRUB_END
   * 
   * Once quick scrub is sent, then no further motion events will be provided.
   * 
   * @deprecated
   */
  @Deprecated
  public void onMotionEvent(android.view.MotionEvent event) throws android.os.RemoteException;
  /**
   * Sent when the user starts to actively scrub the nav bar to switch tasks. Once this event is
   * sent the caller will stop sending any motion events and will no longer preemptively cancel
   * any recents animations started as a part of the motion event handling.
   * 
   * @deprecated
   */
  @Deprecated
  public void onQuickScrubStart() throws android.os.RemoteException;
  /**
   * Sent when the user stops actively scrubbing the nav bar to switch tasks.
   * 
   * @deprecated
   */
  @Deprecated
  public void onQuickScrubEnd() throws android.os.RemoteException;
  /**
   * Sent for each movement over the nav bar while the user is scrubbing it to switch tasks.
   * 
   * @deprecated
   */
  @Deprecated
  public void onQuickScrubProgress(float progress) throws android.os.RemoteException;
  /** Sent when overview button is pressed to toggle show/hide of overview. */
  public void onOverviewToggle() throws android.os.RemoteException;
  /** Sent when overview is to be shown. */
  public void onOverviewShown(boolean triggeredFromAltTab) throws android.os.RemoteException;
  /** Sent when overview is to be hidden. */
  public void onOverviewHidden(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws android.os.RemoteException;
  /**
   * Sent when a user swipes up over the navigation bar to launch overview. Swipe up is determined
   * by passing the touch slop in the direction towards launcher from navigation bar. During and
   * after this event is sent the caller will continue to send motion events. The motion
   * {@param event} passed after the touch slop was exceeded will also be passed after by
   * {@link onMotionEvent}. Since motion events will be sent, motion up or cancel can still be
   * sent to cancel overview regardless the current state of launcher (eg. if overview is already
   * visible, this event will still be sent if user swipes up). When this signal is sent,
   * navigation bar will not handle any gestures such as quick scrub and the home button will
   * cancel (long) press.
   * 
   * @deprecated
   */
  @Deprecated
  public void onQuickStep(android.view.MotionEvent event) throws android.os.RemoteException;
  /** Sent when there was an action on one of the onboarding tips view. */
  public void onTip(int actionType, int viewType) throws android.os.RemoteException;
  /** Sent when device assistant changes its default assistant whether it is available or not. */
  public void onAssistantAvailable(boolean available) throws android.os.RemoteException;
  /** Sent when the assistant changes how visible it is to the user. */
  public void onAssistantVisibilityChanged(float visibility) throws android.os.RemoteException;
  /** Sent when back is triggered. */
  public void onBackAction(boolean completed, int downX, int downY, boolean isButton, boolean gestureSwipeLeft) throws android.os.RemoteException;
  /** Sent when some system ui state changes. */
  public void onSystemUiStateChanged(int stateFlags) throws android.os.RemoteException;
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
