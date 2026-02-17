package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
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
import com.dinosauriojuego.net.ClienteEstado;
import com.dinosauriojuego.net.ClienteListener;
import com.dinosauriojuego.net.HiloClienteDino;
import com.dinosauriojuego.utiles.Assets;
import com.dinosauriojuego.utiles.Constantes;

public class PantallaEspera extends ScreenAdapter implements ClienteListener {

    private final Main           game;
    private final Assets         assets;
    private final HiloClienteDino net;

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

    // física local para la animación del dino (no disponible en Constantes del cliente)
    private static final float GRAVEDAD_LOBBY      = -1700f;
    private static final float VELOCIDAD_SALTO_LOBBY = 700f;

    // El dino "salta" en loop mientras espera
    private float dinoY   = Constantes.Y_PISO;
    private float dinoVY  = 0f;
    private boolean saltando = false;
    private float timerSalto = 0f;

    // nubes
    private static final int NUM_NUBES = 6;
    private float[] nubeX   = new float[NUM_NUBES];
    private float[] nubeY   = new float[NUM_NUBES];
    private float[] nubeSpd = new float[NUM_NUBES];
    private float[] nubeW   = new float[NUM_NUBES];

    // paleta Chrome
    private static final Color COL_BG      = new Color(0.95f, 0.95f, 0.95f, 1f);
    private static final Color COL_BLANCO  = new Color(1f,    1f,    1f,    1f);
    private static final Color COL_NUBE    = new Color(0.82f, 0.82f, 0.82f, 1f);
    private static final Color COL_PISO    = new Color(0.70f, 0.70f, 0.70f, 1f);
    private static final Color COL_GRIS    = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color COL_GRIS_OSC= new Color(0.38f, 0.38f, 0.38f, 1f);
    private static final Color COL_NEGRO   = new Color(0.20f, 0.20f, 0.20f, 1f);

    public PantallaEspera(Main game, Assets assets, HiloClienteDino net) {
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

        float[] alts  = { 430f, 470f, 510f, 400f, 445f, 485f };
        float[] anchs = { 155f, 115f, 195f, 135f, 175f, 108f };
        float[] spds  = {  28f,  22f,  18f,  32f,  25f,  20f };
        java.util.Random rng = new java.util.Random(99);
        for (int i = 0; i < NUM_NUBES; i++) {
            nubeX[i]   = rng.nextFloat() * Constantes.ANCHO_VIRTUAL;
            nubeY[i]   = alts[i];
            nubeSpd[i] = spds[i];
            nubeW[i]   = anchs[i];
        }

        net.setListener(this);
        net.sendListo();
    }

    @Override
    public void render(float delta) {
        tiempo       += delta;
        dinoAnimTick += delta * 60f;

        // si ya empezó, cambiar pantalla
        if (ClienteEstado.empezo) {
            game.setScreen(new PantallaJuegoOnline(game, assets, net));
            return;
        }

        float W = Constantes.ANCHO_VIRTUAL;
        float H = Constantes.ALTO_VIRTUAL;

        // física del dino saltando en loop
        timerSalto += delta;
        if (!saltando && timerSalto > 1.4f) {
            dinoVY    = VELOCIDAD_SALTO_LOBBY;
            saltando  = true;
            timerSalto = 0f;
        }
        if (saltando) {
            dinoVY += GRAVEDAD_LOBBY * delta;
            dinoY  += dinoVY * delta;
            if (dinoY <= Constantes.Y_PISO) {
                dinoY    = Constantes.Y_PISO;
                dinoVY   = 0f;
                saltando = false;
            }
        }

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

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // dino saltando
        com.badlogic.gdx.graphics.Texture dinoTex;
        if (!saltando) {
            dinoTex = ((int) dinoAnimTick % 12 < 6) ? assets.dinoMov1 : assets.dinoMov2;
        } else {
            dinoTex = assets.dinoQuieto;
        }
        batch.draw(dinoTex, 560f, dinoY);

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

        // estado de conexión
        if (!ClienteEstado.conectado) {
            // buscando servidor: puntos animados
            int dots = (int)(tiempo * 2f) % 4;
            fontMedia.setColor(COL_GRIS_OSC);
            fontMedia.getData().setScale(1.5f);
            centrarTexto(fontMedia, "Buscando servidor" + ".".repeat(dots), W, H / 2f + 55f);

            fontChica.setColor(COL_GRIS);
            fontChica.getData().setScale(1.15f);
            centrarTexto(fontChica, "Busca un servidor en la red local por broadcast UDP", W, H / 2f + 14f);

        } else {
            // conectado, esperando al otro jugador
            fontMedia.setColor(COL_NEGRO);
            fontMedia.getData().setScale(1.5f);
            centrarTexto(fontMedia, "Conectado al servidor", W, H / 2f + 55f);

            int dots = (int)(tiempo * 2f) % 4;
            fontChica.setColor(COL_GRIS_OSC);
            fontChica.getData().setScale(1.2f);
            centrarTexto(fontChica, "Esperando al otro jugador" + ".".repeat(dots), W, H / 2f + 14f);
        }

        // indicador de estado de conexion (pequeño circulo)
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        float circX = W / 2f - 10f;
        float circY = H / 2f - 35f;
        if (ClienteEstado.conectado) shape.setColor(COL_NEGRO);
        else {
            float pulse = 0.45f + 0.45f * (float)Math.abs(Math.sin(tiempo * 4f));
            shape.setColor(COL_GRIS_OSC.r, COL_GRIS_OSC.g, COL_GRIS_OSC.b, pulse);
        }
        shape.circle(circX, circY, 7f);
        shape.end();

        batch.begin();
        fontChica.setColor(COL_GRIS);
        fontChica.getData().setScale(1.05f);
        fontChica.draw(batch, ClienteEstado.conectado ? "Online" : "Offline", circX + 16f, circY + 8f);
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

    @Override public void onConectado() {}
    @Override public void onEmpieza()   {}
    @Override public void onSnapshot()  {}

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