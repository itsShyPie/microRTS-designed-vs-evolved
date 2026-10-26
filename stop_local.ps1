$targets = @(Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'run_local\.ps1' })
$targets += @(Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -match 'ai\.modular\.exp\.' })
if (-not $targets) { Write-Host 'Nothing running.'; exit 0 }
foreach ($p in $targets) {
    Write-Host "Stopping $($p.Name) (PID $($p.ProcessId))"
    Invoke-CimMethod -InputObject $p -MethodName Terminate | Out-Null
}
