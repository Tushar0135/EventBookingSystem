module org.example.eventbookingsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;

    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    opens org.example.eventbookingsystem to javafx.fxml;
    opens org.example.eventbookingsystem.controller to javafx.fxml;
    opens org.example.eventbookingsystem.model to javafx.base;



    exports org.example.eventbookingsystem.controller;
    exports org.example.eventbookingsystem.model;
    exports org.example.eventbookingsystem.utilities;

    exports org.example.eventbookingsystem;
}