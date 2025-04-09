package itstep.learning.androidpv211.nbu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import itstep.learning.androidpv211.R;
import itstep.learning.androidpv211.orm.NbuRate;

public class NbuRateAdapter extends RecyclerView.Adapter<NbuRateAdapter.ViewHolder> {
    private final List<NbuRate> dataset;

    public NbuRateAdapter(List<NbuRate> dataset) {
        this.dataset = dataset;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRateCC;
        TextView tvRateTxt;
        TextView tvRateRate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRateCC = itemView.findViewById(R.id.nbu_rate_cc);
            tvRateTxt = itemView.findViewById(R.id.nbu_rate_txt);
            tvRateRate = itemView.findViewById(R.id.nbu_rate_rate);
        }
    }

    @NonNull
    @Override
    public NbuRateAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nbu_rate_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NbuRateAdapter.ViewHolder holder, int position) {
        NbuRate rate = dataset.get(position);
        holder.tvRateCC.setText(rate.getCc());
        holder.tvRateTxt.setText(rate.getTxt());

        // Формування рядків
        String direct = String.format("1 %s = %.5f UAH", rate.getCc(), rate.getRate());
        String reverse = String.format("1 UAH = %.5f %s", 1.0 / rate.getRate(), rate.getCc());
        holder.tvRateRate.setText(direct + "\n" + reverse);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }
}
