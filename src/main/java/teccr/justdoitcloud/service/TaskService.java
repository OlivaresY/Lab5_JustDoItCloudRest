package teccr.justdoitcloud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import teccr.justdoitcloud.data.Task;
import teccr.justdoitcloud.data.User;
import teccr.justdoitcloud.exception.ForbiddenActionException;
import teccr.justdoitcloud.exception.TaskNotFoundException;
import teccr.justdoitcloud.repository.TaskRepository;
import teccr.justdoitcloud.repository.UserRepository;
import teccr.justdoitcloud.service.external.taskgenerator.TaskGenerator;
import teccr.justdoitcloud.service.internal.taskarchiver.TaskArchiver;
import teccr.justdoitcloud.exception.TaskGenerationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskGenerator taskGenerator;
    private final UserRepository userRepository;
    private final TaskArchiver taskArchiver;

    public TaskService(TaskRepository taskRepository,
                       TaskGenerator taskGenerator,
                       UserRepository userRepository,
                       TaskArchiver taskArchiver) {
        this.taskRepository = taskRepository;
        this.taskGenerator = taskGenerator;
        this.userRepository = userRepository;
        this.taskArchiver = taskArchiver;
    }

    public List<Task> getTasksForUser(User user) {
        return taskRepository.findByUserId(user.getId());
    }

    public Task addTaskToUser(User user, Task task) {
        task.setUserId(user.getId());
        task.setCreatedAt(LocalDateTime.now());
        Task taskCreated = taskRepository.save(task);

        Optional<User> maybeUser = userRepository.findById(user.getId());
        if (maybeUser.isPresent()) {
            taskArchiver.archiveTask("tasks-new", maybeUser.get(), taskCreated);
        }

        return taskCreated;
    }

    public Task autogenerateTaskForUser(User user) {
        Task task = taskGenerator.generateTask();

        if (task == null) {
            throw new TaskGenerationException("No se pudo generar la tarea automáticamente");
        }

        task.setUserId(user.getId());
        task.setCreatedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    public Optional<Task> getTaskById(Long id) {
        if (id == null || id < 0) {
            return Optional.empty();
        }
        return taskRepository.findById(id);
    }

    public Optional<Task> getTaskByUser(Long userId, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);

        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        Task task = taskOpt.get();

        if (!task.getUserId().equals(userId)) {
            return Optional.empty();
        }

        return taskOpt;
    }

    public Task updateTaskFields(Long id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(existingTask -> {
                    if (updatedTask.getDescription() != null && !updatedTask.getDescription().trim().isEmpty()) {
                        existingTask.setDescription(updatedTask.getDescription().trim());
                    }
                    if (updatedTask.getStatus() != null) {
                        existingTask.setStatus(updatedTask.getStatus());
                    }
                    return taskRepository.save(existingTask);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public Task updateTaskForUser(long userId, Long taskId, Task updateTask) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);

        if (taskOpt.isEmpty()) {
            throw new TaskNotFoundException("Task not found");
        }

        Task task = taskOpt.get();

        if (!task.getUserId().equals(userId)) {
            throw new TaskNotFoundException("Task not found");
        }

        if (updateTask.getStatus() != null) {
            // Usamos .name() o .toString() para convertir el Enum a String seguro
            String estadoActual = task.getStatus() != null ? task.getStatus().name() : "";
            String nuevoEstado = updateTask.getStatus().name();

            if ("INPROGRESS".equalsIgnoreCase(estadoActual) && "PENDING".equalsIgnoreCase(nuevoEstado)) {
                throw new IllegalArgumentException("No se puede regresar de INPROGRESS a PENDING");
            }

            if ("COMPLETED".equalsIgnoreCase(estadoActual)) {
                if ("PENDING".equalsIgnoreCase(nuevoEstado) || "INPROGRESS".equalsIgnoreCase(nuevoEstado)) {
                    throw new IllegalArgumentException("No se puede modificar el estado de una tarea COMPLETED");
                }
            }
        }

        return updateTaskFields(taskId, updateTask);
    }

    public void deleteTaskForUser(long userId, Long taskId) {
        Optional<Task> maybeTask = taskRepository.findById(taskId);

        if (maybeTask.isEmpty()) {
            throw new TaskNotFoundException("Task not found with id: " + taskId);
        }

        Task task = maybeTask.get();

        if (!task.getUserId().equals(userId)) {
            throw new TaskNotFoundException("Task not found");
        }

        // SOLUCIÓN: Creamos el objeto User manualmente usando el ID sin buscarlo en la BD
        try {
            User mockUser = new User();
            mockUser.setId(task.getUserId());
            // Le seteamos valores genéricos por si el archiver los ocupa
            mockUser.setUserName("User-" + task.getUserId());
            mockUser.setEmail("user@test.com");

            taskArchiver.archiveTask("tasks-deleted", mockUser, task);
        } catch (Exception ignored) {
            log.error("Error archiving task with id {} for user id {}: {}", task.getId(), task.getUserId(), ignored.getMessage());
        }

        // Ejecuta el borrado directo de la tarea
        taskRepository.deleteById(taskId);
    }
}