package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.dinosauriojuego.core.Main;
import com.dinosauriojuego.net.HiloClienteDino;
import com.dinosauriojuego.utiles.Assets;
import com.dinosauriojuego.utiles.Constantes;

public class PantallaMenu extends ScreenAdapter {

    private final Main   game;
    private final Assets assets;

    private OrthographicCamera cam;
    private Viewport    viewport;
    private SpriteBatch batch;
    private ShapeRenderer shape;
    private GlyphLayout layout;

    private BitmapFont fontGrande;
    private BitmapFont fontMedia;
    private BitmapFont fontChica;

    private float tiempo       = 0f;
    private float dinoAnimTick = 0f;

    // nubes
    private static final int NUM_NUBES = 6;
    private float[] nubeX   = new float[NUM_NUBES];
    private float[] nubeY   = new float[NUM_NUBES];
    private float[] nubeSpd = new float[NUM_NUBES];
    private float[] nubeW   = new float[NUM_NUBES];

    // cactus decorativos de fondo
    private float[] cactusDecoX = { 140f, 480f, 760f, 1060f };

    // paleta Chrome
    private static final Color COL_BG      = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color COL_BLANCO  = new Color(1f,    1f,    1f,    1f);
    private static final Color COL_NUBE    = new Color(0.82f, 0.82f, 0.82f, 1f);
    private static final Color COL_PISO    = new Color(0.70f, 0.70f, 0.70f, 1f);
    private static final Color COL_GRIS    = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color COL_GRIS_OSC= new Color(0.38f, 0.38f, 0.38f, 1f);
    private static final Color COL_NEGRO   = new Color(0.20f, 0.20f, 0.20f, 1f);

    public PantallaMenu(Main game, Assets assets) {
        this.game   = game;
        this.assets = assets;
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

        float[] alts  = { 430f, 470f, 510f, 400f, 445f, 485f };
        float[] anchs = { 155f, 115f, 195f, 135f, 175f, 108f };
        float[] spds  = {  28f,  22f,  18f,  32f,  25f,  20f };
        java.util.Random rng = new java.util.Random(13);
        for (int i = 0; i < NUM_NUBES; i++) {
            nubeX[i]   = rng.nextFloat() * Constantes.ANCHO_VIRTUAL;
            nubeY[i]   = alts[i];
            nubeSpd[i] = spds[i];
            nubeW[i]   = anchs[i];
        }
    }

    @Override
    public void render(float delta) {
        tiempo       += delta;
        dinoAnimTick += delta * 60f;

        // input: ENTER conecta y va a espera
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            HiloClienteDino net = new HiloClienteDino();
            net.start();
            game.setScreen(new PantallaEspera(game, assets, net));
            return;
        }

        float W = Constantes.ANCHO_VIRTUAL;
        float H = Constantes.ALTO_VIRTUAL;

        // fondo
        Gdx.gl.glClearColor(COL_BG.r, COL_BG.g, COL_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        viewport.apply();
        cam.update();
        shape.setProjectionMatrix(cam.combined);

        // nubes
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < NUM_NUBES; i++) {
            nubeX[i] -= nubeSpd[i] * delta;
            if (nubeX[i] + nubeW[i] < 0) nubeX[i] = W + 20;
            dibujarNube(nubeX[i], nubeY[i], nubeW[i]);
        }
        shape.end();

        // piso
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PISO);
        shape.rect(0, Constantes.Y_PISO - 4, W, 4f);
        shape.end();

        // cactus de fondo grises
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.setColor(0.78f, 0.78f, 0.78f, 1f);
        for (float cx : cactusDecoX) {
            batch.draw(assets.cactusChico1, cx, Constantes.Y_PISO);
        }
        batch.setColor(COL_BLANCO);

        // dino animado caminando
        com.badlogic.gdx.graphics.Texture dinoTex =
                ((int) dinoAnimTick % 12 < 6) ? assets.dinoMov1 : assets.dinoMov2;
        batch.draw(dinoTex, 300f, Constantes.Y_PISO);

        // HI score decorativo
        fontMedia.setColor(COL_GRIS);
        fontMedia.getData().setScale(1.45f);
        String hiStr = "HI  00000";
        layout.setText(fontMedia, hiStr);
        fontMedia.draw(batch, hiStr, W - layout.width - 24, H - 24);

        // título
        fontGrande.setColor(COL_NEGRO);
        fontGrande.getData().setScale(3.5f);
        centrarTexto(fontGrande, "DINO CHROME", W, H - 88);

        batch.end();

        // línea bajo el título
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_PISO);
        shape.rect(W / 2f - 210, H - 148, 420, 2.5f);
        shape.end();

        batch.begin();

        // subtítulo
        fontChica.setColor(COL_GRIS);
        fontChica.getData().setScale(1.25f);
        centrarTexto(fontChica, "MULTIJUGADOR  —  CLIENTE", W, H - 170);

        // instrucción central parpadeante
        float alpha = 0.5f + 0.5f * (float) Math.abs(Math.sin(tiempo * 3.0f));
        fontMedia.setColor(COL_NEGRO.r, COL_NEGRO.g, COL_NEGRO.b, alpha);
        fontMedia.getData().setScale(1.55f);
        centrarTexto(fontMedia, "Presiona  ENTER  para conectar", W, H / 2f + 20f);

        // descripción debajo
        fontChica.setColor(COL_GRIS_OSC);
        fontChica.getData().setScale(1.15f);
        centrarTexto(fontChica, "Se buscara un servidor en la red local", W, H / 2f - 22f);

        // controles (esquina inferior)
        fontChica.setColor(COL_GRIS);
        fontChica.getData().setScale(1.05f);
        centrarTexto(fontChica, "Controles:  ARRIBA = saltar    ABAJO = agacharse    R = reiniciar", W, 40f);

        batch.end();
    }

    private void dibujarNube(float x, float y, float w) {
        float h = w * 0.40f;
        shape.setColor(COL_NUBE);
        shape.rect(x + w * 0.15f, y,             w * 0.70f, h * 0.52f);
        shape.rect(x + w * 0.30f, y + h * 0.42f, w * 0.40f, h * 0.36f);
        shape.rect(x,             y + h * 0.14f, w * 0.20f, h * 0.34f);
        shape.rect(x + w * 0.80f, y + h * 0.14f, w * 0.20f, h * 0.34f);
    }

    private void centrarTexto(BitmapFont font, String texto, float areaW, float y) {
        layout.setText(font, texto);
        font.draw(batch, texto, (areaW - layout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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