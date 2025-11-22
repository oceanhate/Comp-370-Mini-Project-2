#!/bin/bash
# kill-primary-and-backup.sh - Simultaneously kill primary and one backup server
# Usage: ./kill-primary-and-backup.sh [backup_port]
# Default: kills primary (8090) and backup on 8089

PRIMARY_PORT=8090
BACKUP_PORT=${1:-8089}

echo "========================================="
echo "Simulating Simultaneous Failures"
echo "========================================="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
echo "Target: Primary ($PRIMARY_PORT) + Backup ($BACKUP_PORT)"
echo ""

# Find process IDs (only servers, not clients)
# -sTCP:LISTEN filters to only show processes in LISTEN state (servers)
PRIMARY_PID=$(lsof -ti tcp:$PRIMARY_PORT -sTCP:LISTEN)
BACKUP_PID=$(lsof -ti tcp:$BACKUP_PORT -sTCP:LISTEN)

if [ -z "$PRIMARY_PID" ] && [ -z "$BACKUP_PID" ]; then
    echo "✗ No processes found on ports $PRIMARY_PORT or $BACKUP_PORT"
    exit 1
fi

# Kill both processes simultaneously
KILLED=0

if [ -n "$PRIMARY_PID" ]; then
    echo "Killing primary server (PID: $PRIMARY_PID)..."
    kill -9 $PRIMARY_PID &
    KILLED=$((KILLED + 1))
fi

if [ -n "$BACKUP_PID" ]; then
    echo "Killing backup server (PID: $BACKUP_PID)..."
    kill -9 $BACKUP_PID &
    KILLED=$((KILLED + 1))
fi

# Wait for both kills to complete
wait

echo ""
echo "✓ Killed $KILLED server(s) at $(date '+%Y-%m-%d %H:%M:%S.%3N')"
echo ""
echo "Expected behavior:"
echo "  - Monitor should detect both failures"
echo "  - Remaining backup server should be promoted to primary"
echo "  - System should continue operating if at least one server remains"
echo ""
echo "If all servers have failed, the system will be down until"
echo "servers are restarted with ./run.sh"
echo "========================================="
