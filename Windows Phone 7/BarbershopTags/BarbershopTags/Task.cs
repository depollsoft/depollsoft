using System;

namespace BarbershopTags {
  public class Task<T> {
    private object _ContinuationsLock = new object();
    private Action<T> _Continuations;
    private Action<Exception> _Errors;
    private TaskSource _Source;
    public Task(TaskSource source) {
      _Source = source;
      source.Task = this;
      if (source.IsFinished)
        Finish();
    }

    public void Continue(Action<T> continuation, Action<Exception> error = null) {
      lock (_ContinuationsLock) {
        _Continuations += continuation;
        _Errors += error;
      }
      if (_Source.IsFinished)
        Finish();
    }

    private void Finish() {
      lock (_ContinuationsLock) {
        try {
          if (_Source.Error != null)
            if (_Errors != null) {
              _Errors(_Source.Error);
              return;
            }
          if (_Continuations != null)
            _Continuations(_Source.Result);
        } finally {
          _Continuations = null;
          _Errors = null;
        }
      }
    }

    public class TaskSource {
      private T _Result;
      private Exception _Error;
      internal bool IsFinished { get; private set; }
      internal Task<T> Task { get; set; }
      public T Result {
        get {
          return _Result;
        }
        set {
          _Result = value;
          IsFinished = true;
          if (Task != null)
            Task.Finish();
        }
      }
      public Exception Error {
        get {
          return _Error;
        }
        set {
          _Error = value;
          IsFinished = true;
          if (Task != null)
            Task.Finish();
        }
      }
    }
  }
}
