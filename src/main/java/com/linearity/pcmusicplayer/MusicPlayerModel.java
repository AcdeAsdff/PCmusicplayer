package com.linearity.pcmusicplayer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Model file of the Model-View-Controller design pattern.
 * Contains the variables and methods regarding the mp3 file.
 * Used by MusicPlayerGUI.java
 *
 * @author connoryork (cxy1054@rit.edu)
 * @author mbroman (broman334@tamu.edu)
 */
public class MusicPlayerModel extends Observable {

    public AtomicBoolean loadingFlag = new AtomicBoolean(true);
    /**
     * PRIVATE DATA MEMBERS
     */
    private Clip clip;
    private AudioInputStream audioStream;
    private AudioInputStream decodedStream;
    private AudioFormat baseFormat;
    private AudioFormat decodeFormat;
    private final ObservableList<SongBean> playlist = FXCollections.observableArrayList();
    private final AtomicInteger playlistPosition = new AtomicInteger(0);

    /********************************************************
    *                                                       *
    *      CONSTRUCTORS                                     *
    *                                                       *
    ********************************************************/

    /**
     * Constructor for the model. Essentially sets up the model with everything set to null.
     */
    public MusicPlayerModel() {
        this.clip = null;
        this.audioStream = null;
        this.decodedStream = null;
        this.baseFormat = null;
        this.decodeFormat = null;
    }

    /**
     * Changes the song loaded onto the clip.
     *
     * @param songBean file and index to change to currents song
     */
    public void changeSong(SongBean songBean) {
        File songFile = songBean.songFile();
        try {
            if (this.clip != null) {
                try {
                    this.clip.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.audioStream = AudioSystem.getAudioInputStream(songFile);
            this.baseFormat = audioStream.getFormat();
            this.decodeFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            this.decodedStream = AudioSystem.getAudioInputStream(decodeFormat, audioStream);
            this.clip = AudioSystem.getClip();

            this.clip.open(decodedStream);
            this.clip.setFramePosition(0);
        } catch (Exception e) {
            System.out.println("Failed to load audio.");
            System.out.println(songFile.getAbsolutePath());
            e.printStackTrace();
        } finally {
            announceChanges();
        }
    }

    /**
     * Loads the next song in the playlist, if possible.
     *
     * @return File (.mp3) that was changed to the current song
     */
    public SongBean loadNextSong() {
        int currentIndex = this.playlistPosition.incrementAndGet();
        if (currentIndex >= this.playlist.size()) {
            currentIndex = 0;
            this.playlistPosition.set(currentIndex);
        }
        SongBean song = this.playlist.get(currentIndex);
        this.changeSong(song);
        return song;
    }

    /**
     * Loads the next song in the playlist, if possible.
     *
     * @return File (.mp3) that was changed to the current song
     */
    public SongBean loadSpecificSong(int currentIndex) {
        int playListSize = this.playlist.size();
        while (currentIndex >= this.playlist.size()) {
            currentIndex -= playListSize;
        }
        while (currentIndex < 0) {
            currentIndex += playListSize;
        }
        this.playlistPosition.set(currentIndex);
        SongBean song = this.playlist.get(currentIndex);
        this.changeSong(song);
        return song;
    }

    /**
     * Loads the previous song on the playlist
     *
     * @return File (.mp3) that was changed to the current song
     */
    @Nullable
    public SongBean loadPrevSong() {
        if (this.playlist != null) {
            int currentIndex = this.playlistPosition.decrementAndGet();
            if (currentIndex < 0) {
                currentIndex = this.playlist.size() - 1;
                this.playlistPosition.set(currentIndex);
            }
            SongBean song = this.playlist.get(currentIndex);
            this.changeSong(song);
            return song;
        }
        return null;
    }

    /**
     * Starts the song from its current position.
     */
    public void start() {
        if (this.hasClip() && !this.clip.isRunning()) {
            this.clip.start();
        }
    }

    /**
     * Pauses the clip.
     */
    public void stop() {
        if (this.hasClip() && this.clip.isRunning()) {
            this.clip.stop();
        }
    }

    /**
     * Changes the clips loudness.
     *
     * @param decibels decibels desired by the user
     */
    public void volumeChange(double decibels) {
        if (this.hasClip()) {
            FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (decibels ==  ( getMaxVolume() +  getMinVolume()) / 2.) {
                gainControl.setValue((float) this.getMinVolume());
            } else
                gainControl.setValue((float) decibels);
        }
    }

    /**
     * Rewinds the clip to the start.
     */
    public void rewindToStart() {
        if (this.hasClip()) {
            boolean prevRun = this.clip.isRunning();
            this.clip.stop();
            this.clip.setFramePosition(0);
            if(prevRun) {
                this.clip.start();
            }
        }
    }

    /**
     * Sets the clip's position to the new value.
     *
     * @param position frame position to set song at (0 < position < this.clip.getFrameLength())
     */
    public void setSongPosition(int position) {
        if (this.hasClip()) {
            boolean prevRun = this.clip.isRunning();
            this.clip.stop();
            this.clip.setFramePosition(position);
            if (prevRun) {
                this.clip.start();
            }
        }
    }

    /**
     * Sets the playlist to a new list.
     *
     * @param playlist list of Files to set the new playlist as
     */
    public void setPlaylist(List<File> playlist) {
        this.playlist.clear();
        for (int i=0;i<playlist.size();i++) {
            this.playlist.add(new SongBean(playlist.get(i), i));
        }
        this.playlistPosition.set(0);
    }

    public ObservableList<SongBean> getPlaylist() {
        return playlist;
    }

    /**
     * Gets the minimum decibel volume of the clip.
     * Implies that there is a current song stored in this.clip.
     *
     * @return min decibel volume of the current clip
     */
    public double getMinVolume() {
        if (!this.hasClip()) {return 0.;}
        FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
        return gainControl.getMinimum();
    }

    /**
     * Gets the maximum decibel volume of the clip.
     * Implies that there is a current song stored in this.clip.
     *
     * @return max decibel volume of the current clip
     */
    public double getMaxVolume() {
        if (!this.hasClip()) {return 1.;}
        FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
        return gainControl.getMaximum();
    }

    /**
     * Gets the total length of the current clip.
     * Implies that there is a current song stored in this.clip.
     *
     * @return length of the current clip
     */
    public int getClipLength() {
        if (!this.hasClip()) {return 0;}
        return this.clip.getFrameLength();
    }

    /**
     * Gets the current position of the song.
     * Implies that there is a current song stored in this.clip.
     *
     * @return the current integer position of the song
     */
    public int getClipCurrentValue() {
        return this.clip.getFramePosition();
    }

    /**
     * Checks if the song is at the end.
     *
     * @return true if at the end, false otherwise
     */
    public boolean atEnd() {

        return this.clip.getFrameLength() - this.clip.getFramePosition() <= 50000;
    }

    /**
     * Returns the state of the music player.
     *
     * @return true if song is playing, false otherwise
     */
    public boolean isRunning() {
        return this.hasClip() && (this.clip.isRunning() || this.atEnd());
    }

    /**
     * Returns if the music player has a current song (clip).
     *
     * @return true if song exists, false otherwise
     */
    public boolean hasClip() {
        return this.clip != null;
    }

    /**
     * Returns whether or not there is a current playlist.
     *
     * @return true if has a playlist, false otherwise
     */
    public boolean hasPlaylist() {
        return this.playlist != null;
    }

    /**
     * Utility function to notify GUI that changes were made with the model's instance variables.
     */
    public void announceChanges() {
        setChanged();
        notifyObservers();
    }
}