package itstep.learning.androidpv211;

import android.os.Bundle;
import android.util.Log;

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
        pool = Executors.newFixedThreadPool( 3 );
        // pool.submit( this::loadRates );
        // new Thread( this::loadRates ).start();
        CompletableFuture
                .supplyAsync( this::loadRates, pool )
                .thenAccept( this::parseNbuResponse )
                .thenRun( this::showNbuRates );
        rvContainer = findViewById( R.id.rates_rv_container );
        // Внутрішня організація контенту
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager( this );
        rvContainer.setLayoutManager( layoutManager );
        nbuRateAdapter = new NbuRateAdapter( nbuRates );
        rvContainer.setAdapter( nbuRateAdapter );
    }

    private void showNbuRates() {
        runOnUiThread( () -> {
            nbuRateAdapter.notifyItemRangeChanged( 0, nbuRates.size() );
        } );
    }

    private void parseNbuResponse( String body ) {
        try{
            JSONArray arr = new JSONArray( body ) ;
            // nbuRates = new ArrayList<>();
            int len = arr.length() ;
            for (int i = 0; i < len; i++) {
                nbuRates.add(
                    NbuRate.fromJsonObject(
                            arr.getJSONObject(i) ) ) ;

            }
        }
        catch( JSONException ex ) {
            Log.d( "parseNbuResponse", "JSONException " + ex.getMessage() );
        }
    }

    private String loadRates() {
        try {
            URL url = new URL( nbuRatesUrl );
            InputStream urlStream = url.openStream();   // GET-request
            ByteArrayOutputStream byteBuilder = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while( ( len = urlStream.read( buffer ) ) > 0 ) {
                byteBuilder.write( buffer, 0, len );
            }
            String charsetName = StandardCharsets.UTF_8.name();
            String data = byteBuilder.toString( charsetName );
            urlStream.close();
            return data;
        }
        catch( MalformedURLException ex ) {
            Log.d( "loadRates", "MalformedURLException " + ex.getMessage() );
        }
        catch( IOException ex ) {
            Log.d( "loadRates", "IOException " + ex.getMessage() );
        }
        return null;
        /*
        android.os.NetworkOnMainThreadException - всі запити до мережі мають бути в
        окремих потоках.

        java.lang.SecurityException: Permission denied (missing INTERNET permission?)
        Багато дій в Андроїд вимагає дозволів, зокрема Інтернет. Дозволи декларуються
        у маніфесті <uses-permission android:name="android.permission.INTERNET"/>

        android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
        Для роботи з елементами UI необхідно повернути управління до потоку UI: runOnUiThread();
         */
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }
}
/*
Д.З. Курси валют.
Реалізувати показ дати, на яку виводиться курс - одним полем
(не дублювати на кожному курсі).
У віджетах курсів замість числа сформувати надпис
 1 DZD = 0.30886 HRN
** а також зворотній курс
 1 HRN = 3.3 DZD
 */