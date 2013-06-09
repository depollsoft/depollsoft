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
  public partial class TeachingVideoPreview : UserControl {
    public TeachingVideoPreview() {
      InitializeComponent();
      LayoutRoot.DataContext = this;
    }

    public Tag TargetTag {
      get { return (Tag)GetValue(TargetTagProperty); }
      set { SetValue(TargetTagProperty, value); }
    }

    public static readonly DependencyProperty TargetTagProperty =
        DependencyProperty.Register("TargetTag", typeof(Tag), typeof(TeachingVideoPreview), new PropertyMetadata(default(Tag), OnTargetTagChanged));

    private static void OnTargetTagChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TeachingVideoPreview)obj).OnTargetTagChanged((Tag)args.OldValue, (Tag)args.NewValue);
    }

    private void OnTargetTagChanged(Tag oldValue, Tag newValue) {
      ThumbnailUri = new Uri(string.Format("http://img.youtube.com/vi/{0}/2.jpg", newValue.TeachingVideo), UriKind.Absolute);
      WatchUri = new Uri(string.Format("http://www.youtube.com/watch?v={0}", newValue.TeachingVideo), UriKind.Absolute);
    }


    public Uri ThumbnailUri {
      get { return (Uri)GetValue(ThumbnailUriProperty); }
      private set { SetValue(ThumbnailUriProperty, value); }
    }

    public static readonly DependencyProperty ThumbnailUriProperty =
        DependencyProperty.Register("ThumbnailUri", typeof(Uri), typeof(TeachingVideoPreview), new PropertyMetadata(default(Uri), OnThumbnailUriChanged));

    private static void OnThumbnailUriChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TeachingVideoPreview)obj).OnThumbnailUriChanged((Uri)args.OldValue, (Uri)args.NewValue);
    }

    private void OnThumbnailUriChanged(Uri oldValue, Uri newValue) {
    }


    public Uri WatchUri {
      get { return (Uri)GetValue(WatchUriProperty); }
      set { SetValue(WatchUriProperty, value); }
    }

    public static readonly DependencyProperty WatchUriProperty =
        DependencyProperty.Register("WatchUri", typeof(Uri), typeof(TeachingVideoPreview), new PropertyMetadata(default(Uri), OnWatchUriChanged));

    private static void OnWatchUriChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TeachingVideoPreview)obj).OnWatchUriChanged((Uri)args.OldValue, (Uri)args.NewValue);
    }

    private void OnWatchUriChanged(Uri oldValue, Uri newValue) {
    }
  }
}
