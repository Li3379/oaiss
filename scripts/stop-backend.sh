#!/usr/bin/env bash
# OAISS CHAIN - 后端停止脚本
# 停止监听 8080 端口的 Java 进程

set -euo pipefail

stopped=false

if command -v lsof >/dev/null 2>&1; then
  pids=$(lsof -ti tcp:8080 -sTCP:LISTEN 2>/dev/null || true)
  if [[ -n "${pids}" ]]; then
    for pid in $pids; do
      cmd=$(ps -p "$pid" -o comm= 2>/dev/null || true)
      if [[ "$cmd" == *java* ]]; then
        echo "[STOP] Stopping backend process $pid"
        kill "$pid" 2>/dev/null || true
        stopped=true
      fi
    done
  fi
fi

if [[ "$stopped" == false ]] && command -v netstat >/dev/null 2>&1; then
  pids=$(netstat -ano 2>/dev/null | grep ":8080" | grep "LISTEN" | awk '{print $NF}' | sort -u || true)
  if [[ -n "${pids}" ]]; then
    for pid in $pids; do
      [[ -z "$pid" || "$pid" == "0" ]] && continue
      echo "[STOP] Stopping backend process $pid"
      if command -v taskkill >/dev/null 2>&1; then
        taskkill //F //PID "$pid" >/dev/null 2>&1 || true
      else
        kill "$pid" 2>/dev/null || true
      fi
      stopped=true
    done
  fi
fi

if [[ "$stopped" == false ]]; then
  echo "[STOP] No backend process is listening on :8080"
  exit 0
fi

echo "[STOP] Backend stop command completed"
