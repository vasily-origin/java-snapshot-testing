package au.com.origin.snapshots;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.util.Arrays.isNullOrEmpty;

@Slf4j
@RequiredArgsConstructor
public class SnapshotVerifier {

    private final Class testClass;
    private final SnapshotFile snapshotFile;
    private final Function<Object, String> serializer;
    private final SnapshotConfig config;

    private final List<Snapshot> calledSnapshots = new ArrayList<>();

    @Setter
    private Method testMethod = null;

    public Snapshot expectCondition(Object firstObject, Object... others) {
        Object[] objects = mergeObjects(firstObject, others);
        Method resolvedTestMethod = testMethod == null ? config.getTestMethod(testClass) : testMethod;
        Snapshot snapshot =
                new Snapshot(config, snapshotFile, testClass, resolvedTestMethod, serializer, objects);
        validateExpectCall(snapshot);
        calledSnapshots.add(snapshot);
        return snapshot;
    }

    public void validateSnapshots() {
        Set<String> rawSnapshots = snapshotFile.getRawSnapshots();
        List<String> snapshotNames =
                calledSnapshots.stream().map(Snapshot::getSnapshotName).collect(Collectors.toList());
        List<String> unusedRawSnapshots = new ArrayList<>();

        for (String rawSnapshot : rawSnapshots) {
            boolean foundSnapshot = false;
            for (String snapshotName : snapshotNames) {
                if (rawSnapshot.contains(snapshotName)) {
                    foundSnapshot = true;
                }
            }
            if (!foundSnapshot) {
                unusedRawSnapshots.add(rawSnapshot);
            }
        }
        if (unusedRawSnapshots.size() > 0) {
            log.warn(
                    "All unused Snapshots: "
                            + StringUtils.join(unusedRawSnapshots, "\n")
                            + ". Consider deleting the snapshot file to recreate it!");
        }
    }

    private void validateExpectCall(Snapshot snapshot) {
        for (Snapshot eachSnapshot : calledSnapshots) {
            if (eachSnapshot.getSnapshotName().equals(snapshot.getSnapshotName())) {
                throw new SnapshotMatchException(
                        "You can only call 'expect' once per test method. Try using array of arguments on a single 'expect' call");
            }
        }
    }

    private Object[] mergeObjects(Object firstObject, Object[] others) {
        Object[] objects = new Object[1];
        objects[0] = firstObject;
        if (!isNullOrEmpty(others)) {
            objects = ArrayUtils.addAll(objects, others);
        }
        return objects;
    }
}
