# Emulator helpers

Used to smoke-test the app headlessly on this Fedora machine (see the main README for the
renderer caveat: the emulator only starts with `-gpu angle_indirect` and with the desktop
`DISPLAY`/`WAYLAND_DISPLAY` variables unset).

```bash
# boot AVD "vlog36" on port 5554, wait up to 240 s, log to $EMU_DIR
tools/emulator/boot.sh vlog36 5554 240 -gpu angle_indirect

# drive the UI with uiautomator dumps (needs python3)
python3 tools/emulator/ui.py tap "Products" -1     # last node whose text/desc contains "Products"
python3 tools/emulator/ui.py tapfield "Latitude"   # focus the text field that carries this label
python3 tools/emulator/ui.py type "48.86"
python3 tools/emulator/ui.py hidekb                # BACK only if the keyboard is really shown
python3 tools/emulator/ui.py shot 01-log           # screenshot to $EMU_DIR/01-log.png
python3 tools/emulator/ui.py dump                  # labelled nodes with bounds
```

Gotchas learned the hard way: dump after the keyboard has settled (the helper waits 1 s),
read-only Compose fields (date pickers) are not `EditText` nodes so numeric field indices shift,
and never check emulator liveness with `pgrep -f`, it matches your own shell.
