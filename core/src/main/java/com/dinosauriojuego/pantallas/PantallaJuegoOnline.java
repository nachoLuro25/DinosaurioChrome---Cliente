package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

import com.dinosauriojuego.core.Main;
import com.dinosauriojuego.net.ClienteEstado;
import com.dinosauriojuego.net.HiloClienteDino;
import com.dinosauriojuego.utiles.Assets;
import com.dinosauriojuego.utiles.Constantes;

public class PantallaJuegoOnline extends ScreenAdapter {

    private final Main           game;
    private final Assets         assets;
    private final HiloClienteDino net;

    private OrthographicCamera cam;
    private Viewport      viewport;
    private SpriteBatch   batch;
    private ShapeRenderer shape;
    private GlyphLayout   layout;

    private BitmapFont fontGrande;
    private BitmapFont fontMedia;
    private BitmapFont fontChica;

    private float fondoX         = 0f;
    private float velocidadFondo = 140f;
    private static final int SCORE_CAMBIO = 500;

    private boolean resetEnviado     = false;
    private boolean highscoreGuardado = false;
    private float   tiempo            = 0f;

    private Preferences prefs;

    // paleta Chrome
    private static final Color COL_BG      = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color COL_BLANCO  = new Color(1f,    1f,    1f,    1f);
    private static final Color COL_PISO    = new Color(0.70f, 0.70f, 0.70f, 1f);
    private static final Color COL_GRIS    = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color COL_GRIS_OSC= new Color(0.38f, 0.38f, 0.38f, 1f);
    private static final Color COL_NEGRO   = new Color(0.20f, 0.20f, 0.20f, 1f);

    public PantallaJuegoOnline(Main game, Assets assets, HiloClienteDino net) {
        this.game   = game;
        this.assets = assets;
        this.net    = net;
    }

    @Override
    public void show() {
        cam      = new OrthographicCamera();
        viewport = new FitViewport(Constantes.ANCHO_VIRTUAL, Constantes.ALTO_VIRTUAL, cam);
        batch    = new SpriteBatch();
        shape    = new ShapeRenderer();
        layout   = new GlyphLayout();

        fontGrande = new BitmapFont();
        fontMedia  = new BitmapFont();
        fontChica  = new BitmapFont();

        prefs = Gdx.app.getPreferences("dinochrome_client");
        ClienteEstado.highscore = prefs.getInteger("highscore", 0);

        highscoreGuardado = false;
        resetEnviado      = false;
        fondoX            = 0f;

        assets.startMusic();
    }

    @Override
    public void render(float delta) {
        tiempo += delta;

        boolean jump   = Gdx.input.isKeyJustPressed(Input.Keys.UP);
        boolean crouch = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean reset  = Gdx.input.isKeyJustPressed(Input.Keys.R);

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

        // guardar highscore
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

        ArrayList<ClienteEstado.ObstacleState> obsCopia;
        synchronized (ClienteEstado.obstacles) {
            obsCopia = new ArrayList<>(ClienteEstado.obstacles);
        }

        viewport.apply();
        cam.update();
        batch.setProjectionMatrix(cam.combined);

        boolean esNoche = (ClienteEstado.score / SCORE_CAMBIO) % 2 == 1;
        Texture fondo   = esNoche ? assets.fondoNoche : assets.fondoDia;

        boolean freezeBg = ClienteEstado.terminado || !ClienteEstado.empezo;
        if (!freezeBg) fondoX -= velocidadFondo * delta;

        float w = Constantes.ANCHO_VIRTUAL;

        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // fondo del juego
        batch.draw(fondo, fondoX, 0, w, Constantes.ALTO_VIRTUAL);
        batch.draw(fondo, fondoX + w, 0, w, Constantes.ALTO_VIRTUAL);
        if (fondoX <= -w) fondoX = 0f;

        // obstáculos
        for (ClienteEstado.ObstacleState o : obsCopia) {
            if (o.type == 0) {
                Texture t;
                if      (o.variant == 0) t = assets.cactusChico1;
                else if (o.variant == 1) t = assets.cactusChico2;
                else if (o.variant == 2) t = assets.cactusGrande1;
                else if (o.variant == 3) t = assets.cactusGrande2;
                else                     t = assets.cactusCombinado;
                float y = (o.y != 0f) ? o.y : Constantes.Y_PISO;
                batch.draw(t, o.x, y);
            } else {
                Texture t = (ClienteEstado.tick % 12 < 6) ? assets.ptero1 : assets.ptero2;
                batch.draw(t, o.x, o.y);
            }
        }

        // dinos

        final float DINO_W = 44f;
        final float DINO_H = 60f;
        final float DINO_Y_OFFSET = 20f;
        Texture d1 = elegirDinoTex(ClienteEstado.p1.vivo, ClienteEstado.p1.enPiso,
                ClienteEstado.p1.agachado, ClienteEstado.tick);
        batch.draw(d1, Constantes.X_JUGADOR_1, ClienteEstado.p1.y + DINO_Y_OFFSET, DINO_W, DINO_H);

        Texture d2 = elegirDinoTex(ClienteEstado.p2.vivo, ClienteEstado.p2.enPiso,
                ClienteEstado.p2.agachado, ClienteEstado.tick);
        batch.draw(d2, Constantes.X_JUGADOR_2, ClienteEstado.p2.y + DINO_Y_OFFSET, DINO_W, DINO_H);

        // HUD: HI score + score actuales arriba a la derecha, estilo Chrome
        Color hudColor = esNoche ? new Color(0.85f, 0.85f, 0.85f, 1f) : COL_GRIS_OSC;

        fontMedia.setColor(COL_GRIS);
        fontMedia.getData().setScale(1.45f);
        String hiStr = "HI  " + String.format("%05d", ClienteEstado.highscore);
        layout.setText(fontMedia, hiStr);
        float hiX = Constantes.ANCHO_VIRTUAL - layout.width - 24;
        fontMedia.draw(batch, hiStr, hiX, Constantes.ALTO_VIRTUAL - 24);

        fontMedia.setColor(hudColor);
        fontMedia.getData().setScale(1.45f);
        String scoreStr = String.format("%05d", ClienteEstado.score);
        layout.setText(fontMedia, scoreStr);
        fontMedia.draw(batch, scoreStr,
                hiX - layout.width - 28,
                Constantes.ALTO_VIRTUAL - 24);

        // etiquetas P1 / P2
        fontChica.setColor(COL_GRIS_OSC);
        fontChica.getData().setScale(1.1f);
        fontChica.draw(batch, "P1", Constantes.X_JUGADOR_1 + 8, ClienteEstado.p1.y + 74 + 10f);
        fontChica.draw(batch, "P2", Constantes.X_JUGADOR_2 + 8, ClienteEstado.p2.y + 74 + 10f);

        batch.end();

        // Game Over overlay
        if (ClienteEstado.terminado) {
            dibujarGameOver();
        }
    }

    private void dibujarGameOver() {
        float W = Constantes.ANCHO_VIRTUAL;
        float H = Constantes.ALTO_VIRTUAL;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.setProjectionMatrix(cam.combined);

        // overlay semitransparente
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.95f, 0.95f, 0.95f, 0.82f);
        shape.rect(0, 0, W, H);
        shape.end();

        // caja central
        float bw = 560f, bh = 240f;
        float bx = (W - bw) / 2f, by = (H - bh) / 2f;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_BLANCO);
        shape.rect(bx, by, bw, bh);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shape.setColor(COL_NEGRO);
        shape.rect(bx, by, bw, bh);
        shape.end();
        Gdx.gl.glLineWidth(1f);

        // franja superior negra
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_NEGRO);
        shape.rect(bx, by + bh - 7f, bw, 7f);
        shape.end();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // GAME OVER
        fontGrande.setColor(COL_NEGRO);
        fontGrande.getData().setScale(2.7f);
        centrarTexto(fontGrande, "GAME OVER", W, by + bh - 16);

        // resultado
        fontMedia.setColor(COL_GRIS_OSC);
        fontMedia.getData().setScale(1.6f);
        centrarTexto(fontMedia, ClienteEstado.mensajeFin.toUpperCase(), W, by + bh / 2f + 28);

        // score y highscore
        fontChica.setColor(COL_GRIS);
        fontChica.getData().setScale(1.2f);
        centrarTexto(fontChica,
                "SCORE  " + String.format("%05d", ClienteEstado.score) +
                        "      HI  " + String.format("%05d", ClienteEstado.highscore),
                W, by + bh / 2f - 14f);

        // línea divisoria
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PISO);
        shape.rect(bx + 30, by + bh / 2f - 34f, bw - 60, 2f);
        shape.end();
        batch.begin();

        // instruccion reinicio parpadeante + contador
        float alpha = 0.45f + 0.55f * (float) Math.abs(Math.sin(tiempo * 3.5f));
        fontChica.setColor(COL_NEGRO.r, COL_NEGRO.g, COL_NEGRO.b, alpha);
        fontChica.getData().setScale(1.2f);
        centrarTexto(fontChica,
                "Presiona  R  para reiniciar  (" + ClienteEstado.resetReadyCount + "/2)",
                W, by + 58f);

        batch.end();
    }

    private Texture elegirDinoTex(boolean vivo, boolean enPiso, boolean agachado, int tick) {
        if (!vivo)    return assets.dinoMuerto;
        if (!enPiso)  return assets.dinoQuieto;
        if (agachado) return (tick % 12 < 6) ? assets.dinoAgach1 : assets.dinoAgach2;
        return (tick % 12 < 6) ? assets.dinoMov1 : assets.dinoMov2;
    }

    private void centrarTexto(BitmapFont font, String texto, float areaW, float y) {
        layout.setText(font, texto);
        font.draw(batch, texto, (areaW - layout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch      != null) batch.dispose();
        if (shape      != null) shape.dispose();
        if (fontGrande != null) fontGrande.dispose();
        if (fontMedia  != null) fontMedia.dispose();
        if (fontChica  != null) fontChica.dispose();
    }
}