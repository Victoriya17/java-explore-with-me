# Приложение Explore with me

https://github.com/Victoriya17/java-explore-with-me/pull/4

---

## Схема базы данных
![explore-with-me.png](service/src/main/resources/explore-with-me.png)

---

## Описание сущностей 
### users

- Добавление пользователя с указанием его id, уникального адреса электронной почты и имени.

### categories

- Список категорий для группировки событий (например, «Концерты», «Кино»). Название категории уникально.

### events

- Добавление события с указанием его id, аннотации, категории, даты проведения, описания, инициатора (пользователя), геокоординат (lat, lon), стоимости (paid), лимита участников, статуса модерации и количества просмотров.

### requests

- Заявки пользователей на участие в событиях. Объединяет id события и id запрашивающего пользователя со статусом заявки.

### compilations

- Подборки событий, которые можно закреплять на главной странице (флаг pinned) с указанием названия подборки.

### compilation_events

- Промежуточная таблица для связи «многие-ко-многим», объединяющая id подборки и id входящих в неё событий.

### comments

- Текст комментария, привязанный к конкретному событию и конкретному автору (пользователю), с фиксацией времени создания и обновления.

---

## Примеры запросов для основных операций приложения на языке SQL для Events:

1) Регистрация нового пользователя — createUser():
   ```sql
   -- Шаг 1: Проверка уникальности email в БД перед сохранением
   SELECT id, name, email FROM users WHERE email = {request.getEmail()};

   -- Шаг 2: Вставка новой записи, если email свободен
   INSERT INTO users (name, email) 
   VALUES ({request.getName()}, {request.getEmail()});
   ```

2) Создание новой категории — createCategory():
   ```sql
   INSERT INTO categories (name) 
   VALUES ({request.getName()});
   ```

3) Создание нового события пользователем — createEvent():
   ```sql
   INSERT INTO events (annotation, category_id, confirmed_requests, created_on, description, event_date, initiator_id, lat, lon, paid, participant_limit, request_moderation, state, title, views)
   VALUES ({request.getAnnotation()}, {request.getCategory()}, 0, NOW(), {request.getDescription()}, {request.getEventDate()}, {userId}, {request.getLocation().getLat()}, {request.getLocation().getLon()}, {request.getPaid()}, {request.getParticipantLimit()}, {request.getRequestModeration()}, 'PENDING', {request.getTitle()}, 0);
   ```

4) Добавление комментария к событию — createComment():
   ```sql
   -- Шаг 1: Проверка, что событие существует и опубликовано
   SELECT state FROM events WHERE id = {request.getEventId()};

   -- Шаг 2: Сохранение комментария с привязкой к событию и автору
   INSERT INTO comments (text, event_id, author_id, created, updated) 
   VALUES ({request.getText()}, {request.getEventId()}, {userId}, NOW(), NOW());
   ```

5) Создание подборки событий (связь многие-ко-многим) — createCompilation():
   ```sql
   -- Шаг 1: Создание самой подборки (по умолчанию pinned = false, если не передано)
   INSERT INTO compilations (pinned, title) 
   VALUES (COALESCE({request.getPinned()}, FALSE), {request.getTitle()});

   -- Шаг 2: Привязка списка событий к созданной подборке
   INSERT INTO compilation_events (compilation_id, event_id) 
   VALUES ({compilation_id}, {event_id_1}), ({compilation_id}, {event_id_2});
   ```

6) Изменение статуса заявок на участие в событии — updateStatusEvent():
   ```sql
   -- Перевод выбранных заявок в статус подтвержденных
   UPDATE requests SET status = 'CONFIRMED' WHERE id IN ({request_ids});

   -- Синхронизация счетчика подтвержденных участников в таблице событий
   UPDATE events SET confirmed_requests = confirmed_requests + {count} WHERE id = {eventId};
   ```

---

## Технологический стек

*   Платформа и сборка: Java 21 (Amazon Corretto), Maven (Мультимодульный проект: основной сервис `service` и сервис статистики `stats`)
*   Фреймворки и спецификации: Spring Boot 3.3.2, Spring Data JPA, Spring Web, Jakarta Persistence API
*   База данных: PostgreSQL (с разделением на изолированные БД для основного сервиса и статистики)
*   Валидация и сериализация: Jakarta Validation API, Jackson
*   Качество кода и метрики (QA/CI):
    *   Checkstyle — статический анализ кода на соответствие стандартам оформления.
    *   SpotBugs — автоматический поиск потенциальных багов и уязвимостей в байт-коде.
    *   JaCoCo (Java Code Coverage) — плагин для генерации отчетов и контроля покрытия кода юнит-тестами.
*   Утилиты: Lombok

---

## Системные требования

* Операционная система: Windows, macOS, Linux
* Среда выполнения: Java 21 (Amazon Corretto)
* Инструменты сборки и контейнеризации: Maven 3.9+ и Docker Desktop (с Docker Compose)
* Свободные порты:  8080 (Main Service), 9090 (Stats Service), 6541 (Main DB), 6431 (Stats DB)

---

## Инструкция по развертыванию и запуску

1. Клонируйте репозиторий с проектом:
   git clone https://github.com

2. Перейдите в корневую директорию проекта:
   cd explore-with-me

3. Скомпилируйте исходный код и соберите jar-архивы для всех модулей:
   mvn clean package

4. Запустите всю инфраструктуру (основной сервис, сервис статистики и базы данных) в фоновом режиме:
   docker-compose up -d --build

После успешного запуска основной сервис станет доступен по адресу http://localhost:8080, а сервис статистики — по адресу http://localhost:9090.

---

## Проверка качества кода перед коммитом (QA)

Поскольку в проекте настроены плагины контроля качества, вы можете запустить проверку кода локально без сборки тяжелых jar-файлов:

* Проверка стиля кодирования (Checkstyle) и поиск скрытых багов (SpotBugs):
  mvn compile -P check

* Генерация отчета о покрытии кода тестами (JaCoCo):
  mvn test -P coverage
  (Отчет появится в директории target/site/jacoco/index.html)

---

## Полезные команды для управления контейнерами

* Просмотр логов всех сервисов в реальном времени:
  docker-compose logs -f

* Остановка всех контейнеров проекта:
  docker-compose down

---

## Планы по доработке
* Добавить тесты
* Добавить фичи
