#!/bin/bash
# Shared test helper library for OAISS CHAIN API test scripts
# Source this file: source "$(dirname "$0")/test-helpers.sh"
#
# Required seed data:
#   - Users: admin, enterprise001, thirdparty001 (password from TEST_PASSWORD env, default: admin123)
#   - MySQL accessible at DB_HOST:DB_PORT with DB_USER/DB_PASS credentials
#   - Backend running at BASE_URL (default: http://localhost:8080/api/v1)

set -euo pipefail

# --- Configuration (overridable via environment) ---
BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
TEST_PASSWORD="${TEST_PASSWORD:-admin123}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-123456}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-oaiss_chain}"

# --- Test counters ---
PASS=0
FAIL=0
SKIP=0
TEST_ID=0

# --- Dependency checks (IN-01/WR-02) ---
check_dependencies() {
    local extra_cmds=("$@")
    for cmd in curl grep cut head "${extra_cmds[@]}"; do
        command -v "$cmd" >/dev/null 2>&1 || { echo "[FATAL] Required command '$cmd' not found"; exit 1; }
    done
}

# --- JSON field extraction with jq fallback (WR-03) ---
extract_field() {
    local json="$1" field="$2"
    if command -v jq >/dev/null 2>&1; then
        echo "$json" | jq -r ".$field // empty" 2>/dev/null
    else
        echo "$json" | grep -o "\"$field\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
    fi
}

# --- Login helper (WR-01/WR-05) ---
login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"$TEST_PASSWORD\"}"
}

# --- Token extraction with validation (WR-01) ---
extract_token() {
    local resp="$1" username="$2"
    local token
    token=$(extract_field "$resp" "accessToken")
    if [ -z "$token" ]; then
        echo "  [FATAL] Login failed for $username. Response: $(echo "$resp" | head -c 300)"
        exit 1
    fi
    echo "$token"
}

# --- MySQL helper with defaults-extra-file to hide password (CR-01) ---
run_mysql() {
    local query="$1"
    local mysql_cnf
    mysql_cnf=$(mktemp)
    cat > "$mysql_cnf" <<EOF
[client]
user=$DB_USER
password=$DB_PASS
host=$DB_HOST
port=$DB_PORT
EOF
    local result
    result=$(mysql --defaults-extra-file="$mysql_cnf" "$DB_NAME" -sNe "$query" 2>&1) || {
        rm -f "$mysql_cnf"
        echo "MYSQL_ERROR:$result"
        return 1
    }
    rm -f "$mysql_cnf"
    echo "$result"
}

# --- Integer validation for SQL variables (CR-02) ---
validate_integer() {
    local var_name="$1" var_value="$2"
    if ! [[ "$var_value" =~ ^[0-9]+$ ]]; then
        echo "  [ERROR] Invalid $var_name: '$var_value' (must be a positive integer)"
        return 1
    fi
}

# --- Precise code 200 assertion (CR-03) ---
assert_code_200() {
    local test_name="$1" response="$2"
    TEST_ID=$((TEST_ID + 1))
    if echo "$response" | grep -qE '"code"\s*:\s*200[^0-9]'; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name -- expected code 200"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}

# --- Generic assertion: response contains expected string (CR-03: use -F for fixed string) ---
assert_contains() {
    local test_name="$1" response="$2" expected="$3"
    TEST_ID=$((TEST_ID + 1))
    if echo "$response" | grep -qF "$expected"; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name -- expected '$expected' in response"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}

# --- Assertion: response does NOT contain expected string ---
assert_not_contains() {
    local test_name="$1" response="$2" expected="$3"
    TEST_ID=$((TEST_ID + 1))
    if ! echo "$response" | grep -qF "$expected"; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name -- did NOT expect '$expected' in response"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}

# --- Summary printer (IN-05: includes skip count) ---
print_summary() {
    echo "========================================"
    echo "Results: $PASS passed, $FAIL failed, $SKIP skipped (total: $TEST_ID tests)"
    echo "========================================"
    if [ "$FAIL" -gt 0 ]; then
        exit 1
    fi
}
