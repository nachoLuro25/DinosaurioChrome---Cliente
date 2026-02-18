package com.dinosauriojuego.utiles;

public final class Constantes {

    private Constantes(){}

    public static final int ANCHO_VIRTUAL = 1280;
    public static final int ALTO_VIRTUAL  = 720;

    // Piso de cada pista (pantalla dividida en dos mitades)
    // P1 juega en la mitad SUPERIOR, P2 en la mitad INFERIOR
    public static final float Y_PISO_P1 = 430f; // piso de la pista superior (jugador 1)
    public static final float Y_PISO_P2 = 100f; // piso de la pista inferior (jugador 2)

    // Linea divisoria entre las dos pistas
    public static final float Y_DIVISOR = 310f;

    // X fija de cada dino
    public static final float X_JUGADOR_1 = 180f;
    public static final float X_JUGADOR_2 = 180f;
}