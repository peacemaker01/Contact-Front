<#
.SYNOPSIS
    Build and package Contact Front (tactical mode) into a native app image via jpackage.
    One JVM, no network, no bundled interpreter. Run ON the target OS (no cross-compile).
    Requires Maven on PATH and a JDK 21+ with jpackage in bin/.
#>
$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.1"
}

$Root = (Split-Path -Parent $MyInvocation.MyCommand.Definition) | Split-Path -Parent
$M2 = "$env:USERPROFILE\.m2\repository"
$Mvn = if ($env:MVN) { $env:MVN }
       elseif (Test-Path "$Root\tools\apache-maven-3.9.16\bin\mvn.cmd") { "$Root\tools\apache-maven-3.9.16\bin\mvn.cmd" }
       else { "mvn" }
$AppVersion = "1.0.0"
$FxVersion = "21.0.2"

function Resolve-JPackage {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
        return "$env:JAVA_HOME\bin\jpackage.exe"
    }
    return "jpackage"
}

if ($env:OS -eq "Windows_NT") { $platform = "win" }
elseif ($IsLinux) { $platform = "linux" }
else { $platform = "mac" }

$jpkg = Resolve-JPackage
$Out = Join-Path $Root "build\input"
$Dest = Join-Path $Root "dist"
New-Item -ItemType Directory -Force -Path $Out | Out-Null
New-Item -ItemType Directory -Force -Path $Dest | Out-Null

Write-Host "==> Building Maven reactor (offline)"
Push-Location $Root
& $Mvn -o -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
Pop-Location

function Copy-Jar($src) {
    if (-not (Test-Path $src)) { throw "Missing dependency jar: $src" }
    Copy-Item $src $Out -Force
    Write-Host "    + $(Split-Path -Leaf $src)"
}

function Resolve-Jar {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jar.exe")) {
        return "$env:JAVA_HOME\bin\jar.exe"
    }
    return "jar"
}
$jarCmd = Resolve-Jar

Write-Host "==> Setting Main-Class on UI jar"
& $jarCmd ufe "$Root\ui\target\contact-front-ui-$AppVersion.jar" com.contactfront.ui.App
if ($LASTEXITCODE -ne 0) { throw "jar ufe failed" }

Write-Host "==> Assembling jpackage input ($platform)"
Copy-Jar "$Root\ui\target\contact-front-ui-$AppVersion.jar"
Copy-Jar "$Root\engine\target\contact-front-engine-$AppVersion.jar"
Copy-Jar "$Root\mil-sym-java-2.8.2.jar"
Copy-Jar "$M2\org\json\json\20240303\json-20240303.jar"
foreach ($m in @("base", "controls", "graphics", "swing")) {
    Copy-Jar "$M2\org\openjfx\javafx-$m\$FxVersion\javafx-$m-$FxVersion.jar"
    Copy-Jar "$M2\org\openjfx\javafx-$m\$FxVersion\javafx-$m-$FxVersion-$platform.jar"
}

$iconArg = @()
if ($env:OS -eq "Windows_NT" -and (Test-Path (Join-Path $Root "assets\icon.ico"))) {
    $iconArg = @("--icon", (Join-Path $Root "assets\icon.ico"))
} elseif (Test-Path (Join-Path $Root "assets\icon.png")) {
    $iconArg = @("--icon", (Join-Path $Root "assets\icon.png"))
}

# app-image has no installer-specific options (dir-chooser/shortcuts apply to exe/msi/pkg/dmg).
$platformOpts = @()

# Bundle the full JDK as the runtime image. The app jars (including the modular JavaFX
# jars) are placed on the classpath, where JavaFX loads as automatic modules -- the same
# setup verified to launch correctly. This avoids jlink module-resolution issues offline.
if (-not $env:JAVA_HOME) { throw "JAVA_HOME must be set to a JDK 21+ (used as the bundled runtime)." }
$runtimeImage = $env:JAVA_HOME

$appImage = Join-Path $Dest "ContactFront"
if (Test-Path $appImage) { Remove-Item -Recurse -Force $appImage }

Write-Host "==> Packaging app image (jpackage)"
& $jpkg --type app-image `
    --name "ContactFront" `
    --runtime-image $runtimeImage `
    --input $Out `
    --main-jar "contact-front-ui-$AppVersion.jar" `
    --main-class com.contactfront.ui.App `
    --app-version $AppVersion `
    --dest $Dest `
    --java-options "--module-path `$APPDIR --add-modules javafx.controls,javafx.graphics,javafx.swing" `
    @iconArg @platformOpts

if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

$exe = Join-Path $Dest "ContactFront\ContactFront.exe"
if (Test-Path $exe) { Write-Host "==> App image ready: $exe" }
else { Write-Host "==> App image written to $Dest" }
