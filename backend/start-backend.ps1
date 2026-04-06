$ErrorActionPreference = "Stop"

$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $backendDir
$envFile = Join-Path $backendDir ".env"
$bundledMaven = Join-Path $repoRoot ".tools\apache-maven-3.9.9\bin\mvn.cmd"

function Get-ValidJavaHome {
    function Get-JavaMajorVersion {
        param(
            [Parameter(Mandatory = $true)]
            [string]$JavaHome
        )

        $releaseFile = Join-Path $JavaHome "release"
        if (-not (Test-Path $releaseFile)) {
            return $null
        }

        $versionLine = Get-Content $releaseFile | Where-Object { $_ -match '^JAVA_VERSION=' } | Select-Object -First 1
        if (-not $versionLine) {
            return $null
        }

        $version = ($versionLine -replace '^JAVA_VERSION=', '').Trim('"')
        if ($version.StartsWith("1.")) {
            return [int]($version.Split(".")[1])
        }

        return [int]($version.Split(".")[0])
    }

    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        $javaMajorVersion = Get-JavaMajorVersion -JavaHome $env:JAVA_HOME
        if ($javaMajorVersion -ge 21) {
            return $env:JAVA_HOME
        }
    }

    $preferredPaths = @(
        "C:\Program Files\Java\latest",
        "C:\Program Files\Java\jdk-25.0.2"
    )

    foreach ($path in $preferredPaths) {
        if ((Test-Path (Join-Path $path "bin\java.exe")) -and ((Get-JavaMajorVersion -JavaHome $path) -ge 21)) {
            return $path
        }
    }

    $installedJdks = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -like "jdk-*" -and
            (Test-Path (Join-Path $_.FullName "bin\java.exe")) -and
            ((Get-JavaMajorVersion -JavaHome $_.FullName) -ge 21)
        } |
        Sort-Object Name -Descending

    if ($installedJdks) {
        return $installedJdks[0].FullName
    }

    throw "No usable JDK was found. Install JDK 21+ and try again."
}

function Import-DotEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        Write-Warning "No .env file found at $Path. Continuing with current environment."
        return
    }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $separatorIndex = $line.IndexOf("=")
        if ($separatorIndex -lt 1) {
            return
        }

        $name = $line.Substring(0, $separatorIndex).Trim()
        $value = $line.Substring($separatorIndex + 1)

        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path ("Env:" + $name) -Value $value
    }
}

$javaHome = Get-ValidJavaHome
$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome "bin") + ";" + $env:Path

Import-DotEnvFile -Path $envFile

if (Test-Path $bundledMaven) {
    $mavenCommand = $bundledMaven
    $env:MAVEN_HOME = Split-Path (Split-Path $bundledMaven -Parent) -Parent
}
else {
    $mavenCommandInfo = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
    if ($mavenCommandInfo) {
        $mavenCommand = $mavenCommandInfo.Source
    }

    if (-not $mavenCommand) {
        $mavenCommandInfo = Get-Command "mvn" -ErrorAction SilentlyContinue
        if ($mavenCommandInfo) {
            $mavenCommand = $mavenCommandInfo.Source
        }
    }

    if (-not $mavenCommand) {
        throw "Maven was not found. Install Maven or add it to PATH."
    }
}

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
Write-Host "Using Maven=$mavenCommand"
Write-Host "Starting backend from $backendDir"

Push-Location $backendDir
try {
    & $mavenCommand spring-boot:run
}
finally {
    Pop-Location
}
