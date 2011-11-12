package depollsoft.pitchperfect.lib.ui;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.util.ReflectedProperty;
import depollsoft.pitchperfect.lib.Note;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

public class PitchPipeButton extends Button
{

   private TrackableField<Note> note = new TrackableField<Note>();

   private TrackableField<Boolean> isToggle = new TrackableField<Boolean>(false);

   public PitchPipeButton(Context context)
   {
      super(context);
   }

   public PitchPipeButton(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   public PitchPipeButton(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
   }

   public boolean getIsToggle()
   {
      return this.isToggle.getValue();
   }

   public Note getNote()
   {
      return this.note.getValue();
   }

   @Override
   protected void onAttachedToWindow()
   {
      super.onAttachedToWindow();
      UiBinder.bind(this, new ReflectedProperty(this, "Pressed"),
            "Note.IsPlaying", BindingMode.OneWay);
   }

   @Override
   protected void onDetachedFromWindow()
   {
      UiBinder.unbind(this);
      super.onDetachedFromWindow();
   }

   @Override
   public boolean onTouchEvent(MotionEvent event)
   {
      if (this.getNote() != null)
      {
         switch (event.getAction())
         {
         case MotionEvent.ACTION_DOWN:
            if (this.getIsToggle())
            {
               this.getNote().setIsPlaying(!this.getNote().getIsPlaying());
               return true;
            }
            else
               this.getNote().play();
            break;
         case MotionEvent.ACTION_UP:
         case MotionEvent.ACTION_OUTSIDE:
         case MotionEvent.ACTION_CANCEL:
            if (!this.getIsToggle())
               this.getNote().stop();
            else
               return true;
            break;
         }
      }
      return super.onTouchEvent(event);
   }

   // @Override
   // public boolean onKeyDown(int keyCode, KeyEvent event)
   // {
   // if (this.getNote() != null)
   // this.getNote().play();
   // return super.onKeyDown(keyCode, event);
   // }
   //
   // @Override
   // public boolean onKeyUp(int keyCode, KeyEvent event)
   // {
   // if (this.getNote() != null)
   // this.getNote().stop();
   // return super.onKeyUp(keyCode, event);
   // }

   public void setIsToggle(boolean value)
   {
      this.isToggle.setValue(value);
   }

   public void setNote(Note value)
   {
      this.note.setValue(value);
   }

}
