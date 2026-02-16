package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.dinosauriojuego.DinosaurioChromePrincipal;
import com.dinosauriojuego.network.ClienteEstado;
import com.dinosauriojuego.network.ClienteListener;
import com.dinosauriojuego.network.HiloClienteDino;

/**
 * Pantalla de espera para el primer jugador que se une
 */
public class PantallaEspera implements Screen, ClienteListener {

    private DinosaurioChromePrincipal game;
    private Skin skin;
    private HiloClienteDino clienteRed;

    private Stage stage;
    private Label mensajeLabel;
    private Label estadoLabel;

    private float tiempoAnimacion;

    public PantallaEspera(DinosaurioChromePrincipal game, Skin skin, HiloClienteDino clienteRed) {
        this.game = game;
        this.skin = skin;
        this.clienteRed = clienteRed;
        this.stage = new Stage(new FitViewport(1200, 720));
        this.tiempoAnimacion = 0;

        setupUI();
    }

    private void setupUI() {
        // Mensaje principal
        mensajeLabel = new Label("Esperando al otro jugador", skin, "default");
        mensajeLabel.setFontScale(4.0f);
        mensajeLabel.setColor(Color.BLACK);
        mensajeLabel.setPosition(
                (1200 - mensajeLabel.getWidth() * 4.0f) / 2,
                400
        );
        stage.addActor(mensajeLabel);

        // Estado de conexión
        estadoLabel = new Label("", skin, "default");
        estadoLabel.setFontScale(2.0f);
        estadoLabel.setColor(Color.DARK_GRAY);
        estadoLabel.setPosition(
                (1200 - estadoLabel.getWidth() * 2.0f) / 2,
                300
        );
        stage.addActor(estadoLabel);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // Configurar listener y enviar mensaje de listo
        clienteRed.setListener(this);
        clienteRed.enviarListo();

        actualizarEstado();
    }

    private void actualizarEstado() {
        if (ClienteEstado.conectado) {
            estadoLabel.setText("Conectado al servidor");
        } else {
            estadoLabel.setText("Conectando...");
        }

        // Reposicionar el label de estado
        estadoLabel.pack();
        estadoLabel.setPosition(
                (1200 - estadoLabel.getWidth() * 2.0f) / 2,
                300
        );
    }

    @Override
    public void render(float delta) {
        // Si el juego ya comenzó, cambiar a la pantalla de juego online
        if (ClienteEstado.juegoIniciado) {
            game.setScreen(new DinosaurioGameScreenOnline(skin, clienteRed));
            return;
        }

        // Animación de puntos suspensivos
        tiempoAnimacion += delta;
        if (tiempoAnimacion >= 0.5f) {
            tiempoAnimacion = 0;
            String texto = mensajeLabel.getText().toString();
            if (texto.endsWith("...")) {
                mensajeLabel.setText("Esperando al otro jugador");
            } else if (texto.endsWith("..")) {
                mensajeLabel.setText("Esperando al otro jugador...");
            } else if (texto.endsWith(".")) {
                mensajeLabel.setText("Esperando al otro jugador..");
            } else {
                mensajeLabel.setText("Esperando al otro jugador.");
            }

            mensajeLabel.pack();
            mensajeLabel.setPosition(
                    (1200 - mensajeLabel.getWidth() * 4.0f) / 2,
                    400
            );
        }

        // Renderizar
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.95f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void onConectado() {
        System.out.println("✅ Evento: Conectado al servidor");
        actualizarEstado();
    }

    @Override
    public void onJuegoIniciado() {
        System.out.println("🎮 Evento: Juego iniciado");
        // El cambio de pantalla se hace en render()
    }

    @Override
    public void onSnapshotRecibido() {
    }

    @Override
    public void onDesconectado() {
        System.out.println("🔌 Evento: Desconectado del servidor");
        game.volverAlMenu();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}