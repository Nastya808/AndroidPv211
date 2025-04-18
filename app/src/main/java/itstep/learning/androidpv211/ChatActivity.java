package itstep.learning.androidpv211;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import itstep.learning.androidpv211.chat.ChatMessageAdapter;
import itstep.learning.androidpv211.orm.ChatMessage;

public class ChatActivity extends AppCompatActivity {
    private static final String chatUrl = "https://chat.momentfor.fun/";
    private ExecutorService pool;
    private final List<ChatMessage> messages = new ArrayList<>();
    private EditText etAuthor;
    private EditText etMessage;
    private RecyclerView rvContent;
    private ChatMessageAdapter chatMessageAdapter;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeBars = insets.getInsets( WindowInsetsCompat.Type.ime() );
            v.setPadding( systemBars.left, systemBars.top, systemBars.right,
                    Math.max( systemBars.bottom, imeBars.bottom )
            );
            return insets;
        });
        pool = Executors.newFixedThreadPool( 3 );
        updateChat();
        etAuthor = findViewById( R.id.chat_et_author );
        etMessage = findViewById( R.id.chat_et_message );

        rvContent = findViewById( R.id.chat_rv_content );
        chatMessageAdapter = new ChatMessageAdapter( messages );
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd( true );
        rvContent.setLayoutManager( layoutManager );
        rvContent.setAdapter( chatMessageAdapter );

        findViewById( R.id.chat_btn_send ).setOnClickListener( this::onSendClick );
    }

    private void updateChat() {
        CompletableFuture
                .supplyAsync( () -> Services.fetchUrl( chatUrl ), pool )
                .thenApply( this::parseChatResponse )
                .thenAccept( this::processChatResponse );
        Log.i("updateChat", "updated");
        handler.postDelayed( this::updateChat, 2000 );
    }

    private void onSendClick( View view ) {
        String alertMessage = null;
        String author = etAuthor.getText().toString() ;
        String message = etMessage.getText().toString() ;
        if( author.isBlank() ) {
            alertMessage = getString( R.string.chat_msg_no_author );
        }
        else if( message.isBlank() ) {
            alertMessage = getString( R.string.chat_msg_no_text );
        }
        if( alertMessage != null ) {
            new AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog_Alert)
                    .setTitle( R.string.chat_msg_no_send )
                    .setMessage( alertMessage )
                    .setIcon( android.R.drawable.ic_delete )
                    .setPositiveButton( R.string.chat_msg_no_send_btn, (dlg, btn) -> {} )
                    .setCancelable( false )
                    .show();
            return;
        }
        CompletableFuture.runAsync(
                () -> sendChatMessage( new ChatMessage( author, message ) ),
                pool
        );
    }

    private void sendChatMessage( ChatMessage chatMessage ) {
        /*
        Надсилання даних на прикладі форми
        Метод імітує надсилання форми з полями "author" та "msg":
        POST {chatUrl}
        Content-Type: application/x-www-form-urlencoded
        Accept: application/json
        Accept-Language: uk
        Connection: close
        X-Powered-By: AndroidPv211

        author={author}&msg={message}

        author=The Author&msg=Hello, All!  --- неправильно
        author=The%20Author&msg=Hello,%20All!  --- правильно
         */
        String charset = StandardCharsets.UTF_8.name();
        try {
            String body = String.format( Locale.ROOT,
                    "author=%s&msg=%s",
                    URLEncoder.encode( chatMessage.getAuthor(), charset ),
                    URLEncoder.encode( chatMessage.getText(), charset )
            );
            URL url = new URL( chatUrl );
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput( true );   // очікуємо відповідь (можна читати)
            connection.setDoOutput( true );  // буде передача даних (тіло)
            connection.setRequestMethod( "POST" );
            connection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );   // що передаємо
            connection.setRequestProperty( "Accept", "application/json" );   // що очікуємо у відповідь
            connection.setRequestProperty( "Connection", "close" );
            connection.setRequestProperty( "X-Powered-By", "AndroidPv211" );
            connection.setChunkedStreamingMode( 0 );   // не ділити на частини

            OutputStream bodyStream = connection.getOutputStream();
            bodyStream.write( body.getBytes( charset ) );
            bodyStream.flush();
            bodyStream.close();

            int statusCode = connection.getResponseCode();
            if( statusCode == 201 ) {
                // даний сервер не надає тіла, якщо воно потрібне, то читаємо connection.getInputStream()
                updateChat();
            }
            else {
                // відповідь у стані "помилка" передається через connection.getErrorStream()
                InputStream errorStream = connection.getErrorStream();
                Log.e("sendChatMessage", Services.readAllText( errorStream ) ) ;
                errorStream.close();
            }
            connection.disconnect();
        }
        catch (UnsupportedEncodingException ex) {
            Log.e("sendChatMessage", "UnsupportedEncodingException " + ex.getMessage() );
        }
        catch (MalformedURLException ex) {
            Log.e("sendChatMessage", "MalformedURLException " + ex.getMessage() );
        }
        catch (IOException ex) {
            Log.e("sendChatMessage", "IOException " + ex.getMessage() );
        }
    }

    private void processChatResponse( List<ChatMessage> parsedMessages ) {
        int oldSize = messages.size();
        for( ChatMessage m : parsedMessages ) {
            if( messages.stream().noneMatch( cm -> cm.getId().equals( m.getId() ) ) ) {
                messages.add( m );
            }
        }
        int newSize = messages.size();
        if( newSize > oldSize ) {
            messages.sort(Comparator.comparing(ChatMessage::getMoment));
            runOnUiThread(() -> {
                chatMessageAdapter.notifyItemRangeChanged( oldSize, newSize );
                rvContent.scrollToPosition( newSize - 1 );
            });
        }
    }

    private List<ChatMessage> parseChatResponse(String body) {
        List<ChatMessage> res = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(body);

            // Перевірка статусу
            int status = root.getInt("status");
            if (status != 1) {
                Log.w("parseChatResponse", "Запит завершився зі статусом " + status + ", обробка ігнорується.");
                return res; // повертаємо порожній список
            }

            // Якщо статус коректний, обробляємо масив повідомлень
            JSONArray arr = root.getJSONArray("data");
            int len = arr.length();
            for (int i = 0; i < len; i++) {
                res.add(ChatMessage.fromJsonObject(arr.getJSONObject(i)));
            }
        } catch (JSONException ex) {
            Log.d("parseChatResponse", "JSONException " + ex.getMessage());
        }
        return res;
    }


    @Override
    protected void onDestroy() {
        handler.removeMessages(0);
        pool.shutdownNow();
        super.onDestroy();
    }

}
