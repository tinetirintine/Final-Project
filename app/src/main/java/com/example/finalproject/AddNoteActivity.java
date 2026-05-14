package com.example.finalproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;


public class AddNoteActivity extends AppCompatActivity {

    private long selectedDateMillis;
    private String selectedTime = "No Time";
    private String selectedImagePath = null;
    private Button btnPickDate, btnPickTime;
    private EditText editTitle, editTextNote;
    private ImageView ivNoteImage;
    private View cardImage;
    private NoteRepository noteRepository;
    private int noteId = -1; // -1 means new note
    private int userId;
    private boolean isFavorite = false;
    private boolean isPinned = false;
    private boolean isArchived = false;
    private boolean isDone = false;

    private final Stack<Spannable> undoStack = new Stack<>();
    private final Stack<Spannable> redoStack = new Stack<>();
    private boolean isUndoing = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImagePath = uri.toString();
                    showImage(uri);
                }
            }
    );

    private void showImage(Uri uri) {
        ivNoteImage.setImageURI(uri);
        cardImage.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        userId = getIntent().getIntExtra("user_id", -1);
        noteRepository = new NoteRepository(this);

        editTitle = findViewById(R.id.editTitle);
        editTextNote = findViewById(R.id.editTextNote);
        ivNoteImage = findViewById(R.id.ivNoteImage);
        cardImage = findViewById(R.id.cardImage);
        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        ImageButton btnDone = findViewById(R.id.btnDone);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Toolbar buttons
        ImageButton btnBold = findViewById(R.id.btnBold);
        ImageButton btnItalic = findViewById(R.id.btnItalic);
        ImageButton btnChecklist = findViewById(R.id.btnChecklist);
        ImageButton btnInsertImage = findViewById(R.id.btnInsertImage);
        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        ImageButton btnPin = findViewById(R.id.btnPin);
        ImageButton btnArchive = findViewById(R.id.btnArchive);

        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        ImageButton btnSearch = findViewById(R.id.btnSearch);

        selectedDateMillis = System.currentTimeMillis();

        String[] categories = {"Personal", "School", "Work"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Check if we are editing
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getIntExtra("note_id", -1);
            editTitle.setText(getIntent().getStringExtra("note_title"));

            // Load content as HTML to preserve formatting
            String contentHtml = getIntent().getStringExtra("note_content");
            if (contentHtml != null) {
                editTextNote.setText(Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_LEGACY));
            }

            selectedDateMillis = getIntent().getLongExtra("note_date", System.currentTimeMillis());
            selectedTime = getIntent().getStringExtra("note_time");
            btnPickTime.setText(selectedTime);

            selectedImagePath = getIntent().getStringExtra("note_image");
            if (selectedImagePath != null) {
                showImage(Uri.parse(selectedImagePath));
            }

            isFavorite = getIntent().getBooleanExtra("note_favorite", false);
            isPinned = getIntent().getBooleanExtra("note_pinned", false);
            isArchived = getIntent().getBooleanExtra("note_archived", false);
            isDone = getIntent().getBooleanExtra("note_done", false);

            // Set spinner selection
            String category = getIntent().getStringExtra("note_category");
            for (int i = 0; i < categories.length; i++) {
                if (Objects.equals(categories[i], category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
            updateDateButtonText();
            updateStatusButtons(btnFavorite, btnPin, btnArchive);
        } else if (getIntent().getBooleanExtra("is_template", false)) {
            String templateName = getIntent().getStringExtra("template_name");
            if (templateName == null) templateName = "PERSONAL ROUTINE";
            
            editTitle.setText(templateName);
            
            String template = "";
            if (templateName.equals("Personal Routine") || templateName.equals("PERSONAL ROUTINE")) {
                template = "<b>PERSONAL ROUTINE</b><br><br>" +
                        "MORNING ROUTINE ☀️<br><br>" +
                        "☐ Wake up early<br>" +
                        "☐ Drink water<br>" +
                        "☐ Wash face / skincare<br>" +
                        "☐ Stretching<br>" +
                        "☐ Healthy breakfast<br><br><br>" +
                        "<b>WORKOUT ROUTINE 💪</b><br><br>" +
                        "MONDAY - Upper Body<br>" +
                        "☐ Push-ups<br>" +
                        "☐ Planks<br>" +
                        "☐ Dumbbell Exercises<br><br>" +
                        "TUESDAY - Lower Body<br>" +
                        "☐ Squats<br>" +
                        "☐ Lunges<br>" +
                        "☐ Calf Raises<br><br>" +
                        "WEDNESDAY - Core<br>" +
                        "☐ Crunches<br>" +
                        "☐ Bicycle Crunches<br>" +
                        "☐ Leg Raises<br><br>" +
                        "THURSDAY - Cardio<br>" +
                        "☐ Jogging<br>" +
                        "☐ Jump Rope<br>" +
                        "☐ Walking<br><br>" +
                        "FRIDAY - Full Body<br>" +
                        "☐ Stretching<br>" +
                        "☐ Home Workout<br>" +
                        "☐ Cool Down<br><br><br>" +
                        "<b>WATER INTAKE 💧</b><br><br>" +
                        "☐ 1st Bottle<br>" +
                        "☐ 2nd Bottle<br>" +
                        "☐ 3rd Bottle<br>" +
                        "☐ 4th Bottle<br><br><br>" +
                        "<b>SELF CARE 🌿</b><br><br>" +
                        "☐ Skincare<br>" +
                        "☐ Rest properly<br>" +
                        "☐ Avoid too much screen time<br>" +
                        "☐ Journal / Notes<br><br><br>" +
                        "<b>TODAY'S GOALS ✨</b><br><br>" +
                        "• ____________________<br>" +
                        "• ____________________<br>" +
                        "• ____________________";
            } else if (templateName.equals("School Planner")) {
                template = "<b>SCHOOL PLANNER</b><br><br>" +
                        "Student Name: ____________________<br>" +
                        "Section: ____________________<br>" +
                        "Week Of: ____________________<br><br><br>" +
                        "<b>SUBJECTS & TO-DO LIST</b><br><br>" +
                        "MATHEMATICS<br>" +
                        "☐ Assignment #1<br>" +
                        "☐ Review for Quiz<br>" +
                        "☐ Solve Practice Problems<br><br>" +
                        "ENGLISH<br>" +
                        "☐ Read Chapter 5<br>" +
                        "☐ Essay Draft<br>" +
                        "☐ Vocabulary Review<br><br>" +
                        "SCIENCE<br>" +
                        "☐ Lab Activity<br>" +
                        "☐ Research Topic<br>" +
                        "☐ Study Notes<br><br>" +
                        "PROGRAMMING<br>" +
                        "☐ Finish Coding Activity<br>" +
                        "☐ Debug Errors<br>" +
                        "☐ Submit Project<br><br>" +
                        "P.E.<br>" +
                        "☐ Workout Routine<br>" +
                        "☐ Practice Activity<br><br>" +
                        "NSTP<br>" +
                        "☐ Reflection Paper<br>" +
                        "☐ Community Task<br><br><br>" +
                        "<b>IMPORTANT REMINDERS</b><br><br>" +
                        "• __________________________________<br>" +
                        "• __________________________________<br>" +
                        "• __________________________________<br><br><br>" +
                        "<b>DEADLINES</b><br><br>" +
                        "Monday - ____________________<br>" +
                        "Tuesday - ____________________<br>" +
                        "Wednesday - ____________________<br>" +
                        "Thursday - ____________________<br>" +
                        "Friday - ____________________";
            } else if (templateName.equals("Work Notes")) {
                template = "<b>Your Company</b><br><br>" +
                        "123 Your Street<br>" +
                        "Your City, ST 12345<br>" +
                        "(123) 456-7890<br><br><br>" +
                        "<b>Project Name</b><br><br>" +
                        "4th September 20XX<br><br><br>" +
                        "<b>OVERVIEW</b><br><br>" +
                        "Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper.<br><br><br>" +
                        "<b>GOALS</b><br><br>" +
                        "1. Lorem ipsum dolor sit amet, consectetuer adipiscing elit<br><br>" +
                        "2. Sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.<br><br><br>" +
                        "<b>SPECIFICATIONS</b><br><br>" +
                        "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Typi non habent claritatem insitam.<br><br><br>" +
                        "<b>MILESTONES</b><br><br>" +
                        "Lorem Ipsum<br><br>" +
                        "Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.";
            }

            editTextNote.setText(Html.fromHtml(template, Html.FROM_HTML_MODE_LEGACY));
            // Set category based on template
            if (templateName.equals("School Planner")) {
                spinnerCategory.setSelection(1); // Assuming 1 is School
            } else if (templateName.equals("Work Notes")) {
                spinnerCategory.setSelection(2); // Assuming 2 is Work
            } else {
                spinnerCategory.setSelection(0); // Personal
            }
        }

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnBack.setOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            // Save content as HTML to preserve formatting
            String content = toHtml(editTextNote.getText());
            String category = spinnerCategory.getSelectedItem().toString();

            if (editTextNote.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show();
            } else {
                Note note = new Note(userId, title, content, category, selectedDateMillis, selectedTime);
                note.setFavorite(isFavorite);
                note.setPinned(isPinned);
                note.setArchived(isArchived);
                note.setDone(isDone);
                note.setImagePath(selectedImagePath);

                if (noteId != -1) {
                    note.setId(noteId);
                    noteRepository.updateNote(note, () -> runOnUiThread(() -> {
                        Toast.makeText(this, "Task Updated!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }));
                } else {
                    noteRepository.addNote(note, () -> runOnUiThread(() -> {
                        Toast.makeText(this, "Task Saved!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }));
                }
            }
        });

        // Toolbar Logic
        btnBold.setOnClickListener(v -> toggleStyleSpan(Typeface.BOLD));
        btnItalic.setOnClickListener(v -> toggleStyleSpan(Typeface.ITALIC));
        btnChecklist.setOnClickListener(v -> insertPrefix("[ ] "));
        btnInsertImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateStatusButtons(btnFavorite, btnPin, btnArchive);
            Toast.makeText(this, isFavorite ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
        });

        btnPin.setOnClickListener(v -> {
            isPinned = !isPinned;
            updateStatusButtons(btnFavorite, btnPin, btnArchive);
            Toast.makeText(this, isPinned ? "Note Pinned" : "Note Unpinned", Toast.LENGTH_SHORT).show();
        });

        btnArchive.setOnClickListener(v -> {
            isArchived = !isArchived;
            updateStatusButtons(btnFavorite, btnPin, btnArchive);
            Toast.makeText(this, isArchived ? "Note Archived" : "Note Unarchived", Toast.LENGTH_SHORT).show();
        });

        btnUndo.setOnClickListener(v -> undo());
        btnRedo.setOnClickListener(v -> redo());
        btnSearch.setOnClickListener(v -> Toast.makeText(this, "Search feature coming soon!", Toast.LENGTH_SHORT).show());

        // Spannable Undo stack
        editTextNote.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUndoing) {
                    undoStack.push(new SpannableStringBuilder(s));
                    redoStack.clear();
                }
            }
        });
    }

    // ---- Fixed: API 24+ safe Html conversion ----
    private String toHtml(Spanned spanned) {
        return Html.toHtml(spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }

    private void toggleStyleSpan(int style) {
        int start = editTextNote.getSelectionStart();
        int end = editTextNote.getSelectionEnd();
        if (start != end) {
            Editable editable = editTextNote.getText();
            StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
            boolean exists = false;
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    editable.removeSpan(span);
                    exists = true;
                }
            }
            if (!exists) {
                editable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            Toast.makeText(this, "Select text to apply style", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleUnderlineSpan() {
        int start = editTextNote.getSelectionStart();
        int end = editTextNote.getSelectionEnd();
        if (start != end) {
            Editable editable = editTextNote.getText();
            UnderlineSpan[] spans = editable.getSpans(start, end, UnderlineSpan.class);
            if (spans != null && spans.length > 0) {
                for (UnderlineSpan span : spans) {
                    editable.removeSpan(span);
                }
            } else {
                editable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            Toast.makeText(this, "Select text to underline", Toast.LENGTH_SHORT).show();
        }
    }

    private void insertPrefix(String prefix) {
        int start = editTextNote.getSelectionStart();
        editTextNote.getText().insert(start, prefix);
    }

    private void undo() {
        if (undoStack.size() > 1) {
            isUndoing = true;
            redoStack.push(undoStack.pop());
            editTextNote.setText(undoStack.peek());
            editTextNote.setSelection(editTextNote.getText().length());
            isUndoing = false;
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            isUndoing = true;
            Spannable text = redoStack.pop();
            undoStack.push(text);
            editTextNote.setText(text);
            editTextNote.setSelection(editTextNote.getText().length());
            isUndoing = false;
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            selectedDateMillis = cal.getTimeInMillis();
            updateDateButtonText();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButtonText() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        btnPickDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d",
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)));
    }

    private void updateStatusButtons(ImageButton fav, ImageButton pin, ImageButton arc) {
        fav.setImageDrawable(isFavorite ? ContextCompat.getDrawable(this, android.R.drawable.btn_star_big_on) : ContextCompat.getDrawable(this, android.R.drawable.btn_star_big_off));
        fav.setColorFilter(isFavorite ? ContextCompat.getColor(this, R.color.colorFavorite) : ContextCompat.getColor(this, R.color.text_secondary));

        pin.setColorFilter(isPinned ? ContextCompat.getColor(this, R.color.colorPin) : ContextCompat.getColor(this, R.color.text_secondary));

        arc.setColorFilter(isArchived ? ContextCompat.getColor(this, R.color.colorArchive) : ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            btnPickTime.setText(selectedTime);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }
}