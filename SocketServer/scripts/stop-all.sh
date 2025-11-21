#!/bin/bash
# stop-all.sh - Stop all server processes and monitor gracefully
# Usage: ./stop-all.sh

echo "========================================="
echo "Stopping All Processes"
echo "========================================="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
echo ""

PORTS=(8090 8089 8088 9000)
NAMES=("Primary Server" "Backup Server 1" "Backup Server 2" "Monitor")
KILLED_COUNT=0

for i in "${!PORTS[@]}"; do
    PORT=${PORTS[$i]}
    NAME=${NAMES[$i]}
    
    # Find only server processes in LISTEN state, not clients
    PID=$(lsof -ti tcp:$PORT -sTCP:LISTEN)
    
    if [ -n "$PID" ]; then
        echo "Stopping $NAME on port $PORT (PID: $PID)..."
        kill -9 $PID
        KILLED_COUNT=$((KILLED_COUNT + 1))
    else
        echo "No process found for $NAME on port $PORT"
    fi
done

# Clean up any delay flags
rm -f /tmp/heartbeat_delay.flag

echo ""
echo "✓ Stopped $KILLED_COUNT process(es)"
echo "✓ Cleaned up temporary files"
echo "========================================="
