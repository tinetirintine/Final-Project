package com.example.finalproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddNoteActivity extends AppCompatActivity {

    private long selectedDateMillis;
    private String selectedTime = "No Time";
    
    private List<String> imagePaths = new ArrayList<>();
    private List<String> filePaths = new ArrayList<>();
    private List<String> fileNames = new ArrayList<>();

    private Button btnPickDate, btnPickTime;
    private EditText editTitle, editTextNote;
    private LinearLayout layoutImages, layoutFiles;
    private NestedScrollView nestedScrollView;
    
    private NoteRepository noteRepository;
    private int noteId = -1; 
    private int userId;
    private boolean isFavorite = false, isPinned = false, isArchived = false, isDone = false;
    private long archivedAt;

    private String originalTitle = "", originalContent = "", originalCategory = "", originalTime = "";
    private long originalDateMillis;
    private List<String> originalImagePaths = new ArrayList<>(), originalFilePaths = new ArrayList<>();
    private boolean originalFavorite, originalPinned, originalArchived, originalDone;
    
    private Spinner spinnerCategory;
    private ImageButton btnDoneAction;

    private final Stack<Spannable> undoStack = new Stack<>();
    private final Stack<Spannable> redoStack = new Stack<>();
    private boolean isUndoing = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String localPath = saveToInternalStorage(uri, "img_" + System.currentTimeMillis() + ".jpg");
                    imagePaths.add(localPath); refreshAttachmentsUI();
                }
            }
    );

    private final ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String name = getFileNameFromUri(uri);
                    String localPath = saveToInternalStorage(uri, name);
                    filePaths.add(localPath); fileNames.add(name); refreshAttachmentsUI();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isDarkMode", true)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        userId = getIntent().getIntExtra("user_id", -1);
        noteRepository = new NoteRepository(this);
        editTitle = findViewById(R.id.editTitle);
        editTextNote = findViewById(R.id.editTextNote);
        layoutImages = findViewById(R.id.layoutImages);
        layoutFiles = findViewById(R.id.layoutFiles);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnDoneAction = findViewById(R.id.btnDone);
        ImageButton btnBack = findViewById(R.id.btnBack);

        setupToolbar();
        selectedDateMillis = System.currentTimeMillis();
        String[] categories = {"Personal", "School", "Work"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (getIntent().hasExtra("note_id")) loadNoteData();
        else if (getIntent().getBooleanExtra("is_template", false)) loadTemplate();

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnBack.setOnClickListener(v -> handleBackPress());
        btnDoneAction.setOnClickListener(v -> saveNote(true));

        setupSmartLists(); setupCheckboxToggle(); setupUndoRedo(); setupSearch();
        if (getIntent().getBooleanExtra("is_read_only", false)) setReadOnlyMode();
        captureOriginalState(); refreshAttachmentsUI();
    }

    private void loadNoteData() {
        noteId = getIntent().getIntExtra("note_id", -1);
        editTitle.setText(getIntent().getStringExtra("note_title"));
        String contentHtml = getIntent().getStringExtra("note_content");
        if (contentHtml != null) editTextNote.setText(Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_LEGACY));
        selectedDateMillis = getIntent().getLongExtra("note_date", System.currentTimeMillis());
        selectedTime = getIntent().getStringExtra("note_time");
        btnPickTime.setText(selectedTime);

        String iRaw = getIntent().getStringExtra("note_image_paths");
        if (iRaw != null && !iRaw.isEmpty()) imagePaths = new ArrayList<>(java.util.Arrays.asList(iRaw.split("\\|")));
        String fPRaw = getIntent().getStringExtra("note_file_paths");
        if (fPRaw != null && !fPRaw.isEmpty()) filePaths = new ArrayList<>(java.util.Arrays.asList(fPRaw.split("\\|")));
        String fNRaw = getIntent().getStringExtra("note_file_names");
        if (fNRaw != null && !fNRaw.isEmpty()) fileNames = new ArrayList<>(java.util.Arrays.asList(fNRaw.split("\\|")));

        isFavorite = getIntent().getBooleanExtra("note_favorite", false);
        isPinned = getIntent().getBooleanExtra("note_pinned", false);
        isArchived = getIntent().getBooleanExtra("note_archived", false);
        archivedAt = getIntent().getLongExtra("note_archived_at", 0);
        isDone = getIntent().getBooleanExtra("note_done", false);

        String category = getIntent().getStringExtra("note_category");
        if (category != null) {
            for (int i = 0; i < spinnerCategory.getCount(); i++) {
                if (spinnerCategory.getItemAtPosition(i).equals(category)) { spinnerCategory.setSelection(i); break; }
            }
        }
        updateDateButtonText();
        updateStatusButtons(findViewById(R.id.btnFavorite), findViewById(R.id.btnPin), findViewById(R.id.btnArchive));
    }

    private void refreshAttachmentsUI() {
        layoutImages.removeAllViews();
        for (int i = 0; i < imagePaths.size(); i++) {
            String path = imagePaths.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_attachment_image, layoutImages, false);
            ImageView iv = itemView.findViewById(R.id.ivAttachment);
            ImageButton btnRemove = itemView.findViewById(R.id.btnRemove);
            try { iv.setImageURI(Uri.parse(path)); } catch (Exception e) { iv.setImageResource(android.R.drawable.ic_menu_report_image); }
            final int index = i;
            btnRemove.setOnClickListener(v -> { imagePaths.remove(index); refreshAttachmentsUI(); });
            iv.setOnClickListener(v -> openFile(path));
            layoutImages.addView(itemView);
        }
        layoutFiles.removeAllViews();
        for (int i = 0; i < filePaths.size(); i++) {
            String name = (i < fileNames.size()) ? fileNames.get(i) : "Unknown File";
            String path = filePaths.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_attachment_file, layoutFiles, false);
            TextView tvName = itemView.findViewById(R.id.tvFileName);
            ImageButton btnRemove = itemView.findViewById(R.id.btnRemove);
            tvName.setText(name);
            final int index = i;
            btnRemove.setOnClickListener(v -> { filePaths.remove(index); if (index < fileNames.size()) fileNames.remove(index); refreshAttachmentsUI(); });
            itemView.setOnClickListener(v -> openFile(path));
            layoutFiles.addView(itemView);
        }
    }

    private String saveToInternalStorage(Uri uri, String fileName) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.File file = new java.io.File(getFilesDir(), "attachments/" + fileName);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            java.io.FileOutputStream os = new java.io.FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            is.close(); os.close();
            return Uri.fromFile(file).toString();
        } catch (Exception e) { e.printStackTrace(); return uri.toString(); }
    }

    private void openFile(String path) {
        try {
            Uri uri = Uri.parse(path);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (path.startsWith("file://")) {
                java.io.File file = new java.io.File(uri.getPath());
                uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            String type = getContentResolver().getType(uri);
            if (type == null) {
                String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(path);
                if (extension != null) type = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            intent.setDataAndType(uri, type);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) { Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show(); }
    }

    private void saveNote(boolean showConfirmation) {
        String title = editTitle.getText().toString().trim(), content = toHtml(editTextNote.getText()), category = spinnerCategory.getSelectedItem().toString();
        if (editTextNote.getText().toString().trim().isEmpty()) { Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show(); return; }
        Runnable performSave = () -> {
            Note note = new Note(userId, title, content, category, selectedDateMillis, selectedTime);
            note.setFavorite(isFavorite); note.setPinned(isPinned); note.setArchived(isArchived); note.setArchivedAt(archivedAt); note.setDone(isDone);
            note.setImagePathsList(imagePaths); note.setFilePathsList(filePaths); note.setFileNamesList(fileNames);
            if (noteId != -1) { note.setId(noteId); noteRepository.updateNote(note, () -> runOnUiThread(() -> { Toast.makeText(this, "Task Updated!", Toast.LENGTH_SHORT).show(); setResult(RESULT_OK); finish(); })); }
            else { noteRepository.addNote(note, () -> runOnUiThread(() -> { Toast.makeText(this, "Task Saved!", Toast.LENGTH_SHORT).show(); setResult(RESULT_OK); finish(); })); }
        };
        if (showConfirmation) { new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Save Note").setMessage("Are you sure you want to save this note?").setPositiveButton("Yes", (dialog, which) -> performSave.run()).setNegativeButton("No", null).show(); }
        else performSave.run();
    }

    private boolean hasChanged() {
        String currentTitle = editTitle.getText().toString().trim(), currentContent = toHtml(editTextNote.getText()), currentCategory = spinnerCategory.getSelectedItem().toString();
        return !currentTitle.equals(originalTitle) || !currentContent.equals(originalContent) || !currentCategory.equals(originalCategory) || selectedDateMillis != originalDateMillis || !selectedTime.equals(originalTime) || !imagePaths.equals(originalImagePaths) || !filePaths.equals(originalFilePaths) || isFavorite != originalFavorite || isPinned != originalPinned || isArchived != originalArchived || isDone != originalDone;
    }

    private void captureOriginalState() {
        originalTitle = editTitle.getText().toString().trim(); originalContent = toHtml(editTextNote.getText()); originalCategory = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";
        originalDateMillis = selectedDateMillis; originalTime = selectedTime; originalImagePaths = new ArrayList<>(imagePaths); originalFilePaths = new ArrayList<>(filePaths);
        originalFavorite = isFavorite; originalPinned = isPinned; originalArchived = isArchived; originalDone = isDone;
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) { try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) { if (cursor != null && cursor.moveToFirst()) { int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (nameIndex != -1) result = cursor.getString(nameIndex); } } }
        if (result == null) { result = uri.getPath(); int cut = result != null ? result.lastIndexOf('/') : -1; if (cut != -1) result = result.substring(cut + 1); }
        return result != null ? result : "Unknown File";
    }

    private void setupToolbar() {
        findViewById(R.id.btnBold).setOnClickListener(v -> toggleStyleSpan(Typeface.BOLD)); findViewById(R.id.btnItalic).setOnClickListener(v -> toggleStyleSpan(Typeface.ITALIC));
        findViewById(R.id.btnCaps).setOnClickListener(v -> toggleAllCaps()); findViewById(R.id.btnUnderline).setOnClickListener(v -> toggleUnderlineSpan());
        findViewById(R.id.btnBullet).setOnClickListener(v -> toggleListPrefix("bullet")); findViewById(R.id.btnNumber).setOnClickListener(v -> toggleListPrefix("number"));
        findViewById(R.id.btnAlphabet).setOnClickListener(v -> toggleListPrefix("alpha")); findViewById(R.id.btnAlignLeft).setOnClickListener(v -> setAlignment(Layout.Alignment.ALIGN_NORMAL));
        findViewById(R.id.btnAlignCenter).setOnClickListener(v -> setAlignment(Layout.Alignment.ALIGN_CENTER)); findViewById(R.id.btnAlignRight).setOnClickListener(v -> setAlignment(Layout.Alignment.ALIGN_OPPOSITE));
        findViewById(R.id.btnChecklist).setOnClickListener(v -> toggleListPrefix("checkbox")); findViewById(R.id.btnInsertImage).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btnAttachFile).setOnClickListener(v -> pickFileLauncher.launch("*/*"));
        findViewById(R.id.btnFavorite).setOnClickListener(v -> { isFavorite = !isFavorite; updateStatusButtons(v, findViewById(R.id.btnPin), findViewById(R.id.btnArchive)); });
        findViewById(R.id.btnPin).setOnClickListener(v -> { isPinned = !isPinned; updateStatusButtons(findViewById(R.id.btnFavorite), v, findViewById(R.id.btnArchive)); });
        findViewById(R.id.btnArchive).setOnClickListener(v -> { isArchived = !isArchived; archivedAt = isArchived ? System.currentTimeMillis() : 0; updateStatusButtons(findViewById(R.id.btnFavorite), findViewById(R.id.btnPin), v); });
        findViewById(R.id.btnUndo).setOnClickListener(v -> undo()); findViewById(R.id.btnRedo).setOnClickListener(v -> redo());
    }

    private void updateStatusButtons(View fav, View pin, View arc) {
        ((ImageButton)fav).setImageDrawable(ContextCompat.getDrawable(this, isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline));
        ((ImageButton)fav).setColorFilter(ContextCompat.getColor(this, isFavorite ? R.color.colorFavorite : R.color.text_secondary));
        ((ImageButton)pin).setImageDrawable(ContextCompat.getDrawable(this, isPinned ? R.drawable.ic_pin_filled : R.drawable.ic_pin_outline));
        ((ImageButton)pin).setColorFilter(ContextCompat.getColor(this, isPinned ? R.color.colorPin : R.color.text_secondary));
        ((ImageButton)arc).setColorFilter(ContextCompat.getColor(this, isArchived ? R.color.colorArchive : R.color.text_secondary));
    }

    private String toHtml(Spanned s) { return Html.toHtml(s, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE); }
    private void toggleStyleSpan(int style) {
        int s = editTextNote.getSelectionStart(), e = editTextNote.getSelectionEnd(); Editable ed = editTextNote.getText();
        if (s == e) { int[] b = getWordBounds(ed, s); s = b[0]; e = b[1]; }
        if (s < e) {
            StyleSpan[] spans = ed.getSpans(s, e, StyleSpan.class); boolean exists = false;
            for (StyleSpan span : spans) if (span.getStyle() == style) { ed.removeSpan(span); exists = true; }
            if (!exists) ed.setSpan(new StyleSpan(style), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void toggleAllCaps() {
        int s = editTextNote.getSelectionStart(), e = editTextNote.getSelectionEnd(); Editable ed = editTextNote.getText();
        if (s == e) { int[] b = getWordBounds(ed, s); s = b[0]; e = b[1]; }
        if (s < e) { String t = ed.subSequence(s, e).toString(); ed.replace(s, e, t.equals(t.toUpperCase()) ? t.toLowerCase() : t.toUpperCase()); }
    }
    private void setAlignment(Layout.Alignment a) {
        int s = editTextNote.getSelectionStart(), e = editTextNote.getSelectionEnd(); Editable ed = editTextNote.getText();
        int ps = getLineStart(ed, s), pe = e; for (int i = e; i < ed.length(); i++) { if (ed.charAt(i) == '\n') { pe = i; break; } pe = ed.length(); }
        AlignmentSpan[] spans = ed.getSpans(ps, pe, AlignmentSpan.class); for (AlignmentSpan span : spans) ed.removeSpan(span);
        ed.setSpan(new AlignmentSpan.Standard(a), ps, pe, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    private void toggleUnderlineSpan() {
        int s = editTextNote.getSelectionStart(), e = editTextNote.getSelectionEnd(); Editable ed = editTextNote.getText();
        if (s == e) { int[] b = getWordBounds(ed, s); s = b[0]; e = b[1]; }
        if (s < e) {
            UnderlineSpan[] spans = ed.getSpans(s, e, UnderlineSpan.class);
            if (spans != null && spans.length > 0) for (UnderlineSpan span : spans) ed.removeSpan(span);
            else ed.setSpan(new UnderlineSpan(), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private int[] getWordBounds(Editable ed, int p) { int s = p, e = p; while (s > 0 && !Character.isWhitespace(ed.charAt(s - 1))) s--; while (e < ed.length() && !Character.isWhitespace(ed.charAt(e))) e++; return new int[]{s, e}; }
    private void toggleListPrefix(String type) {
        int s = editTextNote.getSelectionStart(); Editable ed = editTextNote.getText(); int ls = getLineStart(ed, s);
        int le = s; for (int i = s; i < ed.length(); i++) { if (ed.charAt(i) == '\n') { le = i; break; } le = ed.length(); }
        String lt = ed.subSequence(ls, le).toString(); Matcher m = Pattern.compile("^([•☐☑]|\\d+\\.|[a-z]\\.)\\s?").matcher(lt); String fm = m.find() ? m.group() : null;
        String nm = ""; switch (type) { case "bullet": nm = "• "; break; case "number": nm = "1. "; break; case "alpha": nm = "a. "; break; case "checkbox": nm = "☐ "; break; }
        if (fm != null) {
            boolean same = false; if (type.equals("bullet") && fm.startsWith("•")) same = true; else if (type.equals("number") && fm.matches("\\d+\\.\\s?")) same = true; else if (type.equals("alpha") && fm.matches("[a-z]\\.\\s?")) same = true; else if (type.equals("checkbox") && (fm.startsWith("☐") || fm.startsWith("☑"))) same = true;
            if (same) { if (type.equals("checkbox") && fm.startsWith("☐")) ed.replace(ls, ls + fm.length(), "☑ "); else ed.delete(ls, ls + fm.length()); } else ed.replace(ls, ls + fm.length(), nm);
        } else ed.insert(ls, nm);
    }
    private int getLineStart(Editable ed, int p) { int l = 0; for (int i = p - 1; i >= 0; i--) { if (ed.charAt(i) == '\n') { l = i + 1; break; } } return l; }
    private void undo() { if (undoStack.size() > 1) { isUndoing = true; redoStack.push(undoStack.pop()); editTextNote.setText(undoStack.peek()); editTextNote.setSelection(editTextNote.getText().length()); isUndoing = false; } }
    private void redo() { if (!redoStack.isEmpty()) { isUndoing = true; Spannable t = redoStack.pop(); undoStack.push(t); editTextNote.setText(t); editTextNote.setSelection(editTextNote.getText().length()); isUndoing = false; } }
    private void setReadOnlyMode() {
        editTitle.setEnabled(false); editTextNote.setFocusable(false); editTextNote.setClickable(true); editTextNote.setCursorVisible(false); findViewById(R.id.spinnerCategory).setEnabled(false); btnPickDate.setEnabled(false); btnPickTime.setEnabled(false);
        findViewById(R.id.btnDone).setVisibility(View.GONE); findViewById(R.id.btnUndo).setVisibility(View.GONE); findViewById(R.id.btnRedo).setVisibility(View.GONE); findViewById(R.id.bottomToolsCard).setVisibility(View.GONE); editTextNote.setOnTouchListener(null);
    }
    private void showDatePicker() { Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDateMillis); new DatePickerDialog(this, (v, y, m, d) -> { c.set(y, m, d); selectedDateMillis = c.getTimeInMillis(); updateDateButtonText(); }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show(); }
    private void updateDateButtonText() { Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDateMillis); btnPickDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))); }
    private void showTimePicker() { Calendar c = Calendar.getInstance(); new TimePickerDialog(this, (v, h, m) -> { selectedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m); btnPickTime.setText(selectedTime); }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show(); }
    private void setupSmartLists() {
        editTextNote.setOnKeyListener((v, kc, ev) -> {
            if (ev.getAction() == android.view.KeyEvent.ACTION_DOWN && kc == android.view.KeyEvent.KEYCODE_ENTER) {
                int s = editTextNote.getSelectionStart(); Editable ed = editTextNote.getText(); int ls = getLineStart(ed, s); String lt = ed.subSequence(ls, s).toString();
                if (lt.startsWith("☐ ") || lt.startsWith("☑ ") || lt.startsWith("• ")) { if (lt.trim().equals("☐") || lt.trim().equals("☑") || lt.trim().equals("•")) { ed.delete(ls, s); return false; } String p = lt.startsWith("☐ ") ? "\n☐ " : (lt.startsWith("☑ ") ? "\n☑ " : "\n• "); ed.insert(s, p); return true; }
                Matcher nm = Pattern.compile("^(\\d+)\\.\\s(.*)").matcher(lt); if (nm.find()) { String n = nm.group(1), c = nm.group(2); if (c.trim().isEmpty()) { ed.delete(ls, s); return false; } try { ed.insert(s, "\n" + (Integer.parseInt(n) + 1) + ". "); return true; } catch (Exception e) {} }
                Matcher am = Pattern.compile("^([a-z])\\.\\s(.*)").matcher(lt); if (am.find()) { String a = am.group(1), c = am.group(2); if (c.trim().isEmpty()) { ed.delete(ls, s); return false; } char na = (char)(a.charAt(0) + 1); if (a.charAt(0) == 'z') na = 'a'; ed.insert(s, "\n" + na + ". "); return true; }
            } return false;
        });
    }
    private void setupCheckboxToggle() { editTextNote.setOnTouchListener((v, ev) -> { if (ev.getAction() == android.view.MotionEvent.ACTION_UP) { float x = ev.getX() - editTextNote.getPaddingLeft(), y = ev.getY() - editTextNote.getPaddingTop(); int o = editTextNote.getOffsetForPosition(x, y); Editable ed = editTextNote.getText(); if (o < ed.length()) { char c = ed.charAt(o); if (c == '☐') { ed.replace(o, o + 1, "☑"); v.performClick(); return true; } else if (c == '☑') { ed.replace(o, o + 1, "☐"); v.performClick(); return true; } } } return false; }); }
    private void setupUndoRedo() { editTextNote.addTextChangedListener(new TextWatcher() { @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {} @Override public void onTextChanged(CharSequence s, int start, int before, int count) {} @Override public void afterTextChanged(Editable s) { if (!isUndoing) { undoStack.push(new SpannableStringBuilder(s)); redoStack.clear(); } } }); }
    private void setupSearch() {
        LinearLayout sBar = findViewById(R.id.layoutSearchBar); EditText sEt = findViewById(R.id.etSearchInNote); ImageButton cBtn = findViewById(R.id.btnCloseSearch);
        findViewById(R.id.btnSearch).setOnClickListener(v -> { sBar.setVisibility(View.VISIBLE); sEt.requestFocus(); });
        cBtn.setOnClickListener(v -> { sBar.setVisibility(View.GONE); sEt.setText(""); clearHighlights(); });
        sEt.addTextChangedListener(new TextWatcher() { @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {} @Override public void onTextChanged(CharSequence s, int start, int before, int count) { highlightText(s.toString()); } @Override public void afterTextChanged(Editable s) {} });
    }
    private void highlightText(String q) {
        clearHighlights(); if (q.isEmpty()) return; Editable ed = editTextNote.getText(); String t = ed.toString().toLowerCase(), lq = q.toLowerCase(); int idx = t.indexOf(lq);
        if (idx >= 0) { final int fi = idx; editTextNote.post(() -> { Layout l = editTextNote.getLayout(); if (l != null) { int line = l.getLineForOffset(fi), y = l.getLineTop(line); View cv = (View)editTextNote.getParent(), cl = (View)cv.getParent(); nestedScrollView.smoothScrollTo(0, cl.getTop() + cv.getTop() + editTextNote.getTop() + y - 100); } }); }
        while (idx >= 0) { ed.setSpan(new BackgroundColorSpan(ContextCompat.getColor(this, R.color.brand_purple_light)), idx, idx + lq.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); idx = t.indexOf(lq, idx + lq.length()); }
    }
    private void clearHighlights() { Editable ed = editTextNote.getText(); BackgroundColorSpan[] spans = ed.getSpans(0, ed.length(), BackgroundColorSpan.class); for (BackgroundColorSpan span : spans) ed.removeSpan(span); }
    private void handleBackPress() { if (hasChanged()) { new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Unsaved Changes").setMessage("Do you want to save your changes?").setPositiveButton("Save", (d, w) -> saveNote(false)).setNegativeButton("Discard", (d, w) -> finish()).setNeutralButton("Cancel", null).show(); } else finish(); }
    private void loadTemplate() {
        String n = getIntent().getStringExtra("template_name"); if (n == null) n = "PERSONAL ROUTINE"; editTitle.setText(n); String t = ""; if (n.equalsIgnoreCase("Personal Routine")) t = "<b>MORNING☀️</b><br>☐ Wake up<br>☐ Stretch<br><br><b>GOALS✨</b><br>• Goal 1"; else if (n.equals("School Planner")) t = "<b>SUBJECTS📚</b><br>☐ Math<br>☐ Science"; else if (n.equals("Work Notes")) t = "<b>OVERVIEW💼</b><br>Notes...";
        editTextNote.setText(Html.fromHtml(t, Html.FROM_HTML_MODE_LEGACY)); if (n.equals("School Planner")) spinnerCategory.setSelection(1); else if (n.equals("Work Notes")) spinnerCategory.setSelection(2); else spinnerCategory.setSelection(0);
    }
}
