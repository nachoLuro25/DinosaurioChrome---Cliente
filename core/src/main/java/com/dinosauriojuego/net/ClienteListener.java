package com.dinosauriojuego.net;
//interfaz para escuchar eventos del cliente de red
public interface ClienteListener {
    void onConectado(); //cuando se conecta al servidor
    void onEmpieza(); //cuando empieza el juego
    void onSnapshot(); //cuando se recibe un snapshot del estado del juego
}
