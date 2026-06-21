package teccr.justdoitcloud.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import teccr.justdoitcloud.data.Task;
import teccr.justdoitcloud.data.User;
import teccr.justdoitcloud.service.TaskService;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users/{userId}/tasks")
public class TasksController {

    private final TaskService taskService;

    public TasksController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Iterable<Task> getTasksForUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        return taskService.getTasksForUser(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task addTaskToUser(@PathVariable Long userId,
                              @RequestBody(required = false) Task task,
                              @RequestParam(name = "autogenerate", required = false) String autogenerate) {
        User user = new User();
        user.setId(userId);

        boolean auto = autogenerate != null && (autogenerate.isEmpty() || autogenerate.equalsIgnoreCase("true"));

        if (auto) {
            // Ignorar el cuerpo y usar el generador para crear la tarea
            return taskService.autogenerateTaskForUser(user);
        }

        // Flujo normal: crear usando el Task provisto en el body
        if (task == null) {
            throw new IllegalArgumentException("Task body is required when autogenerate is not used");
        }
        return taskService.addTaskToUser(user, task);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long userId, @PathVariable Long id) {
        Optional<Task> taskOpt = taskService.getTaskByUser(userId, id);
        return taskOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long userId, @PathVariable Long id, @RequestBody Task task) {
        Task updatedTask = taskService.updateTaskForUser(userId, id, task);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long userId, @PathVariable Long id) {
        taskService.deleteTaskForUser(userId, id);
    }
}
