package com.dinosauriojuego.core;

import com.badlogic.gdx.Game;
import com.dinosauriojuego.pantallas.PantallaMenu;
import com.dinosauriojuego.utiles.Assets;

public class Main extends Game {

    private Assets assets;
    //carga assets y setea la pantalla del menu
    @Override
    public void create() {
        assets = new Assets();
        assets.cargar();
        setScreen(new PantallaMenu(this, assets));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assets != null) assets.dispose();
    }
}
