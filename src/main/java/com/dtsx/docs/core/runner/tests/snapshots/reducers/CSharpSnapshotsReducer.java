package com.dtsx.docs.core.runner.tests.snapshots.reducers;

import com.dtsx.docs.core.runner.tests.snapshots.verifier.Snapshot;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public enum CSharpSnapshotsReducer implements SnapshotsReducer {
    INSTANCE;

    @Override
    public Snapshot reduceSnapshots(Map<Snapshot, Set<Path>> snapshots) throws SnapshotReductionException {
        if (snapshots.size() == 1) {
            return snapshots.keySet().iterator().next();
        }
        
        if (snapshots.size() == 2) {
            val iter = snapshots.entrySet().iterator();
            val e1 = iter.next();
            val e2 = iter.next();
            
            val e1IsUntyped = e1.getValue().stream().anyMatch(p -> p.toString().contains("untyped"));
            
            val typedSnapshot = (e1IsUntyped) ? e2.getKey() : e1.getKey();
            val untypedSnapshot = (e1IsUntyped) ? e1.getKey() : e2.getKey();

            val typedParts = typedSnapshot.parts();
            val untypedParts = untypedSnapshot.parts();
            
            if (typedParts.size() != untypedParts.size()) {
                throw new SnapshotReductionException();
            }
            
            for (int i = 0; i < typedParts.size(); i++) {
                val typedPart = typedParts.get(i);
                val untypedPart = untypedParts.get(i);
                
                if (!typedPart.name().equals(untypedPart.name())) {
                    throw new SnapshotReductionException();
                }

                if (typedPart.name().endsWith("::jsonify")) {
                    val typedJson = JacksonUtils.parseJson(typedPart.content(), Object.class);
                    val untypedJson = JacksonUtils.parseJson(untypedPart.content(), Object.class);

                    if (!isSubset(typedJson, untypedJson)) {
                        throw new SnapshotReductionException();
                    }
                } else {
                    if (!typedPart.content().equals(untypedPart.content())) {
                        throw new SnapshotReductionException();
                    }
                }
            }
            
            return untypedSnapshot;
        }
        
        throw new SnapshotReductionException();
    }
    
    private boolean isSubset(Object typed, Object untyped) {
        if (typed == null) return true;
        if (untyped == null) return false;

        if (typed instanceof Map<?, ?> tm && untyped instanceof Map<?, ?> um) {
            for (val e : tm.entrySet()) {
                val typedKey = e.getKey();

                val match = um.entrySet().stream()
                    .filter(ue -> keysMatch(typedKey, ue.getKey()))
                    .findFirst();
                
                if (match.isEmpty() || !isSubset(e.getValue(), match.get().getValue())) {
                    return false;
                }
            }
            return true;
        }
        
        if (typed instanceof Iterable<?> ti && untyped instanceof Iterable<?> ui) {
            val tit = ti.iterator();
            val uit = ui.iterator();
            while (tit.hasNext() && uit.hasNext()) {
                if (!isSubset(tit.next(), uit.next())) return false;
            }
            return !tit.hasNext() && !uit.hasNext();
        }
        
        return typed.equals(untyped);
    }

    private boolean keysMatch(Object key1, Object key2) {
        if (key1 instanceof String s1 && key2 instanceof String s2) {
            return normalizeStr(s1).equals(normalizeStr(s2));
        }
        return key1 != null && key1.equals(key2);
    }
    
    private String normalizeStr(String s) {
        return s.toLowerCase().replace("_", "");
    }
}
