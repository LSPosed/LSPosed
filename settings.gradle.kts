rootProject.name = "LSPosed"
include(":core", ":hiddenapi-stubs", ":sandhook-hooklib", ":sandhook-annotation", ":app", ":service", ":interface", ":hiddenapi-bridge", ":manager-service")

val serviceRoot = "service"
project(":interface").projectDir = file("$serviceRoot${File.separator}interface")
project(":service").projectDir = file("$serviceRoot${File.separator}service")
