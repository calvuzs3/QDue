package net.calvuz.qdue.user.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.entities.User;
import net.calvuz.qdue.user.data.models.UserWithOrganization;

import java.util.List;

/**
 * DAO for Establishment operations.
 * Handles company/establishment data persistence.
 */
@Dao
public interface EstablishmentDao {

    @Query("SELECT * FROM establishments ORDER BY name ASC")
    List<Establishment> getAllEstablishments();

    @Query("SELECT * FROM establishments WHERE id = :id")
    Establishment getEstablishmentById(long id);

    @Query("SELECT * FROM establishments WHERE name = :name LIMIT 1")
    Establishment getEstablishmentByName(String name);

    @Query("SELECT * FROM establishments WHERE code = :code LIMIT 1")
    Establishment getEstablishmentByCode(String code);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertEstablishment(Establishment establishment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertEstablishments(List<Establishment> establishments);

    @Update
    void updateEstablishment(Establishment establishment);

    @Delete
    void deleteEstablishment(Establishment establishment);

    @Query("DELETE FROM establishments WHERE id = :id")
    void deleteEstablishmentById(long id);

    @Query("SELECT COUNT(*) FROM establishments")
    int getEstablishmentCount();

    @Query("SELECT EXISTS(SELECT 1 FROM establishments WHERE name = :name)")
    boolean existsByName(String name);

    @Query("SELECT EXISTS(SELECT 1 FROM establishments WHERE code = :code)")
    boolean existsByCode(String code);
}
