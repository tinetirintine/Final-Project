package com.example.finalproject;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;
import java.util.List;


public class CalendarFragment extends Fragment {


    private int userId;
    private NotesAdapter adapter;
    private long selectedDateMillis;
    private NoteRepository noteRepository;
    private RecyclerView recyclerView;

    public static CalendarFragment newInstance(int userId) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putInt("user_id", userId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id");
        }
        noteRepository = new NoteRepository(getContext());
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);


        CalendarView calendarView = view.findViewById(R.id.calendarView);
        recyclerView = view.findViewById(R.id.rvCalendarNotes);


        selectedDateMillis = calendarView.getDate();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            selectedDateMillis = cal.getTimeInMillis();
            refreshNotes();
        });


        setupAdapter();


        return view;
    }


    private void setupAdapter() {
        noteRepository.getNotesByDate(userId, selectedDateMillis, result -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new NotesAdapter(result, new NotesAdapter.OnNoteInteractionListener() {
                        @Override
                        public void onDeleteClick(Note note) {
                            new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Note")
                                .setMessage("Move this note to trash?")
                                .setPositiveButton("Move to Trash", (dialog, which) -> {
                                    note.setDeleted(true);
                                    noteRepository.updateNote(note, () -> refreshNotes());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        }

                        @Override
                        public void onDoneChanged(Note note, boolean isDone) {
                            note.setDone(isDone);
                            noteRepository.updateNote(note, () -> refreshNotes());
                        }

                        @Override
                        public void onNoteClick(Note note) {
                            Intent intent = new Intent(getActivity(), AddNoteActivity.class);
                            intent.putExtra("user_id", userId);
                            intent.putExtra("note_id", note.getId());
                            intent.putExtra("note_title", note.getTitle());
                            intent.putExtra("note_content", note.getContent());
                            intent.putExtra("note_category", note.getCategory());
                            intent.putExtra("note_date", note.getDateMillis());
                            intent.putExtra("note_time", note.getTime());
                            intent.putExtra("note_favorite", note.isFavorite());
                            intent.putExtra("note_pinned", note.isPinned());
                            intent.putExtra("note_archived", note.isArchived());
                            intent.putExtra("note_done", note.isDone());
                            intent.putExtra("note_image", note.getImagePath());
                            startActivity(intent);
                        }
                    });
                    recyclerView.setAdapter(adapter);
                });
            }
        });
    }


    private void refreshNotes() {
        if (noteRepository != null) {
            noteRepository.getNotesByDate(userId, selectedDateMillis, result -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter != null) {
                            adapter.updateNotes(result);
                        }
                    });
                }
            });
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshNotes();
    }
}
