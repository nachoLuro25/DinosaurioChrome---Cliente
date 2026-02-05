package com.dinosauriojuego.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.dinosauriojuego.DinosaurioChromePrincipal;

/**
 * Pantalla de menú principal con opción de juego local y multijugador
 */
public class MenuScreen implements Screen {
    private DinosaurioChromePrincipal game;
    private Stage stage;
    private Skin skin;

    public MenuScreen(DinosaurioChromePrincipal game, Skin skin) {
        this.game = game;
        this.skin = skin;
        this.stage = new Stage(new FitViewport(1200, 720));

        crearMenu();
    }

    private void crearMenu() {
        // Título del juego
        Label titulo = new Label("DINOSAURIO CHROME", skin, "default");
        titulo.setFontScale(6.0f);
        titulo.setColor(Color.BLACK);
        titulo.setPosition(
                (1200 - titulo.getWidth() * 6.0f) / 2,
                550
        );
        stage.addActor(titulo);

        // Botón JUGAR (Local)
        TextButton botonJugar = new TextButton("JUGAR LOCAL", skin, "default");
        botonJugar.getLabel().setFontScale(3.0f);
        botonJugar.setSize(400, 80);
        botonJugar.setPosition(
                (1200 - 400) / 2,
                380
        );
        botonJugar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.iniciarJuego();
            }
        });
        stage.addActor(botonJugar);

        // Botón MULTIJUGADOR
        TextButton botonMultijugador = new TextButton("MULTIJUGADOR", skin, "default");
        botonMultijugador.getLabel().setFontScale(3.0f);
        botonMultijugador.setSize(400, 80);
        botonMultijugador.setPosition(
                (1200 - 400) / 2,
                280
        );
        botonMultijugador.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.iniciarMultijugador();
            }
        });
        stage.addActor(botonMultijugador);

        // Botón SALIR
        TextButton botonSalir = new TextButton("SALIR", skin, "default");
        botonSalir.getLabel().setFontScale(3.0f);
        botonSalir.setSize(400, 80);
        botonSalir.setPosition(
                (1200 - 400) / 2,
                180
        );
        botonSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        stage.addActor(botonSalir);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
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
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}