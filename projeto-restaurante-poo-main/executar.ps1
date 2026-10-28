$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$javaCompiler = (Get-Command javac -ErrorAction SilentlyContinue).Source
$javaRuntime = (Get-Command java -ErrorAction SilentlyContinue).Source

if (-not $javaCompiler -or -not $javaRuntime) {
    $jdkCandidates = @()
    if ($env:JAVA_HOME) { $jdkCandidates += $env:JAVA_HOME }
    $ideaRoot = Join-Path $env:ProgramFiles 'JetBrains'
    if (Test-Path -LiteralPath $ideaRoot) {
        $jdkCandidates += Get-ChildItem -LiteralPath $ideaRoot -Directory -Filter 'IntelliJ IDEA*' |
            Sort-Object LastWriteTime -Descending |
            ForEach-Object { Join-Path $_.FullName 'jbr' }
    }
    $javaRoot = Join-Path $env:ProgramFiles 'Java'
    if (Test-Path -LiteralPath $javaRoot) {
        $jdkCandidates += Get-ChildItem -LiteralPath $javaRoot -Directory |
            Where-Object { $_.Name -match 'jdk[-_]?([0-9]+)' -and [int]$Matches[1] -ge 11 } |
            ForEach-Object { $_.FullName }
    }

    foreach ($candidate in $jdkCandidates) {
        $compiler = Join-Path $candidate 'bin\javac.exe'
        $runtime = Join-Path $candidate 'bin\java.exe'
        if ((Test-Path -LiteralPath $compiler) -and (Test-Path -LiteralPath $runtime)) {
            $javaCompiler = $compiler
            $javaRuntime = $runtime
            break
        }
    }
}

if (-not $javaCompiler -or -not $javaRuntime) {
    throw 'JDK 11 ou superior não encontrado. Instale um JDK ou configure JAVA_HOME e o PATH.'
}

$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName }
if (-not $sourceFiles) {
    throw 'Nenhum arquivo Java foi encontrado em src.'
}

$compilerArgs = @('-encoding', 'UTF-8', '-d', 'out') + $sourceFiles
& $javaCompiler @compilerArgs
if ($LASTEXITCODE -ne 0) {
    throw "A compilação Java falhou com código $LASTEXITCODE."
}

& $javaRuntime -cp 'out' br.com.restaurante.main.Principal
