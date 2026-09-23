package com.viktor.edgeguard;

import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class EdgeGuardPrivilegedService extends IEdgeGuardPrivilegedService.Stub {

    public EdgeGuardPrivilegedService() {}

    @Override
    public String getDisplaySizeInfo() throws RemoteException {
        return run("wm size");
    }

    @Override
    public String applyViewport(int widthPx, int heightPx) throws RemoteException {
        if (widthPx < 320 || heightPx < 320) {
            throw new IllegalArgumentException("Viewport is too small");
        }
        String result = run("wm size " + widthPx + "x" + heightPx + " && wm scaling off");
        return result + "\n" + run("wm size");
    }

    @Override
    public String resetViewport() throws RemoteException {
        String result = run("wm size reset && wm scaling auto");
        return result + "\n" + run("wm size");
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    private static String run(String command) throws RemoteException {
        try {
            Process process = new ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            int code = process.waitFor();
            if (code != 0) {
                throw new RemoteException("Command failed (" + code + "): " + out);
            }
            return out.toString().trim();
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("Shell error: " + e.getMessage());
        }
    }
}
