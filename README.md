<!-- @format -->

# Comp-370-Mini-Project - Primary-Backup Server with Heartbeat Monitor

**Group #6 [Fusion Five]** - COMP 370 Server Redundancy Project

## Table of Contents

- [Project 2: Design Pattern Refactoring](#project-2-design-pattern-refactoring)
- [Project Overview](#project-overview)
- [System Architecture](#system-architecture)
- [Quick Start](#quick-start)
- [Port Configuration](#port-configuration)
- [Component Details](#component-details)
- [Testing Scripts](#testing-scripts)
- [Test Scenarios](#test-scenarios)
- [Requirements](#requirements)
- [Troubleshooting](#troubleshooting)

---

## Project 2: Design Pattern Refactoring

This project has been refactored to implement key design patterns for improved maintainability, extensibility, and code quality:

### **1. Singleton Pattern** ✅

**Implementation:**

- **Monitor Class**: Ensures only one Monitor instance exists system-wide
  - Private constructor prevents direct instantiation
  - `getInstance()` provides synchronized access to single instance
  - Manages all heartbeat monitoring and failover logic centrally
- **Client Class**: Single client instance manages all server connections
  - Prevents multiple conflicting client instances
  - Centralized connection management and primary server discovery

**Benefits:**

- Guaranteed single point of control for monitoring
- Prevents resource conflicts and duplicate monitoring
- Global access point without global variables

### **2. Observer Pattern** ✅

**Implementation:**

- **Observer Interface**: Defines `update(String event)` contract
- **Monitor (Subject)**: Maintains observer list and notifies on events
- **Concrete Observers**:
  - `LoggingObserver`: Logs all system events with `[LOG]` prefix
  - `AlertObserver`: Filters and alerts on critical events (DEAD, FAILED, FAILOVER) with `[ALERT!]` prefix

**Events Tracked:**

- `SERVER_ALIVE`: Server comes online or recovers
- `SERVER_DEATH`: Server failure detected
- `FAILOVER_INITIATED`: Primary failure triggers failover
- `PROMOTION_SUCCESS`: Backup promoted to primary
- `PROMOTION_FAILED`: No available servers to promote

**Benefits:**

- Decoupled notification logic from core monitoring
- Easy to add new observers (e.g., email alerts, metrics tracking, dashboard updates)
- Single Responsibility: Monitor focuses on detection, observers handle reactions
- Open-Closed Principle: Add observers without modifying Monitor

### **3. Abstraction-Occurrence Pattern** ✅

**Implementation:**

- **ServerProcess (Abstraction)**: Abstract base class containing common server behavior
  - Shared attributes: `serverPort`, `messageCount`, `isPrimary`, `running`
  - Common methods: `process()`, `sendHeartbeats()`, `handleClient()`, `replicateStateToBackups()`
  - Abstract hook: `onPromotedToPrimary()` for subclass customization
- **ServerNode (Occurrence)**: Concrete server instances extending `ServerProcess`
  - Each instance represents a specific server with unique port and role
  - Adds role-specific behavior through `Role` enum (PRIMARY/BACKUP)
  - Implements promotion behavior via `onPromotedToPrimary()` hook
- **ClusterConfig.NodeInfo (Configuration Abstraction)**: Separates configuration from instances
  - Holds port and role pairs as abstraction
  - `NODES` array contains all server occurrence configurations

**Benefits:**

- Eliminates code duplication across Primary and Backup servers
- Centralizes common server logic in one maintainable location
- Easy to add new servers (just create new `ServerNode` instance)
- Clear separation between what varies (port, role) and what stays the same (server behavior)
- Simplified configuration management through `ClusterConfig`

### **Design Pattern Impact**

| Metric                   | Before                          | After                          | Improvement        |
| ------------------------ | ------------------------------- | ------------------------------ | ------------------ |
| Monitor Instance Control | Multiple possible               | Single (Singleton)             | ✅ Consistency     |
| Client Instance Control  | Multiple possible               | Single (Singleton)             | ✅ Consistency     |
| Server Code Duplication  | Separate Primary/Backup classes | Single `ServerProcess` base    | ✅ DRY Principle   |
| Notification Coupling    | Tight (direct prints)           | Loose (Observer)               | ✅ Extensibility   |
| Adding Event Handlers    | Modify Monitor                  | Add new Observer               | ✅ Open-Closed     |
| Adding New Servers       | Create new class                | New `ServerNode` instance      | ✅ Simplicity      |
| Configuration Management | Hardcoded in multiple places    | Centralized in `ClusterConfig` | ✅ Maintainability |

**Files Locations:**

- Refactored code: `/SocketServer/src/` (Monitor.java, Client.java, Observer.java, LoggingObserver.java, AlertObserver.java, ServerProcess.java, ServerNode.java, ClusterConfig.java)
- Original code (pre-refactoring): Root directory files for comparison

---

## Project Overview

This project implements a **fault-tolerant primary-backup server system** with:

- **1 Monitor**: Detects server failures via heartbeat mechanism
- **3 Servers**: 1 Primary + 2 Backups with automatic failover
- **Client Application**: Automatically discovers and connects to primary server
- **Heartbeat Protocol**: Continuous health monitoring (2-second intervals)
- **Automatic Promotion**: Backup servers promoted to primary on failure detection
- **State Replication**: Message counter synchronized to all backups in real-time

### State Replication Details

The system maintains a **message counter** that tracks the total number of client messages processed:

- **Primary Server**: Increments counter for each client message received
- **Real-time Sync**: After each message, state is replicated to all backup servers
- **Seamless Failover**: When backup is promoted, it continues with the same count
- **No Data Loss**: Clients see consistent message counts across failovers

**Example:**

```
Primary receives 5 messages → Counter = 5 → Synced to backups
Primary fails → Backup promoted → New primary continues at Counter = 5
```

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Monitor                              │
│  Port 9000 (Heartbeat) | Port 9001 (Client API)            │
│  - Receives heartbeats from all servers                     │
│  - Detects failures (5-second timeout)                      │
│  - Promotes backups to primary                              │
│  - Informs clients of primary changes                       │
└─────────────────────────────────────────────────────────────┘
                    ↑              ↑              ↑
                    │ Heartbeat    │ Heartbeat    │ Heartbeat
                    │              │              │
         ┌──────────┴─────┐  ┌────┴──────┐  ┌────┴──────┐
         │  Server 1      │  │ Server 2  │  │ Server 3  │
         │  Port 8090     │  │ Port 8089 │  │ Port 8088 │
         │  (Primary)     │  │ (Backup)  │  │ (Backup)  │
         └────────┬───────┘  └───────────┘  └───────────┘
                  │
                  │ Client Requests
                  │
         ┌────────┴────────┐
         │     Client      │
         │  Auto-discovery │
         │  Auto-reconnect │
         └─────────────────┘
```

**Platform Compatibility:**

- **Core System**: Fully cross-platform (Java-based)
- **Bash Scripts**: The `run.sh` script automatically detects your OS and adapts:
  - **macOS**: Uses `osascript` to open new Terminal windows ✅ Confirmed working
  - **Linux**: Tries `gnome-terminal`, `xterm`, or `konsole` in order of preference
  - **Windows**: Supports Git Bash/Cygwin with `cmd.exe`
- **Kill/Stop/Status Scripts**: Work on Linux/macOS (use `lsof` command)
  - Windows users: Use WSL (Windows Subsystem for Linux)
- **Manual Operation**: Works on all platforms - just run each component in separate terminals:
  ```bash
  java -cp src Monitor
  java -cp src primary 8090
  java -cp src backup 8089
  java -cp src backup2 8088
  java -cp src Client
  ```

---

## Quick Start

### Prerequisites

- Java JDK 8 or higher
- **macOS/Linux**: Bash shell (default on both)
- **Windows**: Git Bash or WSL recommended for script execution
- `lsof` utility (standard on macOS/Linux) for status and kill scripts

### Step 1: Make Scripts Executable

```bash
cd SocketServer/scripts
chmod +x *.sh
```

### Step 2: Start the System

```bash
./run.sh
```

This will:

1. Compile all Java files
2. Start Monitor in a new Terminal window
3. Start 3 Server instances in separate Terminal windows
4. Start Client in a new Terminal window

![All Servers Running](photos/allservers_running.png)
_System running with all servers active_

### Step 3: Test the System

```bash
# Check system status
./status.sh

# Test primary crash scenario
./kill-primary.sh

# Test backup crash scenario
./kill-backup.sh

```

### Step 4: Stop the System

```bash
./stop-all.sh
```

---

## Port Configuration

| Component    | Port | Purpose                          |
| ------------ | ---- | -------------------------------- |
| **Monitor**  | 9000 | Heartbeat reception from servers |
| **Monitor**  | 9001 | Client API for primary discovery |
| **Server 1** | 8090 | Primary server (initially)       |
| **Server 2** | 8089 | Backup server 1                  |
| **Server 3** | 8088 | Backup server 2                  |

**Promotion Priority**: Servers are promoted in descending port order (8090 → 8089 → 8088)

---

## Component Details

### Monitor (`Monitor.java`)

- **Heartbeat Listener**: Receives heartbeats on port 9000
- **Death Checker**: Runs every 2 seconds to detect server failures
- **Timeout**: 5 seconds without heartbeat = server presumed dead
- **Promotion Logic**: Promotes highest-port backup to primary on failure
- **No Auto-Demotion**: When failed primary restarts, it rejoins as backup (current primary stays primary)
- **Client API**: Port 9001 serves primary server information to clients

**Key Functions:**

- `handleHeartbeat()`: Processes incoming heartbeats with port# and timestamp
- `runDeathChecker()`: Monitors server liveness and triggers promotion only when primary fails
- `runClientApiListener()`: Responds to client requests for current primary

### ServerProcess (`ServerProcess.java`)

Abstract base class for all server instances.

**Features:**

- Sends heartbeats every 2 seconds (port# | timestamp format)
- Accepts client connections and handles requests
- Processes PROMOTE command from monitor
- Multi-threaded: separate threads for server listen and heartbeat sending
- **State Management**: Maintains message counter (`messageCount`)
- **State Replication**: Primary replicates state to backups after each message

**Key Methods:**

- `process()`: Starts server and heartbeat threads
- `sendHeartbeat()`: Sends heartbeat to monitor
- `handleClient()`: Processes client requests and replicates state
- `replicateStateToBackups()`: Sends state updates to all backup servers
- `onPromotedToPrimary()`: Hook for promotion logic

**State Protocol:**

- Primary increments `messageCount` on each client message
- Primary sends `STATE_UPDATE:<count>` to all backups
- Backups update their local `messageCount` to stay synchronized
- On promotion, backup already has current state

### Server Classes

- **`primary.java`**: Initially starts as primary server (port 8090)
- **`backup.java`**: Backup server 1 (port 8089)
- **`backup2.java`**: Backup server 2 (port 8088)

All extend `ServerProcess` and can be promoted to primary.

### Client (`Client.java`)

**Features:**

- **Strict Primary Connection**: Only connects to monitor's designated primary server
- Auto-discovers primary server via Monitor API (port 9001)
- Waits for monitor to promote new primary on failure (no fallback connections)
- Interactive message sending interface

**Behavior:**

- Queries Monitor for current primary on startup and after disconnections
- **Only connects to the server the monitor designates as primary**
- Does NOT connect to backup servers
- If primary is down, waits for monitor to detect failure and promote backup
- Retries connection every 5 seconds until new primary is available
- Ensures all messages go to the actual primary server

---

## Testing Scripts

All scripts are located in `SocketServer/scripts/`. See `SocketServer/scripts/README.md` for detailed documentation.

### Core Scripts (Required by Project)

#### 1. `run.sh` - Start Complete System

```bash
./run.sh
```

Starts Monitor, 3 servers, and client in separate Terminal windows.

**OS-Specific Behavior:**

- **macOS**: Opens new Terminal.app windows using AppleScript
- **Linux**: Tries gnome-terminal → xterm → konsole in order
- **Windows (Git Bash/Cygwin)**: Uses cmd.exe to launch bash terminals
- **Auto-detected**: Script automatically detects your OS and uses appropriate commands

#### 2. `kill-primary.sh` - Simulate Primary Crash

```bash
./kill-primary.sh
```

Kills the primary server process (port 8090) with timestamps.

**Important Notes:**

- All kill scripts use `-sTCP:LISTEN` flag to only kill server processes, not client connections. This ensures clients remain running and can reconnect after failover.
- **By design**, `kill-primary.sh` always kills port 8090, and `kill-backup.sh` kills port 8089 by default. However, the **actual primary** may be running on a different port after failover (like if 8090 was killed and 8089 became primary). The script names refer to the static port numbers, not the dynamic primary role.

#### 3. `delay-heartbeat.sh` - Simulate Network Delay

```bash
./delay-heartbeat.sh [seconds]
```

Creates flag file to trigger artificial delay in heartbeat processing.

### Additional Testing Scripts

#### 4. `kill-backup.sh` - Kill Backup Server

```bash
./kill-backup.sh [port]        # Default: 8089
./kill-backup.sh 8088           # Kill specific backup
```

**Note:** Kills the server on the specified port (8089 by default). The actual role (primary/backup) depends on current system state.

#### 5. `kill-primary-and-backup.sh` - Simultaneous Failures

```bash
./kill-primary-and-backup.sh [backup_port]
```

Kills primary (port 8090) and one backup simultaneously.

**Note:** Targets static ports 8090 and specified backup port, regardless of which server is currently acting as primary.

#### 6. `restart-server.sh` - Restart Crashed Server

**Auto-detect mode (Recommended)** - Automatically restarts any down servers:

```bash
./restart-server.sh             # Auto-detect and restart down servers
./restart-server.sh all         # Same as above
```

**Manual mode** - Restart specific server:

```bash
./restart-server.sh [port]
./restart-server.sh 8090        # Restart primary
./restart-server.sh 8089        # Restart backup on 8089
./restart-server.sh 8088        # Restart backup on 8088
```

**Features:**

- ✓ Auto-detects which servers are down (no arguments needed)
- ✓ Opens each restarted server in a new Terminal window
- ✓ Safe - won't restart servers that are already running
- ✓ Smart - only restarts what's needed
- ✓ **All restarted servers rejoin as backups** (even port 8090)
- ✓ Monitor will only promote to primary if current primary fails

**Important Behavior:**
When a failed server is restarted, it **always rejoins as a backup server**, regardless of what port it's on. The monitor maintains the current primary and will only trigger a new promotion if that primary fails. This prevents unnecessary disruption to the system.

## Test Scenarios

### Scenario 1: Normal Operation

**Steps:**

1. Run `./run.sh`
2. Verify heartbeats in Monitor window
3. Verify primary server selected (port 8090)
4. Send client requests
5. Verify responses and state replication

**Expected:**

- ✓ Primary server handles all client requests
- ✓ Backups receive state replication
- ✓ Heartbeats sent every 2 seconds

### Scenario 2: Primary Crash

**Steps:**

1. Run `./run.sh`
2. Run `./kill-primary.sh`
3. Record timestamps: kill time, detection time, failover time

**Expected:**

- ✓ Monitor detects failure within 5 seconds
- ✓ Backup promoted to primary (port 8089 or 8088)
- ✓ Clients automatically reconnect
- ✓ No request loss after reconnection

![Primary Killed](photos/kill_primary.png)
_Failover behavior when primary server is killed_

### Scenario 3: Backup Crash

**Steps:**

1. Run `./run.sh`
2. Run `./kill-backup.sh 8089`
3. Verify primary continues
4. Run `./restart-server.sh 8089`
5. Verify backup synchronizes state

**Expected:**

- ✓ Primary continues without disruption
- ✓ Restarted backup rejoins cluster
- ✓ State synchronized from primary

### Scenario 4: Simultaneous Failures

**Steps:**

1. Run `./run.sh`
2. Run `./kill-primary-and-backup.sh`
3. Run `./status.sh` to check survivors

**Expected:**

- ✓ Remaining server promoted to primary
- ✓ System continues if ≥1 server alive
- ✓ Graceful degradation documented

![Simultaneous Failures](photos/kill_primary_and_backup.png)
_System behavior when primary and backup are killed simultaneously_

### Scenario 5: Recovery

**Steps:**

1. Run `./stop-all.sh`
2. Wait 5 seconds
3. Run `./run.sh`
4. Observe server rejoin and state sync

**Expected:**

- ✓ Servers reconnect to monitor
- ✓ State synchronized correctly
- ✓ Correct roles assigned
- ✓ Normal operation resumes

---

## Requirements

### Functional Requirements (FR)

- ✓ FR1: Primary-backup server architecture
- ✓ FR2: Heartbeat-based failure detection
- ✓ FR3: Automatic backup promotion on primary failure
- ✓ FR4: Client auto-discovery and reconnection
- ✓ FR5: State replication to backup servers
- ✓ FR6: Support for 3 server instances minimum

### Non-Functional Requirements (NFR)

- ✓ NFR1: Failure detection within 5 seconds
- ✓ NFR2: Seamless failover with minimal downtime
- ✓ NFR3: No data loss during failover
- ✓ NFR4: System handles simultaneous failures gracefully
- ✓ NFR5: Scripts for reproducible testing

---

## Troubleshooting

### Port Already in Use

```bash
./stop-all.sh
sleep 2
./run.sh
```

### Scripts Won't Execute

```bash
chmod +x SocketServer/scripts/*.sh
```

### Compilation Errors

```bash
cd SocketServer/src
javac *.java
```

### Check What's Running

```bash
./status.sh
lsof -i :8090    # Check specific port
```

### Kill Specific Process

```bash
kill -9 $(lsof -ti :8090)
```

### Clean Restart

```bash
./stop-all.sh
rm /tmp/heartbeat_delay.flag
sleep 3
./run.sh
```

---

## Documentation

- **Detailed Script Documentation**: See `SocketServer/scripts/README.md`
- **Quick Reference**: See `SocketServer/scripts/QUICK_REFERENCE.md`
- **Test Results**: Record timestamps and observations for each scenario

### Recording Test Data

For each test scenario, record:

1. **Time of Event**: When the failure occurred
   ```bash
   date '+%Y-%m-%d %H:%M:%S.%3N'
   ```
2. **Detection Time**: How long until Monitor detected failure
3. **Failover Time**: Time from detection to new primary accepting requests
4. **State Consistency**: Any inconsistencies observed and how resolved

---

## Project Structure

```
Comp-370-Mini-Project-/
├── README.md                          # This file
├── SocketServer/
│   ├── scripts/
│   │   ├── run.sh                    # Start system
│   │   ├── kill-primary.sh           # Kill primary server
│   │   ├── kill-backup.sh            # Kill backup server
│   │   ├── kill-primary-and-backup.sh# Simultaneous failures
│   │   ├── delay-heartbeat.sh        # Simulate network delay
│   │   ├── restart-server.sh         # Restart crashed server
│   │   ├── stop-all.sh               # Stop all processes
│   └── src/
│       ├── Monitor.java              # Heartbeat monitor & failover controller
│       ├── ServerProcess.java        # Abstract server base class
│       ├── primary.java              # Primary server implementation
│       ├── backup.java               # Backup server 1
│       ├── backup2.java              # Backup server 2
│       └── Client.java               # Client with auto-discovery
└── [Other directories...]
```

## Team Information

**Group #6 - Fusion Five**

- COMP 370 - Server Redundancy Project
- Implementation: Primary-Backup Server with Heartbeat Monitor

---

## Updates Log

### Latest Updates

- ✅ Implemented comprehensive bash testing scripts
- ✅ Added automatic timestamp recording for all scenarios
- ✅ Created interactive test scenario guide
- ✅ Added system status monitoring
- ✅ Implemented server restart capability
- ✅ Added network delay simulation support
- ✅ Complete documentation for all testing scenarios

### Previous Updates

- ✅ Fixed shutdown functionality
- ✅ Implemented Backup Server 2
- ✅ Implemented automatic promotion logic
- ✅ Updated Monitor and Client for promotion handling
- ✅ Added simulation commands in Main class
- ✅ Client auto-detection of primary disconnection
- ✅ Monitor signals client on primary changes

---

**Order of Operation**: Monitor → Servers → Client (Important for correct startup!)
