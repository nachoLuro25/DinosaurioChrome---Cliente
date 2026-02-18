package com.dinosauriojuego.net;

import java.util.ArrayList;

//guarda el ultimo estado recibido del servidor para que el hilo de render lo pueda leer
public class ClienteEstado {

    public static boolean conectado = false;
    public static boolean empezo   = false;

    //contador de ticks para saber si llego un estado nuevo del servidor
    public static int tick  = 0;
    public static int score = 0;

    public static boolean terminado  = false;
    public static String  mensajeFin = "";

    public static int resetReadyCount = 0;

    public static int highscore = 0;

    //estado de un dinosaurio (posicion, si esta en el piso, agachado, vivo)
    public static class DinoState {
        public float   y;
        public boolean enPiso;
        public boolean agachado;
        public boolean vivo;
    }

    //estado de un obstaculo (tipo, variante, posicion)
    public static class ObstacleState {
        public int   type;
        public int   variant;
        public float x;
        public float y;
    }

    public static final DinoState p1 = new DinoState();
    public static final DinoState p2 = new DinoState();

    // cada pista tiene su propia lista de obstaculos
    // se usan synchronized al leer/escribir para evitar conflictos entre hilos
    public static final ArrayList<ObstacleState> obstaculosP1 = new ArrayList<>();
    public static final ArrayList<ObstacleState> obstaculosP2 = new ArrayList<>();
}