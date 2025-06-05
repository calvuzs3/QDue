package net.calvuz.qdue.user.data.repository;

import android.content.Context;

import net.calvuz.qdue.user.data.database.UserDatabase;
import net.calvuz.qdue.user.data.dao.EstablishmentDao;
import net.calvuz.qdue.user.data.dao.MacroDepartmentDao;
import net.calvuz.qdue.user.data.dao.SubDepartmentDao;
import net.calvuz.qdue.user.data.entities.Establishment;
import net.calvuz.qdue.user.data.entities.MacroDepartment;
import net.calvuz.qdue.user.data.entities.SubDepartment;
import net.calvuz.qdue.user.data.models.CompleteOrganizationalHierarchy;
import net.calvuz.qdue.user.data.models.EstablishmentWithDepartments;
import net.calvuz.qdue.utils.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for organizational data operations.
 * Handles establishments, macro departments, and sub departments.
 */
public class OrganizationRepository {

    private static final String TAG = "OrganizationRepository";

    private final EstablishmentDao establishmentDao;
    private final MacroDepartmentDao macroDepartmentDao;
    private final SubDepartmentDao subDepartmentDao;
    private final ExecutorService executorService;
    private static volatile OrganizationRepository INSTANCE;

    private OrganizationRepository(Context context) {
        UserDatabase database = UserDatabase.getInstance(context);
        establishmentDao = database.establishmentDao();
        macroDepartmentDao = database.macroDepartmentDao();
        subDepartmentDao = database.subDepartmentDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    public static OrganizationRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (OrganizationRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OrganizationRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // Establishment operations
    public void getAllEstablishments(OnEstablishmentListListener listener) {
        executorService.execute(() -> {
            try {
                List<Establishment> establishments = establishmentDao.getAllEstablishments();
                if (listener != null) {
                    listener.onSuccess(establishments);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting establishments: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void insertEstablishment(Establishment establishment, OnEstablishmentOperationListener listener) {
        executorService.execute(() -> {
            try {
                long id = establishmentDao.insertEstablishment(establishment);
                establishment.setId(id);
                if (listener != null) {
                    listener.onSuccess(establishment);
                }
                Log.d(TAG, "Establishment inserted with ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting establishment: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void updateEstablishment(Establishment establishment, OnEstablishmentOperationListener listener) {
        executorService.execute(() -> {
            try {
                establishmentDao.updateEstablishment(establishment);
                if (listener != null) {
                    listener.onSuccess(establishment);
                }
                Log.d(TAG, "Establishment updated: " + establishment.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error updating establishment: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Macro Department operations
    public void getMacroDepartmentsByEstablishment(long establishmentId, OnMacroDepartmentListListener listener) {
        executorService.execute(() -> {
            try {
                List<MacroDepartment> departments = macroDepartmentDao.getMacroDepartmentsByEstablishment(establishmentId);
                if (listener != null) {
                    listener.onSuccess(departments);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting macro departments: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void insertMacroDepartment(MacroDepartment macroDepartment, OnMacroDepartmentOperationListener listener) {
        executorService.execute(() -> {
            try {
                long id = macroDepartmentDao.insertMacroDepartment(macroDepartment);
                macroDepartment.setId(id);
                if (listener != null) {
                    listener.onSuccess(macroDepartment);
                }
                Log.d(TAG, "Macro department inserted with ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting macro department: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Sub Department operations
    public void getSubDepartmentsByMacroDepartment(long macroDepartmentId, OnSubDepartmentListListener listener) {
        executorService.execute(() -> {
            try {
                List<SubDepartment> subDepartments = subDepartmentDao.getSubDepartmentsByMacroDepartment(macroDepartmentId);
                if (listener != null) {
                    listener.onSuccess(subDepartments);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting sub departments: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void insertSubDepartment(SubDepartment subDepartment, OnSubDepartmentOperationListener listener) {
        executorService.execute(() -> {
            try {
                long id = subDepartmentDao.insertSubDepartment(subDepartment);
                subDepartment.setId(id);
                if (listener != null) {
                    listener.onSuccess(subDepartment);
                }
                Log.d(TAG, "Sub department inserted with ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting sub department: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Validation methods
    public void checkEstablishmentNameExists(String name, OnValidationListener listener) {
        executorService.execute(() -> {
            try {
                boolean exists = establishmentDao.existsByName(name);
                if (listener != null) {
                    listener.onValidationResult(exists);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking establishment name: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void checkMacroDepartmentNameExists(long establishmentId, String name, OnValidationListener listener) {
        executorService.execute(() -> {
            try {
                boolean exists = macroDepartmentDao.existsByNameAndEstablishment(establishmentId, name);
                if (listener != null) {
                    listener.onValidationResult(exists);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking macro department name: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    // Cleanup
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Callback interfaces
    public interface OnEstablishmentListListener {
        void onSuccess(List<Establishment> establishments);
        void onError(Exception e);
    }

    public interface OnEstablishmentOperationListener {
        void onSuccess(Establishment establishment);
        void onError(Exception e);
    }

    public interface OnMacroDepartmentListListener {
        void onSuccess(List<MacroDepartment> macroDepartments);
        void onError(Exception e);
    }

    public interface OnMacroDepartmentOperationListener {
        void onSuccess(MacroDepartment macroDepartment);
        void onError(Exception e);
    }

    public interface OnSubDepartmentListListener {
        void onSuccess(List<SubDepartment> subDepartments);
        void onError(Exception e);
    }

    public interface OnSubDepartmentOperationListener {
        void onSuccess(SubDepartment subDepartment);
        void onError(Exception e);
    }

    public interface OnValidationListener {
        void onValidationResult(boolean exists);
        void onError(Exception e);
    }
}