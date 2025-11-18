module dev.assignment {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive langchain4j.open.ai; // must add langchain4j references
    requires transitive langchain4j.core;
    requires transitive langchain4j;
    requires transitive org.apache.logging.log4j; // must add log4j references
    requires transitive org.slf4j; // must add slf4j
    requires transitive java.net.http; // needed if HttpTimeoutException occurs
    requires com.fasterxml.jackson.core; // needed if assistant is null

    // Apache POI for document text extraction
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.scratchpad;

    // Apache PDFBox for PDF text extraction
    requires org.apache.pdfbox;

    // SQLite JDBC for database storage
    requires java.sql;

    // dotenv-java for loading environment variables
    requires io.github.cdimascio.dotenv.java;

    // Open packages to javafx.fxml for reflection-based access
    opens dev.assignment to javafx.fxml;
    opens dev.assignment.controller to javafx.fxml;

    // Export packages for internal module access
    exports dev.assignment;
    exports dev.assignment.controller;
    exports dev.assignment.handler;
    exports dev.assignment.model;
    exports dev.assignment.service;
    exports dev.assignment.view;
    exports dev.assignment.util;
}
