package edu.gatech.seclass.myapplication2;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.view.View;
import android.widget.TextView;


public class TranslationHistoryAdapter extends RecyclerView.Adapter<TranslationHistoryAdapter.HistoryViewHolder>{

    private List<TranslationHistory> historyList;
    private SimpleDateFormat dateFormat;

    //Creating a history list along with dating format for default systm
    public TranslationHistoryAdapter(){
        this.historyList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translation_history, parent, false);
        return new HistoryViewHolder(view);
    }
    //Creating text output for view holder for current input
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position){
        TranslationHistory history = historyList.get(position);

        //Attributes
        holder.originalText.setText(history.getOriginalText());
        holder.translatedText.setText(history.getTranslatedText());
        holder.languageChange.setText("English to "+ history.getTargetLangDisplay());

        if(history.getTimeStamp() != null){
            holder.timestamp.setText(dateFormat.format(history.getTimeStamp()));

        }else{
            holder.timestamp.setText("unknown");
        }

    }

    //Getting total amount of items in history
    @Override
    public int getItemCount(){
        return historyList.size();
    }

    //Updates history based off date change (since it goes down to the second)
    public void updateHistoryList(List<TranslationHistory> newHistory){
        this.historyList = newHistory;
        notifyDataSetChanged();
    }

    public void addTranslation(TranslationHistory translation){
        historyList.add(0,translation);
        notifyItemInserted(0);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder{
        TextView originalText, translatedText, languageChange, timestamp;

        public HistoryViewHolder(@NonNull View itemView){
            super(itemView);
            originalText =itemView.findViewById(R.id.originalText);
            translatedText  =itemView.findViewById(R.id.translatedText);
            languageChange  =itemView.findViewById(R.id.languageChange);
            timestamp  =itemView.findViewById(R.id.timestamp);
        }
    }
}
