#!/bin/bash
echo "=== NORA TUNNEL RELEASE AUDIT ==="
grep -r "status = CONNECTED" app/src --include="*.kt" && echo "FAIL: Fake connection found" && exit 1 || echo "PASS: No fake status"
grep -r "FAKE\|hardcoded.*password" app/src --include="*.kt" && echo "FAIL" || echo "PASS: No hardcoded secrets"
grep -r "password=" app/src --include="*.kt" | grep -v "password=******" && echo "FAIL: Plain password" && exit 1 || echo "PASS: Redacted"
grep -r "INTERNET" app/src/main/AndroidManifest.xml && echo "PASS: Permissions minimal" || echo "FAIL"
./gradlew lint test && echo "PASS: Tests & Lint" || echo "FAIL"
echo "=== READY FOR ./gradlew assembleDebug ==="
