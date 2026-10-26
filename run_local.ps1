param([int]$Threads = 16, [int]$FirstRun = 1, [int]$LastRun = 50, [int]$InterimEvalAfter = 10,
      [string]$Out = 'runs/2026-10_final', [int]$Gens = 0, [int]$Pop = 0)

$ErrorActionPreference = 'Continue'
$java = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe'
Set-Location $PSScriptRoot
$out = $Out
$sizeArgs = @()
if ($Gens -gt 0) { $sizeArgs = @("$Gens", "$Pop") }
New-Item -ItemType Directory -Force $out | Out-Null
$log = Join-Path $out 'local_log.txt'
$cp = 'bin;lib/*;lib/bots/*'

function Write-Log($msg) {
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $msg"
    Add-Content $log $line
    Write-Host $line
}

Add-Type -Namespace Win32 -Name Power -MemberDefinition '[DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint esFlags);'
$awake = [Win32.Power]::SetThreadExecutionState([uint32]2147483649)
(Get-Process -Id $PID).PriorityClass = 'BelowNormal'

function Invoke-Java([string[]]$javaArgs) {
    & $java @javaArgs 2>&1 | ForEach-Object { Add-Content $log "    $_" }
    return $LASTEXITCODE
}

function Invoke-Eval([string]$label) {
    Write-Log "$label evaluation started (training paused so the timed opponents get full CPU)"
    $code = Invoke-Java @('-Xmx4g', '-Djava.awt.headless=true', '-cp', $cp, 'ai.modular.exp.EvalExperiment', $out, '2', '8')
    if ($code -ne 0) { Write-Log "$label evaluation FAILED (exit $code)"; exit 1 }
    Write-Log "$label evaluation done: $out/summary.md"
}

Write-Log "Started: runs $FirstRun..$LastRun, $Threads threads (PID $PID); keep-awake $(if ($awake -ne 0) { 'on' } else { 'FAILED: set Windows not to sleep' })"
for ($run = $FirstRun; $run -le $LastRun; $run++) {
    foreach ($bot in 'gp', 'score', 'hybrid') {
        $t0 = Get-Date
        $code = Invoke-Java (@('-Xmx2g', '-Djava.awt.headless=true', '-cp', $cp, 'ai.modular.exp.RunExperiment', $bot, "$run", "$run", $out, "$Threads") + $sizeArgs)
        if ($code -ne 0) { Write-Log "FAILED: $bot run $run (exit $code); rerun this script to resume"; exit 1 }
        Write-Log ("{0,-6} run {1,2} done ({2:N0} min)" -f $bot, $run, ((Get-Date) - $t0).TotalMinutes)
    }
    if ($run -eq $InterimEvalAfter -and $LastRun -gt $InterimEvalAfter) { Invoke-Eval "Interim (runs 1-$run)" }
}
Invoke-Eval "Final"
Write-Log "All done."
