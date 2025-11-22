#!/bin/bash
# run.sh - Start monitor, 3 server instances, and client
# Usage: ./run.sh

# Detect operating system
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    elif [[ "$OSTYPE" == "cygwin" || "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
        echo "windows"
    else
        echo "unknown"
    fi
}

OS=$(detect_os)
echo "Detected OS: $OS"

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$PROJECT_ROOT/src"

echo "========================================="
echo "Starting Heartbeat Monitor System"
echo "========================================="

# Compile all Java files
echo "Compiling Java files..."
javac "$SRC_DIR"/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed. Please fix errors and try again."
    exit 1
fi

echo "Compilation successful."
echo ""

# Clean up any delay flags from previous runs
rm -f /tmp/heartbeat_delay.flag

# Function to open new terminal based on OS
open_terminal() {
    local title="$1"
    local command="$2"
    
    case "$OS" in
        macos)
            osascript -e 'tell application "Terminal" to do script "cd '"$PROJECT_ROOT"' && echo \"=== '"$title"' ===\" && '"$command"'"'
            ;;
        linux)
            # Try different terminal emulators in order of preference
            if command -v gnome-terminal &> /dev/null; then
                gnome-terminal -- bash -c "cd '$PROJECT_ROOT' && echo '=== $title ===' && $command; exec bash"
            elif command -v xterm &> /dev/null; then
                xterm -e "cd '$PROJECT_ROOT' && echo '=== $title ===' && $command; bash" &
            elif command -v konsole &> /dev/null; then
                konsole -e bash -c "cd '$PROJECT_ROOT' && echo '=== $title ===' && $command; exec bash" &
            else
                echo "No supported terminal emulator found. Please install gnome-terminal, xterm, or konsole."
                exit 1
            fi
            ;;
        windows)
            # For Git Bash or Cygwin on Windows
            if command -v cmd.exe &> /dev/null; then
                cmd.exe /c start bash -c "cd '$PROJECT_ROOT' && echo '=== $title ===' && $command; exec bash"
            else
                echo "Windows terminal support requires Git Bash or Cygwin."
                exit 1
            fi
            ;;
        *)
            echo "Unsupported OS: $OS"
            exit 1
            ;;
    esac
}

# Activate terminal on macOS
if [ "$OS" == "macos" ]; then
    osascript -e 'tell application "Terminal" to activate'
    sleep 0.5
fi

# Start Monitor in a new Terminal window
echo "Starting Monitor on port 9000..."
open_terminal "MONITOR" "java -cp src Monitor"
sleep 1

# Start Server 1 (Primary - port 8090) in separate process
echo "Starting Server 1 (port 8090)..."
open_terminal "SERVER 1 (8090)" "java -cp src ServerNode 8090 PRIMARY"
sleep 1

# Start Server 2 (Backup - port 8089) in separate process
echo "Starting Server 2 (port 8089)..."
open_terminal "SERVER 2 (8089)" "java -cp src ServerNode 8089 BACKUP"
sleep 1

# Start Server 3 (Backup - port 8088) in separate process
echo "Starting Server 3 (port 8088)..."
open_terminal "SERVER 3 (8088)" "java -cp src ServerNode 8088 BACKUP"
sleep 1

# Start Client
echo "Starting Client..."
open_terminal "CLIENT" "java -cp src Client"

echo ""
echo "========================================="
echo "All components started successfully!"
echo "========================================="
echo "Monitor:  Port 9000 (heartbeat), 9001 (client API)"
echo "Server 1: Port 8090"
echo "Server 2: Port 8089"
echo "Server 3: Port 8088"
echo ""
echo "Use kill-primary.sh to kill the primary server"
echo "Use kill-backup.sh to kill a backup server"
echo "Use delay-heartbeat.sh to simulate network delays"
echo "========================================="
