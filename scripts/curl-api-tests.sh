#!/usr/bin/env bash
# Скрипт проверки публичных HTTP-интерфейсов (OZON + payment).
# OZON: сначала POST /api/auth/login → JWT Bearer (роли режутся по сетевым политикам и IP).
# Опционально MOCK_CLIENT_IP — заголовок X-Client-IP (см. app.security.use-client-ip-header).
#
# Переменные: OZON_URL, PAYMENT_URL, PAYMENT_SERVICE_*, MOCK_CLIENT_IP

set -euo pipefail

OZON="${OZON_URL:-http://localhost:8080}"
PAY="${PAYMENT_URL:-http://localhost:8081}"
PAY_USER="${PAYMENT_SERVICE_USERNAME:-ozon-integration}"
PAY_PASS="${PAYMENT_SERVICE_PASSWORD:-ozon-secret}"

ozon_login_json() {
  local user="$1" pass="$2"
  local args=(-sS -X POST "${OZON}/api/auth/login" -H "Content-Type: application/json")
  if [[ -n "${MOCK_CLIENT_IP:-}" ]]; then
    args+=(-H "X-Client-IP: ${MOCK_CLIENT_IP}")
  fi
  args+=(-d "{\"username\":\"${user}\",\"password\":\"${pass}\"}")
  curl "${args[@]}"
}

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    echo "Нужен jq для разбора JSON и токена" >&2
    exit 1
  fi
}

require_jq

echo "=== OZON: публичный ping (без авторизации) ==="
curl -sS "${OZON}/api/public/ping"
echo

echo "=== OZON: логин user1 → JWT ==="
LOGIN_U1=$(ozon_login_json user1 password)
echo "${LOGIN_U1}" | jq .
TOKEN_U1=$(echo "${LOGIN_U1}" | jq -r '.accessToken // empty')
if [[ -z "${TOKEN_U1}" ]]; then
  echo "Логин user1 не удался" >&2
  exit 1
fi

echo "=== OZON: текущий пользователь (user1, Bearer) ==="
curl -sS -H "Authorization: Bearer ${TOKEN_U1}" "${OZON}/api/me"
echo

echo "=== OZON: создание заказа (user1) ==="
CREATE_JSON=$(curl -sS -H "Authorization: Bearer ${TOKEN_U1}" -X POST "${OZON}/api/order" \
  -H 'Content-Type: application/json' \
  -d '{"pickup_point_id":1,"delivery_address":"Тестовый адрес","amount_kopecks":10000}')
echo "${CREATE_JSON}"

ORDER_ID=$(echo "${CREATE_JSON}" | jq -r '.orderId // empty')
PAYMENT_ID=$(echo "${CREATE_JSON}" | jq -r '.paymentId // empty')
echo "orderId=${ORDER_ID} paymentId=${PAYMENT_ID}"

echo "=== OZON: список заказов (user1) ==="
curl -sS -H "Authorization: Bearer ${TOKEN_U1}" "${OZON}/api/order"
echo

echo "=== PAYMENT: статус платежа (интеграционный Basic) ==="
if [[ -n "${PAYMENT_ID}" ]]; then
  curl -sS -u "${PAY_USER}:${PAY_PASS}" "${PAY}/api/payment/${PAYMENT_ID}"
  echo
fi

echo "=== PAYMENT: неуспешная оплата (неверный CVC → FAILED в БД) ==="
if [[ -n "${PAYMENT_ID}" ]]; then
  curl -sS -o /dev/null -w "HTTP %{http_code}\n" -u "${PAY_USER}:${PAY_PASS}" -X POST "${PAY}/api/payment/pay" \
    -H 'Content-Type: application/json' \
    -d "{\"cardId\":\"4000000000000002\",\"month\":12,\"yearTail\":29,\"cvc\":\"999\",\"paymentId\":\"${PAYMENT_ID}\"}" || true
  echo "Ожидаемый ответ: 400 Bad Request, статус платежа в БД — FAILED"
fi

echo "=== PAYMENT: статус после ошибки ==="
if [[ -n "${PAYMENT_ID}" ]]; then
  curl -sS -u "${PAY_USER}:${PAY_PASS}" "${PAY}/api/payment/${PAYMENT_ID}"
  echo
fi

echo "=== OZON: ждём синхронизацию статуса (PaymentRetriever ~1 мин) ==="
if [[ -n "${ORDER_ID}" ]]; then
  echo "sleep 65..."
  sleep 65
  echo "Список заказов после опроса payment:"
  curl -sS -H "Authorization: Bearer ${TOKEN_U1}" "${OZON}/api/order"
  echo
  echo "=== OZON: отмена заказа DELETE (только из PAYMENT_ERROR) ==="
  curl -sS -o /dev/null -w "HTTP %{http_code}\n" -H "Authorization: Bearer ${TOKEN_U1}" -X DELETE "${OZON}/api/order/${ORDER_ID}" || true
  echo
fi

echo "=== PAYMENT: успешная оплата (новый платёж) ==="
PAY_NEW=$(curl -sS -u "${PAY_USER}:${PAY_PASS}" -X POST "${PAY}/api/payment" \
  -H 'Content-Type: application/json' \
  -d '{"amountKopecks":5000}')
echo "${PAY_NEW}"
PID_OK=$(echo "${PAY_NEW}" | jq -r '.paymentId // empty')
if [[ -n "${PID_OK}" ]]; then
  curl -sS -o /dev/null -w "pay HTTP %{http_code}\n" -u "${PAY_USER}:${PAY_PASS}" -X POST "${PAY}/api/payment/pay" \
    -H 'Content-Type: application/json' \
    -d "{\"cardId\":\"4000000000000002\",\"month\":12,\"yearTail\":29,\"cvc\":\"111\",\"paymentId\":\"${PID_OK}\"}"
  curl -sS -u "${PAY_USER}:${PAY_PASS}" "${PAY}/api/payment/${PID_OK}"
  echo
fi

echo "=== OZON: логин ppadmin → JWT ==="
TOKEN_PP=$(ozon_login_json ppadmin password | jq -r '.accessToken // empty')
echo "=== OZON: заказы по ПВЗ (ppadmin) ==="
curl -sS -H "Authorization: Bearer ${TOKEN_PP}" "${OZON}/api/order"
echo

echo "=== OZON: логин superadmin → JWT ==="
TOKEN_AD=$(ozon_login_json superadmin password | jq -r '.accessToken // empty')
echo "=== OZON: все заказы (superadmin) ==="
curl -sS -H "Authorization: Bearer ${TOKEN_AD}" "${OZON}/api/order"
echo

echo "=== Готово ==="
echo "Демо RBAC/CIDR: пользователь netdemo (роли USER+PICKUP+ADMIN), пароль password:"
echo "  MOCK_CLIENT_IP=173.0.0.5 $0   # ожидаемо только USER в effectiveRoles"
echo "  MOCK_CLIENT_IP=173.12.34.56 $0 # USER+PICKUP (маска /8 побеждает /0)"
echo "Ручки: PATCH ${OZON}/api/order/{id}/issue (ppadmin), PATCH .../status (superadmin)"
