package net.calvuz.qdue.smartshifts.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.smartshifts.data.entities.TeamContact;

import java.util.List;

/**
 * Data Access Object for TeamContact entity
 * Handles team contacts for shift coordination and swapping
 */
@Dao
public interface TeamContactDao {

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a new team contact
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TeamContact contact);

    /**
     * Insert multiple contacts
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TeamContact> contacts);

    // ===== UPDATE OPERATIONS =====

    /**
     * Update an existing contact
     */
    @Update
    void update(TeamContact contact);

    /**
     * Update contact basic information
     */
    @Query("UPDATE team_contacts SET contact_name = :name, phone_number = :phone, email = :email, notes = :notes WHERE id = :id")
    void updateContactInfo(String id, String name, String phone, String email, String notes);

    /**
     * Link contact to Android contact
     */
    @Query("UPDATE team_contacts SET android_contact_id = :androidContactId WHERE id = :id")
    void linkToAndroidContact(String id, String androidContactId);

    /**
     * Unlink contact from Android contact
     */
    @Query("UPDATE team_contacts SET android_contact_id = NULL WHERE id = :id")
    void unlinkFromAndroidContact(String id);

    /**
     * Soft delete - mark as inactive
     */
    @Query("UPDATE team_contacts SET is_active = 0 WHERE id = :id")
    void softDelete(String id);

    /**
     * Reactivate a soft-deleted contact
     */
    @Query("UPDATE team_contacts SET is_active = 1 WHERE id = :id")
    void reactivate(String id);

    // ===== DELETE OPERATIONS =====

    /**
     * Hard delete a contact
     */
    @Delete
    void delete(TeamContact contact);

    /**
     * Delete all contacts for a user
     */
    @Query("DELETE FROM team_contacts WHERE user_id = :userId")
    void deleteAllContactsForUser(String userId);

    // ===== QUERY OPERATIONS =====

    /**
     * Get all active contacts for a user
     */
    @Query("SELECT * FROM team_contacts WHERE user_id = :userId AND is_active = 1 ORDER BY contact_name ASC")
    LiveData<List<TeamContact>> getContactsForUser(String userId);

    /**
     * Get all active contacts for a user (synchronous)
     */
    @Query("SELECT * FROM team_contacts WHERE user_id = :userId AND is_active = 1 ORDER BY contact_name ASC")
    List<TeamContact> getContactsForUserSync(String userId);

    /**
     * Get contact by ID
     */
    @Query("SELECT * FROM team_contacts WHERE id = :id")
    TeamContact getContactById(String id);

    /**
     * Get contact by ID (LiveData)
     */
    @Query("SELECT * FROM team_contacts WHERE id = :id")
    LiveData<TeamContact> getContactByIdLive(String id);

    /**
     * Get contacts by shift pattern
     */
    @Query("SELECT * FROM team_contacts WHERE shift_pattern_id = :patternId AND is_active = 1 ORDER BY contact_name ASC")
    List<TeamContact> getContactsByShiftPattern(String patternId);

    /**
     * Get contacts linked to Android contacts
     */
    @Query("SELECT * FROM team_contacts WHERE user_id = :userId AND android_contact_id IS NOT NULL AND is_active = 1")
    List<TeamContact> getLinkedContacts(String userId);

    /**
     * Search contacts by name
     */
    @Query("SELECT * FROM team_contacts WHERE user_id = :userId AND contact_name LIKE :searchQuery AND is_active = 1 ORDER BY contact_name ASC")
    LiveData<List<TeamContact>> searchContactsByName(String userId, String searchQuery);

    /**
     * Get contacts with phone numbers
     */
    @Query("SELECT * FROM team_contacts WHERE user_id = :userId AND phone_number IS NOT NULL AND phone_number != '' AND is_active = 1 ORDER BY contact_name ASC")
    List<TeamContact> getContactsWithPhoneNumbers(String userId);

    /**
     * Get contacts with email addresses
     */
    @Query("SELECT * FROM team_contacts WHERE user_id = :userId AND email IS NOT NULL AND email != '' AND is_active = 1 ORDER BY contact_name ASC")
    List<TeamContact> getContactsWithEmails(String userId);

    /**
     * Count contacts for user
     */
    @Query("SELECT COUNT(*) FROM team_contacts WHERE user_id = :userId AND is_active = 1")
    int getContactsCountForUser(String userId);

    /**
     * Check if contact name already exists for user
     */
    @Query("SELECT COUNT(*) FROM team_contacts WHERE user_id = :userId AND contact_name = :name AND is_active = 1 AND id != :excludeId")
    int countContactsByNameForUser(String userId, String name, String excludeId);

    /**
     * Get contact by Android contact ID
     */
    @Query("SELECT * FROM team_contacts WHERE android_contact_id = :androidContactId AND is_active = 1 LIMIT 1")
    TeamContact getContactByAndroidContactId(String androidContactId);
}