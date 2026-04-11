# Сценарии проверки (OZON + payment)

Подставь свои URL. По умолчанию для `docker compose`:

```bash
export OZON=http://localhost:8080    # если WildFly с контекстом — добавь суффикс, напр. /ozon
export PAY=http://localhost:8081
export PAY_USER=ozon-integration
export PAY_PASS=ozon-secret
```

Нужен **`jq`** для разбора JSON (или смотри ответ глазами).

---

## 1. Публичное (без токена)

```bash
curl -sS "$OZON/api/public/ping"
# ожидание: pong (или JSON-обёртка — как у тебя в контроллере)
```

---

## 2. Логин: неверный пароль

```bash
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"wrong"}'
# ожидание: 401
```

---

## 3. Логин: успех, взять JWT

```bash
curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password"}' | jq .

export TOKEN=$(curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password"}' | jq -r '.accessToken')

echo "$TOKEN"
# в ответе также effectiveRoles, privileges, expiresInMs
```

---

## 4. Запрос без токена на защищённую ручку

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" "$OZON/api/me"
# ожидание: 401
```

---

## 5. Запрос с Bearer

```bash
curl -sS -H "Authorization: Bearer $TOKEN" "$OZON/api/me"
# ожидание: 200, тело — username
```

---

## 6. Сетевые политики + подмена IP (`netdemo`)

Пользователь **`netdemo`**, пароль **`password`**, три роли в БД. Включён заголовок **`X-Client-IP`** (`CLIENT_IP_HEADER_ENABLE=true`).

**6.1. IP `173.0.0.5` — побеждает политика с /24 → только роль USER в effectiveRoles**

```bash
curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Client-IP: 173.0.0.5" \
  -d '{"username":"netdemo","password":"password"}' | jq '.effectiveRoles'
# ожидание: ["USER"] (порядок может отличаться)
```

**6.2. IP `173.12.34.56` — /24 не матчит, побеждает /8 → USER + PICKUP_POINT_ADMIN**

```bash
curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Client-IP: 173.12.34.56" \
  -d '{"username":"netdemo","password":"password"}' | jq '.effectiveRoles'
# ожидание: USER и PICKUP_POINT_ADMIN
```

**6.3. Обычный docker-IP без заголовка — обычно матчит только 0.0.0.0/0 → все три роли**

```bash
curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"netdemo","password":"password"}' | jq '.effectiveRoles'
```

В логах сервера ищи строки **`RBAC/CIDR:`**.

---

## 7. Инвалидация JWT при смене IP

```bash
# Выпустить токен «как будто» с 10.0.0.1
T=$(curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Client-IP: 10.0.0.1" \
  -d '{"username":"user1","password":"password"}' | jq -r '.accessToken')

# Запрос БЕЗ X-Client-IP (реальный remote или X-Forwarded-For) — IP не совпадёт с claim в токене
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $T" "$OZON/api/me"
# ожидание: 401

# Тот же токен, но с тем же X-Client-IP — должно быть 200
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $T" \
  -H "X-Client-IP: 10.0.0.1" \
  "$OZON/api/me"
# ожидание: 200
```

Если **`CLIENT_IP_HEADER_ENABLE=false`**, подмена через `X-Client-IP` отключена — сценарий меняй на реальный прокси или другой способ фиксации IP.

---

## 8. Заказы (USER)

```bash
TOKEN=$(curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password"}' | jq -r '.accessToken')

curl -sS -H "Authorization: Bearer $TOKEN" -X POST "$OZON/api/order" \
  -H "Content-Type: application/json" \
  -d '{"pickup_point_id":1,"delivery_address":"Адрес","amount_kopecks":10000}' | jq .

curl -sS -H "Authorization: Bearer $TOKEN" "$OZON/api/order" | jq .
```

**Отмена заказа** только из **`PAYMENT_ERROR`**: сначала довести платёж до FAILED (см. п. 10), дождаться синхронизации ozon или руками, затем:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  -X DELETE "$OZON/api/order/ORDER_ID"
```

---

## 9. ПВЗ и админ по заказам

```bash
T_PP=$(curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"ppadmin","password":"password"}' | jq -r '.accessToken')

curl -sS -H "Authorization: Bearer $T_PP" "$OZON/api/order" | jq .

# Выдача: PATCH .../issue (заказ в PAID)
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $T_PP" \
  -X PATCH "$OZON/api/order/1/issue"

T_AD=$(curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"password"}' | jq -r '.accessToken')

curl -sS -H "Authorization: Bearer $T_AD" "$OZON/api/order" | jq .

curl -sS -H "Authorization: Bearer $T_AD" -X PATCH "$OZON/api/order/1/status" \
  -H "Content-Type: application/json" \
  -d '{"orderStatus":"NEW"}' -o /dev/null -w "HTTP %{http_code}\n"
```

---

## 10. Payment-service (Basic)

```bash
curl -sS -u "$PAY_USER:$PAY_PASS" -X POST "$PAY/api/payment" \
  -H "Content-Type: application/json" \
  -d '{"amountKopecks":5000}' | jq .

PID="<uuid из ответа>"

curl -sS -u "$PAY_USER:$PAY_PASS" "$PAY/api/payment/$PID" | jq .

# Ошибка оплаты → FAILED (неверный CVC)
curl -sS -u "$PAY_USER:$PAY_PASS" -X POST "$PAY/api/payment/pay" \
  -H "Content-Type: application/json" \
  -d "{\"cardId\":\"4000000000000002\",\"month\":12,\"yearTail\":29,\"cvc\":\"999\",\"paymentId\":\"$PID\"}"

# Успех (карта из сидов)
curl -sS -u "$PAY_USER:$PAY_PASS" -X POST "$PAY/api/payment/pay" \
  -H "Content-Type: application/json" \
  -d "{\"cardId\":\"4000000000000002\",\"month\":12,\"yearTail\":29,\"cvc\":\"111\",\"paymentId\":\"$PID\"}"
```

**Ручной просроченный PENDING → FAILED (Quartz дублирует раз в минуту):**

```bash
curl -sS -u "$PAY_USER:$PAY_PASS" -X POST "$PAY/api/payment/admin/expire-stale-pending" | jq .
```

**Инвалидация FAILED → INVALID (после отмены заказа в ozon тоже вызывается):**

```bash
curl -sS -u "$PAY_USER:$PAY_PASS" -X DELETE "$PAY/api/payment/$PID" -o /dev/null -w "HTTP %{http_code}\n"
```

---

## 11. CRUD сетевых политик (нужен superadmin + JWT)

```bash
T_AD=$(curl -sS -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"password"}' | jq -r '.accessToken')

curl -sS -H "Authorization: Bearer $T_AD" "$OZON/api/admin/network-policies" | jq .

curl -sS -H "Authorization: Bearer $T_AD" -X POST "$OZON/api/admin/network-policies" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-policy",
    "description": "описание",
    "addresses": [{"addr": "10.10.0.0/16", "description": "офис"}],
    "roleNames": ["USER"]
  }' | jq .
```

---

## 12. Пользователь без покрытия политиками (если настроишь в БД)

Если у всех ролей пользователя **нет** строк в `network_policy_roles`, при логине:

```bash
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$OZON/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"some_user","password":"password"}'
# ожидание: 403 и сообщение про отсутствие effective roles
```

---

## 13. Автопрогон скрипта

```bash
bash scripts/curl-api-tests.sh
# при необходимости: OZON_URL=... PAYMENT_URL=... MOCK_CLIENT_IP=...
```

---

## Краткая матрица «кто что может»

| Действие | Нужные привилегии (после логина) |
|----------|----------------------------------|
| POST /api/order | ORDER_CREATE |
| GET /api/order (свои) | ORDER_VIEW_OWN (+ политики по IP) |
| DELETE /api/order/{id} | ORDER_CANCEL_OWN |
| PATCH .../issue | ORDER_UPDATE_PICKUP_POINT_STATUS |
| PATCH .../status | ORDER_UPDATE_ALL |
| CRUD /api/admin/network-policies | ORDER_UPDATE_ALL |

Фактический набор привилегий в JWT зависит от **ролей в БД** и **сетевых политик + IP** (см. `RBAC_CIDR_KONSPECT.md`).
