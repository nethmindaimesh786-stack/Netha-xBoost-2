; NETHA X PYTHON LAB - Android build configuration
[app]

title = NETHA X PYTHON LAB
package.name = nethaxpython
package.domain = com.nethax

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,txt

version = 0.1.0

; Use the python-for-android develop branch: it fixes ARM64 wheel installation
; by passing the Android platform tag during both dependency resolution and install.
p4a.branch = develop

; Keep the charset-normalizer pin as an additional compatibility safeguard.
requirements = python3,kivy,charset-normalizer==2.1.1

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
