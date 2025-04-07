package itstep.learning.androidpv211;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById( R.id.main_btn_calc  ).setOnClickListener( this::onCalcButtonClick  );
        findViewById( R.id.main_btn_rates ).setOnClickListener( this::onRatesButtonClick );

    }

    private void onCalcButtonClick( View view ) {
        startActivity( new Intent( this, CalcActivity.class ) );
    }
    private void onRatesButtonClick( View view ) {
        startActivity( new Intent( this, RatesActivity.class ) );
    }
}
