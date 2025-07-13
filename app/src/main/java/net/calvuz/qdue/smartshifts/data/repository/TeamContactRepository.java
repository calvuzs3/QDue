package net.calvuz.qdue.smartshifts.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import net.calvuz.qdue.smartshifts.data.dao.TeamContactDao;
import net.calvuz.qdue.smartshifts.data.entities.TeamContact;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for managing team contacts
 */
@Singleton
public class TeamContactRepository {

    private final TeamContactDao contactDao;
    private final Context context;

    @Inject
    public TeamContactRepository(TeamContactDao contactDao, Context context) {
        this.contactDao = contactDao;
        this.context = context;
    }

    /**
     * Get all contacts for user
     */
    public LiveData<List<TeamContact>> getContactsForUser(String userId) {
        return contactDao.getContactsForUser(userId);
    }

    /**
     * Get contact by ID
     */
    public LiveData<TeamContact> getContactById(String contactId) {
        return contactDao.getContactByIdLive(contactId);
    }

    /**
     * Create new contact
     */
    public CompletableFuture<String> createContact(
            String userId,
            String name,
            String phone,
            String email,
            String notes
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String contactId = UUID.randomUUID().toString();
                TeamContact contact = new TeamContact(contactId, userId, name, phone, email, notes);
                contactDao.insert(contact);
                return contactId;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Update existing contact
     */
    public CompletableFuture<Boolean> updateContact(TeamContact contact) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                contactDao.update(contact);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Delete contact
     */
    public CompletableFuture<Boolean> deleteContact(String contactId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                contactDao.softDelete(contactId);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Search contacts by name
     */
    public LiveData<List<TeamContact>> searchContactsByName(String userId, String query) {
        String searchQuery = "%" + query + "%";
        return contactDao.searchContactsByName(userId, searchQuery);
    }

    /**
     * Link contact to Android contact
     */
    public CompletableFuture<Boolean> linkToAndroidContact(String contactId, String androidContactId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                contactDao.linkToAndroidContact(contactId, androidContactId);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}