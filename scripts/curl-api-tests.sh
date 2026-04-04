#!/usr/bin/env bash
# Скрипт проверки публичных HTTP-интерфейсов (OZON + payment).
# Запуск: из корня репозитория — bash scripts/curl-api-tests.sh
# Опционально: OZON_URL, PAYMENT_URL, PAYMENT_SERVICE_USERNAME, PAYMENT_SERVICE_PASSWORD
#
# Учётки OZON (Basic): user1 / ppadmin / superadmin, пароль везде: password
# Учётка вызова payment API: ozon-integration / ozon-secret (как у ozon-service)

set -euo pipefail

OZON="${OZON_URL:-http://localhost:8080}"
PAY="${PAYMENT_URL:-http://localhost:8081}"
PAY_USER="${PAYMENT_SERVICE_USERNAME:-ozon-integration}"
PAY_PASS="${PAYMENT_SERVICE_PASSWORD:-ozon-secret}"

echo "=== OZON: публичный ping (без авторизации) ==="
curl -sS "${OZON}/api/public/ping"
echo

echo "=== OZON: текущий пользователь (user1) ==="
curl -sS -u 'user1:password' "${OZON}/api/me"
echo

echo "=== OZON: создание заказа (user1) ==="
CREATE_JSON=$(curl -sS -u 'user1:password' -X POST "${OZON}/api/order" \
  -H 'Content-Type: application/json' \
  -d '{"pickup_point_id":1,"delivery_address":"Тестовый адрес","amount_kopecks":10000}')
echo "${CREATE_JSON}"

ORDER_ID=""
PAYMENT_ID=""
if command -v jq >/dev/null 2>&1; then
  ORDER_ID=$(echo "${CREATE_JSON}" | jq -r '.orderId // empty')
  PAYMENT_ID=$(echo "${CREATE_JSON}" | jq -r '.paymentId // empty')
  echo "orderId=${ORDER_ID} paymentId=${PAYMENT_ID}"
else
  echo "(установите jq, чтобы автоматически вытащить orderId/paymentId из JSON)"
fi

echo "=== OZON: список заказов (user1) ==="
curl -sS -u 'user1:password' "${OZON}/api/order"
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
  curl -sS -u 'user1:password' "${OZON}/api/order"
  echo
  echo "=== OZON: отмена заказа DELETE (только из PAYMENT_ERROR) ==="
  curl -sS -o /dev/null -w "HTTP %{http_code}\n" -u 'user1:password' -X DELETE "${OZON}/api/order/${ORDER_ID}" || true
  echo
fi

echo "=== PAYMENT: успешная оплата (новый платёж) ==="
PAY_NEW=$(curl -sS -u "${PAY_USER}:${PAY_PASS}" -X POST "${PAY}/api/payment" \
  -H 'Content-Type: application/json' \
  -d '{"amountKopecks":5000}')
echo "${PAY_NEW}"
PID_OK=""
if command -v jq >/dev/null 2>&1; then
  PID_OK=$(echo "${PAY_NEW}" | jq -r '.paymentId // empty')
fi
if [[ -n "${PID_OK}" ]]; then
  curl -sS -o /dev/null -w "pay HTTP %{http_code}\n" -u "${PAY_USER}:${PAY_PASS}" -X POST "${PAY}/api/payment/pay" \
    -H 'Content-Type: application/json' \
    -d "{\"cardId\":\"4000000000000002\",\"month\":12,\"yearTail\":29,\"cvc\":\"111\",\"paymentId\":\"${PID_OK}\"}"
  curl -sS -u "${PAY_USER}:${PAY_PASS}" "${PAY}/api/payment/${PID_OK}"
  echo
fi

echo "=== OZON: заказы по ПВЗ (ppadmin) ==="
curl -sS -u 'ppadmin:password' "${OZON}/api/order"
echo

echo "=== OZON: все заказы (superadmin) ==="
curl -sS -u 'superadmin:password' "${OZON}/api/order"
echo

echo "=== Готово ==="
echo "Ручки вручную: PATCH ${OZON}/api/order/{id}/issue (ppadmin, PAID→ISSUED)"
echo "               PATCH ${OZON}/api/order/{id}/status (superadmin, тело {\"orderStatus\":\"NEW\"|...})"
