package com.linearity.pcmusicplayer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Popup;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * View/Controller in the Model-View-Controller design pattern for MusicPlayer.
 * Deals with GUI construction, event handling, and updating the GUI.
 *
 * @author connoryork (cxy1054@rit.edu)
 * @author mbroman (broman334@tamu.edu)
 */
public class MusicPlayerGUI extends Application implements Observer {

    public static final Random randomGenerator = new SecureRandom();
    public static <T> void shuffleArray(T[] array){
        int n = array.length;
        // Loop over array.
        
        for (int i = 0; i < array.length; i++) {
            // Get a random index of the array past the current index.
            // ... The argument is an exclusive bound.
            //     It will not go past the array send.
            int randomValue = i + randomGenerator.nextInt(n - i);
            // Swap the random element with the present element.
            T randomElement = array[randomValue];
            array[randomValue] = array[i];
            array[i] = randomElement;
        }
    }
    public static void shuffleArray(int[] array){
        int n = array.length;
        // Loop over array.
        for (int i = 0; i < array.length; i++) {
            // Get a random index of the array past the current index.
            // ... The argument is an exclusive bound.
            //     It will not go past the array send.
            int randomValue = i + randomGenerator.nextInt(n - i);
            // Swap the random element with the present element.
            int randomElement = array[randomValue];
            array[randomValue] = array[i];
            array[i] = randomElement;
        }
    }
    private static void executeFile(File f, List<File> Songlist){
        executeFile(f,Songlist,new HashSet<>());
    }
    private static void executeFile(File f, List<File> Songlist, Set<String> Folders) {
        if (f.isDirectory()){
            if (Folders.contains(f.getAbsolutePath())){
                return;
            }
            Folders.add(f.getAbsolutePath());
            File[] files = f.listFiles();
            if (files == null){return;}
            for (File f0:files){
                executeFile(f0,Songlist,Folders);
            }
            return;
        }
        String fileAbs = f.getAbsolutePath();
        String end = fileAbs.toLowerCase();
        String[] arr = end.split("\\.");
        end = arr[arr.length-1];
        if (end.equals("mp3")
                || end.equals("wav")) {//I don't want to check it.
            Songlist.add(f);
        }
        else if(end.equals("musiclist") && f.canRead()){
            try {
                FileInputStream fileInputStream = new FileInputStream(f);
                byte[] fileBytes = new byte[(int) f.length()];
                fileInputStream.read(fileBytes);
                fileInputStream.close();
                String fileStr = new String(fileBytes, StandardCharsets.UTF_8);
                for (String str:fileStr.split("\n")){
//                                str = str.replace("\n","\\\n");
                    if (str.isEmpty()){continue;}
                    if (str.endsWith("\r")){
                        str = str.substring(0,str.length() - 1);
                    }
                    File file1 = new File(str);
                    if (file1.exists()) {
                        //and I'll always check it.
                        String str1 = str.toLowerCase();
                        if (str1.endsWith(".mp3") || str1.endsWith(".wav")){
                            Songlist.add(file1);
                        }
                        else if (file1.isDirectory() || str1.endsWith(".musiclist")){
                            executeFile(file1,Songlist,Folders);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        Log.d("linearity", String.valueOf(Songlist.size()));
    }


    /** CONSTANTS FOR GUI */
    private static final int DEFAULT_PADDING = 5;
    private static final int DEFAULT_SPACING = 10;
    private static final int DEFAULT_SLIDER_HEIGHT = 180;
    private static final double DEFAULT_UPDATER_DURATION = 2.0;

    /** Model for easy access */
    private MusicPlayerModel model;

    /** Play/Pause button for easy access */
    private Button play;
    /** Volume Slider for easy access */
    private Slider volumeSlider;
    /** Song Slider for easy access */
    private Slider songSlider;
    /** Stage for easy access */
    private Stage stage;

    /**
     * Launches the GUI.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes the model and sets the GUI as an observer.
     */
    @Override
    public void init() {
        this.model = new MusicPlayerModel();
        this.model.addObserver(this);
    }

    /**
     * JavaFX start method. Builds and displays the GUI.
     *
     * @param primaryStage stage to build GUI on
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        Scene s = new Scene(buildRoot());
//        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.getIcons().add(new Image("musicnotelarge.png"));
        primaryStage.setTitle("No Song Selected ~ MusicPlayer");
        primaryStage.setScene(s);
        primaryStage.setResizable(true);
        primaryStage.setAlwaysOnTop(false);

        /* Starts a TimeLine that automatically updates the gui every second.
        This allows for the song slider to move with the song's position */
        KeyFrame updater = new KeyFrame(Duration.seconds(DEFAULT_UPDATER_DURATION), event -> notifytheGUI());
        Timeline t = new Timeline(updater);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        primaryStage.show();

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(.60 * screen.getWidth());
        primaryStage.setY(screen.getHeight() - primaryStage.getHeight());


        {
            List<File> actuallyPlaylist = new ArrayList<>();
            File defaultList = new File("all.musiclist");
            if (defaultList.exists() && defaultList.isFile()) {
                executeFile(defaultList,actuallyPlaylist);
                File[] files = actuallyPlaylist.toArray(new File[0]);
                shuffleArray(files);
                int counter = 0;
                for (File f:files){
                    System.out.printf(counter+".");
                    System.out.println(f.getName());
                    counter += 1;
                }
                this.model.setPlaylist(List.of(files));
                loadPlaylistSong();
            }
        }
    }

    /*******************************************************
    *                                                      *
    *  HELPER METHODS FOR CONSTRUCTING GUI                 *
    *                                                      *
    *******************************************************/

    /**
     * Builds the root node for the GUI.
     *
     * @return BorderPane Node
     */
    private BorderPane buildRoot() {
        BorderPane bp = new BorderPane();
        bp.setPrefSize(300, 80);
        bp.setCenter(buildCenter());
        bp.setRight(buildVolumeSlider());
        bp.setTop(buildMenuBar());
        bp.setBottom(buildSongSlider());
        bp.setLeft(buildSongList());
        return bp;
    }
    private ListView<SongBean> buildSongList(){
        ListView<SongBean> listView = new ListView<>();
        listView.setPrefSize(300, 80);
        listView.setItems(this.model.getPlaylist());
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2){
                SongBean selected = listView.getSelectionModel().getSelectedItem();
                int index = selected.index();
                this.model.loadSpecificSong(index);
                boolean wasRunning = false;
                if (this.model.hasClip() && this.model.isRunning()) {
                    this.model.stop();
                    wasRunning = true;
                }
                loadSongTitleAndVolume(selected, wasRunning);
                this.model.start();
            }
        });
        return listView;
    }

    /**
     * Builds HBox and corresponding buttons.
     *
     * @return center buttons in a HBox
     */
    private HBox buildCenter() {
        HBox box = new HBox();
        box.setSpacing(DEFAULT_SPACING);
        box.setPadding(new Insets(DEFAULT_PADDING,DEFAULT_PADDING,0,DEFAULT_PADDING));
        box.setAlignment(Pos.CENTER);
        box.getChildren().add(buildRewind());
        box.getChildren().add(buildPlayPause());
        box.getChildren().add(buildNext());
        return box;
    }

    /**
     * Builds rewind button, which restarts the current song.
     *
     * @return rewind Button
     */
    private Button buildRewind() {
        Button rewind = new Button();
        setImage(rewind, "rewind.png");
        rewind.setOnAction(e -> {
            if (this.model.hasPlaylist() && this.model.getClipCurrentValue() == 0)
                this.loadPrevPlaylistSong();
            else
                this.model.rewindToStart();
        });
        return rewind;
    }

    /**
     * Builds play button, which pauses and plays the song.
     *
     * @return play Button
     */
    private Button buildPlayPause() {
        Button play = new Button();
        setImage(play, "play.png");
        play.setOnAction(e -> {
            if (this.model.hasClip()) {
                if (this.model.atEnd()) { // song ended but play button was pressed
                    this.model.setSongPosition(0);
                    this.songSlider.setValue(0);
                    setImage(play, "pause.png");
                    this.model.start();
                } else if (!this.model.isRunning()) { // paused
                    setImage(play, "pause.png");
                    this.model.start();
                }else if (this.model.getClipCurrentValue() == this.model.getClipLength()) {
                    this.loadPlaylistSong();
                }else { // song was playing
                    setImage(play, "play.png");
                    this.model.stop();
                }
            } else {
                setImage(play, "play.png");
            }
        });
        this.play = play;
        return play;
    }

    /**
     * Builds next button, which skips the current song and plays the next song in the playlist.
     *
     * @return next Button
     */
    private Button buildNext() {
        Button next = new Button();
        setImage(next, "fastforward.png");
        next.setOnAction(e -> {
            if (this.model.hasPlaylist()) {
                this.loadPlaylistSong();
            }
        });
        return next;
    }

    /**
     * Sets the image of the Button.
     *
     * @param b Button object
     * @param filename filename of image
     */
    private void setImage(ButtonBase b, String filename) {
        Image image = new Image(getClass().getClassLoader().getResourceAsStream(filename));
        b.setGraphic(new ImageView(image));
    }

    /**
     * Builds a slider that controls the volume of the GUI.
     *
     * @return Slider which controls volume
     */
    private Slider buildVolumeSlider() {
        Slider slider = new Slider(0, 0, 0);
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.setPadding(new Insets(DEFAULT_PADDING,DEFAULT_PADDING,0,DEFAULT_PADDING));
        slider.setMaxHeight(DEFAULT_SLIDER_HEIGHT);
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            setCurrentVolume(newValue.doubleValue());
            this.model.volumeChange(newValue.doubleValue());
        });
        this.volumeSlider = slider;
        return slider;
    }

    /**
     * Builds the menu bar, which allows picking of songs.
     *
     * @return MenuBar object to add to the stage
     */
    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu menuChoose = new Menu("Choose...");
        // create the song chooser
        MenuItem songItem = new MenuItem("Song", new ImageView(new Image("musicnote.png")));
        songItem.setOnAction(event -> {
            FileChooser songChooser = new FileChooser();
            songChooser.setTitle("Choose mp3 file");
            songChooser.setInitialDirectory(new File("/"));
            songChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File newSong = songChooser.showOpenDialog(this.stage);
            if (newSong != null) {
                loadSong(new SongBean(newSong,0));
                this.model.setPlaylist(Collections.emptyList());
            }

        });
        // create the playlist chooser
        MenuItem playlistItem = new MenuItem("Playlist", new ImageView(new Image("musicnotes.png")));
        playlistItem.setOnAction(event -> {
            FileChooser playlistChooser = new FileChooser();
            playlistChooser.setTitle("Choose mp3 files");
            playlistChooser.setInitialDirectory(new File("/"));
            playlistChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"),
                    new FileChooser.ExtensionFilter("MusicList Files", "*.musiclist"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            List<File> newPlaylist = playlistChooser.showOpenMultipleDialog(this.stage);
            List<File> actuallyPlaylist = new ArrayList<>();
            for (File file : newPlaylist) {
                if (file.getName().endsWith(".musiclist")) {
                    executeFile(file,actuallyPlaylist);
                } else {
                    actuallyPlaylist.add(file);
                }
            }
            File[] files = actuallyPlaylist.toArray(new File[0]);
            shuffleArray(files);
            newPlaylist = List.of(files);
            this.model.setPlaylist(newPlaylist);
            loadPlaylistSong();

        });
        menuChoose.getItems().addAll(songItem, playlistItem);
        menuBar.getMenus().addAll(menuChoose);
        return menuBar;
    }

    /**
     * Builds the song slider, which changes song position based on mouse movement
     * of the slider. Also shows time positions in the song by moving the cursor over
     * the slider.
     *
     * @return Slider that manages song position
     */
    private Slider buildSongSlider() {
        Slider songSlider = new Slider(0, 0, 0);
        songSlider.setShowTickMarks(true);
        songSlider.setShowTickLabels(false);
        songSlider.setMajorTickUnit(60 * 44231.5636364);

        Label label = new Label();
        Popup popup = new Popup();
        popup.getContent().add(label);
        songSlider.setOnMouseClicked(event -> this.model.setSongPosition(((int) songSlider.getValue())));
        songSlider.setOnMouseMoved(e -> {
            NumberAxis axis = (NumberAxis) songSlider.lookup(".axis");
            Point2D location = axis.sceneToLocal(e.getSceneX(), e.getSceneY());
            double mouseX = location.getX();
            double value = axis.getValueForDisplay(mouseX).doubleValue();
            if (value >= songSlider.getMin() && value <= songSlider.getMax()) {
                label.setText(String.format("%d:%02d",(int)(value/44231.5636364)/60,(int)(value/44231.5636364)%60));
            } else {
                label.setText("");
            }
            popup.setAnchorX(e.getScreenX() - 5);
            popup.setAnchorY(e.getScreenY() - 20);
        });

        songSlider.setOnMouseEntered(e -> popup.show(songSlider, e.getScreenX() - 5, e.getScreenY() - 20));
        songSlider.setOnMouseExited(e -> popup.hide());

        this.songSlider = songSlider;
        songSlider.setPadding(new Insets(DEFAULT_PADDING));
        return songSlider;
    }

     /*******************************************************
     *                                                      *
     *  HELPER METHODS FOR MANIPULATING THE MODEL           *
     *  AND UPDATING THE GUI                                *
     *                                                      *
     *******************************************************/

    /**
     * Loads a new song. Updates the song slider and volume slider accordingly.
     *
     * @param songBean song file and index to load
     */
    public void loadSong(SongBean songBean) {
        File songFile = songBean.songFile();
        if (this.model.hasClip() && this.model.isRunning()) {
            this.model.stop();
        }
        this.model.changeSong(songBean);
        this.stage.setTitle(songFile.getName() + " ~ MusicPlayer");
        int MIN_VOLUME = (int) this.model.getMinVolume();
        int MAX_VOLUME = (int) this.model.getMaxVolume();
        // update volume slider
        int half = (MAX_VOLUME + MIN_VOLUME)/2;
        this.volumeSlider.setMax(MAX_VOLUME);
        this.volumeSlider.setMin(half);
        this.volumeSlider.setValue((MAX_VOLUME + half) /2.);
        // update song slider
        this.songSlider.setMax(this.model.getClipLength());
        this.songSlider.setMin(0);
        this.songSlider.setValue(0);
    }

    private final AtomicLong currentVolume = new AtomicLong(Double.doubleToLongBits(Double.NaN));
    public double getCurrentVolume() {
        if (Double.isNaN(Double.longBitsToDouble(currentVolume.get()))) {

            int MIN_VOLUME = (int) this.model.getMinVolume();
            int MAX_VOLUME = (int) this.model.getMaxVolume();
            int half = (MAX_VOLUME + MIN_VOLUME)/2;

            this.currentVolume.set(Double.doubleToLongBits((MAX_VOLUME * 0.7 + half * 0.3)));
        }
        return Double.longBitsToDouble(currentVolume.get());
    }

    public void setCurrentVolume(double currentVolume) {
        this.currentVolume.set(Double.doubleToLongBits(currentVolume));
    }

    /**
     * Loads the next song from the playlist. Updates the song slider and volume slider accordingly.
     */
    public void loadPlaylistSong() {
        boolean wasRunning = false;
        if (this.model.hasClip() && this.model.isRunning()) {
            this.model.stop();
            wasRunning = true;
        }
        SongBean song = this.model.loadNextSong();
        this.model.changeSong(song);
        loadSongTitleAndVolume(song, wasRunning);
    }

    private void loadSongTitleAndVolume(SongBean songBean, boolean wasRunning) {
        if (songBean == null) {
            this.stage.setTitle("MusicPlayer");
        }else {
            File songFile = songBean.songFile();
            this.stage.setTitle(songFile.getName() + " ~ MusicPlayer");
            int MIN_VOLUME = (int) this.model.getMinVolume();
            int MAX_VOLUME = (int) this.model.getMaxVolume();
            // update volume slider
            int half = (MAX_VOLUME + MIN_VOLUME) / 2;
            this.volumeSlider.setMax(MAX_VOLUME);
            this.volumeSlider.setMin(half);
            this.volumeSlider.setValue(getCurrentVolume());

            this.model.volumeChange(getCurrentVolume());
            // update song slider
            this.songSlider.setMax(this.model.getClipLength());
            this.songSlider.setMin(0);
            this.songSlider.setValue(0);
            // update play/pause button
            if (wasRunning) {
                setImage(this.play, "pause.png");
                this.model.start();
            }
            this.model.volumeChange(getCurrentVolume());
        }
    }


    /**
     * Loads the previous song from the playlist. Updates the song slider and volume slider accordingly.
     */
    public void loadPrevPlaylistSong() {
        boolean wasRunning = false;
        if (this.model.hasClip() && this.model.isRunning()) {
            this.model.stop();
            wasRunning = true;
        }
        SongBean song = this.model.loadPrevSong();
        loadSongTitleAndVolume(song, wasRunning);
//        this.stage.setTitle(song.getName() + " ~ MusicPlayer");
//        int MIN_VOLUME = (int) this.model.getMinVolume();
//        int MAX_VOLUME = (int) this.model.getMaxVolume();
//        // update volume slider
//        int half = (MAX_VOLUME + MIN_VOLUME) / 2;
//        this.volumeSlider.setMax(MAX_VOLUME);
//        this.volumeSlider.setMin(half);
//        this.volumeSlider.setValue(getCurrentVolume());
//        this.model.volumeChange(getCurrentVolume());
//        // update song slider
//        this.songSlider.setMax(this.model.getClipLength());
//        this.songSlider.setMin(0);
//        this.songSlider.setValue(0);
//        // update play/pause button
//        if (wasRunning) {
//            setImage(this.play, "pause.png");
//            this.model.start();
//        }
//        this.model.volumeChange(getCurrentVolume());
    }

    /**
     * Notifies the GUI to update.
     *
     * Used by the TimeLine to update the song sliders position.
     */
    public void notifytheGUI() {
        this.model.announceChanges();
    }


    /**
     * Updates GUI based on changes in the model
     *
     * @param o not used
     * @param arg not used
     */
    @Override
    public void update(java.util.Observable o, Object arg) {
        // make sure play button is in sync
        if (this.model.isRunning()) {
            setImage(this.play, "pause.png");
        }
        else {
            setImage(this.play, "play.png");
        }
        if (this.model.hasClip()) {
            if (this.model.atEnd()) {
                if (this.model.hasPlaylist()) {
                    this.loadPlaylistSong();
                } else {
                    setImage(this.play, "play.png");
                }
            }
            // update slider based on current song position
            this.songSlider.setValue(this.model.getClipCurrentValue());
        }

    }
}
