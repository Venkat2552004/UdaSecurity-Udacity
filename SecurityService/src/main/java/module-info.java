module SecurityService {
    requires transitive image.service;
    requires transitive com.miglayout.swing;
    requires java.desktop;
    requires java.prefs;
    requires transitive com.google.gson;
    requires transitive dev.mccue.guava.collect;
    requires transitive dev.mccue.guava.reflect;
    
    opens security.data;
}