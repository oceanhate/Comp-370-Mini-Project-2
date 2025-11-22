#!/bin/bash
# kill-backup.sh - Kill a backup server process
# Usage: ./kill-backup.sh [port]
# Default: kills server on port 8089 if no port specified

BACKUP_PORT=${1:-8089}

echo "========================================="
echo "Killing Backup Server Process"
echo "========================================="

# Find the process ID listening on the backup port (only the server, not clients)
# -sTCP:LISTEN filters to only show processes in LISTEN state (servers)
PID=$(lsof -ti tcp:$BACKUP_PORT -sTCP:LISTEN)

if [ -n "$PID" ]; then
    echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
    echo "Found backup server on port $BACKUP_PORT (PID: $PID)"
    echo "Killing process..."
    kill -9 $PID
    
    # Verify the process was killed
    sleep 0.5
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ Backup server on port $BACKUP_PORT successfully killed at $(date '+%Y-%m-%d %H:%M:%S.%3N')"
        echo "Primary server should continue serving requests without disruption."
    else
        echo "✗ Failed to kill process $PID"
        exit 1
    fi
else
    echo "✗ No process found listening on port $BACKUP_PORT"
    echo "The backup server may not be running or is on a different port."
    echo ""
    echo "Available backup ports: 8089, 8088"
    echo "Usage: ./kill-backup.sh [port]"
    exit 1
fi

echo "========================================="
