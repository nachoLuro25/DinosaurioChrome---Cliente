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

    private final Main            game;
    private final Assets          assets;
    private final HiloClienteDino net;

    private OrthographicCamera cam;
    private Viewport      viewport;
    private SpriteBatch   batch;
    private ShapeRenderer shape;
    private GlyphLayout   layout;

    private BitmapFont fontGrande;
    private BitmapFont fontMedia;
    private BitmapFont fontChica;

    // cada pista tiene su propio offset de scroll del fondo
    private float fondoXP1 = 0f;
    private float fondoXP2 = 0f;
    private static final float VEL_FONDO = 140f;
    private static final int   SCORE_CAMBIO = 500;

    private boolean resetEnviado      = false;
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
        fondoXP1 = fondoXP2 = 0f;

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
            if (reset && !resetEnviado) { net.sendReset(); resetEnviado = true; }
        }
        if (!ClienteEstado.terminado) resetEnviado = false;

        // guardar highscore al terminar
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

        // copias locales de los obstaculos para no bloquear el hilo de red
        ArrayList<ClienteEstado.ObstacleState> obsP1, obsP2;
        synchronized (ClienteEstado.obstaculosP1) { obsP1 = new ArrayList<>(ClienteEstado.obstaculosP1); }
        synchronized (ClienteEstado.obstaculosP2) { obsP2 = new ArrayList<>(ClienteEstado.obstaculosP2); }

        // avanzar el scroll de cada fondo
        boolean freeze = ClienteEstado.terminado || !ClienteEstado.empezo;
        if (!freeze) {
            fondoXP1 -= VEL_FONDO * delta;
            fondoXP2 -= VEL_FONDO * delta;
        }
        float W = Constantes.ANCHO_VIRTUAL;
        float H = Constantes.ALTO_VIRTUAL;
        if (fondoXP1 <= -W) fondoXP1 = 0f;
        if (fondoXP2 <= -W) fondoXP2 = 0f;

        boolean esNoche = (ClienteEstado.score / SCORE_CAMBIO) % 2 == 1;
        Texture fondo   = esNoche ? assets.fondoNoche : assets.fondoDia;

        // ---- FONDO GENERAL ----
        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        cam.update();

        // convertir coordenadas virtuales a pixels reales para glScissor
        // (el viewport puede estar escalado si la ventana no es exactamente 1280x720)
        float scaleX = (float) Gdx.graphics.getWidth()  / W;
        float scaleY = (float) Gdx.graphics.getHeight() / H;

        // altura en pixels reales de cada zona
        int pista1Y = Math.round(Constantes.Y_DIVISOR * scaleY); // pista P1 empieza aqui
        int pista1H = Math.round((H - Constantes.Y_DIVISOR) * scaleY);
        int pista2Y = 0;
        int pista2H = Math.round(Constantes.Y_DIVISOR * scaleY);

        // ---- DIBUJAR FONDO PISTA P1 (zona superior) con scissor ----
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, pista1Y, Gdx.graphics.getWidth(), pista1H);

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        // el fondo se dibuja completo (1280x720) pero el scissor solo deja ver la zona superior
        float offsetP1 = Constantes.Y_PISO_P1 - 210f;
        batch.draw(fondo, fondoXP1,     offsetP1, W, H);
        batch.draw(fondo, fondoXP1 + W, offsetP1, W, H);
        batch.end();

        // ---- DIBUJAR FONDO PISTA P2 (zona inferior) con scissor ----
        Gdx.gl.glScissor(0, pista2Y, Gdx.graphics.getWidth(), pista2H);

        batch.begin();
        float offsetP2 = Constantes.Y_PISO_P2 - 210f;
        batch.draw(fondo, fondoXP2,     offsetP2, W, H);
        batch.draw(fondo, fondoXP2 + W, offsetP2, W, H);
        batch.end();

        // apagar scissor para dibujar el resto sin restriccion
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ---- DIBUJAR PISTAS (dinos, obstaculos, etiquetas) ----
        batch.begin();
        dibujarPista(obsP1, ClienteEstado.p1, Constantes.X_JUGADOR_1, Constantes.Y_PISO_P1, "P1");
        dibujarPista(obsP2, ClienteEstado.p2, Constantes.X_JUGADOR_2, Constantes.Y_PISO_P2, "P2");

        // HUD
        Color hudColor = esNoche ? new Color(0.85f, 0.85f, 0.85f, 1f) : COL_GRIS_OSC;
        fontMedia.setColor(COL_GRIS);
        fontMedia.getData().setScale(1.45f);
        String hiStr = "HI  " + String.format("%05d", ClienteEstado.highscore);
        layout.setText(fontMedia, hiStr);
        float hiX = W - layout.width - 24;
        fontMedia.draw(batch, hiStr, hiX, H - 24);

        fontMedia.setColor(hudColor);
        fontMedia.getData().setScale(1.45f);
        String scoreStr = String.format("%05d", ClienteEstado.score);
        layout.setText(fontMedia, scoreStr);
        fontMedia.draw(batch, scoreStr, hiX - layout.width - 28, H - 24);

        batch.end();

        // pisos y divisor encima de todo
        dibujarPisosYDivisor();

        if (ClienteEstado.terminado) dibujarGameOver();
    }

    // dibuja los elementos de una pista: obstaculos, dino y etiqueta
    private void dibujarPista(ArrayList<ClienteEstado.ObstacleState> obs,
                              ClienteEstado.DinoState dino,
                              float xJugador, float yPiso, String etiqueta) {
        final float DINO_W     = 74.8f;
        final float DINO_Y_OFF = 40f;

        for (ClienteEstado.ObstacleState o : obs) {
            if (o.type == 0) {
                Texture t;
                if      (o.variant == 0) t = assets.cactusChico1;
                else if (o.variant == 1) t = assets.cactusChico2;
                else if (o.variant == 2) t = assets.cactusGrande1;
                else if (o.variant == 3) t = assets.cactusGrande2;
                else                     t = assets.cactusCombinado;
                batch.draw(t, o.x, yPiso);
            } else {
                Texture t = (ClienteEstado.tick % 12 < 6) ? assets.ptero1 : assets.ptero2;
                batch.draw(t, o.x, o.y);
            }
        }

        float dinoH = dino.agachado ? 51f : 102f;
        Texture dinoTex = elegirDinoTex(dino.vivo, dino.enPiso, dino.agachado, ClienteEstado.tick);
        batch.draw(dinoTex, xJugador, dino.y + DINO_Y_OFF, DINO_W, dinoH);

        fontChica.setColor(COL_GRIS_OSC);
        fontChica.getData().setScale(1.1f);
        fontChica.draw(batch, etiqueta, xJugador + 8, dino.y + DINO_Y_OFF + dinoH + 6);
    }

    // dibuja los pisos de cada pista y la linea divisoria
    private void dibujarPisosYDivisor() {
        float W = Constantes.ANCHO_VIRTUAL;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PISO);
        shape.rect(0, Constantes.Y_PISO_P1 - 4, W, 4f); // piso pista P1
        shape.rect(0, Constantes.Y_PISO_P2 - 4, W, 4f); // piso pista P2
        shape.end();
    }

    private void dibujarGameOver() {
        float W = Constantes.ANCHO_VIRTUAL;
        float H = Constantes.ALTO_VIRTUAL;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(cam.combined);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.95f, 0.95f, 0.95f, 0.82f);
        shape.rect(0, 0, W, H);
        shape.end();

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

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_NEGRO);
        shape.rect(bx, by + bh - 7f, bw, 7f);
        shape.end();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        fontGrande.setColor(COL_NEGRO);
        fontGrande.getData().setScale(2.7f);
        centrarTexto(fontGrande, "GAME OVER", W, by + bh - 16);

        fontMedia.setColor(COL_GRIS_OSC);
        fontMedia.getData().setScale(1.6f);
        centrarTexto(fontMedia, ClienteEstado.mensajeFin.toUpperCase(), W, by + bh / 2f + 28);

        fontChica.setColor(COL_GRIS);
        fontChica.getData().setScale(1.2f);
        centrarTexto(fontChica,
                "SCORE  " + String.format("%05d", ClienteEstado.score) +
                        "      HI  " + String.format("%05d", ClienteEstado.highscore),
                W, by + bh / 2f - 14f);

        batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PISO);
        shape.rect(bx + 30, by + bh / 2f - 34f, bw - 60, 2f);
        shape.end();

        batch.begin();
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

    @Override public void resize(int w, int h) { if (viewport != null) viewport.update(w, h, true); }

    @Override
    public void dispose() {
        if (batch      != null) batch.dispose();
        if (shape      != null) shape.dispose();
        if (fontGrande != null) fontGrande.dispose();
        if (fontMedia  != null) fontMedia.dispose();
        if (fontChica  != null) fontChica.dispose();
    }
}