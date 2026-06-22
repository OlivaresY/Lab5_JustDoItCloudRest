-- 1. Crear las tablas
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_name VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    type VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_username (user_name)
    );

CREATE TABLE IF NOT EXISTS tasks (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     description TEXT,
                                     deadline DATE,
                                     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_tasks_per_user_description (user_id, description(100))
    );

-- 2. Insertar usuario inicial de forma idempotente
INSERT INTO users (user_name, name, email, type)
VALUES ('john_doe', 'John Doe', 'john@gmail.com', 'REGULAR')
    ON DUPLICATE KEY UPDATE user_name = user_name;

-- 3. Insertar tareas para 'john_doe' de forma idempotente
-- Obtenemos el ID dinámicamente mediante una subconsulta
INSERT INTO tasks (user_id, description, status)
SELECT u.id, 'Comprar leche', 'PENDING'
FROM users u WHERE u.user_name = 'john_doe'
    ON DUPLICATE KEY UPDATE description = description;

INSERT INTO tasks (user_id, description, status)
SELECT u.id, 'Reparar llantas del carro', 'INPROGRESS'
FROM users u WHERE u.user_name = 'john_doe'
    ON DUPLICATE KEY UPDATE description = description;