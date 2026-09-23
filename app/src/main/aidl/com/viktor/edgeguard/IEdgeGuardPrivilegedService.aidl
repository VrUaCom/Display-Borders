package com.viktor.edgeguard;

interface IEdgeGuardPrivilegedService {
    String getDisplaySizeInfo();
    String applyViewport(int widthPx, int heightPx);
    String resetViewport();
    void destroy() = 16777114;
}
