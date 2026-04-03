module com.tamagotchi.committracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires org.eclipse.jgit;
    requires java.desktop;
    requires java.prefs;
    requires java.logging;
    requires okhttp3;
    requires com.google.gson;
    
    exports com.tamagotchi.committracker;
    exports com.tamagotchi.committracker.ui.widget;
    exports com.tamagotchi.committracker.ui.components;
    exports com.tamagotchi.committracker.git;
    exports com.tamagotchi.committracker.pokemon;
    exports com.tamagotchi.committracker.domain;
    exports com.tamagotchi.committracker.config;
    exports com.tamagotchi.committracker.util;
    exports com.tamagotchi.committracker.github;
}