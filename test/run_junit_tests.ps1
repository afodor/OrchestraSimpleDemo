$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$buildDir = Join-Path $scriptDir "build"
$classesDir = Join-Path $buildDir "classes"
$libDir = Join-Path $scriptDir "lib"
$junitJar = Join-Path $libDir "junit-platform-console-standalone-1.10.2.jar"
$junitUrl = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $libDir | Out-Null

if (-not (Test-Path $junitJar)) {
    Invoke-WebRequest -Uri $junitUrl -OutFile $junitJar
}

$javaSources = Get-ChildItem -Path (Join-Path $scriptDir "java") -Filter *.java -Recurse | ForEach-Object { $_.FullName }
if (-not $javaSources) {
    throw "No Java test sources were found under $scriptDir\java"
}

Push-Location $projectRoot
try {
    javac -cp $junitJar -d $classesDir $javaSources
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed with exit code $LASTEXITCODE"
    }

    java -jar $junitJar execute --class-path $classesDir --scan-class-path
    if ($LASTEXITCODE -ne 0) {
        throw "JUnit test execution failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
