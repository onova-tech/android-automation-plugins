# WhatsApp plugin (example, not validated)

Sends a message through the `wa.me` deeplink and reads the last messages of a chat.

**Status:** the element hints in `targets/chat.yaml` are guesses and have not been checked on
the real app. The resolver falls back to intent, role and region when they are wrong. Validate
with a `uiautomator` dump of a real chat screen before using it, and add the tested app version
to `plugin.yaml`.

Build: `./gradlew :agp:run --args="build plugins/whatsapp --libs plugins/libraries -o build/whatsapp.agp"`
