package depollsoft.tagmaster;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class TagQueryActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagqueryactivity);
    EdgeToEdgeKt.setUpToolbar(this, true);
  }

  @Override
  public boolean onSupportNavigateUp() {
    return EdgeToEdgeKt.navigateUpOrHome(this);
  }
}
