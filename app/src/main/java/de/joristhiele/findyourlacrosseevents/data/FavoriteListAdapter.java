package de.joristhiele.findyourlacrosseevents.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parse.ParseObject;

import java.util.List;

import de.joristhiele.findyourlacrosseevents.Constants;
import de.joristhiele.findyourlacrosseevents.R;

public class FavoriteListAdapter extends RecyclerView.Adapter<FavoriteListAdapter.ViewHolder> {

    private final List<Event> favoritesDataSet;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvAddress;
        private final TextView tvDates;
        private final TextView tvEventType;
        private final TextView tvGenders;
        private final TextView tvDisciplines;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.favorite_title);
            tvAddress = itemView.findViewById(R.id.favorites_address);
            tvDates = itemView.findViewById(R.id.favorites_dates);
            tvEventType = itemView.findViewById(R.id.favorites_event_type);
            tvGenders = itemView.findViewById(R.id.favorites_genders);
            tvDisciplines = itemView.findViewById(R.id.favorites_disciplines);
        }

        public TextView getTvTitle() {
            return tvTitle;
        }
        public TextView getTvAddress() {
            return tvAddress;
        }
        public TextView getTvDates() {
            return tvDates;
        }
        public TextView getTvEventType() {
            return tvEventType;
        }
        public TextView getTvGenders() {
            return tvGenders;
        }
        public TextView getTvDisciplines() {
            return tvDisciplines;
        }
    }

    public FavoriteListAdapter(List<Event> favoritesDataSet) {
        this.favoritesDataSet = favoritesDataSet;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_favorites, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = favoritesDataSet.get(position);

        holder.getTvTitle().setText(event.getName());
        holder.getTvAddress().setText(event.getAddress());
        holder.getTvDates().setText(holder.itemView.getContext().getString(R.string.favorites_dates_value, event.getStartDate(), event.getEndDate()));
        if (event.getEventType() != null && event.getGenders() != null && event.getDisciplines() != null) {
            holder.getTvEventType().setText(event.getEventType().getString("EventTypeName"));
            StringBuilder builder = new StringBuilder();
            for (ParseObject gender : event.getGenders())
                builder.append(gender.getString("GenderName")).append(Constants.EVENT_CATEGORY_DELIMITER);
            builder.setLength(builder.length() - Constants.EVENT_CATEGORY_DELIMITER.length());
            holder.getTvGenders().setText(builder.toString());
            builder = new StringBuilder();
            for (ParseObject gender : event.getDisciplines())
                builder.append(gender.getString("DisciplineName")).append(Constants.EVENT_CATEGORY_DELIMITER);
            builder.setLength(builder.length() - Constants.EVENT_CATEGORY_DELIMITER.length());
            holder.getTvDisciplines().setText(builder.toString());
        }
    }

    @Override
    public int getItemCount() {
        return favoritesDataSet.size();
    }
}
