[app]
# -------------------------------------------------
# Basic metadata
# -------------------------------------------------
title            = Starborn
package.name     = starborn
package.domain   = org.jcthomas

# -------------------------------------------------
# Source / entry point
# -------------------------------------------------
source.dir       = .
source.main      = main.py
source.include_exts = py,kv,png,jpg,ttf,otf,json,mp3,ogg,wav

# -------------------------------------------------
# Version & display
# -------------------------------------------------
version          = 0.1
fullscreen       = 1
orientation      = portrait

# -------------------------------------------------
# Python / Kivy requirements
# (add more packages later, comma-separated, **no** inline comments)
# -------------------------------------------------
requirements     = python3,kivy,pygame,numpy,Pillow,plyer

android.gradle_properties = org.gradle.jvmargs=-Xmx4g

source.exclude_dirs = .venv,gemini,.tmp.driveupload,.git,__pycache__

[buildozer]
log_level        = 2
# icon.filename  = images/icon.png    # set if you have one


[android]
# -------------------------------------------------
# SDK / NDK targets
# -------------------------------------------------
android.api      = 34
android.minapi   = 24

# -------------------------------------------------
# Single architecture keeps the build lighter
# -------------------------------------------------
android.archs    = arm64-v8a

# -------------------------------------------------
# Debug build flag (set to 0 for release)
# -------------------------------------------------
android.debug    = 1
