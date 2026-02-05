package com.dinosauriojuego.network;

/**
 * Interfaz para escuchar eventos del cliente de red
 */
public interface ClienteListener {

    /**
     * Se llama cuando el cliente se conecta exitosamente al servidor
     */
    void onConectado();

    /**
     * Se llama cuando el juego comienza (ambos jugadores listos)
     */
    void onJuegoIniciado();

    /**
     * Se llama cuando se recibe un snapshot actualizado del servidor
     */
    void onSnapshotRecibido();

    /**
     * Se llama cuando el servidor fuerza una desconexión
     */
    void onDesconectado();
}