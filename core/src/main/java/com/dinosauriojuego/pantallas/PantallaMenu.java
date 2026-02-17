package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.dinosauriojuego.core.Main;
import com.dinosauriojuego.net.HiloClienteDino;
import com.dinosauriojuego.utiles.*;

public class PantallaMenu extends ScreenAdapter {

    private final Main game;
    private final Assets assets;

    private SpriteBatch batch;
    private BitmapFont font;
    private Viewport viewport;

    public PantallaMenu(Main game, Assets assets) {
        this.game = game;
        this.assets = assets;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);
        viewport = new FitViewport(Constantes.ANCHO_VIRTUAL, Constantes.ALTO_VIRTUAL);
    }


    @Override
    //Renderiza la pantalla del menu, al presionar ENTER inicia el hilo del cliente y cambia a la pantalla de espera
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            HiloClienteDino net = new HiloClienteDino();
            net.start();
            game.setScreen(new PantallaEspera(game, assets, net));
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        batch.draw(assets.fondoDia, 0, 0, Constantes.ANCHO_VIRTUAL, Constantes.ALTO_VIRTUAL);
        font.draw(batch, "Presiona 'ENTER para jugar", 420, 360);
        batch.end();
    }
}