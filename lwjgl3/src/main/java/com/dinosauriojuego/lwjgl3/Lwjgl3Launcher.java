package com.dinosauriojuego.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.*;
import com.dinosauriojuego.core.Main;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DinoChrome - Cliente");
        config.setWindowedMode(1280, 720);
        new Lwjgl3Application(new Main(), config);
    }
}
