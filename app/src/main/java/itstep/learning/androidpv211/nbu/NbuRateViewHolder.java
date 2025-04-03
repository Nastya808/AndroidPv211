package itstep.learning.androidpv211.nbu;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import itstep.learning.androidpv211.R;
import itstep.learning.androidpv211.orm.NbuRate;

public class NbuRateViewHolder extends RecyclerView.ViewHolder {
    private final TextView tvTxt;
    private final TextView tvCc;
    private final TextView tvRate;

    private NbuRate nbuRate;

    public NbuRateViewHolder(@NonNull View itemView) {
        super(itemView);
        tvTxt  = itemView.findViewById( R.id.nbu_rate_txt  );
        tvCc   = itemView.findViewById( R.id.nbu_rate_cc   );
        tvRate = itemView.findViewById( R.id.nbu_rate_rate );
    }

    private void showData() {
        tvTxt.setText( nbuRate.getTxt() );
        tvCc.setText( nbuRate.getCc() );
        tvRate.setText( String.valueOf( nbuRate.getRate() ) );
    }

    public void setNbuRate(NbuRate nbuRate) {
        this.nbuRate = nbuRate;
        showData();
    }

    public NbuRate getNbuRate() {
        return nbuRate;
    }
}
/*
Посередник між розміткою та даними
 */