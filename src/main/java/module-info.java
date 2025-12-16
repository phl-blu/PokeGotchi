module com.tamagotchi.committracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.jgit;
    requires java.desktop;
    requires java.prefs;
    
    exports com.tamagotchi.committracker;
    exports com.tamagotchi.committracker.ui.widget;
    exports com.tamagotchi.committracker.ui.components;
    exports com.tamagotchi.committracker.git;
    exports com.tamagotchi.committracker.pokemon;
    exports com.tamagotchi.committracker.domain;
    exports com.tamagotchi.committracker.config;
    exports com.tamagotchi.committracker.util;
}