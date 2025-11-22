#!/bin/bash
# restart-server.sh - Restart servers that are not running
# Usage: ./restart-server.sh [port|all]
# Example: ./restart-server.sh 8090   (restart specific server)
#          ./restart-server.sh all    (restart all down servers)
#          ./restart-server.sh        (auto-detect and restart down servers)

PORT=${1}

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Function to check if a server is running on a port
is_server_running() {
    local port=$1
    lsof -ti tcp:$port -sTCP:LISTEN > /dev/null 2>&1
    return $?
}

# Function to restart a server on a specific port
restart_server_on_port() {
    local port=$1
    local server_class=$2
    local server_name=$3
    
    echo "Starting $server_name on port $port..."
    
    # Build the command to run in the new Terminal window
    local cmd="cd '$PROJECT_ROOT' && echo '=== RESTARTED: $server_name (Port $port) ===' && java -cp src ServerNode $port $role"
    
    # Open Terminal and bring it to the front
    osascript -e 'tell application "Terminal" to activate'
    
    # Start server in a new Terminal window
    osascript -e "tell application \"Terminal\" to do script \"$cmd\""
    
    # Wait a moment for the server to start
    sleep 1
    
    # Verify the server is actually running
    if is_server_running $port; then
        echo "✓ $server_name started successfully in new Terminal window"
        return 0
    else
        echo "✗ WARNING: $server_name may not have started properly"
        echo "  Check the Terminal window for errors"
        return 1
    fi
}

# Auto-detect mode: restart all down servers
if [ -z "$PORT" ] || [ "$PORT" = "all" ]; then
    echo "========================================="
    echo "Auto-Restart Down Servers"
    echo "========================================="
    echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
    echo ""
    
    RESTARTED_COUNT=0
    
    # Check Server on 8090
    # IMPORTANT: Restart as backup, not primary! The monitor will promote if needed.
    if ! is_server_running 8090; then
        restart_server_on_port 8090 "BACKUP" "Server on 8090"
        RESTARTED_COUNT=$((RESTARTED_COUNT + 1))
        sleep 0.5
    else
        echo "Server on 8090 is already running"
    fi
    
    # Check Backup 1 (8089)
    if ! is_server_running 8089; then
        restart_server_on_port 8089 "BACKUP" "Backup Server 1"
        RESTARTED_COUNT=$((RESTARTED_COUNT + 1))
        sleep 0.5
    else
        echo "Backup Server 1 (8089) is already running"
    fi
    
    # Check Backup 2 (8088)
    if ! is_server_running 8088; then
        restart_server_on_port 8088 "BACKUP" "Backup Server 2"
        RESTARTED_COUNT=$((RESTARTED_COUNT + 1))
        sleep 0.5
    else
        echo "Backup Server 2 (8088) is already running"
    fi
    
    echo ""
    if [ $RESTARTED_COUNT -eq 0 ]; then
        echo "✓ All servers are already running - nothing to restart"
    else
        echo "✓ Restarted $RESTARTED_COUNT server(s)"
        echo ""
        echo "Restarted servers should:"
        echo "  1. Connect to the monitor"
        echo "  2. Start sending heartbeats"
        echo "  3. Synchronize with the current primary"
        echo "  4. Resume normal operation"
    fi
    echo "========================================="
    exit 0
fi

    exit 0
fi

# Manual mode: restart specific server
echo "========================================="
echo "Restarting Server on Port $PORT"
echo "========================================="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"

# Check if a server is already listening on this port (ignore client connections)
# -sTCP:LISTEN filters to only show processes in LISTEN state (servers)
PID=$(lsof -ti tcp:$PORT -sTCP:LISTEN)
if [ -n "$PID" ]; then
    echo "✗ A server is already listening on port $PORT (PID $PID)"
    echo "Please stop the existing server first or use a different port."
    exit 1
fi

# Determine which server class to run based on port
# IMPORTANT: All restarted servers start as backup class!
# The monitor will promote to primary if needed.
case $PORT in
    8090)
        SERVER_CLASS="BACKUP"
        SERVER_NAME="Server on 8090 (restarting as backup)"
        ;;
    8089)
        SERVER_CLASS="BACKUP"
        SERVER_NAME="Backup Server 1"
        ;;
    8088)
        SERVER_CLASS="BACKUP"
        SERVER_NAME="Backup Server 2"
        ;;
    *)
        echo "✗ Unknown port: $PORT"
        echo "Valid ports are: 8090, 8089, 8088"
        exit 1
        ;;
esac

echo "Starting $SERVER_NAME on port $PORT..."

# Open Terminal and bring it to the front
osascript -e 'tell application "Terminal" to activate'

# Start server in a new Terminal window
osascript -e 'tell application "Terminal" to do script "cd '"$PROJECT_ROOT"' && echo \"=== RESTARTED: '$SERVER_NAME' (Port '$PORT') ===\" && java -cp src ServerNode '$PORT' '$SERVER_ROLE'"'

echo "✓ $SERVER_NAME restarted successfully"
echo ""
echo "The server should:"
echo "  1. Connect to the monitor"
echo "  2. Start sending heartbeats"
echo "  3. Synchronize state with the current primary"
echo "  4. Resume normal operation as backup"
echo "========================================="
