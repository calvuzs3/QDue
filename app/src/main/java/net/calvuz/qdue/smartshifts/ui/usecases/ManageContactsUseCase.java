package net.calvuz.qdue.smartshifts.ui.usecases;

import android.content.Context;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.R;
import net.calvuz.qdue.smartshifts.data.entities.TeamContact;
import net.calvuz.qdue.smartshifts.data.repository.TeamContactRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Use case for managing team contacts
 */
public class ManageContactsUseCase {

    private final TeamContactRepository contactRepository;
    private final Context context;

    @Inject
    public ManageContactsUseCase(TeamContactRepository contactRepository, Context context) {
        this.contactRepository = contactRepository;
        this.context = context;
    }

    /**
     * Get all contacts for user
     */
    public LiveData<List<TeamContact>> getUserContacts(String userId) {
        return contactRepository.getContactsForUser(userId);
    }

    /**
     * Create new contact with validation
     */
    public CompletableFuture<ContactResult> createContact(
            String userId,
            String name,
            String phone,
            String email,
            String notes
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate input
            ContactValidation validation = validateContactInput(name, phone, email);
            if (!validation.isValid) {
                ContactResult result = new ContactResult();
                result.success = false;
                result.errorMessage = validation.errorMessage;
                return result;
            }

            try {
                String contactId = contactRepository.createContact(userId, name, phone, email, notes).get();

                ContactResult result = new ContactResult();
                result.success = contactId != null;
                result.contactId = contactId;
                if (result.success) {
                    result.message = context.getString(
                            R.string.success_contact_saved
                    );
                }

                return result;

            } catch (Exception e) {
                ContactResult result = new ContactResult();
                result.success = false;
                result.errorMessage = e.getMessage();
                return result;
            }
        });
    }

    /**
     * Update existing contact
     */
    public CompletableFuture<ContactResult> updateContact(TeamContact contact) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate input
            ContactValidation validation = validateContactInput(
                    contact.contactName,
                    contact.phoneNumber,
                    contact.email
            );
            if (!validation.isValid) {
                ContactResult result = new ContactResult();
                result.success = false;
                result.errorMessage = validation.errorMessage;
                return result;
            }

            try {
                boolean success = contactRepository.updateContact(contact).get();

                ContactResult result = new ContactResult();
                result.success = success;
                result.contactId = contact.id;
                if (success) {
                    result.message = context.getString(
                            R.string.success_contact_saved
                    );
                }

                return result;

            } catch (Exception e) {
                ContactResult result = new ContactResult();
                result.success = false;
                result.errorMessage = e.getMessage();
                return result;
            }
        });
    }

    /**
     * Delete contact
     */
    public CompletableFuture<ContactResult> deleteContact(String contactId) {
        return contactRepository.deleteContact(contactId)
                .thenApply(success -> {
                    ContactResult result = new ContactResult();
                    result.success = success;
                    if (success) {
                        result.message = context.getString(
                                R.string.success_contact_deleted
                        );
                    }
                    return result;
                });
    }

    /**
     * Search contacts by name
     */
    public LiveData<List<TeamContact>> searchContacts(String userId, String query) {
        return contactRepository.searchContactsByName(userId, query);
    }

    /**
     * Link contact to Android contact
     */
    public CompletableFuture<Boolean> linkToAndroidContact(String contactId, String androidContactId) {
        return contactRepository.linkToAndroidContact(contactId, androidContactId);
    }

    /**
     * Validate contact input data
     */
    private ContactValidation validateContactInput(String name, String phone, String email) {
        ContactValidation validation = new ContactValidation();

        // Validate name (required)
        if (name == null || name.trim().isEmpty()) {
            validation.isValid = false;
            validation.errorMessage = context.getString(
                    R.string.validation_contact_name_required
            );
            return validation;
        }

        // Validate phone (optional, but if provided must be valid)
        if (phone != null && !phone.trim().isEmpty() && !isValidPhoneNumber(phone)) {
            validation.isValid = false;
            validation.errorMessage = context.getString(
                    R.string.validation_invalid_phone
            );
            return validation;
        }

        // Validate email (optional, but if provided must be valid)
        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            validation.isValid = false;
            validation.errorMessage = context.getString(
                    R.string.validation_invalid_email
            );
            return validation;
        }

        validation.isValid = true;
        return validation;
    }

    /**
     * Basic phone number validation
     */
    private boolean isValidPhoneNumber(String phone) {
        // Simple validation - can be enhanced
        return phone.matches("^[+]?[0-9\\s\\-\\(\\)]{6,20}$");
    }

    /**
     * Basic email validation
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ===== RESULT CLASSES =====

    public static class ContactResult {
        public boolean success;
        public String contactId;
        public String message;
        public String errorMessage;
    }

    public static class ContactValidation {
        public boolean isValid = true;
        public String errorMessage;
    }
}
