#!/bin/sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR_NAME=${JAR_NAME:-botjava-0.0.1.jar}
JAR_PATH="$APP_HOME/$JAR_NAME"
JAVA_BIN=${JAVA_BIN:-java}
SERVER_PORT=${SERVER_PORT:-8081}
PID_FILE=${PID_FILE:-$APP_HOME/botjava.pid}
LOG_FILE=${LOG_FILE:-$APP_HOME/logs/botjava.log}

if [ -n "${CONFIG_FILE:-}" ]; then
    case "$CONFIG_FILE" in
        /*) ;;
        *) CONFIG_FILE="$APP_HOME/$CONFIG_FILE" ;;
    esac
elif [ -f "$APP_HOME/application.yml" ]; then
    CONFIG_FILE="$APP_HOME/application.yml"
elif [ -f "$APP_HOME/config/application.yml" ]; then
    CONFIG_FILE="$APP_HOME/config/application.yml"
else
    echo "Configuration file not found: application.yml or config/application.yml" >&2
    exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR file not found: $JAR_PATH" >&2
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE" >&2
    exit 1
fi

if [ -f "$PID_FILE" ]; then
    OLD_PID=$(sed -n '1p' "$PID_FILE" 2>/dev/null || true)
    case "$OLD_PID" in
        ''|*[!0-9]*) ;;
        *)
            if kill -0 "$OLD_PID" 2>/dev/null; then
                echo "botjava is already running, pid=$OLD_PID"
                exit 0
            fi
            ;;
    esac
    rm -f "$PID_FILE"
fi

mkdir -p "$(dirname -- "$LOG_FILE")" "$(dirname -- "$PID_FILE")"

if [ -n "${JAVA_OPTS:-}" ]; then
    # JAVA_OPTS is intentionally split into separate JVM arguments.
    # shellcheck disable=SC2086
    set -- $JAVA_OPTS
else
    set --
fi

nohup "$JAVA_BIN" "$@" -jar "$JAR_PATH" \
    "--spring.config.additional-location=file:$CONFIG_FILE" \
    "--server.port=$SERVER_PORT" \
    >> "$LOG_FILE" 2>&1 &

PID=$!
printf '%s\n' "$PID" > "$PID_FILE"
sleep 2

if kill -0 "$PID" 2>/dev/null; then
    echo "botjava started, pid=$PID, port=$SERVER_PORT"
    echo "config=$CONFIG_FILE"
    echo "log=$LOG_FILE"
else
    rm -f "$PID_FILE"
    echo "botjava failed to start; recent log output:" >&2
    tail -n 50 "$LOG_FILE" >&2 || true
    exit 1
fi