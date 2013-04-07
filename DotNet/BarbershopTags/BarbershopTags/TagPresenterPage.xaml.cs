using System;
using System.Linq;
using System.Windows;
using System.Windows.Navigation;
using BarbershopTags.Barbershop;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using SLaB.Utilities;
using System.ComponentModel;
using System.Windows.Controls;
using System.Windows.Input;
using Microsoft.Phone.Tasks;

namespace BarbershopTags {
  public partial class TagPresenterPage : PhoneApplicationPage {
    private bool _Loaded;
    private Tag _Tag;
    public TagPresenterPage() {
      InitializeComponent();
      this.BusyIndicator.DataContext = this;
    }

    protected override void OnNavigatedTo(NavigationEventArgs e) {
      if (!_Loaded)
        LoadQueryItem();
      this.AppBarFavorites = ApplicationBar.Buttons[0] as ApplicationBarIconButton;
      this.AppBarFavorites.IsEnabled = !FavoritesModel.IsFavorite(int.Parse(NavigationContext.QueryString["id"]));
      this.AppBarAddTeachableTag = ApplicationBar.MenuItems[0] as ApplicationBarMenuItem;
      this.AppBarAddTeachableTag.IsEnabled = !TeachableTagsModel.IsTeachable(int.Parse(NavigationContext.QueryString["id"]));
      CanRate = !RatingsModel.IsRated(int.Parse(NavigationContext.QueryString["id"]));
    }


    public string StatusText {
      get { return (string)GetValue(StatusTextProperty); }
      set { SetValue(StatusTextProperty, value); }
    }

    public static readonly DependencyProperty StatusTextProperty =
        DependencyProperty.Register("StatusText", typeof(string), typeof(TagPresenterPage), new PropertyMetadata(default(string), OnStatusTextChanged));

    private static void OnStatusTextChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TagPresenterPage)obj).OnStatusTextChanged((string)args.OldValue, (string)args.NewValue);
    }

    private void OnStatusTextChanged(string oldValue, string newValue) {
    }

    public bool IsLoading {
      get { return (bool)GetValue(IsLoadingProperty); }
      set { SetValue(IsLoadingProperty, value); }
    }

    public static readonly DependencyProperty IsLoadingProperty =
        DependencyProperty.Register("IsLoading", typeof(bool), typeof(TagPresenterPage), new PropertyMetadata(default(bool), OnIsLoadingChanged));

    private static void OnIsLoadingChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TagPresenterPage)obj).OnIsLoadingChanged((bool)args.OldValue, (bool)args.NewValue);
    }

    private void OnIsLoadingChanged(bool oldValue, bool newValue) {
    }


    public Track SelectedTrack {
      get { return (Track)GetValue(SelectedTrackProperty); }
      set { SetValue(SelectedTrackProperty, value); }
    }

    public static readonly DependencyProperty SelectedTrackProperty =
        DependencyProperty.Register("SelectedTrack", typeof(Track), typeof(TagPresenterPage), new PropertyMetadata(default(Track), OnSelectedTrackChanged));

    private static void OnSelectedTrackChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TagPresenterPage)obj).OnSelectedTrackChanged((Track)args.OldValue, (Track)args.NewValue);
    }

    private void OnSelectedTrackChanged(Track oldValue, Track newValue) {
    }

    private void LoadQueryItem(bool refresh = false) {
      IsLoading = true;
      StatusText = "Loading...";
      SelectedTrack = null;
      Barbershop.Tag.LoadTagById(int.Parse(this.NavigationContext.QueryString["id"]), refresh).Continue(tag => UiUtilities.ExecuteOnUiThread(() => {
            LayoutRoot.DataContext = _Tag = tag;
            //SelectedTrack = tag.Tracks.FirstOrDefault();
            IsLoading = false;
            _Loaded = true;
          }),
          err => UiUtilities.ExecuteOnUiThread(() => {
                StatusText = "An error has occurred.  Refresh to try again or hit the back button to choose a different tag.";
              }));
    }

    private void AppBarRefreshClick(object sender, EventArgs e) {
      if (IsLoading)
        return;
      LoadQueryItem(true);
    }

    private void AppBarFavoriteClick(object sender, EventArgs e) {
      FavoritesModel.AddFavorite(int.Parse(this.NavigationContext.QueryString["id"]));
      this.AppBarFavorites.IsEnabled = false;
    }

    private void MediaFailed(object sender, ErrorEventArgs e) {
      if (e.Error is NotSupportedException)
        MessageBox.Show("Track is in an unsupported format and cannot be played in this application.");
      else
        MessageBox.Show("Track failed to load.");
    }

    public bool IsRating {
      get { return (bool)GetValue(IsRatingProperty); }
      set { SetValue(IsRatingProperty, value); }
    }

    public static readonly DependencyProperty IsRatingProperty =
        DependencyProperty.Register("IsRating", typeof(bool), typeof(TagPresenterPage), new PropertyMetadata(default(bool), OnIsRatingChanged));

    private static void OnIsRatingChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TagPresenterPage)obj).OnIsRatingChanged((bool)args.OldValue, (bool)args.NewValue);
    }

    private void OnIsRatingChanged(bool oldValue, bool newValue) {
    }


    public bool CanRate {
      get { return (bool)GetValue(CanRateProperty); }
      set { SetValue(CanRateProperty, value); }
    }

    public static readonly DependencyProperty CanRateProperty =
        DependencyProperty.Register("CanRate", typeof(bool), typeof(TagPresenterPage), new PropertyMetadata(default(bool), OnCanRateChanged));

    private static void OnCanRateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TagPresenterPage)obj).OnCanRateChanged((bool)args.OldValue, (bool)args.NewValue);
    }

    private void OnCanRateChanged(bool oldValue, bool newValue) {
    }

    protected override void OnBackKeyPress(CancelEventArgs e) {
      if (RatingPopup.Visibility == Visibility.Visible) {
        RatingPopup.Visibility = Visibility.Collapsed;
        e.Cancel = true;
      }
    }

    private void RatingSelected(object sender, SelectionChangedEventArgs e) {
      var ratingPicker = sender as ListBox;
      IsRating = true;
      _Tag.Rate((int)ratingPicker.SelectedItem).Continue(val => {
            UiUtilities.ExecuteOnUiThread(() => {
                  if (val) {
                    RatingsModel.AddRating(_Tag.Id);
                    CanRate = false;
                  } else
                    MessageBox.Show("Failed to submit rating.  Please try again later.", "Failure", MessageBoxButton.OK);
                  RatingPopup.Visibility = Visibility.Collapsed;
                  IsRating = false;
                });
          },
          ex => {
            UiUtilities.ExecuteOnUiThread(() => {
                  MessageBox.Show("Failed to submit rating.  Please try again later.", "Failure", MessageBoxButton.OK);
                  RatingPopup.Visibility = Visibility.Collapsed;
                  IsRating = false;
                });
          });

    }

    private void RateClick(object sender, RoutedEventArgs e) {
      RatingPopup.Visibility = Visibility.Visible;
    }

    private void RatingStatusLoaded(object sender, RoutedEventArgs e) {
      ((FrameworkElement)sender).DataContext = this;
    }

    private void RatingBackgroundClick(object sender, MouseButtonEventArgs e) {
      RatingPopup.Visibility = Visibility.Collapsed;
    }

    private void AppBarEmailClick(object sender, EventArgs e) {
      EmailComposeTask ect = new EmailComposeTask();
      ect.Subject = _Tag.Title + " - Tag Master for Windows Phone";
      ect.Body = string.Format("Tag Title: {0}\n{1}\n\n\nSent from Tag Master for Windows Phone\nhttp://www.davidpoll.com/applications/tag-master", _Tag.Title, _Tag.TagUri);
      ect.Show();
    }

    private void AppBarSmsClick(object sender, EventArgs e) {
      SmsComposeTask sct = new SmsComposeTask();
      sct.Body = string.Format("{0} {1} - Sent from Tag Master", _Tag.Title, _Tag.TagUri);
      sct.Show();
    }

    private void AppBarAddTeachableTagClick(object sender, EventArgs e) {
      TeachableTagsModel.AddTeachable(int.Parse(this.NavigationContext.QueryString["id"]));
      this.AppBarAddTeachableTag.IsEnabled = false;
    }
  }
}