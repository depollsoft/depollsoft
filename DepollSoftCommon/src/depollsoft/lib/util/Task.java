package depollsoft.lib.util;

import java.util.ArrayList;
import java.util.List;

public class Task<T>
{
   public static class TaskSource<T>
   {
      private T result;
      private Exception error;
      private boolean isFinished;
      private Task<T> task;

      public Exception getError()
      {
         return this.error;
      }

      public T getResult()
      {
         return this.result;
      }

      public void setError(Exception value)
      {
         this.error = value;
         this.isFinished = true;
         if (this.task != null)
            this.task.finish();
      }

      public void setResult(T value)
      {
         this.result = value;
         this.isFinished = true;
         if (this.task != null)
            this.task.finish();
      }
   }

   private Object continuationsLock = new Object();
   private List<Action<T>> continuations;
   private List<Action<Exception>> errors;
   private TaskSource<T> source;

   public Task(TaskSource<T> source)
   {
      this.continuations = new ArrayList<Action<T>>();
      this.errors = new ArrayList<Action<Exception>>();
      this.source = source;
      this.source.task = this;
      if (source.isFinished)
         this.finish();
   }

   public void continueWith(Action<T> continuation, Action<Exception> error)
   {
      synchronized (this.continuationsLock)
      {
         if (continuation != null)
            this.continuations.add(continuation);
         if (error != null)
            this.errors.add(error);
      }
      if (this.source.isFinished)
         this.finish();
   }

   public void finish()
   {
      synchronized (this.continuationsLock)
      {
         try
         {
            if (this.source.getError() != null)
            {
               for (Action<Exception> error : this.errors)
               {
                  error.invoke(this.source.getError());
               }
               return;
            }
            for (Action<T> continuation : this.continuations)
            {
               continuation.invoke(this.source.getResult());
            }
         }
         finally
         {
            this.continuations.clear();
            this.errors.clear();
         }
      }
   }

   public T waitFor()
   {
      while (!this.source.isFinished);
      return this.source.result;
   }
}
