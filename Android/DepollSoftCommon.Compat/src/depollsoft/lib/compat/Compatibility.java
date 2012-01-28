package depollsoft.lib.compat;

public final class Compatibility {
  private Compatibility() {
  }

  public static boolean tryWithFallback(Runnable toRun) {
    try {
      runWithExceptions(toRun);
      return true;
    }
    catch (NoSuchFieldError e) {
    }
    catch (NoSuchMethodError e) {
    }
    catch (NoClassDefFoundError e) {
    }
    catch (ClassNotFoundException e) {
    }
    catch (NoSuchFieldException e) {
    }
    catch (NoSuchMethodException e) {
    }
    catch (NullPointerException e) {
    }
    return false;
  }

  private static void runWithExceptions(Runnable toRun)
      throws ClassNotFoundException, NoSuchFieldError, NoSuchFieldException,
      NoClassDefFoundError, NoSuchMethodError, NoSuchMethodException {
    toRun.run();
  }
}
