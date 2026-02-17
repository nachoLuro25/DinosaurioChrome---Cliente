package com.dinosauriojuego.utiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;

public class Assets {

    public Texture cactusChico1, cactusChico2, cactusGrande1, cactusGrande2, cactusCombinado;
    public Texture dinoQuieto, dinoMov1, dinoMov2, dinoAgach1, dinoAgach2, dinoMuerto;
    public Texture ptero1, ptero2;
    public Texture fondoDia, fondoNoche;

    // audio
    private Music music;
    private Sound sfxJump;
    private Sound sfxDie;

    private Preferences prefs;
    private float volumen = 0.5f;

    public void cargar() {
        cactusChico1 = new Texture("Cactus/cactuschico1.png");
        cactusChico2 = new Texture("Cactus/cactuschico2.png");
        cactusGrande1 = new Texture("Cactus/cactusgrande1.png");
        cactusGrande2 = new Texture("Cactus/cactusgrande2.png");
        cactusCombinado = new Texture("Cactus/cactuscombinado.png");

        dinoQuieto = new Texture("Dinosaurio2/dino2quieto.png");
        dinoMov1   = new Texture("Dinosaurio2/dino2movimiento1.png");
        dinoMov2   = new Texture("Dinosaurio2/dino2movimiento2.png");
        dinoAgach1 = new Texture("Dinosaurio2/dino2agachado1.png");
        dinoAgach2 = new Texture("Dinosaurio2/dino2agachado2.png");
        dinoMuerto = new Texture("Dinosaurio2/dino2muerto.png");

        ptero1 = new Texture("Ptedoractilos/pterodactilo1.png");
        ptero2 = new Texture("Ptedoractilos/pterodactilo2.png");

        fondoDia   = new Texture("Cactus/fondodia.png");
        fondoNoche = new Texture("Cactus/fondonoche.png");

        prefs = Gdx.app.getPreferences("dinochrome_client");
        volumen = clamp(prefs.getFloat("volumen", 0.5f));


        //musica y audio, en caso de que no se encuentre el archivo, se asigna null
        try {
            music = Gdx.audio.newMusic(Gdx.files.internal("audio/musica.mp3"));
            music.setLooping(true);
            music.setVolume(volumen);
        } catch (Exception ignored) {
            music = null;
        }
        try { sfxJump = Gdx.audio.newSound(Gdx.files.internal("audio/salto.wav")); } catch (Exception ignored) { sfxJump = null; }
        try { sfxDie  = Gdx.audio.newSound(Gdx.files.internal("audio/muerte.wav"));  } catch (Exception ignored) { sfxDie = null; }
    }

    public void startMusic() {
        if (music != null && !music.isPlaying()) {
            music.setVolume(volumen);
            music.play();
        }
    }

    public void playJump() {
        if (sfxJump != null) sfxJump.play(volumen);
    }

    public void playDie() {
        if (sfxDie != null) sfxDie.play(volumen);
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public void dispose() {
        cactusChico1.dispose();
        cactusChico2.dispose();
        cactusGrande1.dispose();
        cactusGrande2.dispose();
        cactusCombinado.dispose();

        dinoQuieto.dispose();
        dinoMov1.dispose();
        dinoMov2.dispose();
        dinoAgach1.dispose();
        dinoAgach2.dispose();
        dinoMuerto.dispose();

        ptero1.dispose();
        ptero2.dispose();
        fondoDia.dispose();
        fondoNoche.dispose();

        if (music != null) music.dispose();
        if (sfxJump != null) sfxJump.dispose();
        if (sfxDie != null) sfxDie.dispose();
    }
}
