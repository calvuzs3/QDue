package net.calvuz.qdue.domain.calendar.usecases;

import androidx.annotation.NonNull;

import net.calvuz.qdue.core.services.models.OperationResult;
import net.calvuz.qdue.domain.calendar.models.Team;
import net.calvuz.qdue.domain.calendar.repositories.TeamRepository;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeamUseCases {

    // TAG
    private static final String TAG = "TeamUseCases";

    // Executor
    private final ExecutorService mExecutorService;

    // Dependencies
    private final TeamRepository teamRepository;

    public TeamUseCases(@NonNull TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
        this.mExecutorService = Executors.newCachedThreadPool();
    }

    /**
     * Get team by ID.
     */
    public class GetTeamUseCase {
        public CompletableFuture<OperationResult<Team>> execute(
                @NonNull String id
        ) {
            return CompletableFuture.supplyAsync( () -> {
                Log.d( TAG, "Get Team" );
                Team result = teamRepository.getTeamById( id ).join();

                if (result == null) {
                    return OperationResult.failure( "Team not found", OperationResult.OperationType.READ );
                }
                return OperationResult.success( result, OperationResult.OperationType.READ );
            }, mExecutorService);
        }
    }

    public GetTeamUseCase getGetTeamUseCase() {
        return new GetTeamUseCase();
    }
}
