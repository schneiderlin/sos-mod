package your.mod;

import clojure.java.api.Clojure;
import clojure.lang.*;
import init.Main;

import java.io.IOException;
import java.util.HashMap;

public class MyMain {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = MyMain.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        RT.load("clojure/core");
        RT.load("clojure/core/reducers");

        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.core.reducers"));

        Var warnVar = RT.var("clojure.core", "*warn-on-reflection*");

        // Create a Clojure map
        Object bindings = PersistentHashMap.create(warnVar, false);

        Var.pushThreadBindings((Associative) bindings);

        try {
            Main.main(args);
        } finally {
            Var.popThreadBindings();
        }
    }
}
