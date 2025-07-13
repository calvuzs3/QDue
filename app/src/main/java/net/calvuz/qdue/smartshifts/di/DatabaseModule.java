package net.calvuz.qdue.smartshifts.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import net.calvuz.qdue.smartshifts.data.database.SmartShiftsDatabase;
import net.calvuz.qdue.smartshifts.data.dao.ShiftTypeDao;
import net.calvuz.qdue.smartshifts.data.dao.ShiftPatternDao;
import net.calvuz.qdue.smartshifts.data.dao.UserShiftAssignmentDao;
import net.calvuz.qdue.smartshifts.data.dao.SmartShiftEventDao;
import net.calvuz.qdue.smartshifts.data.dao.TeamContactDao;

import javax.inject.Singleton;

/**
 * Hilt module for providing database dependencies
 * Manages database instance and DAO injection
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * Provide SmartShifts database instance
     */
    @Provides
    @Singleton
    public SmartShiftsDatabase provideSmartShiftsDatabase(@ApplicationContext Context context) {
        return SmartShiftsDatabase.getDatabase(context);
    }

    /**
     * Provide ShiftType DAO
     */
    @Provides
    public ShiftTypeDao provideShiftTypeDao(SmartShiftsDatabase database) {
        return database.shiftTypeDao();
    }

    /**
     * Provide ShiftPattern DAO
     */
    @Provides
    public ShiftPatternDao provideShiftPatternDao(SmartShiftsDatabase database) {
        return database.shiftPatternDao();
    }

    /**
     * Provide UserShiftAssignment DAO
     */
    @Provides
    public UserShiftAssignmentDao provideUserShiftAssignmentDao(SmartShiftsDatabase database) {
        return database.userAssignmentDao();
    }

    /**
     * Provide SmartShiftEvent DAO
     */
    @Provides
    public SmartShiftEventDao provideSmartShiftEventDao(SmartShiftsDatabase database) {
        return database.smartEventDao();
    }

    /**
     * Provide TeamContact DAO
     */
    @Provides
    public TeamContactDao provideTeamContactDao(SmartShiftsDatabase database) {
        return database.teamContactDao();
    }
}
