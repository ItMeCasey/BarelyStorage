"""Runs the dev screenshot tour on a real game client for one layer and collects the screenshots.

Usage: python scripts/client_tour.py <layer> [<layer> ...]     e.g. neoforge-1.21.1 forge-26.2

Starts `gradlew runClient` with BARELYSTORAGE_SHOTS set, moves the game window to DISPLAY6 (the right-hand monitor) as soon as it
exists, waits for the tour to finish (it quits the game itself), and prints the screenshot names. The game is muted and the
tour lets go of the mouse, so nothing needs a person at the keyboard. Screenshots land in <layer>/run/screenshots.
"""
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PLACE = r"C:\Users\casey\.claude\tools\place-window.ps1"
TIMEOUT = 600
OPTIONS = (
    "soundCategory_master:0.0\nmusic:0.0\npauseOnLostFocus:false\nonboardAccessibility:false\n"
    "skipMultiplayerWarning:true\njoinedFirstServer:true\ntutorialStep:none\nfullscreen:false\nguiScale:3\nlang:en_us\n"
)


def tour(layer):
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "options.txt").write_text(OPTIONS)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    env = dict(os.environ, BARELYSTORAGE_SHOTS="1")
    log = []
    proc = subprocess.Popen(["cmd", "/c", str(folder / "gradlew.bat"), "runClient", "--no-daemon", "-q", "-PdevTools"] + (["-Pviewers=" + os.environ["BARELYSTORAGE_VIEWERS"]] if os.environ.get("BARELYSTORAGE_VIEWERS") else []), cwd=folder,
                            env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")

    def pump():
        for line in proc.stdout:
            log.append(line.rstrip())

    threading.Thread(target=pump, daemon=True).start()

    def place():
        # The window title starts with "Minecraft"; this waits up to two minutes for it and moves it to DISPLAY6.
        # Only Java windows count: other windows (a browser tab about Minecraft) must never be moved.
        subprocess.run(["powershell", "-NoProfile", "-File", PLACE, "-ProcessName", "java*", "-Match", "Minecraft",
                        "-Wait", "180"],
                       capture_output=True)

    threading.Thread(target=place, daemon=True).start()
    start = time.time()
    while proc.poll() is None and time.time() - start < TIMEOUT:
        time.sleep(2)
    timed_out = proc.poll() is None
    if timed_out:
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
    (run / "client_tour.log").write_text("\n".join(log), encoding="utf-8")
    names = sorted(p.name for p in shots.glob("*.png")) if shots.exists() else []
    done = any("BARELYSTORAGE-SHOTS DONE" in line for line in log)
    errors = [l for l in log if re.search(r"barelystorage.*(Exception|Error)|ModLoadingException|Crash report saved", l, re.I)]
    status = "TOUR PASS" if done and not timed_out and not errors else "TOUR FAIL"
    detail = "" if status == "TOUR PASS" else f": done={done} timed_out={timed_out} first_error={errors[:1]}"
    return f"{status} {layer} ({len(names)} screenshots, {int(time.time() - start)}s){detail}"


if __name__ == "__main__":
    bad = 0
    for layer in sys.argv[1:]:
        result = tour(layer)
        print(result, flush=True)
        bad += result.startswith("TOUR FAIL")
    sys.exit(1 if bad else 0)
