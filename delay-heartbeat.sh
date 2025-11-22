#!/bin/bash
# delay-heartbeat.sh - Simulate network delay in heartbeat handling
# Usage: ./delay-heartbeat.sh [seconds]
# Default delay is 3 seconds if not specified

DELAY_SECONDS=${1:-3}
HEARTBEAT_DELAY_FLAG="/tmp/heartbeat_delay.flag"

echo "========================================="
echo "Simulating Heartbeat Delay"
echo "========================================="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
echo "Setting delay of $DELAY_SECONDS seconds..."

# Create flag file with the delay duration
echo "$DELAY_SECONDS" > "$HEARTBEAT_DELAY_FLAG"

echo "✓ Heartbeat delay flag created at: $HEARTBEAT_DELAY_FLAG"
echo ""
echo "Your Monitor or ServerProcess code should:"
echo "  1. Check if this file exists"
echo "  2. Read the delay value from it"
echo "  3. Sleep for that duration before processing heartbeats"
echo ""
echo "To remove the delay, run: rm $HEARTBEAT_DELAY_FLAG"
echo "========================================="

# Optional: Auto-remove after a duration (uncomment if desired)
# echo "Delay will be automatically removed after 30 seconds..."
# sleep 30
# rm -f "$HEARTBEAT_DELAY_FLAG"
# echo "✓ Delay removed at $(date '+%Y-%m-%d %H:%M:%S.%3N')"

