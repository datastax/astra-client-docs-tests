package com.dtsx.docs.builder.meta.reps;

import lombok.NonNull;

import static com.dtsx.docs.builder.meta.reps.BaseMetaYmlRep.TestType.COMPILES;

public record CompilesTestMetaYmlRep(
    @NonNull String $schema,
    @NonNull TestBlock test
) implements BaseMetaYmlRep {
    @Override
    public TestType expectTestType() {
        return COMPILES;
    }
}
