package com.dinosauriojuego.network;

import java.util.ArrayList;

/*** Estado del cliente - Almacena toda la información sincronizada del servidor*/
public class ClienteEstado {

    public static boolean conectado = false;
    public static boolean juegoIniciado = false;

    // Contador de ticks para sincronización
    public static int tick = 0;
    public static int puntuacion = 0;

    // Estado del juego
    public static boolean juegoTerminado = false;
    public static String mensajeFinJuego = "";

    // Contador de jugadores listos para reiniciar
    public static int jugadoresListosReset = 0;

    // Highscore
    public static int highscore = 0;

    /*** Estado de un dinosaurio*/
    public static class EstadoDinosaurio {
        public float y;
        public boolean enSuelo;
        public boolean agachado;
        public boolean vivo;
        public int spriteActual; // Para animación
    }

    /*** Estado de un obstáculo*/
    public static class EstadoObstaculo {
        public int tipo;
        public int variante;
        public float x;
        public float y;
        public int spriteActual;
    }

    // Estados de los dos jugadores
    public static final EstadoDinosaurio jugador1 = new EstadoDinosaurio();
    public static final EstadoDinosaurio jugador2 = new EstadoDinosaurio();

    // Lista de obstáculos sincronizada
    public static final ArrayList<EstadoObstaculo> obstaculos = new ArrayList<>();

    /*** Reinicia el estado del cliente*/
    public static void reset() {
        tick = 0;
        puntuacion = 0;
        juegoTerminado = false;
        mensajeFinJuego = "";
        jugadoresListosReset = 0;

        jugador1.y = 0;
        jugador1.enSuelo = true;
        jugador1.agachado = false;
        jugador1.vivo = true;
        jugador1.spriteActual = 0;

        jugador2.y = 0;
        jugador2.enSuelo = true;
        jugador2.agachado = false;
        jugador2.vivo = true;
        jugador2.spriteActual = 0;

        synchronized (obstaculos) {
            obstaculos.clear();
        }
    }
}