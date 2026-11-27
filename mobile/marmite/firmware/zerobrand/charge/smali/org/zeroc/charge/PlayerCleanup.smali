.class public Lorg/zeroc/charge/PlayerCleanup;
.super Ljava/lang/Object;
.implements Landroid/media/MediaPlayer$OnCompletionListener;
.implements Landroid/media/MediaPlayer$OnErrorListener;

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public onCompletion(Landroid/media/MediaPlayer;)V
    .locals 0

    invoke-static {}, Lorg/zeroc/charge/PowerReceiver;->releasePlayer()V

    return-void
.end method

.method public onError(Landroid/media/MediaPlayer;II)Z
    .locals 0

    invoke-static {}, Lorg/zeroc/charge/PowerReceiver;->releasePlayer()V

    const/4 p0, 0x0

    return p0
.end method
