package com.resumerocket.resume;

import java.util.List;

public record CompareResponse(TailoredResumeResponse left, TailoredResumeResponse right, List<SectionDiff> diff) {}
