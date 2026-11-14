package com.itmecasey.barelystorage.script;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS server scripts a global BarelyStorage. ModJS is optional, so everything goes through reflection: ModJS offers
 * ExtraBindings.register(BindingProvider), and the provider's install(type, cx, scope) adds globals to every script scope.
 * Without ModJS nothing is loaded and nothing fails.
 */
public final class ScriptBridge {
    private static final Logger LOG = Logger.getLogger("barelystorage");
    private static final StorageApi API = new StorageApi();

    private ScriptBridge() {
    }

    /** Whether the global belongs in a scope of this ModJS script type (the network lives on the server, so not for client scripts). */
    static boolean wanted(Object scriptType) {
        return !String.valueOf(scriptType).equalsIgnoreCase("CLIENT");
    }

    /** Called once from each loader's mod constructor; returns whether the binding was registered. */
    public static boolean install() {
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    if (wanted(args[0])) {
                        put.invoke(null, args[2], "BarelyStorage", javaToJs.invoke(null, API, args[2]));
                    }
                    return null;
                }
                return switch (method.getName()) {
                    case "toString" -> "BarelyStorage binding";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            LOG.info("BarelyStorage: ModJS server scripts can use the BarelyStorage global");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("BarelyStorage: could not add the BarelyStorage global to ModJS: " + e);
            return false;
        }
    }
}
