"""Starts a playable dev client: a creative flat world with the server room built, stocked and powered, and every mod item in the inventory.

Usage: python scripts/play.py [layer] [viewers]     default neoforge-26.2 and smwti,bei (viewers: smwti,bei,jei,rei,architectury,cloth)

The world is recreated on every launch. The window is moved to DISPLAY6. The script returns as soon as the game is starting; close the
game window when done.
"""
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PLACE = r"C:\Users\casey\.claude\tools\place-window.ps1"
OPTIONS = ("pauseOnLostFocus:false\nonboardAccessibility:false\nskipMultiplayerWarning:true\njoinedFirstServer:true\n"
           "tutorialStep:none\nfullscreen:false\nlang:en_us\n")


def main():
    layer = sys.argv[1] if len(sys.argv) > 1 else "neoforge-26.2"
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "options.txt").write_text(OPTIONS)
    env = dict(os.environ, BARELYSTORAGE_SHOTS="play")
    log = open(run / "play.log", "w")
    subprocess.Popen(["cmd", "/c", str(folder / "gradlew.bat"), "runClient", "--no-daemon", "-q", "-PdevTools", "-Pviewers=" + (sys.argv[2] if len(sys.argv) > 2 else "smwti,bei")], cwd=folder, env=env,
                     stdout=log, stderr=subprocess.STDOUT)
    subprocess.Popen(["powershell", "-NoProfile", "-File", PLACE, "-ProcessName", "java*", "-Match", "Minecraft", "-Wait", "240"],
                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(f"starting {layer}; the world is built a few seconds after it loads")


main()
