[CmdletBinding()]
param()

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeDir = Join-Path $projectRoot '.lifeskill-runtime'
$escapedRoot = [regex]::Escape($projectRoot)

function Stop-RecordedProcessTree([string]$Name, [string]$ExpectedCommand) {
    $pidFile = Join-Path $runtimeDir "$Name.pid"
    if (-not (Test-Path $pidFile)) { return }
    $recordedPid = 0
    if (-not [int]::TryParse((Get-Content $pidFile -Raw).Trim(), [ref]$recordedPid)) { return }
    $rootProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $recordedPid" -ErrorAction SilentlyContinue
    # PID 可能被系统复用；只有命令仍与启动器记录的服务一致时才允许终止整棵进程树。
    if ($rootProcess -and $rootProcess.CommandLine -match $ExpectedCommand) {
        $allProcesses = Get-CimInstance Win32_Process
        $tree = [System.Collections.Generic.List[int]]::new()
        $queue = [System.Collections.Generic.Queue[int]]::new()
        $queue.Enqueue($recordedPid)
        while ($queue.Count -gt 0) {
            $parent = $queue.Dequeue()
            foreach ($child in $allProcesses | Where-Object ParentProcessId -eq $parent) {
                $tree.Add($child.ProcessId)
                $queue.Enqueue($child.ProcessId)
            }
        }
        foreach ($processId in @($tree.ToArray() | Sort-Object -Descending) + $recordedPid) {
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        }
        Write-Host "Stopped $Name started by LifeSkill Hub."
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

Stop-RecordedProcessTree 'backend' 'mvnw\.cmd spring-boot:run'
Stop-RecordedProcessTree 'frontend' 'npm run dev'

foreach ($port in 8080, 5173) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine -match $escapedRoot) {
            Stop-Process -Id $connection.OwningProcess -Force
            Write-Host "Stopped LifeSkill Hub process on port $port."
        }
    }
}
