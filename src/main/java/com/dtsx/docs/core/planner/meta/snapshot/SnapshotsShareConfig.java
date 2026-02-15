package com.dtsx.docs.core.planner.meta.snapshot;

import com.dtsx.docs.core.planner.meta.PerLanguageToggle;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;

import java.util.Map;

public class SnapshotsShareConfig extends PerLanguageToggle<Boolean> {
    public SnapshotsShareConfig(Map<ClientLanguage, Boolean> languages) {
        super(languages);
    }

    public boolean isShared(ClientLanguage language) {
        return languages.getOrDefault(language, true);
    }
}
