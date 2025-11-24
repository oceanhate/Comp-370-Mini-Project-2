#!/bin/bash
# restart-server.sh - Restart servers that are not running

PORT=${1}

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Check if a server is running on a port
is_server_running() {
    local port=$1
    lsof -ti tcp:$port -sTCP:LISTEN > /dev/null 2>&1
    return $?
}

# Restart a server on a specific port
restart_server_on_port() {
    local port=$1
    local role=$2
    local server_name=$3

    echo "Starting $server_name on port $port..."

    local cmd="cd '$PROJECT_ROOT' && echo '=== RESTARTED: $server_name (Port $port) ===' && java -cp src ServerNode $port $role"

    osascript -e 'tell application "Terminal" to activate'
    osascript -e "tell application \"Terminal\" to do script \"$cmd\""

    sleep 1

    if is_server_running $port; then
        echo "✓ $server_name started successfully"
    else
        echo "✗ WARNING: $server_name may not have started"
    fi
}

# -------------------------------------------
# AUTO-DETECT MODE (no port or 'all')
# -------------------------------------------
if [ -z "$PORT" ] || [ "$PORT" = "all" ]; then
    echo "========================================="
    echo "Auto-Restart Down Servers"
    echo "========================================="

    RESTARTED_COUNT=0

    if ! is_server_running 8090; then
        restart_server_on_port 8090 "BACKUP" "Server on 8090"
        RESTARTED_COUNT=$((RESTARTED_COUNT + 1))
    else
        echo "Server on 8090 is already running"
    fi

    if ! is_server_running 8089; then
        restart_server_on_port 8089 "BACKUP" "Backup Server 1"
        RESTARTED_COUNT=$((RESTARTED_COUNT + 1))
    else
        echo "Backup Server 1 (8089) is already running"
    fi

    if ! is_server_running 8088; then
        restart_server_on_port 8088 "BACKUP" "Backup Server 2"
        RESTARTED_COUNT=$((RESTARTED_COUNT + 1))
    else
        echo "Backup Server 2 (8088) is already running"
    fi

    echo ""
    echo "✓ Restarted $RESTARTED_COUNT server(s)"
    echo "========================================="
    exit 0
fi

# -------------------------------------------
# MANUAL MODE (restart specific port)
# -------------------------------------------

echo "========================================="
echo "Restarting Server on Port $PORT"
echo "========================================="

PID=$(lsof -ti tcp:$PORT -sTCP:LISTEN)
if [ -n "$PID" ]; then
    echo "✗ A server is already listening on port $PORT (PID $PID)"
    exit 1
fi

echo "Starting ServerNode on port $PORT (role BACKUP)..."

osascript -e 'tell application "Terminal" to activate'
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -cp src ServerNode $PORT BACKUP\""

echo "✓ Server restarted successfully"
echo "========================================="
