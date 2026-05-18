package com.example.finalproject;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class NotesFragment extends Fragment {


    private int userId;
    private String category = "All";
    private NotesAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutEmpty;
    private NoteRepository noteRepository;

    public static NotesFragment newInstance(int userId, String category) {
        NotesFragment fragment = new NotesFragment();
        Bundle args = new Bundle();
        args.putInt("user_id", userId);
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id");
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

        Button btnEmptyTrash = view.findViewById(R.id.btnEmptyTrash);
        if ("Trash".equals(category)) {
            btnEmptyTrash.setVisibility(View.VISIBLE);
            btnEmptyTrash.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Empty Trash")
                        .setMessage("Are you sure you want to permanently delete all items in trash?")
                        .setPositiveButton("Empty", (dialog, which) -> {
                            noteRepository.emptyTrash(userId, () -> refreshNotes());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        recyclerView = view.findViewById(R.id.rvNotes);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupAdapter();
        setupSwipe();

        return view;
    }

    private void setupSwipe() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Note note = adapter.getNoteAt(position);
                
                if (direction == ItemTouchHelper.LEFT) {
                    showSwipeActions(note, position);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void showSwipeActions(Note note, int position) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_note_actions, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageButton btnPin = dialogView.findViewById(R.id.btnPinAction);
        ImageButton btnArchive = dialogView.findViewById(R.id.btnArchiveAction);
        ImageButton btnTrash = dialogView.findViewById(R.id.btnTrashAction);
        ImageButton btnFavorite = dialogView.findViewById(R.id.btnFavoriteAction);
        ImageButton btnClose = dialogView.findViewById(R.id.btnCloseAction);

        btnPin.setColorFilter(note.isPinned() ? ContextCompat.getColor(requireContext(), R.color.colorPin) : Color.GRAY);
        btnArchive.setColorFilter(note.isArchived() ? ContextCompat.getColor(requireContext(), R.color.colorArchive) : Color.GRAY);
        btnFavorite.setColorFilter(note.isFavorite() ? ContextCompat.getColor(requireContext(), R.color.colorFavorite) : Color.GRAY);

        btnPin.setOnClickListener(v -> {
            note.setPinned(!note.isPinned());
            noteRepository.updateNote(note, () -> refreshNotes());
            dialog.dismiss();
        });

        btnArchive.setOnClickListener(v -> {
            note.setArchived(!note.isArchived());
            noteRepository.updateNote(note, () -> refreshNotes());
            dialog.dismiss();
        });

        btnFavorite.setOnClickListener(v -> {
            note.setFavorite(!note.isFavorite());
            noteRepository.updateNote(note, () -> refreshNotes());
            dialog.dismiss();
        });

        btnTrash.setOnClickListener(v -> {
            note.setDeleted(true);
            noteRepository.updateNote(note, () -> refreshNotes());
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> adapter.notifyItemChanged(position));
        dialog.show();
    }

    private void setupAdapter() {
        loadNotes();
    }

    private void loadNotes() {
        NoteRepository.Callback<List<Note>> callback = result -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (adapter == null) {
                        adapter = new NotesAdapter(result, new NotesAdapter.OnNoteInteractionListener() {
                            @Override
                            public void onDeleteClick(Note note) {
                                if (category.equals("Trash")) {
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("Permanently Delete")
                                            .setMessage("Are you sure you want to delete this note permanently?")
                                            .setPositiveButton("Delete", (dialog, which) -> {
                                                noteRepository.deleteNote(note, () -> refreshNotes());
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                } else {
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
                            }

                            @Override
                            public void onDoneChanged(Note note, boolean isDone) {
                                note.setDone(isDone);
                                noteRepository.updateNote(note, () -> refreshNotes());
                            }

                            @Override
                            public void onRestoreClick(Note note) {
                                new AlertDialog.Builder(requireContext())
                                    .setTitle("Restore Note")
                                    .setMessage("Do you want to restore this note?")
                                    .setPositiveButton("Restore", (dialog, which) -> {
                                        note.setDeleted(false);
                                        noteRepository.updateNote(note, () -> refreshNotes());
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
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
                                intent.putExtra("is_read_only", "Trash".equals(category));
                                startActivity(intent);
                            }
                        });
                        adapter.setTrashMode("Trash".equals(category));
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.setTrashMode("Trash".equals(category));
                        adapter.updateNotes(result);
                    }
                    layoutEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        };

        if (category.equals("Favorites")) {
            noteRepository.getFavoriteNotes(userId, callback);
        } else if (category.equals("Archive")) {
            noteRepository.getArchivedNotes(userId, callback);
        } else if (category.equals("Trash")) {
            noteRepository.getDeletedNotes(userId, callback);
        } else {
            noteRepository.getNotesByCategory(userId, category, callback);
        }
    }

    public void refreshNotes() {
        if (noteRepository != null) {
            loadNotes();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNotes();
    }
}
