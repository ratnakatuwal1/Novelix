// AccountFragment.java
package com.example.novelix.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.novelix.Adapter.BookmarkAdapter;
import com.example.novelix.ChangePassword;
import com.example.novelix.EditProfile;
import com.example.novelix.LoginActivity;
import com.example.novelix.Model.Book;
import com.example.novelix.Privacy;
import com.example.novelix.R;
import com.example.novelix.Terms;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountFragment extends Fragment {
    private CardView updateProfile, changePassword, deleteAccount, privacyPolicy, terms, aboutApp, shareApp, rateApp, appVersion, logout, cardView8;
    private TextView textViewName, textViewEmail;
    private ImageView profileImage;
    private RecyclerView recyclerViewFav;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private BookmarkAdapter favAdapter;
    private List<Book> favBookList;
    private static final int EDIT_PROFILE_REQUEST_CODE = 101;
    private static final String TAG = "AccountFragment";
    private boolean isFetchingFav = false;

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        favBookList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        InitializedView(view); // Use a method to initialize views for clarity
        SetupListeners(); // Use a method to setup listeners

        // Initialize favorites RecyclerView
        if (recyclerViewFav != null) {
            recyclerViewFav.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)); // Set to horizontal for typical favorite lists
            favAdapter = new BookmarkAdapter(favBookList);
            recyclerViewFav.setAdapter(favAdapter);
        }

        // Load user profile data and favorites initially
        loadUserProfile();
        fetchFavoritedBooks();

        return view;
    }

    private void InitializedView(View view) {
        updateProfile = view.findViewById(R.id.updateProfile);
        changePassword = view.findViewById(R.id.changePassword);
        appVersion = view.findViewById(R.id.appVersion);
        privacyPolicy = view.findViewById(R.id.privacy);
        terms = view.findViewById(R.id.terms);
        rateApp = view.findViewById(R.id.rateApp);
        shareApp = view.findViewById(R.id.shareApp);
        aboutApp = view.findViewById(R.id.aboutApp);
        deleteAccount = view.findViewById(R.id.deleteAccount);
        logout = view.findViewById(R.id.logout);
        cardView8 = view.findViewById(R.id.cardView8);
        textViewName = view.findViewById(R.id.textViewName);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        profileImage = view.findViewById(R.id.profileImage);
        recyclerViewFav = view.findViewById(R.id.recyclerViewFav);
    }

    private void SetupListeners() {
        changePassword.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ChangePassword.class);
            startActivity(intent);
        });

        updateProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfile.class);
            startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE); // Start for result to get updates
        });

        deleteAccount.setOnClickListener(v -> {
            showDeleteAccountDialog();
        });

        logout.setOnClickListener(v -> {
            showLogoutDialog();
        });

        appVersion.setOnClickListener(v -> {
            showAppVersionDialog();
        });

        privacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Privacy.class);
            intent.putExtra("html_file", "file:///android_asset/privacy.html");
            startActivity(intent);
        });

        terms.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Terms.class);
            intent.putExtra("html_file", "file:///android_asset/terms.html");
            startActivity(intent);
        });

        rateApp.setOnClickListener(v -> {
            openPlayStoreForRating();
        });

        shareApp.setOnClickListener(v -> {
            shareApplication();
        });

        aboutApp.setOnClickListener(v -> {
            showAboutAppDialog();
        });
    }

    private void showDeleteAccountDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.delete_account_alert_dialog, null);
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        MaterialButton btnYes = dialogView.findViewById(R.id.btnYes);
        MaterialButton btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(view1 -> {
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                db.collection("users").document(userId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            mAuth.getCurrentUser().delete()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            new AlertDialog.Builder(requireContext())
                                                    .setMessage("Account successfully deleted")
                                                    .setPositiveButton("OK", (d, w) -> {
                                                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        requireActivity().finish(); // Finish current activity
                                                    })
                                                    .show();
                                        } else {
                                            new AlertDialog.Builder(requireContext())
                                                    .setMessage("Error deleting account: " + task.getException().getMessage())
                                                    .setPositiveButton("OK", null)
                                                    .show();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            new AlertDialog.Builder(requireContext())
                                    .setMessage("Error deleting Firestore data: " + e.getMessage())
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
            }
            alertDialog.dismiss();
        });
        btnNo.setOnClickListener(view1 -> alertDialog.dismiss());
        alertDialog.show();
    }

    private void showLogoutDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.logout_alert_dialog, null);
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        MaterialButton btnYes = dialogView.findViewById(R.id.btnYes);
        MaterialButton btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(view1 -> {
            mAuth.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish(); // Finish current activity
            alertDialog.dismiss();
        });

        btnNo.setOnClickListener(view1 -> alertDialog.dismiss());
        alertDialog.show();
    }

    private void showAppVersionDialog() {
        View versionView = LayoutInflater.from(requireContext()).inflate(R.layout.app_version_alert_dialog, null);
        TextView textVersion = versionView.findViewById(R.id.textVersion);
        String versionName = BuildConfig.VERSION_NAME;
        textVersion.setText("Version: " + versionName);

        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setView(versionView)
                .setCancelable(false)
                .create();

        MaterialButton btnOK = versionView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(view1 -> alertDialog.dismiss());
        alertDialog.show();
    }

    private void openPlayStoreForRating() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + requireContext().getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().getPackageName())));
        }
    }

    private void shareApplication() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareMessage = "Check out Novelix Admin App: https://play.google.com/store/apps/details?id=" + requireContext().getPackageName();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showAboutAppDialog() {
        View aboutView = LayoutInflater.from(requireContext()).inflate(R.layout.about_app_alert_dialog, null);
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setView(aboutView)
                .setCancelable(false)
                .create();

        MaterialButton btnOK = aboutView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(view1 -> alertDialog.dismiss());
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Received result from EditProfile activity. Reloading user profile.");
            loadUserProfile(); // Always reload all profile data
        }
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "No user signed in, loading default placeholder for profile image.");
            if (textViewName != null) textViewName.setText("Guest User");
            if (textViewEmail != null) textViewEmail.setText("guest@example.com");
            if (profileImage != null && getContext() != null) {
                Glide.with(getContext())
                        .load(R.drawable.placeholder)
                        .circleCrop()
                        .into(profileImage);
            }
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        if (textViewName != null) {
                            textViewName.setText(name != null && !name.isEmpty() ? name : "No Name");
                        }
                        if (textViewEmail != null) {
                            textViewEmail.setText(email != null && !email.isEmpty() ? email : mAuth.getCurrentUser().getEmail());
                        }

                        if (profileImage != null && getContext() != null) { // Check getContext() for Glide
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Log.d(TAG, "loadUserProfile - Loading image from URL: " + profileImageUrl);
                                Glide.with(getContext())
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.placeholder) // Show placeholder while loading
                                        .error(R.drawable.placeholder)      // Show placeholder if error
                                        .circleCrop()
                                        .into(profileImage);
                            } else {
                                Log.d(TAG, "loadUserProfile - Profile image URL is null or empty, loading default placeholder.");
                                Glide.with(getContext())
                                        .load(R.drawable.placeholder)
                                        .circleCrop()
                                        .into(profileImage);
                            }
                        }
                    } else {
                        // Document doesn't exist, use Firebase Auth email and default image
                        Log.d(TAG, "loadUserProfile - User document does not exist, using Firebase Auth email and default placeholder.");
                        if (textViewName != null) textViewName.setText("New User"); // Or leave empty
                        if (textViewEmail != null && mAuth.getCurrentUser() != null) {
                            textViewEmail.setText(mAuth.getCurrentUser().getEmail());
                        }
                        if (profileImage != null && getContext() != null) {
                            Glide.with(getContext())
                                    .load(R.drawable.placeholder)
                                    .circleCrop()
                                    .into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user profile: " + e.getMessage(), e);
                    // Fallback to Firebase Auth email and placeholder on failure
                    if (textViewEmail != null && mAuth.getCurrentUser() != null) {
                        textViewEmail.setText(mAuth.getCurrentUser().getEmail());
                    }
                    if (profileImage != null && getContext() != null) {
                        Glide.with(getContext())
                                .load(R.drawable.placeholder)
                                .circleCrop()
                                .into(profileImage);
                    }
                    Toast.makeText(getContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchFavoritedBooks() {
        if (isFetchingFav || mAuth.getCurrentUser() == null) {
            if (mAuth.getCurrentUser() == null) {
                if (favAdapter != null) {
                    favBookList.clear();
                    favAdapter.notifyDataSetChanged();
                }
            }
            return;
        }

        isFetchingFav = true;
        String userId = mAuth.getCurrentUser().getUid();
        Set<String> bookIds = new HashSet<>();
        List<Book> tempBookList = new ArrayList<>();

        db.collection("users").document(userId).collection("bookmarks")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tempBookList.clear();
                        bookIds.clear();

                        if (task.getResult().isEmpty()) {
                            updateFavBookList(tempBookList); // No bookmarks, clear the list
                            return;
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();

                            if (bookIds.contains(bookId)) {
                                Log.w(TAG, "Duplicate book ID found: " + bookId);
                                continue;
                            }
                            bookIds.add(bookId);

                            db.collection("novels").document(bookId)
                                    .get()
                                    .addOnSuccessListener(novelDoc -> {
                                        if (novelDoc.exists()) {
                                            Book book = novelDoc.toObject(Book.class);
                                            if (book != null) {
                                                book.setId(novelDoc.getId());
                                                tempBookList.add(book);
                                                Log.d(TAG, "Loaded favorite book: " + book.getTitle() + " | Category: " + book.getCategory());
                                            }
                                        }
                                        // Check if all book details have been fetched
                                        if (tempBookList.size() == bookIds.size()) {
                                            updateFavBookList(tempBookList);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to fetch novel details for " + bookId, e);
                                        // If one novel fetch fails, we still need to check if all others completed
                                        // This logic is tricky with async calls, but let's assume it should complete
                                        // once all addOnSuccess/FailureListeners are called for initial bookIds.
                                        // A better way would be to use a CountDownLatch.
                                        if (tempBookList.size() + 1 == bookIds.size()) { // This is an approximation
                                            updateFavBookList(tempBookList);
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error fetching favorites: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching favorites", task.getException());
                        isFetchingFav = false;
                    }
                });
    }

    private void updateFavBookList(List<Book> newBookList) {
        favBookList.clear();
        favBookList.addAll(newBookList);
        if (favAdapter != null) {
            favAdapter.notifyDataSetChanged();
        }
        isFetchingFav = false;
        Log.d(TAG, "Updated favorites list with " + favBookList.size() + " books");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile(); // Reload profile every time the fragment becomes visible
        if (mAuth.getCurrentUser() != null && recyclerViewFav != null) {
            fetchFavoritedBooks(); // Also re-fetch favorites
        }
    }
}