#!/bin/bash
# kill-primary.sh - Kill the JVM process running the current primary server
# Usage: ./kill-primary.sh

echo "========================================="
echo "Killing Primary Server Process"
echo "========================================="

# The primary server that runs on port 8090 by default once the port changes t
PRIMARY_PORT=8090
##

# Find the process ID listening on the primary port (only the server, not clients)
# -sTCP:LISTEN filters to only show processes in LISTEN state (servers)
PID=$(lsof -ti tcp:$PRIMARY_PORT -sTCP:LISTEN)

if [ -n "$PID" ]; then
    echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
    echo "Found primary server on port $PRIMARY_PORT (PID: $PID)"
    echo "Killing process..."
    kill -9 $PID
    
    # Verify the process was killed
    sleep 0.5
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ Primary server successfully killed at $(date '+%Y-%m-%d %H:%M:%S.%3N')"
        echo "Monitor should detect failure and promote a backup server."
    else
        echo "✗ Failed to kill process $PID"
        exit 1
    fi
else
    echo "✗ No process found listening on port $PRIMARY_PORT"
    echo "The primary server may not be running or is on a different port."
    exit 1
fi

echo "========================================="

