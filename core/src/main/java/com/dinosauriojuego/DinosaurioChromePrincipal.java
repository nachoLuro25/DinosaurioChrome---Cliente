package com.dinosauriojuego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.dinosauriojuego.network.HiloClienteDino;
import com.dinosauriojuego.pantallas.DinosaurioGameScreen;
import com.dinosauriojuego.pantallas.MenuScreen;
import com.dinosauriojuego.pantallas.PantallaEspera;

/*** Clase principal de la aplicación con soporte para modo local y multijugador*/
public class DinosaurioChromePrincipal extends Game {
    private Skin skin;
    private MenuScreen menuScreen;
    private DinosaurioGameScreen gameScreen;
    private HiloClienteDino clienteRed;

    @Override
    public void create() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        menuScreen = new MenuScreen(this, skin);
        setScreen(menuScreen);
    }

    /**
     * Inicia el juego en modo local
     */
    public void iniciarJuego() {
        if (gameScreen != null) {
            gameScreen.dispose();
        }
        gameScreen = new DinosaurioGameScreen(skin);
        setScreen(gameScreen);
    }

    /**
     * Inicia el modo multijugador
     */
    public void iniciarMultijugador() {
        // Cerrar cliente anterior si existe
        if (clienteRed != null) {
            clienteRed.cerrar();
        }

        // Crear nuevo cliente de red
        clienteRed = new HiloClienteDino();
        clienteRed.start();

        // Ir a pantalla de espera
        setScreen(new PantallaEspera(this, skin, clienteRed));
    }

    /*** Vuelve al menú principal*/
    public void volverAlMenu() {
        // Cerrar conexión de red si existe
        if (clienteRed != null) {
            clienteRed.cerrar();
            clienteRed = null;
        }

        setScreen(menuScreen);
    }

    @Override
    public void dispose() {
        if (skin != null) {
            skin.dispose();
        }
        if (menuScreen != null) {
            menuScreen.dispose();
        }
        if (gameScreen != null) {
            gameScreen.dispose();
        }
        if (clienteRed != null) {
            clienteRed.cerrar();
        }
    }
}