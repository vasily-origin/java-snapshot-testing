package au.com.origin.snapshots;


import java.lang.reflect.Method;
import java.util.stream.Stream;

public class TestSnapshotConfig implements SnapshotConfig {

    protected boolean hasTestAnnotation(Method method) {
        return method.isAnnotationPresent(org.junit.jupiter.params.ParameterizedTest.class)
                || method.isAnnotationPresent(org.junit.jupiter.api.Test.class)
                || method.isAnnotationPresent(org.junit.jupiter.api.BeforeAll.class);
    }


    private StackTraceElement findStackTraceElement() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int elementsToSkip = 1; // Start after stackTrace
        while (stackTraceElements[elementsToSkip].getClassName().contains(".Snapshot")) {
            elementsToSkip++;
        }

        return Stream.of(stackTraceElements)
            .skip(elementsToSkip)
            .filter(
                stackTraceElement ->
                    hasTestAnnotation(
                        ReflectionUtilities.getMethod(
                            getClassForName(stackTraceElement.getClassName()),
                            stackTraceElement.getMethodName())))
            .findFirst()
            .orElseThrow(() -> new SnapshotMatchException(
                                    "Could not locate a method with one of supported test annotations"));
    }

    private Class<?> getClassForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Class<?> getTestClass() {
        try {
            return Class.forName(findStackTraceElement().getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable resolve class name", e);
        }
    }

    @Override
    public Method getTestMethod(Class<?> testClass) {
        StackTraceElement stackTraceElement = findStackTraceElement();
        return ReflectionUtilities.getMethod(testClass, stackTraceElement.getMethodName());
    }
}
