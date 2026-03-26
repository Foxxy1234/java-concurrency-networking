module info.kgeorgiy.ja.Davydov {
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.implementor.tools;
    requires java.compiler;

    exports info.kgeorgiy.ja.Davydov.implementor;

    opens info.kgeorgiy.ja.Davydov.implementor to java.compiler;
}
