module com.linearity.pcmusicplayer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires javafx.media;
    requires java.desktop;

    opens com.linearity.pcmusicplayer to javafx.fxml;
    exports com.linearity.pcmusicplayer;
}