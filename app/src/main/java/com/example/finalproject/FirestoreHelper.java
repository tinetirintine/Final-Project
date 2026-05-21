package com.example.finalproject;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {
    private static final String USERS_COLLECTION = "users";
    private static final String NOTES_COLLECTION = "notes";
    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void uploadUser(User user) {
        db.collection(USERS_COLLECTION).document(user.email)
                .set(user, SetOptions.merge());
    }

    public void uploadNote(Note note, String userEmail) {
        db.collection(USERS_COLLECTION)
                .document(userEmail)
                .collection(NOTES_COLLECTION)
                .document(String.valueOf(note.getId()))
                .set(note, SetOptions.merge());
    }

    public void deleteNote(Note note, String userEmail) {
        db.collection(USERS_COLLECTION)
                .document(userEmail)
                .collection(NOTES_COLLECTION)
                .document(String.valueOf(note.getId()))
                .delete();
    }

    public void getAllNotes(String userEmail, com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        db.collection(USERS_COLLECTION)
                .document(userEmail)
                .collection(NOTES_COLLECTION)
                .get()
                .addOnSuccessListener(listener);
    }
}
