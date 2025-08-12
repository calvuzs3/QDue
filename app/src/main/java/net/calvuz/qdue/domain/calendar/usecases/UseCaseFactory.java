package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;

import net.calvuz.qdue.domain.calendar.repositories.WorkScheduleRepository;

/**
 * ✅ FIXED: UseCaseFactory with proper instantiation
 */
public class UseCaseFactory {

    private final WorkScheduleRepository workScheduleRepository;

    // Cached instances
    private GetWorkScheduleForMonthUseCase getWorkScheduleForMonthUseCase;
//    private GetWorkScheduleForDateUseCase getWorkScheduleForDateUseCase;
//    private IsWorkingDayUseCase isWorkingDayUseCase;
//    private GetUserWorkScheduleUseCase getUserWorkScheduleUseCase;

    public UseCaseFactory(@NonNull WorkScheduleRepository workScheduleRepository) {
        this.workScheduleRepository = workScheduleRepository;
    }

    // ==================== USE CASE PROVIDERS ====================

    public GetWorkScheduleForMonthUseCase getWorkScheduleForMonthUseCase() {
        if (getWorkScheduleForMonthUseCase == null) {
            getWorkScheduleForMonthUseCase = new GetWorkScheduleForMonthUseCase(workScheduleRepository);
        }
        return getWorkScheduleForMonthUseCase;
    }
//
//    public GetWorkScheduleForDateUseCase getWorkScheduleForDateUseCase() {
//        if (getWorkScheduleForDateUseCase == null) {
//            getWorkScheduleForDateUseCase = new GetWorkScheduleForDateUseCase(workScheduleRepository);
//        }
//        return getWorkScheduleForDateUseCase;
//    }
//
//    public IsWorkingDayUseCase getIsWorkingDayUseCase() {
//        if (isWorkingDayUseCase == null) {
//            isWorkingDayUseCase = new IsWorkingDayUseCase(workScheduleRepository);
//        }
//        return isWorkingDayUseCase;
//    }
//
//    public GetUserWorkScheduleUseCase getUserWorkScheduleUseCase() {
//        if (getUserWorkScheduleUseCase == null) {
//            getUserWorkScheduleUseCase = new GetUserWorkScheduleUseCase(
//                    workScheduleRepository,
//                    getWorkScheduleForMonthUseCase()
//            );
//        }
//        return getUserWorkScheduleUseCase;
//    }

    public void cleanup() {
        getWorkScheduleForMonthUseCase = null;
//        getWorkScheduleForDateUseCase = null;
//        isWorkingDayUseCase = null;
//        getUserWorkScheduleUseCase = null;
    }
}
