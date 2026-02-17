package com.dinosauriojuego.net;

import java.util.ArrayList;
//inputs recibidos del servidor
public class ClienteEstado {

    public static boolean conectado = false;
    public static boolean empezo = false;
    //contador de ticks para sincronizar estados entre cliente y servidor y saber si hay estados nuevos
    public static int tick = 0;
    public static int score = 0;

    public static boolean terminado = false;
    public static String mensajeFin = "";

    public static int resetReadyCount = 0;


    public static int highscore = 0;
    //estados de los dinosaurios
    public static class DinoState {
        public float y;
        public boolean enPiso;
        public boolean agachado;
        public boolean vivo;
    }
    //estados de los obstaculos
    public static class ObstacleState {
        public int type;
        public int variant;
        public float x;
        public float y;
    }

    public static final DinoState p1 = new DinoState();
    public static final DinoState p2 = new DinoState();

    public static final ArrayList<ObstacleState> obstacles = new ArrayList<>();
}
