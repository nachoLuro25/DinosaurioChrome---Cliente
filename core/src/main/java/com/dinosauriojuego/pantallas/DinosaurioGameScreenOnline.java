package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dinosauriojuego.network.ClienteEstado;
import com.dinosauriojuego.network.ClienteListener;
import com.dinosauriojuego.network.HiloClienteDino;

import java.util.ArrayList;

/*** Pantalla de juego online - Renderiza el estado recibido del servidor*/
public class DinosaurioGameScreenOnline implements Screen, ClienteListener {
    private static final float GAME_WIDTH = 1200;
    private static final float GAME_HEIGHT = 360;

    private HiloClienteDino clienteRed;

    private OrthographicCamera cameraJugador1;
    private OrthographicCamera cameraJugador2;
    private Viewport viewportJugador1;
    private Viewport viewportJugador2;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;

    // Texturas - Dinosaurios
    private Texture dinoCyan1, dinoCyan2;
    private Texture dinoOrange1, dinoOrange2;
    private Texture dinoAgachado1, dinoAgachado2;
    private Texture dinoMuerto;

    // Texturas - Obstáculos
    private Texture cactusTexture;
    private Texture pajaro1Texture, pajaro2Texture;

    // Textura de fondo
    private Texture fondoTexture;
    private float fondoOffsetJ1, fondoOffsetJ2;
    private static final float VELOCIDAD_FONDO = 140f;

    // Textura del botón reiniciar
    private Texture botonReiniciarTexture;

    // Sonidos
    private Sound sonidoSalto;
    private Sound sonidoMuerte;

    // UI
    private Stage stageJugador1;
    private Stage stageJugador2;
    private Stage stageGlobal;
    private Skin skin;

    private Label jugador1Label;
    private Label jugador2Label;
    private Label puntuacionLabel;
    private Label highscoreLabel;
    private Label mensajeFinLabel;
    private Label reiniciarLabel;

    // Control de botón reiniciar
    private float botonReiniciarX, botonReiniciarY;
    private float botonReiniciarAncho, botonReiniciarAlto;

    // Estado local
    private boolean resetEnviado = false;
    private boolean highscoreGuardado = false;
    private boolean sonidoMuerteReproducido = false;

    // Preferencias
    private Preferences preferencias;

    // Control de inputs anteriores
    private boolean saltoPrevio = false;

    public DinosaurioGameScreenOnline(Skin skin, HiloClienteDino clienteRed) {
        this.skin = skin;
        this.clienteRed = clienteRed;

        // Configurar cámaras y viewports
        this.cameraJugador1 = new OrthographicCamera();
        this.cameraJugador1.setToOrtho(false, GAME_WIDTH, GAME_HEIGHT);

        this.cameraJugador2 = new OrthographicCamera();
        this.cameraJugador2.setToOrtho(false, GAME_WIDTH, GAME_HEIGHT);

        this.viewportJugador1 = new FitViewport(GAME_WIDTH, GAME_HEIGHT, cameraJugador1);
        this.viewportJugador2 = new FitViewport(GAME_WIDTH, GAME_HEIGHT, cameraJugador2);

        this.shapeRenderer = new ShapeRenderer();
        this.batch = new SpriteBatch();

        this.stageJugador1 = new Stage(viewportJugador1);
        this.stageJugador2 = new Stage(viewportJugador2);
        this.stageGlobal = new Stage(new FitViewport(GAME_WIDTH, 720));

        this.fondoOffsetJ1 = 0;
        this.fondoOffsetJ2 = 0;

        cargarRecursos();
        setupUI();

        // Cargar highscore
        preferencias = Gdx.app.getPreferences("dinosaurio_chrome_online");
        ClienteEstado.highscore = preferencias.getInteger("highscore", 0);
    }

    private void cargarRecursos() {
        try {
            dinoCyan1 = new Texture(Gdx.files.internal("dino1.png"));
            dinoCyan2 = new Texture(Gdx.files.internal("dino2.png"));
            dinoOrange1 = new Texture(Gdx.files.internal("dino1.png"));
            dinoOrange2 = new Texture(Gdx.files.internal("dino2.png"));
            dinoAgachado1 = new Texture(Gdx.files.internal("dinoAgachado1.png"));
            dinoAgachado2 = new Texture(Gdx.files.internal("dinoAgachado2.png"));

            cactusTexture = new Texture(Gdx.files.internal("cactus.png"));
            pajaro1Texture = new Texture(Gdx.files.internal("pajaro1.png"));
            pajaro2Texture = new Texture(Gdx.files.internal("pajaro2.png"));

            fondoTexture = new Texture(Gdx.files.internal("fondo.png"));
            fondoTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

            botonReiniciarTexture = new Texture(Gdx.files.internal("reiniciar.png"));

            sonidoSalto = Gdx.audio.newSound(Gdx.files.internal("sonidoSalto.ogg"));
            sonidoMuerte = Gdx.audio.newSound(Gdx.files.internal("sonidoMuerte.ogg"));
        } catch (Exception e) {
            System.out.println("⚠️ No se pudieron cargar algunas texturas o sonidos");
        }
    }

    private void setupUI() {
        // Labels para cada jugador
        jugador1Label = new Label("JUGADOR 1", skin, "default");
        jugador1Label.setFontScale(2.5f);
        jugador1Label.setPosition(20, 20);
        stageJugador1.addActor(jugador1Label);

        jugador2Label = new Label("JUGADOR 2", skin, "default");
        jugador2Label.setFontScale(2.5f);
        jugador2Label.setPosition(20, 20);
        stageJugador2.addActor(jugador2Label);

        // Labels globales
        puntuacionLabel = new Label("", skin, "default");
        puntuacionLabel.setFontScale(2.0f);
        puntuacionLabel.setPosition(20, 680);
        stageGlobal.addActor(puntuacionLabel);

        highscoreLabel = new Label("", skin, "default");
        highscoreLabel.setFontScale(2.0f);
        highscoreLabel.setPosition(20, 640);
        stageGlobal.addActor(highscoreLabel);

        mensajeFinLabel = new Label("", skin, "default");
        mensajeFinLabel.setFontScale(5.0f);
        mensajeFinLabel.setVisible(false);
        stageGlobal.addActor(mensajeFinLabel);

        reiniciarLabel = new Label("", skin, "default");
        reiniciarLabel.setFontScale(2.0f);
        reiniciarLabel.setVisible(false);
        stageGlobal.addActor(reiniciarLabel);

        // Configurar botón reiniciar
        botonReiniciarAncho = 120;
        botonReiniciarAlto = 120;
        botonReiniciarX = (GAME_WIDTH - botonReiniciarAncho) / 2;
        botonReiniciarY = 280;

        Gdx.input.setInputProcessor(stageGlobal);
    }

    @Override
    public void show() {
        clienteRed.setListener(this);
    }

    @Override
    public void render(float delta) {
        // Capturar inputs
        boolean saltoPresionado = Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
                Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean agacharPresionado = Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                Gdx.input.isKeyPressed(Input.Keys.S);

        if (!ClienteEstado.juegoTerminado) {
            clienteRed.enviarInput(saltoPresionado, agacharPresionado);

            // Reproducir sonido de salto localmente
            if (saltoPresionado && !saltoPrevio && sonidoSalto != null) {
                sonidoSalto.play(1.0f);
            }

            saltoPrevio = saltoPresionado;
        }

        // Manejar reinicio
        if (ClienteEstado.juegoTerminado) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !resetEnviado) {
                clienteRed.enviarReset();
                resetEnviado = true;
            }

            // Reproducir sonido de muerte una vez
            if (!sonidoMuerteReproducido && sonidoMuerte != null) {
                sonidoMuerte.play(1.0f);
                sonidoMuerteReproducido = true;
            }

            // Guardar highscore
            if (!highscoreGuardado && ClienteEstado.puntuacion > ClienteEstado.highscore) {
                ClienteEstado.highscore = ClienteEstado.puntuacion;
                preferencias.putInteger("highscore", ClienteEstado.highscore).flush();
                highscoreGuardado = true;
            }
        } else {
            resetEnviado = false;
            highscoreGuardado = false;
            sonidoMuerteReproducido = false;
        }

        // Actualizar offset del fondo
        if (!ClienteEstado.juegoTerminado) {
            fondoOffsetJ1 += VELOCIDAD_FONDO * delta;
            fondoOffsetJ2 += VELOCIDAD_FONDO * delta;
        }

        // Copiar obstáculos de forma segura
        ArrayList<ClienteEstado.EstadoObstaculo> obstaculosCopia;
        synchronized (ClienteEstado.obstaculos) {
            obstaculosCopia = new ArrayList<>(ClienteEstado.obstaculos);
        }

        // Actualizar UI
        actualizarUI();

        // Renderizar
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderJuego(ClienteEstado.jugador1, viewportJugador1, cameraJugador1, stageJugador1,
                0, 360, Color.CYAN, dinoCyan1, dinoCyan2, fondoOffsetJ1, obstaculosCopia);
        renderJuego(ClienteEstado.jugador2, viewportJugador2, cameraJugador2, stageJugador2,
                0, 0, Color.ORANGE, dinoOrange1, dinoOrange2, fondoOffsetJ2, obstaculosCopia);

        // Línea divisoria
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.setProjectionMatrix(stageGlobal.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 358, GAME_WIDTH, 4);
        shapeRenderer.end();

        // UI global
        stageGlobal.act(delta);
        stageGlobal.draw();

        // Botón de reiniciar si el juego terminó
        if (ClienteEstado.juegoTerminado) {
            renderBotonReiniciar();
        }
    }

    private void actualizarUI() {
        // Determinar si es modo noche
        boolean modoNoche = (ClienteEstado.puntuacion / 500) % 2 == 1;
        Color colorTexto = modoNoche ? Color.WHITE : Color.BLACK;

        jugador1Label.setColor(colorTexto);
        jugador2Label.setColor(colorTexto);

        puntuacionLabel.setText("Puntuación: " + ClienteEstado.puntuacion);
        puntuacionLabel.setColor(colorTexto);

        highscoreLabel.setText("Mejor: " + ClienteEstado.highscore);
        highscoreLabel.setColor(colorTexto);

        if (ClienteEstado.juegoTerminado) {
            mensajeFinLabel.setText(ClienteEstado.mensajeFinJuego);
            mensajeFinLabel.setColor(colorTexto);
            mensajeFinLabel.setVisible(true);
            mensajeFinLabel.pack();
            mensajeFinLabel.setPosition((GAME_WIDTH - mensajeFinLabel.getWidth()) / 2, 520);

            reiniciarLabel.setText("R para reiniciar (" + ClienteEstado.jugadoresListosReset + "/2)");
            reiniciarLabel.setColor(colorTexto);
            reiniciarLabel.setVisible(true);
            reiniciarLabel.pack();
            reiniciarLabel.setPosition((GAME_WIDTH - reiniciarLabel.getWidth()) / 2, 450);
        } else {
            mensajeFinLabel.setVisible(false);
            reiniciarLabel.setVisible(false);
        }
    }

    private void renderJuego(ClienteEstado.EstadoDinosaurio dino, Viewport viewport,
                             OrthographicCamera camera, Stage stage, int offsetX, int offsetY,
                             Color colorJugador, Texture dinoTex1, Texture dinoTex2,
                             float fondoOffset, ArrayList<ClienteEstado.EstadoObstaculo> obstaculos) {

        // Determinar colores según modo día/noche
        boolean modoNoche = (ClienteEstado.puntuacion / 500) % 2 == 1;
        Color colorFondo = modoNoche ? new Color(0.05f, 0.05f, 0.1f, 1) : new Color(0.9f, 0.9f, 0.95f, 1);

        viewport.apply();
        viewport.setScreenBounds(offsetX, offsetY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() / 2);

        // Limpiar con color de fondo
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(offsetX, offsetY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() / 2);
        Gdx.gl.glClearColor(colorFondo.r, colorFondo.g, colorFondo.b, colorFondo.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Dibujar fondo scrolleando
        if (fondoTexture != null) {
            float fondoAncho = fondoTexture.getWidth();
            float fondoAlto = 60;
            int repeticiones = (int) Math.ceil(GAME_WIDTH / fondoAncho) + 2;
            float offsetNormalizado = fondoOffset % fondoAncho;

            for (int i = -1; i < repeticiones; i++) {
                float x = i * fondoAncho - offsetNormalizado;
                batch.draw(fondoTexture, x, 22, fondoAncho, fondoAlto);
            }
        }

        // Dibujar dinosaurio
        Texture texturaDino = elegirTexturaDino(dino);
        if (texturaDino != null) {
            batch.setColor(colorJugador);
            // Altura fija para dinosaurio
            float altoDino = dino.agachado ? 30 : 60;  // Mitad de altura cuando está agachado
            float anchoDino = 50;
            batch.draw(texturaDino, 50, dino.y, anchoDino, altoDino);
            batch.setColor(Color.WHITE);
        }

        // Dibujar obstáculos
        for (ClienteEstado.EstadoObstaculo obs : obstaculos) {
            if (obs.tipo == 0) { // Cactus
                if (cactusTexture != null) {
                    batch.draw(cactusTexture, obs.x, obs.y, 30, 50);
                }
            } else { // Pájaro
                Texture pajaroTex = (ClienteEstado.tick % 12 < 6) ? pajaro1Texture : pajaro2Texture;
                if (pajaroTex != null) {
                    batch.draw(pajaroTex, obs.x, obs.y, 50, 25);
                }
            }
        }

        batch.end();

        // UI del jugador
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    private Texture elegirTexturaDino(ClienteEstado.EstadoDinosaurio dino) {
        if (!dino.vivo && dinoMuerto != null) {
            return dinoMuerto;
        }

        if (!dino.enSuelo) {
            return dinoCyan1; // Quieto cuando está en el aire
        }

        if (dino.agachado) {
            return (ClienteEstado.tick % 12 < 6) ? dinoAgachado1 : dinoAgachado2;
        }

        return (ClienteEstado.tick % 12 < 6) ? dinoCyan1 : dinoCyan2;
    }

    private void renderBotonReiniciar() {
        if (botonReiniciarTexture == null) return;

        batch.setProjectionMatrix(stageGlobal.getCamera().combined);
        batch.begin();
        batch.draw(botonReiniciarTexture, botonReiniciarX, botonReiniciarY,
                botonReiniciarAncho, botonReiniciarAlto);
        batch.end();

        // Detectar clic en el botón
        if (Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            float factorX = GAME_WIDTH / (float) Gdx.graphics.getWidth();
            float factorY = 720 / (float) Gdx.graphics.getHeight();
            mouseX *= factorX;
            mouseY *= factorY;

            if (mouseX >= botonReiniciarX && mouseX <= botonReiniciarX + botonReiniciarAncho &&
                    mouseY >= botonReiniciarY && mouseY <= botonReiniciarY + botonReiniciarAlto) {
                if (!resetEnviado) {
                    clienteRed.enviarReset();
                    resetEnviado = true;
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewportJugador1.update(width, height / 2, true);
        viewportJugador2.update(width, height / 2, true);
        stageGlobal.getViewport().update(width, height, true);
    }

    // Implementación de ClienteListener
    @Override
    public void onConectado() {
        System.out.println("✅ Conectado (desde pantalla de juego)");
    }

    @Override
    public void onJuegoIniciado() {
        System.out.println("🎮 Juego iniciado (desde pantalla de juego)");
    }

    @Override
    public void onSnapshotRecibido() {
    }

    @Override
    public void onDesconectado() {
        System.out.println("🔌 Desconectado del servidor");
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        stageJugador1.dispose();
        stageJugador2.dispose();
        stageGlobal.dispose();

        if (dinoCyan1 != null) dinoCyan1.dispose();
        if (dinoCyan2 != null) dinoCyan2.dispose();
        if (dinoOrange1 != null) dinoOrange1.dispose();
        if (dinoOrange2 != null) dinoOrange2.dispose();
        if (dinoAgachado1 != null) dinoAgachado1.dispose();
        if (dinoAgachado2 != null) dinoAgachado2.dispose();
        if (cactusTexture != null) cactusTexture.dispose();
        if (pajaro1Texture != null) pajaro1Texture.dispose();
        if (pajaro2Texture != null) pajaro2Texture.dispose();
        if (fondoTexture != null) fondoTexture.dispose();
        if (botonReiniciarTexture != null) botonReiniciarTexture.dispose();
        if (sonidoSalto != null) sonidoSalto.dispose();
        if (sonidoMuerte != null) sonidoMuerte.dispose();
    }
}