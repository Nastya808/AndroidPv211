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
import java.util.concurrent.locks.ReentrantLock;

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
    private boolean isAuthorLocked = false;
    private final ReentrantLock messagesLock = new ReentrantLock(); // Для синхронізації потоків

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

        // Завантажуємо повідомлення з БД асинхронно
        loadMessagesFromDbAsync();

        // Оновлюємо чат з сервера
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

    private void loadMessagesFromDbAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                // Отримуємо повідомлення з БД
                List<ChatMessage> dbMessages = getMessagesFromDatabase();

                messagesLock.lock();
                try {
                    // Додаємо тільки унікальні повідомлення
                    for (ChatMessage message : dbMessages) {
                        if (messages.stream().noneMatch(cm -> cm.getId().equals(message.getId()))) {
                            messages.add(message);
                        }
                    }

                    // Сортуємо повідомлення
                    messages.sort(Comparator.comparing(ChatMessage::getMoment));

                    // Оновлюємо UI
                    runOnUiThread(() -> {
                        chatMessageAdapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            rvContent.scrollToPosition(messages.size() - 1);
                        }
                    });
                } finally {
                    messagesLock.unlock();
                }
            } catch (Exception e) {
                Log.e("loadMessagesFromDb", "Error loading messages from DB", e);
            }
        }, pool);
    }

    // Заглушка для методу отримання повідомлень з БД
    private List<ChatMessage> getMessagesFromDatabase() {
        // Тут має бути реалізація отримання повідомлень з локальної БД
        // Повертаємо порожній список, якщо БД ще не реалізована
        return new ArrayList<>();
    }

    private void updateChat() {
        CompletableFuture
                .supplyAsync(() -> Services.fetchUrl(chatUrl), pool)
                .thenApply(this::parseChatResponse)
                .thenAccept(this::processChatResponse);
        Log.i("updateChat", "updated");
        handler.postDelayed(this::updateChat, 2000);
    }

    private void processChatResponse(List<ChatMessage> parsedMessages) {
        messagesLock.lock();
        try {
            int oldSize = messages.size();
            for (ChatMessage m : parsedMessages) {
                if (messages.stream().noneMatch(cm -> cm.getId().equals(m.getId()))) {
                    messages.add(m);
                }
            }
            int newSize = messages.size();
            if (newSize > oldSize) {
                messages.sort(Comparator.comparing(ChatMessage::getMoment));
                runOnUiThread(() -> {
                    chatMessageAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize);
                    rvContent.scrollToPosition(newSize - 1);
                });
            }
        } finally {
            messagesLock.unlock();
        }
    }

    // Інші методи залишаються без змін
    private void onSendClick(View view) {
        String alertMessage = null;
        String author = etAuthor.getText().toString();
        String message = etMessage.getText().toString();

        if (author.isBlank()) {
            alertMessage = getString(R.string.chat_msg_no_author);
        } else if (message.isBlank()) {
            alertMessage = getString(R.string.chat_msg_no_text);
        }

        if (alertMessage != null) {
            new AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog_Alert)
                    .setTitle(R.string.chat_msg_no_send)
                    .setMessage(alertMessage)
                    .setIcon(android.R.drawable.ic_delete)
                    .setPositiveButton(R.string.chat_msg_no_send_btn, (dlg, btn) -> {
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        if (!isAuthorLocked) {
            isAuthorLocked = true;
            runOnUiThread(() -> etAuthor.setEnabled(false));
        }

        CompletableFuture.runAsync(
                () -> sendChatMessage(new ChatMessage(author, message)),
                pool
        ).thenRun(() -> {
            runOnUiThread(() -> {
                etMessage.setText("");
            });
        });
    }

    private void sendChatMessage(ChatMessage chatMessage) {
        String charset = StandardCharsets.UTF_8.name();
        try {
            String body = String.format(Locale.ROOT,
                    "author=%s&msg=%s",
                    URLEncoder.encode(chatMessage.getAuthor(), charset),
                    URLEncoder.encode(chatMessage.getText(), charset)
            );
            URL url = new URL(chatUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("X-Powered-By", "AndroidPv211");
            connection.setChunkedStreamingMode(0);

            OutputStream bodyStream = connection.getOutputStream();
            bodyStream.write(body.getBytes(charset));
            bodyStream.flush();
            bodyStream.close();

            int statusCode = connection.getResponseCode();
            if (statusCode == 201) {
                updateChat();
            } else {
                InputStream errorStream = connection.getErrorStream();
                Log.e("sendChatMessage", Services.readAllText(errorStream));
                errorStream.close();
            }
            connection.disconnect();
        } catch (UnsupportedEncodingException ex) {
            Log.e("sendChatMessage", "UnsupportedEncodingException " + ex.getMessage());
        } catch (MalformedURLException ex) {
            Log.e("sendChatMessage", "MalformedURLException " + ex.getMessage());
        } catch (IOException ex) {
            Log.e("sendChatMessage", "IOException " + ex.getMessage());
        }
    }

    private List<ChatMessage> parseChatResponse(String body) {
        List<ChatMessage> res = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(body);
            int status = root.getInt("status");
            if (status != 1) {
                Log.w("parseChatResponse", "Запит завершився зі статусом " + status + ", обробка ігнорується.");
                return res;
            }

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