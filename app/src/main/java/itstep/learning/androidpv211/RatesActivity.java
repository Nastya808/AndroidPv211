package itstep.learning.androidpv211;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import itstep.learning.androidpv211.nbu.NbuRateAdapter;
import itstep.learning.androidpv211.orm.NbuRate;

public class RatesActivity extends AppCompatActivity {
    private static final String nbuRatesUrl = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json";
    private ExecutorService pool;
    private final List<NbuRate> nbuRates = new ArrayList<>();
    private NbuRateAdapter nbuRateAdapter;
    private RecyclerView rvContainer;
    private TextView tvRateDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rates);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ініціалізація
        tvRateDate = findViewById(R.id.nbu_rate_date);
        rvContainer = findViewById(R.id.rates_rv_container);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rvContainer.setLayoutManager(layoutManager);
        nbuRateAdapter = new NbuRateAdapter(nbuRates);
        rvContainer.setAdapter(nbuRateAdapter);

        pool = Executors.newFixedThreadPool(3);

        CompletableFuture
                .supplyAsync(this::loadRates, pool)
                .thenAccept(this::parseNbuResponse)
                .thenRun(this::showNbuRates);
    }

    private void showNbuRates() {
        runOnUiThread(() -> {
            nbuRateAdapter.notifyItemRangeChanged(0, nbuRates.size());
        });
    }

    private void parseNbuResponse(String body) {
        try {
            JSONArray arr = new JSONArray(body);
            nbuRates.clear(); // очистити перед додаванням
            if (arr.length() > 0) {
                String date = arr.getJSONObject(0).getString("exchangedate");
                runOnUiThread(() -> tvRateDate.setText("Курси валют на " + date));
            }

            for (int i = 0; i < arr.length(); i++) {
                nbuRates.add(NbuRate.fromJsonObject(arr.getJSONObject(i)));
            }
        } catch (JSONException ex) {
            Log.d("parseNbuResponse", "JSONException " + ex.getMessage());
        }
    }

    private String loadRates() {
        try {
            URL url = new URL(nbuRatesUrl);
            InputStream urlStream = url.openStream();
            ByteArrayOutputStream byteBuilder = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = urlStream.read(buffer)) > 0) {
                byteBuilder.write(buffer, 0, len);
            }
            urlStream.close();
            return byteBuilder.toString(StandardCharsets.UTF_8.name());
        } catch (MalformedURLException ex) {
            Log.d("loadRates", "MalformedURLException " + ex.getMessage());
        } catch (IOException ex) {
            Log.d("loadRates", "IOException " + ex.getMessage());
        }
        return null;
    }


    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }
}
