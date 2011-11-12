package depollsoft.lib.ui;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import depollsoft.lib.binding.TrackableField;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.widget.ImageView;

public class LoadingImageView extends ImageView
{

   private TrackableField<String> source = new TrackableField<String>();

   public LoadingImageView(Context context)
   {
      super(context);
   }

   public LoadingImageView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   public LoadingImageView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
   }

   private void fetchSource()
   {
      Thread t = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               final String currentSource = LoadingImageView.this.getSource();
               final Bitmap bitmap = BitmapFactory
                     .decodeStream((InputStream) new URL(LoadingImageView.this
                           .getSource()).getContent());
               LoadingImageView.this.post(new Runnable()
               {

                  public void run()
                  {
                     if (currentSource == LoadingImageView.this.getSource())
                        LoadingImageView.this.setImageBitmap(bitmap);
                  }
               });

            }
            catch (MalformedURLException e)
            {
               e.printStackTrace();
            }
            catch (IOException e)
            {
               e.printStackTrace();
            }
         }
      };
      t.start();
   }

   public String getSource()
   {
      return this.source.getValue();
   }

   public void setSource(String value)
   {
      this.source.setValue(value);
      if (value != null)
         this.fetchSource();
   }

}
