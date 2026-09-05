#!/bin/bash
# usage: try.sh <avd> <port> <maxwait_s> <emulator args...>
AVD=$1; PORT=$2; MAXW=$3; shift 3
export ANDROID_HOME=$HOME/Android/Sdk ANDROID_SDK_ROOT=$HOME/Android/Sdk ANDROID_AVD_HOME=$HOME/.android/avd
unset DISPLAY WAYLAND_DISPLAY XAUTHORITY
export NODEVICE_SELECT=1 VK_LOADER_LAYERS_DISABLE='*'
export VK_ICD_FILENAMES=$HOME/Android/Sdk/emulator/lib64/vulkan/vk_swiftshader_icd.json VK_DRIVER_FILES=$VK_ICD_FILENAMES
LOG=${EMU_DIR:-/tmp/vineyard-emu}/try-$AVD-$PORT.log
cd $HOME/Android/Sdk/emulator || exit 1
nohup ./emulator -avd "$AVD" -port "$PORT" -no-window -no-audio -no-boot-anim -accel on -no-snapshot "$@" > "$LOG" 2>&1 &
PID=$!
echo "$AVD on $PORT pid=$PID args: $*"
ADB=$HOME/Android/Sdk/platform-tools/adb
for i in $(seq 1 $((MAXW/5))); do
  if ! kill -0 "$PID" 2>/dev/null; then wait "$PID"; echo "RESULT: $AVD DIED (exit $?) after ~$((i*5))s"; tail -3 "$LOG"; exit 1; fi
  b=$($ADB -s emulator-$PORT shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [ "$b" = "1" ]; then echo "RESULT: $AVD BOOTED after ~$((i*5))s (pid $PID)"; exit 0; fi
  sleep 5
done
echo "RESULT: $AVD still alive but not booted after ${MAXW}s (pid $PID)"; $ADB devices; exit 2
