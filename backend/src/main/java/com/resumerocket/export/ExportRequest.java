package com.resumerocket.export;

import jakarta.validation.constraints.NotNull;

public record ExportRequest(@NotNull Long templateId, @NotNull ExportFormat format) {}
