# Лабораторная №2 — что сделано (сводка)

Один файл со всем, что было доработано в репозитории: транзакции (декларативно), JTA под WildFly (профиль), Spring Security + БД, платежи, заказы, скрипт проверки API.

---

## 1. Роли и привилегии (ozon-service)

Привилегии (Spring authorities):

| Привилегия | Назначение |
|------------|------------|
| `ORDER_CREATE` | Создание заказа |
| `ORDER_VIEW_OWN` | Просмотр своих заказов |
| `ORDER_CANCEL_OWN` | Отмена своего заказа |
| `ORDER_VIEW_PICKUP_POINT` | Просмотр заказов своего ПВЗ |
| `ORDER_UPDATE_PICKUP_POINT_STATUS` | Смена статуса в рамках ПВЗ (PAID → ISSUED) |
| `ORDER_VIEW_ALL` | Просмотр всех заказов |
| `ORDER_UPDATE_ALL` | Любая смена статуса заказа (админ) |

Роли и маппинг задаются в коде (`AppUserDetailsService`), в БД хранятся только имена ролей у пользователя.

| Роль | Привилегии |
|------|------------|
| `USER` | ORDER_CREATE, ORDER_VIEW_OWN, ORDER_CANCEL_OWN |
| `PICKUP_POINT_ADMIN` | ORDER_VIEW_PICKUP_POINT, ORDER_UPDATE_PICKUP_POINT_STATUS |
| `ADMIN` | ORDER_VIEW_ALL, ORDER_UPDATE_ALL |

Аутентификация: **HTTP Basic**. Пользователи в таблицах **`app_users`**, **`app_user_roles`** (Liquibase: `ozon-service/.../1774000000-order-status-and-auth.sql`). У админа ПВЗ в **`pickup_point_id`** указан ПВЗ (в сидах — `1`).

Тестовые учётки (пароль **`password`**):

- `user1` — USER  
- `ppadmin` — PICKUP_POINT_ADMIN, ПВЗ id = 1  
- `superadmin` — ADMIN  

---

## 2. REST API (ozon-service)

Базовый префикс заказов: **`/api/order`**.

| Метод | Путь | Доступ |
|-------|------|--------|
| POST | `/api/order` | `ORDER_CREATE` |
| GET | `/api/order` | одна из: ORDER_VIEW_OWN, ORDER_VIEW_PICKUP_POINT, ORDER_VIEW_ALL |
| DELETE | `/api/order/{orderId}` | `ORDER_CANCEL_OWN` — только свой заказ, только из статуса **PAYMENT_ERROR** → заказ **CANCELLED**, в payment вызывается инвалидация |
| PATCH | `/api/order/{orderId}/issue` | `ORDER_UPDATE_PICKUP_POINT_STATUS` — только заказы своего ПВЗ, **PAID** → **ISSUED** |
| PATCH | `/api/order/{orderId}/status` | `ORDER_UPDATE_ALL` — тело `{"orderStatus":"..."}` (`UpdateOrderStatusRequest`) |

Ответ создания заказа: **`orderId`** и **`paymentId`** (`CreateOrderResponse`).

Остальное как раньше: swagger, `/api/public/ping`, `/api/me` (нужен Basic), доставка и ПВЗ под общим правилом «любой аутентифицированный», если не переопределяли отдельно.

---

## 3. Платежи (payment-service)

- В enum **`payment_status`** добавлено значение **`INVALID`** (Liquibase: `1774000000-payment-status-invalid.sql`).
- В коде (`schema-registry`) в **`PaymentStatus`** добавлен **`INVALID`**.
- **DELETE `/api/payment/{paymentId}`** — перевод **FAILED** → **INVALID** (для сценария отмены заказа).
- При неверных реквизитах карты или нехватке баланса платёж переводится в **FAILED** (чтобы ордер через планировщик мог стать **PAYMENT_ERROR**).
- Эндпоинты **`/api/payment/**`** закрыты **HTTP Basic**; технический пользователь **`ozon-integration`** / **`ozon-secret`** (настраивается через `payment.integration.*` и зеркально в ozon `spring.clients.payment-service.*`).

---

## 4. Интеграция ozon → payment

Класс **`PaymentServiceClient`** использует **`RestTemplate`** с Basic (bean **`paymentRestTemplate`** в `PaymentClientConfiguration`).

---

## 5. Транзакции и JTA

- На сервисах на ключевых операциях стоят **`@Transactional`** (создание заказа, списки read-only, отмена, выдача в ПВЗ, админская смена статуса; в payment — создание платежа, оплата, инвалидация).
- Конфиг **Jakarta EE JTA** для WildFly: класс **`TransactionConfiguration`** помечен **`@Profile("wildfly")`**, JNDI **`java:/OzonDS`**, **`JtaTransactionManager`**, настройки Hibernate JTA.
- Файл **`application-wildfly.properties`** отключает автоконфиг DataSource/JPA при деплое на сервер; для локального запуска и Docker профиль **wildfly не включается** — работает обычный datasource из `application.properties`.

Упаковка ozon: **WAR** (`ozon.war`), в **`ozon.Dockerfile`** копируется war.

---

## 6. Модель заказа

Статусы заказа: **NEW**, **PAID**, **PAYMENT_ERROR**, **CANCELLED**, **ISSUED** (выдан в ПВЗ). В Liquibase к типу **`order_status`** добавлены **CANCELLED** и **ISSUED**.

Планировщик **`PaymentRetriever`** подтягивает статус платежа; для **INVALID** только лог, без смены ордера.

---

## 7. Docker Compose

- У **ozon-service**: `SERVER_PORT=8080`, URL payment, креды клиента к payment.
- У **payment-service**: `SERVER_PORT=8081`.

Поднятие: **`docker compose up --build`** из корня репозитория.

---

## 8. Скрипт проверки API

Файл **`scripts/curl-api-tests.sh`**: ping, me, создание заказа, список, ошибочная оплата, ожидание ~1 мин под синхронизацию, DELETE отмены, успешный pay, списки ppadmin/superadmin.

Запуск:

```bash
bash scripts/curl-api-tests.sh
```

Переменные: `OZON_URL`, `PAYMENT_URL`, `PAYMENT_SERVICE_USERNAME`, `PAYMENT_SERVICE_PASSWORD`. Для разбора JSON удобно иметь **`jq`**.

---

## 9. Затронутые артефакты (по модулям)

**schema-registry**

- `PaymentStatus`: значение **INVALID**.

**ozon-service**

- Сущности/репозитории: **`AppUser`**, **`AppUserRepository`**, расширения **`OrderRepository`**.
- Security: **`AppUserPrincipal`**, **`AppUserDetailsService`**, обновлённый **`SecurityConfiguration`**, **`PasswordEncoderConfiguration`**, **`@EnableMethodSecurity`** в **`OzonApplication`**.
- **`PaymentClientConfiguration`**, переработанный **`PaymentServiceClient`**.
- **`OrderService`**, **`OrderController`**, DTO **`CreateOrderResponse`**, **`UpdateOrderStatusRequest`**.
- **`HelloWorldController`** — principal **`AppUserPrincipal`**.
- **`PaymentRetriever`** — ветка **INVALID**.
- Миграция **`1774000000-order-status-and-auth.sql`** + запись в **`db-changelog-master.xml`**.
- **`application.properties`**, **`application-wildfly.properties`**.
- **`TransactionConfiguration`**: только профиль **wildfly**.

**payment-service**

- **`PaymentService`**, **`PaymentController`** (DELETE).
- Security: **`SecurityConfiguration`**, **`PasswordEncoderConfiguration`**, **`PaymentIntegrationUserConfiguration`** (технический пользователь для вызовов `/api/payment/**`).
- Миграция **INVALID** + changelog.

**Корень репозитория**

- **`docker-compose.yml`**, **`ozon.Dockerfile`**.
- **`scripts/curl-api-tests.sh`**, этот файл **`LAB2_CHANGES.md`**.

---

## 10. Деплой на helios (WildFly)

Собрать WAR, задеплоить на WildFly, включить профиль **`wildfly`**, настроить datasource с JNDI **`java:/OzonDS`**, поднять PostgreSQL и прогнать Liquibase (или доверить приложению при старте).
