; NETHA X PYTHON LAB - Android build configuration
[app]

title = NETHA X PYTHON LAB
package.name = nethaxpython
package.domain = com.nethax

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,txt

version = 0.1.0

requirements = python3,kivy

orientation = portrait
fullscreen = 0

android.api = 35
android.minapi = 24
android.ndk = 28c
android.archs = arm64-v8a
android.accept_sdk_license = True
android.private_storage = True

[buildozer]
log_level = 2
warn_on_root = 1
