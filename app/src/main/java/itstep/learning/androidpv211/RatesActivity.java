package itstep.learning.androidpv211;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import itstep.learning.androidpv211.nbu.NbuRateAdapter;
import itstep.learning.androidpv211.orm.NbuRate;

public class RatesActivity extends AppCompatActivity {

    private static final String nbuRatesUrl = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json";
    private ExecutorService pool;

    private final List<NbuRate> allRates = new ArrayList<>(); // Полный список (неизменный)
    private final List<NbuRate> filteredRates = new ArrayList<>(); // Отображаемый список (фильтрованный)

    private NbuRateAdapter nbuRateAdapter;
    private RecyclerView rvContainer;
    private TextView tvRateDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rates);

        tvRateDate = findViewById(R.id.nbu_rate_date);
        rvContainer = findViewById(R.id.rates_rv_container);
        rvContainer.setLayoutManager(new LinearLayoutManager(this));
        nbuRateAdapter = new NbuRateAdapter(filteredRates);
        rvContainer.setAdapter(nbuRateAdapter);

        pool = Executors.newFixedThreadPool(3);

        // Загрузка курсов
        CompletableFuture
                .supplyAsync(this::loadRates, pool)
                .thenAccept(this::parseNbuResponse)
                .thenRun(this::showNbuRates);

        // Поиск
        SearchView svFilter = findViewById(R.id.rates_sv_filter);
        svFilter.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return onFilterChange(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return onFilterChange(newText);
            }
        });

        // Выбор даты
        Button btnPickDate = findViewById(R.id.btn_pick_date);
        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String date = String.format("%04d%02d%02d", year, month + 1, dayOfMonth);
                        String dateLabel = String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year);
                        loadRatesByDate(date, dateLabel);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private boolean onFilterChange(String query) {
        String searchQuery = query.trim().toLowerCase();
        filteredRates.clear();

        for (NbuRate rate : allRates) {
            String name = rate.getTxt() != null ? rate.getTxt().toLowerCase() : "";
            String code = rate.getCc() != null ? rate.getCc().toLowerCase() : "";

            if (name.contains(searchQuery) || code.contains(searchQuery)) {
                filteredRates.add(rate);
            }
        }

        runOnUiThread(() -> nbuRateAdapter.notifyDataSetChanged());
        return true;
    }

    private void showNbuRates() {
        runOnUiThread(() -> nbuRateAdapter.notifyDataSetChanged());
    }

    private void parseNbuResponse(String body) {
        try {
            JSONArray arr = new JSONArray(body);
            allRates.clear();
            filteredRates.clear();

            if (arr.length() > 0) {
                String date = arr.getJSONObject(0).getString("exchangedate");
                runOnUiThread(() -> tvRateDate.setText("Курсы валют на " + date));
            }

            for (int i = 0; i < arr.length(); i++) {
                NbuRate rate = NbuRate.fromJsonObject(arr.getJSONObject(i));
                allRates.add(rate);
            }

            // Копируем все в отображаемый список
            filteredRates.addAll(allRates);
        } catch (JSONException ex) {
            Log.d("parseNbuResponse", "JSONException " + ex.getMessage());
        }
    }

    private String loadRates() {
        return loadRatesFromUrl(nbuRatesUrl);
    }

    private void loadRatesByDate(String yyyymmdd, String dateLabel) {
        String url = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?date=" + yyyymmdd + "&json";
        CompletableFuture
                .supplyAsync(() -> loadRatesFromUrl(url), pool)
                .thenAccept(response -> {
                    parseNbuResponse(response);
                    runOnUiThread(() -> tvRateDate.setText("Курсы валют на " + dateLabel));
                    showNbuRates();
                });
    }

    private String loadRatesFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
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
