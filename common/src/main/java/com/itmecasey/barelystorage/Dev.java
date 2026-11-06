package com.itmecasey.barelystorage;

import java.lang.reflect.Method;

/**
 * Development switches. The self test, the test energy sink and the screenshot tour live in src/dev folders that only build
 * with {@code -PdevTools}, so release jars never contain them; this class reaches them by name when they are there.
 */
public final class Dev {
    /** True in dev runs (the Gradle run tasks set the property when -PdevTools is given). */
    public static final boolean ENABLED = System.getProperty("barelystorage.dev") != null;

    private Dev() {
    }

    /** Calls a static method of a dev class by name if the class exists in this build; does nothing in a release jar. */
    public static void call(String className, String method, Object... args) {
        if (!ENABLED) {
            return;
        }
        try {
            Class<?> type = Class.forName(className);
            for (Method m : type.getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == args.length) {
                    m.invoke(null, args);
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            // A build without -PdevTools: nothing to do.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Dev hook " + className + "." + method + " failed", e);
        }
    }
}
