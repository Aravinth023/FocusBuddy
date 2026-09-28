import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * High-performance Windows foreground application monitor and safe process terminator.
 *
 * Uses a single persistent background PowerShell process communicating via standard I/O
 * with pre-compiled native user32.dll P/Invoke types. Each check takes ~1ms without
 * spawning child processes or recompiling C# on every tick.
 *
 * Guaranteed never to freeze the Swing Event Dispatch Thread.
 */
public class ProcessMonitor {

    public interface ProcessMonitorListener {
        /**
         * @param foregroundProcessName normalized name of the foreground app (e.g. "chrome.exe"),
         *                              or null if unknown/none.
         * @param runningProcessNames   set of running process names.
         */
        void onTick(String foregroundProcessName, Set<String> runningProcessNames);
    }

    public static class TerminationOutcome {
        public final boolean success;
        public final String message;

        public TerminationOutcome(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final boolean isWindows;
    private ScheduledExecutorService scheduler;
    private ProcessMonitorListener listener;
    private volatile int intervalSeconds = 1;
    private volatile boolean running = false;

    // Persistent PowerShell process handle
    private Process psProcess;
    private BufferedReader psReader;
    private BufferedWriter psWriter;
    private volatile boolean psReady = false;
    private final Object psLock = new Object();

    public ProcessMonitor() {
        this.isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public void setListener(ProcessMonitorListener listener) {
        this.listener = listener;
    }

    public void setIntervalSeconds(int seconds) {
        this.intervalSeconds = Math.max(1, seconds);
        if (running) {
            stop();
            start();
        }
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        if (isWindows) {
            initPowerShellAsync();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FocusBuddy-ProcessMonitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::scanSafely, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        shutdownPowerShell();
    }

    private void scanSafely() {
        try {
            scan();
        } catch (Exception e) {
            Utils.debug("ProcessMonitor", "Scan error: " + e.getMessage());
        }
    }

    private void scan() {
        String foreground = isWindows ? getForegroundProcessName() : null;
        if (listener != null) {
            listener.onTick(foreground, Collections.emptySet());
        }
    }

    /**
     * Initializes the background PowerShell bridge asynchronously so UI startup is instant.
     */
    private void initPowerShellAsync() {
        Thread initThread = new Thread(() -> {
            synchronized (psLock) {
                if (!running) return;
                startPowerShellProcess();
            }
        }, "FocusBuddy-PS-Init");
        initThread.setDaemon(true);
        initThread.start();
    }

    private void startPowerShellProcess() {
        try {
            String script =
                "$code = @'\n" +
                "using System;\n" +
                "using System.Runtime.InteropServices;\n" +
                "public class FocusBuddyNative {\n" +
                "    [DllImport(\"user32.dll\")]\n" +
                "    public static extern IntPtr GetForegroundWindow();\n" +
                "    [DllImport(\"user32.dll\")]\n" +
                "    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);\n" +
                "}\n" +
                "'@\n" +
                "Add-Type -TypeDefinition $code\n" +
                "Write-Output \"READY\"\n" +
                "[Console]::Out.Flush()\n" +
                "while ($line = [Console]::In.ReadLine()) {\n" +
                "    if ($line -eq 'QUIT') { break }\n" +
                "    try {\n" +
                "        $hwnd = [FocusBuddyNative]::GetForegroundWindow()\n" +
                "        [uint32]$procId = 0\n" +
                "        [FocusBuddyNative]::GetWindowThreadProcessId($hwnd, [ref]$procId) | Out-Null\n" +
                "        if ($procId -gt 0) {\n" +
                "            $p = Get-Process -Id $procId -ErrorAction SilentlyContinue\n" +
                "            if ($p) {\n" +
                "                Write-Output \"FOCUSBUDDY_ACTIVE:$($p.ProcessName).exe:$procId\"\n" +
                "            } else {\n" +
                "                Write-Output \"FOCUSBUDDY_ACTIVE:UNKNOWN:$procId\"\n" +
                "            }\n" +
                "        } else {\n" +
                "            Write-Output \"FOCUSBUDDY_ACTIVE:NONE:0\"\n" +
                "        }\n" +
                "    } catch {\n" +
                "        Write-Output \"FOCUSBUDDY_ACTIVE:ERROR:0\"\n" +
                "    }\n" +
                "    [Console]::Out.Flush()\n" +
                "}\n";

            byte[] bytes = script.getBytes(StandardCharsets.UTF_16LE);
            String encoded = Base64.getEncoder().encodeToString(bytes);

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded
            );
            pb.redirectErrorStream(true);
            psProcess = pb.start();

            psReader = new BufferedReader(new InputStreamReader(psProcess.getInputStream(), StandardCharsets.UTF_8));
            psWriter = new BufferedWriter(new OutputStreamWriter(psProcess.getOutputStream(), StandardCharsets.UTF_8));

            // Wait for READY signal
            String line;
            while ((line = psReader.readLine()) != null) {
                if ("READY".equals(line.trim())) {
                    psReady = true;
                    Utils.debug("ProcessMonitor", "PowerShell native bridge is READY.");
                    break;
                }
            }
        } catch (Exception e) {
            Utils.debug("ProcessMonitor", "Failed to start PowerShell bridge: " + e.getMessage());
            psReady = false;
        }
    }

    private void shutdownPowerShell() {
        synchronized (psLock) {
            psReady = false;
            if (psWriter != null) {
                try {
                    psWriter.write("QUIT\n");
                    psWriter.flush();
                } catch (Exception ignored) {}
            }
            if (psProcess != null) {
                try {
                    boolean finished = psProcess.waitFor(1, TimeUnit.SECONDS);
                    if (!finished) {
                        psProcess.destroyForcibly();
                    }
                } catch (Exception e) {
                    psProcess.destroyForcibly();
                }
                psProcess = null;
            }
            psReader = null;
            psWriter = null;
        }
    }

    /**
     * Sends a query to the running PowerShell process and reads the foreground app.
     * Completes in ~1ms.
     */
    private String getForegroundProcessName() {
        synchronized (psLock) {
            if (!psReady || psProcess == null || !psProcess.isAlive()) {
                // Try restarting if it died unexpectedly
                if (running) {
                    Utils.debug("ProcessMonitor", "PowerShell bridge died, restarting...");
                    shutdownPowerShell();
                    startPowerShellProcess();
                }
                if (!psReady) {
                    return null;
                }
            }

            try {
                psWriter.write("TICK\n");
                psWriter.flush();

                String response = psReader.readLine();
                if (response == null) {
                    psReady = false;
                    return null;
                }

                if (response.startsWith("FOCUSBUDDY_ACTIVE:")) {
                    String[] parts = response.substring("FOCUSBUDDY_ACTIVE:".length()).split(":", 2);
                    String procName = parts[0].trim();
                    String pidStr = parts.length > 1 ? parts[1].trim() : "0";

                    Utils.debug("ProcessMonitor", "Foreground PID: " + pidStr);
                    Utils.debug("ProcessMonitor", "Process: " + procName);

                    if ("NONE.exe".equalsIgnoreCase(procName) || "NONE".equalsIgnoreCase(procName)
                            || "UNKNOWN".equalsIgnoreCase(procName) || "ERROR".equalsIgnoreCase(procName)) {
                        return null;
                    }

                    return AppInfo.normalizeProcessName(procName);
                }
            } catch (Exception e) {
                Utils.debug("ProcessMonitor", "Error querying foreground app: " + e.getMessage());
                psReady = false;
            }
        }
        return null;
    }

    /**
     * Safely terminates all instances of a target application process.
     *
     * Shields critical system processes and FocusBuddy itself.
     */
    public TerminationOutcome terminateProcessByName(String processName, AppManager appManager) {
        if (processName == null || processName.trim().isEmpty()) {
            return new TerminationOutcome(false, "Empty process name.");
        }

        String normalized = AppInfo.normalizeProcessName(processName);

        // 1. Never terminate protected processes
        if (appManager.isProtected(normalized)) {
            return new TerminationOutcome(false, "Refusing to terminate protected system process: " + normalized);
        }

        // 2. Shield FocusBuddy itself
        long currentPid = ProcessHandle.current().pid();
        if (normalized.contains("java") || normalized.contains("focusbuddy")) {
            return new TerminationOutcome(false, "FocusBuddy cannot terminate itself.");
        }

        // 3. Verify it is a valid monitored application
        if (appManager.findByProcessName(normalized) == null) {
            return new TerminationOutcome(false, "Process is not a monitored application: " + normalized);
        }

        Utils.debug("ProcessMonitor", "Attempting safe termination of: " + normalized);

        if (isWindows) {
            return terminateWindowsProcess(normalized);
        } else {
            return terminateGenericProcess(normalized);
        }
    }

    private TerminationOutcome terminateWindowsProcess(String normalized) {
        try {
            // First attempt: graceful close via taskkill
            ProcessBuilder pbGraceful = new ProcessBuilder("taskkill", "/IM", normalized);
            Process p1 = pbGraceful.start();
            boolean done1 = p1.waitFor(2, TimeUnit.SECONDS);

            if (done1 && p1.exitValue() == 0) {
                return new TerminationOutcome(true, "Application closed gracefully.");
            }

            // If not closed or timed out, force terminate
            ProcessBuilder pbForce = new ProcessBuilder("taskkill", "/F", "/IM", normalized);
            Process p2 = pbForce.start();
            boolean done2 = p2.waitFor(3, TimeUnit.SECONDS);

            if (done2) {
                int exitCode = p2.exitValue();
                if (exitCode == 0) {
                    return new TerminationOutcome(true, "Application closed.");
                } else if (exitCode == 128) {
                    // ERROR_MOD_NOT_FOUND or Process not found
                    return new TerminationOutcome(true, "Process was already closed.");
                } else {
                    return new TerminationOutcome(false, "Access denied or process locked (exit code " + exitCode + ").");
                }
            } else {
                p2.destroyForcibly();
                return new TerminationOutcome(false, "Termination timed out.");
            }
        } catch (Exception e) {
            return new TerminationOutcome(false, "Error closing application: " + e.getMessage());
        }
    }

    private TerminationOutcome terminateGenericProcess(String normalized) {
        boolean anyTerminated = false;
        try {
            List<ProcessHandle> targets = ProcessHandle.allProcesses()
                    .filter(ph -> ph.info().command()
                            .map(cmd -> AppInfo.normalizeProcessName(cmd).equals(normalized))
                            .orElse(false))
                    .toList();

            if (targets.isEmpty()) {
                return new TerminationOutcome(true, "Process was already closed.");
            }

            for (ProcessHandle ph : targets) {
                ph.destroy();
                try {
                    ph.onExit().get(1, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    ph.destroyForcibly();
                }
                anyTerminated = true;
            }
        } catch (Exception e) {
            return new TerminationOutcome(false, "Failed to terminate: " + e.getMessage());
        }
        return anyTerminated ? new TerminationOutcome(true, "Application closed.")
                             : new TerminationOutcome(false, "Could not terminate process.");
    }
}
