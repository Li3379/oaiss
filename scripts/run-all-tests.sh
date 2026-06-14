#!/bin/bash
set +e
cd "$(dirname "${BASH_SOURCE[0]}")/.."

SCRIPTS=(
    "admin-test.sh"
    "enterprise-test.sh"
    "reviewer-test.sh"
    "user-test.sh"
    "login-test.sh"
    "credit-score-test.sh"
    "coin-balance-test.sh"
    "emission-test.sh"
    "sign-test.sh"
    "file-test.sh"
    "search-test.sh"
    "thirdparty-test.sh"
    "double-auction-test.sh"
    "p2p-trade-test.sh"
    "project-lifecycle-test.sh"
    "market-prediction-test.sh"
    "captcha-test.sh"
    "blockchain-test.sh"
    "edge-test.sh"
    "bugfix-test.sh"
    "aop-test.sh"
    "health-check.sh"
    "skill-verify.sh"
)

PASS_COUNT=0
FAIL_COUNT=0
RESULTS=()

for s in "${SCRIPTS[@]}"; do
    echo ""
    echo "=========================================="
    echo " $s"
    echo "=========================================="
    # Skip aop-test.sh as it requires Windows native environment and backend restart
    if [[ "$s" == "aop-test.sh" ]]; then
        echo "SKIP: aop-test.sh (requires Windows native environment)"
        RESULTS+=("$s: SKIP (requires Windows native environment)")
        continue
    fi

    # Add delay between scripts to avoid rate limiting
    sleep 3
    OUTPUT=$(bash "./scripts/$s" 2>&1)
    EXIT_CODE=$?
    echo "$OUTPUT"
    
    # Extract summary line
    SUMMARY=$(echo "$OUTPUT" | grep "Results:" | head -1)
    
    if [ $EXIT_CODE -eq 0 ]; then
        STATUS="PASS"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        STATUS="FAIL"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
    RESULTS+=("$s: $STATUS (exit=$EXIT_CODE) $SUMMARY")
done

echo ""
echo ""
echo "=========================================="
echo " BATCH EXECUTION SUMMARY"
echo "=========================================="
echo "Total: ${#SCRIPTS[@]} scripts"
echo "Passed: $PASS_COUNT"
echo "Failed: $FAIL_COUNT"
echo ""
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
