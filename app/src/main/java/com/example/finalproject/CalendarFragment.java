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


public class CalendarFragment extends Fragment {


    private NotesAdapter adapter;
    private long selectedDateMillis;
    private NoteRepository noteRepository;
    private RecyclerView recyclerView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);


        noteRepository = new NoteRepository(getContext());
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
        noteRepository.getNotesByDate(selectedDateMillis, result -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new NotesAdapter(result, new NotesAdapter.OnNoteInteractionListener() {
                        @Override
                        public void onDeleteClick(Note note) {
                            new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Note")
                                .setMessage("Are you sure you want to delete this note permanently?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    noteRepository.deleteNote(note, () -> refreshNotes());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        }

                        @Override
                        public void onNoteClick(Note note) {
                            Intent intent = new Intent(getActivity(), AddNoteActivity.class);
                            intent.putExtra("note_id", note.getId());
                            intent.putExtra("note_title", note.getTitle());
                            intent.putExtra("note_content", note.getContent());
                            intent.putExtra("note_category", note.getCategory());
                            intent.putExtra("note_date", note.getDateMillis());
                            intent.putExtra("note_time", note.getTime());
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
            noteRepository.getNotesByDate(selectedDateMillis, result -> {
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
