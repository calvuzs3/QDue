package net.calvuz.qdue.user.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.calvuz.qdue.user.data.entities.SubDepartment;

import java.util.List;

/**
 * DAO for SubDepartment operations.
 * Handles sub-department data within macro departments.
 */
@Dao
public interface SubDepartmentDao {

    @Query("SELECT * FROM sub_departments ORDER BY name ASC")
    List<SubDepartment> getAllSubDepartments();

    @Query("SELECT * FROM sub_departments WHERE id = :id")
    SubDepartment getSubDepartmentById(long id);

    @Query("SELECT * FROM sub_departments WHERE macro_department_id = :macroDepartmentId ORDER BY name ASC")
    List<SubDepartment> getSubDepartmentsByMacroDepartment(long macroDepartmentId);

    @Query("SELECT * FROM sub_departments WHERE macro_department_id = :macroDepartmentId AND name = :name LIMIT 1")
    SubDepartment getSubDepartmentByNameAndMacroDepartment(long macroDepartmentId, String name);

    @Query("SELECT sd.* FROM sub_departments sd " +
            "INNER JOIN macro_departments md ON sd.macro_department_id = md.id " +
            "WHERE md.establishment_id = :establishmentId ORDER BY sd.name ASC")
    List<SubDepartment> getSubDepartmentsByEstablishment(long establishmentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSubDepartment(SubDepartment subDepartment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertSubDepartments(List<SubDepartment> subDepartments);

    @Update
    void updateSubDepartment(SubDepartment subDepartment);

    @Delete
    void deleteSubDepartment(SubDepartment subDepartment);

    @Query("DELETE FROM sub_departments WHERE id = :id")
    void deleteSubDepartmentById(long id);

    @Query("DELETE FROM sub_departments")
    void deleteAllSubDepartments();

    @Query("DELETE FROM sub_departments WHERE macro_department_id = :macroDepartmentId")
    void deleteSubDepartmentsByMacroDepartment(long macroDepartmentId);

    @Query("SELECT COUNT(*) FROM sub_departments WHERE macro_department_id = :macroDepartmentId")
    int getSubDepartmentCountByMacroDepartment(long macroDepartmentId);

    @Query("SELECT EXISTS(SELECT 1 FROM sub_departments WHERE macro_department_id = :macroDepartmentId AND name = :name)")
    boolean existsByNameAndMacroDepartment(long macroDepartmentId, String name);
}
