using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using BarbershopTags.Barbershop;

namespace BarbershopTags {
  public partial class VideoPreview : UserControl {
    public VideoPreview() {
      InitializeComponent();
      LayoutRoot.DataContext = this;
    }


    public Video Video {
      get { return (Video)GetValue(VideoProperty); }
      set { SetValue(VideoProperty, value); }
    }

    public static readonly DependencyProperty VideoProperty =
        DependencyProperty.Register("Video", typeof(Video), typeof(VideoPreview), new PropertyMetadata(default(Video), OnVideoChanged));

    private static void OnVideoChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((VideoPreview)obj).OnVideoChanged((Video)args.OldValue, (Video)args.NewValue);
    }

    private void OnVideoChanged(Video oldValue, Video newValue) {
      ThumbnailUri = new Uri(string.Format("http://img.youtube.com/vi/{0}/2.jpg", newValue.YouTubeCode), UriKind.Absolute);
      WatchUri = new Uri(string.Format("http://www.youtube.com/watch?v={0}", newValue.YouTubeCode), UriKind.Absolute);
    }


    public Uri ThumbnailUri {
      get { return (Uri)GetValue(ThumbnailUriProperty); }
      private set { SetValue(ThumbnailUriProperty, value); }
    }

    public static readonly DependencyProperty ThumbnailUriProperty =
        DependencyProperty.Register("ThumbnailUri", typeof(Uri), typeof(VideoPreview), new PropertyMetadata(default(Uri), OnThumbnailUriChanged));

    private static void OnThumbnailUriChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((VideoPreview)obj).OnThumbnailUriChanged((Uri)args.OldValue, (Uri)args.NewValue);
    }

    private void OnThumbnailUriChanged(Uri oldValue, Uri newValue) {
    }


    public Uri WatchUri {
      get { return (Uri)GetValue(WatchUriProperty); }
      set { SetValue(WatchUriProperty, value); }
    }

    public static readonly DependencyProperty WatchUriProperty =
        DependencyProperty.Register("WatchUri", typeof(Uri), typeof(VideoPreview), new PropertyMetadata(default(Uri), OnWatchUriChanged));

    private static void OnWatchUriChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((VideoPreview)obj).OnWatchUriChanged((Uri)args.OldValue, (Uri)args.NewValue);
    }

    private void OnWatchUriChanged(Uri oldValue, Uri newValue) {
    }
  }
}
