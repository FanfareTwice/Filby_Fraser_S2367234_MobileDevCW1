package org.me.gcu.filby_fraser_s2367234.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.me.gcu.filby_fraser_s2367234.R;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SituationAdapter extends ListAdapter<Situation, SituationAdapter.Holder> {

    public interface OnSituationClick { void onClick(Situation s); }

    private final OnSituationClick listener;
    private String selectedKey;

    public SituationAdapter(OnSituationClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setSelectedKey(String key) {
        if (Objects.equals(key, selectedKey)) return;
        selectedKey = key;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void submit(List<Situation> list) {
        submitList(list == null ? null : new ArrayList<>(list));
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Situation s = getItem(position);
        Context c = h.itemView.getContext();

        h.title.setText(s.getTitle());
        h.category.setText(s.getCategory() == Situation.Category.ROADWORKS ? "Roadworks" : "Incident");
        h.comment.setText(s.getComment() != null ? s.getComment() : "No description provided");

        h.icon.setImageResource(SituationStyle.iconFor(s.getType()));
        h.icon.setBackgroundTintList(ColorStateList.valueOf(SituationStyle.colourFor(c, s.getType())));
        h.strip.setBackgroundColor(SituationStyle.stripColour(c, s));

        List<String> meta = new ArrayList<>();
        if (s.getRoadName() != null) meta.add(s.getRoadName());
        if (s.getCategory() == Situation.Category.ROADWORKS) {
            meta.add(SituationStyle.delayLabel(s.getImpact().getDelayLevel()));
            String d = SituationStyle.formatDuration(s.getDuration());
            if (d != null) meta.add(d);
        } else if (s.getStartTime() != null) {
            meta.add("Since " + SituationStyle.formatTime(s.getStartTime()));
        }
        h.meta.setText(String.join("  •  ", meta));

        // Outline the card whose details are open
        float density = c.getResources().getDisplayMetrics().density;
        ((MaterialCardView) h.itemView).setStrokeWidth(
                s.getKey().equals(selectedKey) ? Math.round(2 * density) : 0);

        h.itemView.setContentDescription(s.getTitle() + ". " + h.comment.getText());
        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View strip;
        final ImageView icon;
        final TextView title, category, comment, meta;

        Holder(View v) {
            super(v);
            strip = v.findViewById(R.id.strip);
            icon = v.findViewById(R.id.icon);
            title = v.findViewById(R.id.title);
            category = v.findViewById(R.id.category);
            comment = v.findViewById(R.id.comment);
            meta = v.findViewById(R.id.meta);
        }
    }

    private static final DiffUtil.ItemCallback<Situation> DIFF = new DiffUtil.ItemCallback<Situation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return a.getKey().equals(b.getKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return Objects.equals(a.getVersion(), b.getVersion())
                    && Objects.equals(a.getComment(), b.getComment());
        }
    };
}
