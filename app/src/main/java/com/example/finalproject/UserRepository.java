package com.example.finalproject;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private UserDao userDao;
    private ExecutorService executorService;
    private FirestoreHelper firestoreHelper;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        executorService = Executors.newSingleThreadExecutor();
        firestoreHelper = new FirestoreHelper();
        
        // Clean up accounts deleted more than 30 days ago
        long threshold = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        executorService.execute(() -> userDao.permanentlyDeleteOldAccounts(threshold));
    }

    public void checkAvailability(String email, String phone, Callback<String> callback) {
        executorService.execute(() -> {
            // 1. Check local DB
            User existingEmail = userDao.getUserByEmail(email);
            if (existingEmail != null) {
                callback.onResult("Email already exists locally");
                return;
            }
            User existingPhone = userDao.getUserByPhone(phone);
            if (existingPhone != null) {
                callback.onResult("Phone number already exists locally");
                return;
            }

            // 2. Check Firestore (if online)
            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            firestore.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(emailResult -> {
                    if (!emailResult.isEmpty()) {
                        callback.onResult("Email already registered in cloud");
                    } else {
                        firestore.collection("users").whereEqualTo("phone", phone).get()
                            .addOnSuccessListener(phoneResult -> {
                                if (!phoneResult.isEmpty()) {
                                    callback.onResult("Phone number already registered in cloud");
                                } else {
                                    callback.onResult(null); // Everything available
                                }
                            })
                            .addOnFailureListener(e -> callback.onResult(null)); // Treat failure as available for offline support
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
        });
    }

    public void register(User user, Callback<String> callback) {
        executorService.execute(() -> {
            // 1. Check local DB
            User existingEmail = userDao.getUserByEmail(user.email);
            if (existingEmail != null) {
                callback.onResult("Email already exists locally");
                return;
            }
            User existingPhone = userDao.getUserByPhone(user.phone);
            if (existingPhone != null) {
                callback.onResult("Phone number already exists locally");
                return;
            }

            // 2. Check Firestore (if online)
            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            // Check Email in Firestore
            firestore.collection("users").whereEqualTo("email", user.email).get()
                .addOnSuccessListener(emailResult -> {
                    if (!emailResult.isEmpty()) {
                        callback.onResult("Email already registered in cloud");
                    } else {
                        // Check Phone in Firestore
                        firestore.collection("users").whereEqualTo("phone", user.phone).get()
                            .addOnSuccessListener(phoneResult -> {
                                if (!phoneResult.isEmpty()) {
                                    callback.onResult("Phone number already registered in cloud");
                                } else {
                                    // Everything clear, proceed to register
                                    executorService.execute(() -> {
                                        userDao.registerUser(user);
                                        User registeredUser = userDao.getUserByEmail(user.email);
                                        firestoreHelper.uploadUser(registeredUser);
                                        callback.onResult(null); // Success
                                    });
                                }
                            })
                            .addOnFailureListener(e -> {
                                // If Firestore fails (maybe offline), we allow local-only registration
                                executorService.execute(() -> {
                                    userDao.registerUser(user);
                                    User registeredUser = userDao.getUserByEmail(user.email);
                                    firestoreHelper.uploadUser(registeredUser);
                                    callback.onResult(null);
                                });
                            });
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to local only
                    executorService.execute(() -> {
                        userDao.registerUser(user);
                        User registeredUser = userDao.getUserByEmail(user.email);
                        firestoreHelper.uploadUser(registeredUser);
                        callback.onResult(null);
                    });
                });
        });
    }

    public void updateUser(User user, Runnable onComplete) {
        executorService.execute(() -> {
            userDao.updateUser(user);
            firestoreHelper.uploadUser(user);
            if (onComplete != null) onComplete.run();
        });
    }

    public void getUserByEmail(String email, Callback<User> callback) {
        executorService.execute(() -> {
            User user = userDao.getUserByEmail(email);
            callback.onResult(user);
        });
    }

    public void getAllUsers(Callback<java.util.List<User>> callback) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                java.util.List<User> users = new java.util.ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    User user = doc.toObject(User.class);
                    if (user != null && !"adminpogi".equals(user.email)) {
                        users.add(user);
                    }
                }
                callback.onResult(users);
            })
            .addOnFailureListener(e -> callback.onResult(new java.util.ArrayList<>()));
    }

    public void deleteUserPermanently(Context context, String email, Runnable onComplete) {
        executorService.execute(() -> {
            // Admin "Soft Delete": Mark as deleted but keep in cloud for restoration
            User user = userDao.getUserByEmail(email);
            if (user != null) {
                user.isAdminDeleted = true;
                userDao.updateUser(user);
            }
                
            // Update Cloud
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("isAdminDeleted", true);
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(email)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (onComplete != null) onComplete.run();
                });
        });
    }

    public void restoreUserByAdmin(String email, Runnable onComplete) {
        executorService.execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (user != null) {
                user.isAdminDeleted = false;
                userDao.updateUser(user);
            }
                
            // Update Cloud
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("isAdminDeleted", false);
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(email)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (onComplete != null) onComplete.run();
                });
        });
    }

    public void deleteAccountByUser(String email, Runnable onComplete) {
        executorService.execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (user != null) {
                user.isUserDeleted = true;
                user.deletedAt = System.currentTimeMillis();
                userDao.updateUser(user);
                
                // Update Cloud
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("isUserDeleted", true);
                data.put("deletedAt", user.deletedAt);
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(email)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (onComplete != null) onComplete.run();
                    });
            }
        });
    }

    public void restoreAccountByUser(String email, Runnable onComplete) {
        executorService.execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (user != null) {
                user.isUserDeleted = false;
                user.deletedAt = 0;
                userDao.updateUser(user);
                
                // Update Cloud
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("isUserDeleted", false);
                data.put("deletedAt", 0);
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(email)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (onComplete != null) onComplete.run();
                    });
            }
        });
    }

    public void checkUserExists(String email, Callback<Boolean> callback) {
        executorService.execute(() -> {
            User localUser = userDao.getUserByEmail(email);
            if (localUser != null) {
                callback.onResult(true);
                return;
            }

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        callback.onResult(!queryDocumentSnapshots.isEmpty());
                    })
                    .addOnFailureListener(e -> callback.onResult(false));
        });
    }

    public void login(String email, String password, Callback<User> callback) {
        executorService.execute(() -> {
            // 1. Admin Bypass
            if (email.equals("adminpogi") && password.equals(SecurityUtils.hashPassword("sayolangako"))) {
                User admin = userDao.getUserByEmail("adminpogi");
                if (admin == null) {
                    admin = new User("Admin Pogi", "adminpogi", password, "000", "01/01/2000", "Male");
                    userDao.registerUser(admin);
                    admin = userDao.getUserByEmail("adminpogi");
                }
                callback.onResult(admin);
                return;
            }

            // 2. Try Local Login
            User localUser = userDao.loginUser(email, password);
            if (localUser != null) {
                if (localUser.isAdminDeleted) {
                    callback.onResult(null); // Block login for admin-deleted users
                } else {
                    callback.onResult(localUser); // Return user (even if isUserDeleted is true)
                }
                return;
            }

            // 3. Try Cloud Login (if not found locally)
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            User cloudUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                            if (cloudUser != null) {
                                if (cloudUser.isAdminDeleted) {
                                    callback.onResult(null); // Block login
                                } else {
                                    // Save to local DB for future offline access
                                    executorService.execute(() -> {
                                        userDao.registerUser(cloudUser);
                                        User savedUser = userDao.getUserByEmail(cloudUser.email);
                                        callback.onResult(savedUser);
                                    });
                                }
                            } else {
                                callback.onResult(null);
                            }
                        } else {
                            callback.onResult(null);
                        }
                    })
                    .addOnFailureListener(e -> callback.onResult(null));
        });
    }

    public void updatePassword(String email, String newHashedPassword, Runnable onComplete) {
        executorService.execute(() -> {
            // 1. Update Local
            userDao.updatePassword(email, newHashedPassword);

            // 2. Update Cloud (Use set with merge to be more robust than update)
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("password", newHashedPassword);

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(email)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (onComplete != null) onComplete.run();
                    });
        });
    }

    public void nukeEverything(Context context, Runnable onComplete) {
        executorService.execute(() -> {
            // 1. Clear Local Room DB (Preserve Admin)
            User admin = userDao.getUserByEmail("adminpogi");
            int adminId = (admin != null) ? admin.id : -1;
            
            userDao.nukeUsersExceptAdmin();
            AppDatabase.getInstance(context).noteDao().nukeNotesExceptAdmin(adminId);

            // 2. Clear Firestore (Cloud) (Preserve Admin)
            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            firestore.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String email = doc.getString("email");
                    if ("adminpogi".equals(email)) continue; // Skip Admin

                    // Delete sub-collection notes first
                    doc.getReference().collection("notes").get().addOnSuccessListener(noteSnapshots -> {
                        for (com.google.firebase.firestore.DocumentSnapshot noteDoc : noteSnapshots.getDocuments()) {
                            noteDoc.getReference().delete();
                        }
                        // Delete user document
                        doc.getReference().delete();
                    });
                }
                if (onComplete != null) onComplete.run();
            }).addOnFailureListener(e -> {
                if (onComplete != null) onComplete.run();
            });
        });
    }

    public void nukeLocalOnly(Context context, Runnable onComplete) {
        executorService.execute(() -> {
            User admin = userDao.getUserByEmail("adminpogi");
            int adminId = (admin != null) ? admin.id : -1;

            userDao.nukeUsersExceptAdmin();
            AppDatabase.getInstance(context).noteDao().nukeNotesExceptAdmin(adminId);
            if (onComplete != null) onComplete.run();
        });
    }

    public void restoreEverything(Context context, Runnable onComplete) {
        executorService.execute(() -> {
            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            firestore.collection("users").get().addOnSuccessListener(userSnaps -> {
                if (userSnaps.isEmpty()) {
                    if (onComplete != null) onComplete.run();
                    return;
                }

                int totalUsers = userSnaps.size();
                java.util.concurrent.atomic.AtomicInteger processedUsers = new java.util.concurrent.atomic.AtomicInteger(0);

                for (com.google.firebase.firestore.DocumentSnapshot userDoc : userSnaps.getDocuments()) {
                    User cloudUser = userDoc.toObject(User.class);
                    if (cloudUser != null) {
                        executorService.execute(() -> {
                            // Insert user locally
                            userDao.registerUser(cloudUser);
                            User savedUser = userDao.getUserByEmail(cloudUser.email);
                            
                            if (savedUser != null) {
                                // Fetch notes for this user
                                userDoc.getReference().collection("notes").get().addOnSuccessListener(noteSnaps -> {
                                    executorService.execute(() -> {
                                        for (com.google.firebase.firestore.DocumentSnapshot noteDoc : noteSnaps.getDocuments()) {
                                            Note cloudNote = noteDoc.toObject(Note.class);
                                            if (cloudNote != null) {
                                                cloudNote.setUserId(savedUser.id);
                                                AppDatabase.getInstance(context).noteDao().insertNote(cloudNote);
                                                ReminderHelper.scheduleReminder(context, cloudNote);
                                            }
                                        }
                                        
                                        if (processedUsers.incrementAndGet() == totalUsers) {
                                            if (onComplete != null) onComplete.run();
                                        }
                                    });
                                }).addOnFailureListener(e -> {
                                    if (processedUsers.incrementAndGet() == totalUsers) {
                                        if (onComplete != null) onComplete.run();
                                    }
                                });
                            } else {
                                if (processedUsers.incrementAndGet() == totalUsers) {
                                    if (onComplete != null) onComplete.run();
                                }
                            }
                        });
                    } else {
                        if (processedUsers.incrementAndGet() == totalUsers) {
                            if (onComplete != null) onComplete.run();
                        }
                    }
                }
            }).addOnFailureListener(e -> {
                if (onComplete != null) onComplete.run();
            });
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}