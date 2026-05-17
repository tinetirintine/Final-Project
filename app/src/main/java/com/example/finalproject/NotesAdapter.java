package com.example.finalproject;

import android.graphics.Paint;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
        void onDoneChanged(Note note, boolean isDone);
        void onRestoreClick(Note note);
    }

    private boolean isTrashMode = false;

    public void setTrashMode(boolean isTrashMode) {
        this.isTrashMode = isTrashMode;
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
        int categoryColor;
        switch (note.getCategory()) {
            case "Personal": categoryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPersonal); break;
            case "School": categoryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSchool); break;
            case "Work": categoryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWork); break;
            default: categoryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_purple); break;
        }
        holder.viewCategoryStrip.setBackgroundColor(categoryColor);
        holder.tvCategory.getBackground().setTint(categoryColor);

        String dateStr = dateFormat.format(new Date(note.getDateMillis()));
        holder.tvDateTime.setText(dateStr + " - " + note.getTime());

        // Check if out of date or done
        boolean isPast = isPastDate(note.getDateMillis());
        boolean shouldCrossOut = note.isDone() || isPast;

        if (shouldCrossOut) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorDone));
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        }

        if (isPast && !note.isDone()) {
            holder.tvDateTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPastDue));
        } else {
            holder.tvDateTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.welcome_subtitle));
        }

        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(note.isDone());
        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            interactionListener.onDoneChanged(note, isChecked);
        });

        if (isTrashMode) {
            holder.btnRestore.setVisibility(View.VISIBLE);
            holder.cbDone.setVisibility(View.GONE);
            holder.ivPinned.setVisibility(View.GONE);
            holder.ivFavorite.setVisibility(View.GONE);
            holder.ivArchived.setVisibility(View.GONE);
        } else {
            holder.btnRestore.setVisibility(View.GONE);
            holder.cbDone.setVisibility(View.VISIBLE);
            holder.cbDone.setEnabled(true);
            holder.ivPinned.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
            holder.ivFavorite.setVisibility(note.isFavorite() ? View.VISIBLE : View.GONE);
            holder.ivFavorite.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorFavorite));
            holder.ivArchived.setVisibility(note.isArchived() ? View.VISIBLE : View.GONE);
        }

        holder.btnRestore.setOnClickListener(v -> interactionListener.onRestoreClick(note));
        holder.btnDelete.setOnClickListener(v -> interactionListener.onDeleteClick(note));
        holder.itemView.setOnClickListener(v -> interactionListener.onNoteClick(note));
    }

    private boolean isPastDate(long dateMillis) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar noteDate = Calendar.getInstance();
        noteDate.setTimeInMillis(dateMillis);
        noteDate.set(Calendar.HOUR_OF_DAY, 0);
        noteDate.set(Calendar.MINUTE, 0);
        noteDate.set(Calendar.SECOND, 0);
        noteDate.set(Calendar.MILLISECOND, 0);

        return noteDate.before(today);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    public Note getNoteAt(int position) {
        return notes.get(position);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvCategory, tvDateTime;
        ImageButton btnDelete, btnRestore;
        CheckBox cbDone;
        ImageView ivPinned, ivFavorite, ivArchived;
        View viewCategoryStrip;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvContent = itemView.findViewById(R.id.tvNoteContent);
            tvCategory = itemView.findViewById(R.id.tvNoteCategory);
            tvDateTime = itemView.findViewById(R.id.tvNoteDateTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            cbDone = itemView.findViewById(R.id.cbDone);
            ivPinned = itemView.findViewById(R.id.ivPinned);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            ivArchived = itemView.findViewById(R.id.ivArchived);
            viewCategoryStrip = itemView.findViewById(R.id.viewCategoryStrip);
        }
    }
}