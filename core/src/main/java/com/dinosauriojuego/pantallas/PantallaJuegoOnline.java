package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

import com.dinosauriojuego.core.Main;
import com.dinosauriojuego.net.ClienteEstado;
import com.dinosauriojuego.net.HiloClienteDino;
import com.dinosauriojuego.utiles.Assets;
import com.dinosauriojuego.utiles.Constantes;

public class PantallaJuegoOnline extends ScreenAdapter {

    private final Main game;
    private final Assets assets;
    private final HiloClienteDino net;

    private OrthographicCamera cam;
    private Viewport viewport;

    private SpriteBatch batch;
    private BitmapFont font;

    private float fondoX = 0f;
    private float velocidadFondo = 140f;
    private static final int SCORE_CAMBIO = 500;

    private boolean resetEnviado = false;

    // highscore
    private Preferences prefs;
    private boolean highscoreGuardado = false;

    public PantallaJuegoOnline(Main game, Assets assets, HiloClienteDino net) {
        this.game = game;
        this.assets = assets;
        this.net = net;
    }

    @Override

    public void show() {
        cam = new OrthographicCamera();
        viewport = new FitViewport(Constantes.ANCHO_VIRTUAL, Constantes.ALTO_VIRTUAL, cam);

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        assets.startMusic();


        prefs = Gdx.app.getPreferences("dinochrome_client");
        ClienteEstado.highscore = prefs.getInteger("highscore", 0);

        highscoreGuardado = false;
        resetEnviado = false;
        fondoX = 0f;
    }

    @Override
    public void render(float delta) {
        // input
        boolean jump = (Gdx.input.isKeyJustPressed(Input.Keys.UP));
        boolean crouch = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean reset = Gdx.input.isKeyJustPressed(Input.Keys.R);

        //si el juego no termino, manda los inputs de saltar o agacharse, si termino manda el reset si se apreto R
        if (!ClienteEstado.terminado) {
            net.sendInput(jump, crouch);
            if (jump) assets.playJump();
        } else {
            if (reset && !resetEnviado) {
                net.sendReset();
                resetEnviado = true;
            }
        }
        if (!ClienteEstado.terminado) resetEnviado = false;

        //guardar highscore solo 1 vez cuando termina con condicion de que haya superado el highscore anterior
        if (ClienteEstado.terminado) {
            if (!highscoreGuardado) {
                if (ClienteEstado.score > ClienteEstado.highscore) {
                    ClienteEstado.highscore = ClienteEstado.score;
                    prefs.putInteger("highscore", ClienteEstado.highscore).flush();
                }
                highscoreGuardado = true;
            }
        } else {
            highscoreGuardado = false;
        }

        //copia del sincrhonized los obstaculos para que no se crashee
        ArrayList<ClienteEstado.ObstacleState> obsCopia;
        synchronized (ClienteEstado.obstacles) {
            obsCopia = new ArrayList<>(ClienteEstado.obstacles);
        }

        // iewport/cam
        viewport.apply();
        cam.update();
        batch.setProjectionMatrix(cam.combined);

        // fondo
        boolean esNoche = (ClienteEstado.score / SCORE_CAMBIO) % 2 == 1;
        Texture fondo = esNoche ? assets.fondoNoche : assets.fondoDia;

        boolean freezeBg = ClienteEstado.terminado || !ClienteEstado.empezo;
        if (!freezeBg) fondoX -= velocidadFondo * delta;

        float w = Constantes.ANCHO_VIRTUAL;

        //render
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        batch.draw(fondo, fondoX, 0, w, Constantes.ALTO_VIRTUAL);
        batch.draw(fondo, fondoX + w, 0, w, Constantes.ALTO_VIRTUAL);
        if (fondoX <= -w) fondoX = 0f;

        //obstaculos
        for (ClienteEstado.ObstacleState o : obsCopia) {
            if (o.type == 0) {
                Texture t;
                if (o.variant == 0) t = assets.cactusChico1;
                else if (o.variant == 1) t = assets.cactusChico2;
                else if (o.variant == 2) t = assets.cactusGrande1;
                else if (o.variant == 3) t = assets.cactusGrande2;
                else t = assets.cactusCombinado;

                float y = (o.y != 0f) ? o.y : Constantes.Y_PISO;
                batch.draw(t, o.x, y);
            } else {
                Texture t = (ClienteEstado.tick % 12 < 6) ? assets.ptero1 : assets.ptero2;
                batch.draw(t, o.x, o.y);
            }
        }

        //dinos
        Texture d1 = elegirDinoTex(ClienteEstado.p1.vivo, ClienteEstado.p1.enPiso, ClienteEstado.p1.agachado, ClienteEstado.tick);
        batch.draw(d1, Constantes.X_JUGADOR_1, ClienteEstado.p1.y);

        Texture d2 = elegirDinoTex(ClienteEstado.p2.vivo, ClienteEstado.p2.enPiso, ClienteEstado.p2.agachado, ClienteEstado.tick);
        batch.draw(d2, Constantes.X_JUGADOR_2, ClienteEstado.p2.y);

        //hud
        font.draw(batch, "Score: " + ClienteEstado.score, 20, Constantes.ALTO_VIRTUAL - 20);
        font.draw(batch, "Highscore: " + ClienteEstado.highscore, 20, Constantes.ALTO_VIRTUAL - 55);

        if (ClienteEstado.terminado) {
            font.getData().setScale(2.0f);
            font.draw(batch, "FIN: " + ClienteEstado.mensajeFin, 420, 520);
            font.getData().setScale(1.5f);
            font.draw(batch, "R para reiniciar (" + ClienteEstado.resetReadyCount + "/2)", 440, 480);
        }

        batch.end();
    }

    private Texture elegirDinoTex(boolean vivo, boolean enPiso, boolean agachado, int tick) {
        if (!vivo) return assets.dinoMuerto;
        if (!enPiso) return assets.dinoQuieto;
        if (agachado) return (tick % 12 < 6) ? assets.dinoAgach1 : assets.dinoAgach2;
        return (tick % 12 < 6) ? assets.dinoMov1 : assets.dinoMov2;
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
