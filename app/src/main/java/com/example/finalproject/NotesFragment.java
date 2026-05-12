package com.example.finalproject;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class NotesFragment extends Fragment {


    private String category = "All";
    private NotesAdapter adapter;
    private RecyclerView recyclerView;
    private NoteRepository noteRepository;

    public static NotesFragment newInstance(String category) {
        NotesFragment fragment = new NotesFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString("category");
        }
        noteRepository = new NoteRepository(getContext());
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        TextView tvLabel = view.findViewById(R.id.tvCategoryLabel);
        tvLabel.setText(getString(R.string.category_notes_label, category));


        recyclerView = view.findViewById(R.id.rvNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupAdapter();

        return view;
    }

    private void setupAdapter() {
        noteRepository.getNotesByCategory(category, result -> {
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

    public void refreshNotes() {
        if (noteRepository != null) {
            noteRepository.getNotesByCategory(category, result -> {
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
