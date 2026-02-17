package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.dinosauriojuego.core.Main;
import com.dinosauriojuego.net.ClienteEstado;
import com.dinosauriojuego.net.ClienteListener;
import com.dinosauriojuego.net.HiloClienteDino;
import com.dinosauriojuego.utiles.Assets;

public class PantallaEspera extends ScreenAdapter implements ClienteListener {

    private final Main game;
    private final Assets assets;
    private final HiloClienteDino net;

    private SpriteBatch batch;
    private BitmapFont font;

    //se llama cuando un jugador esta listo y espera al otro, al recibir el evento de que el juego empieza, cambia a la pantalla de juego online
    public PantallaEspera(Main game, Assets assets, HiloClienteDino net) {
        this.game = game;
        this.assets = assets;
        this.net = net;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);
        net.setListener(this);
        net.sendListo();
    }

    @Override
    public void render(float delta) {
        if (ClienteEstado.empezo) {
            game.setScreen(new PantallaJuegoOnline(game, assets, net));
            return;
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        font.draw(batch, "Esperando al otro jugador...", 380, 360);
        batch.end();
    }

    @Override public void onConectado() {}
    @Override public void onEmpieza() {}
    @Override public void onSnapshot() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
