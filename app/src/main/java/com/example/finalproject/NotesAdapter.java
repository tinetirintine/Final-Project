package com.example.finalproject;

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes;
    private OnNoteInteractionListener interactionListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnNoteInteractionListener {
        void onDeleteClick(Note note);
        void onNoteClick(Note note);
    }

    public NotesAdapter(List<Note> notes, OnNoteInteractionListener interactionListener) {
        this.notes = notes;
        this.interactionListener = interactionListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.getTitle());
        
        // Render content as HTML
        if (note.getContent() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.tvContent.setText(Html.fromHtml(note.getContent(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.tvContent.setText(Html.fromHtml(note.getContent()));
            }
        }

        holder.tvCategory.setText(note.getCategory());

        String dateStr = dateFormat.format(new Date(note.getDateMillis()));
        holder.tvDateTime.setText(dateStr + " - " + note.getTime());

        holder.btnDelete.setOnClickListener(v -> interactionListener.onDeleteClick(note));
        holder.itemView.setOnClickListener(v -> interactionListener.onNoteClick(note));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvCategory, tvDateTime;
        ImageButton btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvContent = itemView.findViewById(R.id.tvNoteContent);
            tvCategory = itemView.findViewById(R.id.tvNoteCategory);
            tvDateTime = itemView.findViewById(R.id.tvNoteDateTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}