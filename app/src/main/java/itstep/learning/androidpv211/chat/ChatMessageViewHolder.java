package itstep.learning.androidpv211.chat;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import itstep.learning.androidpv211.R;
import itstep.learning.androidpv211.orm.ChatMessage;

public class ChatMessageViewHolder extends RecyclerView.ViewHolder {
    public static final SimpleDateFormat momentFormat =
            new SimpleDateFormat("dd.MM HH:mm", Locale.ROOT );

    private ChatMessage chatMessage;
    private final TextView tvAuthor;
    private final TextView tvText;
    private final TextView tvMoment;

    public ChatMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        tvAuthor = itemView.findViewById( R.id.chat_msg_author );
        tvText   = itemView.findViewById( R.id.chat_msg_text   );
        tvMoment = itemView.findViewById( R.id.chat_msg_moment );
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
        tvAuthor.setText(this.chatMessage.getAuthor());
        tvText.setText(this.chatMessage.getText());
        tvMoment.setText(getSmartMoment(this.chatMessage.getMoment()));
    }

    private String getSmartMoment(java.util.Date moment) {
        long now = System.currentTimeMillis();
        long msgTime = moment.getTime();

        long diff = now - msgTime;

        // Милісекунди в одиницях часу
        long minute = 60_000L;
        long hour = 60 * minute;
        long day = 24 * hour;

        java.util.Calendar msgCal = java.util.Calendar.getInstance();
        msgCal.setTime(moment);

        java.util.Calendar nowCal = java.util.Calendar.getInstance();

        // Сьогодні
        if (nowCal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(moment);
        }

        // Вчора
        nowCal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        if (nowCal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "вчора " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(moment);
        }

        // Протягом останнього тижня
        long daysAgo = diff / day;
        if (daysAgo < 7) {
            return daysAgo + " дні(в) тому";
        }

        // Інакше — повна дата і час
        return momentFormat.format(moment);
    }



}
