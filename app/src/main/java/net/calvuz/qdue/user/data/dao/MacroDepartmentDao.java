package net.calvuz.qdue.user.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.user.data.entities.MacroDepartment;

import java.util.List;

/**
 * DAO for MacroDepartment operations.
 * Handles macro department data within establishments.
 */
@Dao
public interface MacroDepartmentDao {

    @Query("SELECT * FROM macro_departments ORDER BY name ASC")
    List<MacroDepartment> getAllMacroDepartments();

    @Query("SELECT * FROM macro_departments WHERE id = :id")
    MacroDepartment getMacroDepartmentById(long id);

    @Query("SELECT * FROM macro_departments WHERE establishment_id = :establishmentId ORDER BY name ASC")
    List<MacroDepartment> getMacroDepartmentsByEstablishment(long establishmentId);

    @Query("SELECT * FROM macro_departments WHERE establishment_id = :establishmentId AND name = :name LIMIT 1")
    MacroDepartment getMacroDepartmentByNameAndEstablishment(long establishmentId, String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMacroDepartment(MacroDepartment macroDepartment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertMacroDepartments(List<MacroDepartment> macroDepartments);

    @Update
    void updateMacroDepartment(MacroDepartment macroDepartment);

    @Delete
    void deleteMacroDepartment(MacroDepartment macroDepartment);

    @Query("DELETE FROM macro_departments WHERE id = :id")
    void deleteMacroDepartmentById(long id);

    @Query("DELETE FROM macro_departments")
    void deleteAllMacroDepartments();

    @Query("DELETE FROM macro_departments WHERE establishment_id = :establishmentId")
    void deleteMacroDepartmentsByEstablishment(long establishmentId);

    @Query("SELECT COUNT(*) FROM macro_departments WHERE establishment_id = :establishmentId")
    int getMacroDepartmentCountByEstablishment(long establishmentId);

    @Query("SELECT EXISTS(SELECT 1 FROM macro_departments WHERE establishment_id = :establishmentId AND name = :name)")
    boolean existsByNameAndEstablishment(long establishmentId, String name);
}
