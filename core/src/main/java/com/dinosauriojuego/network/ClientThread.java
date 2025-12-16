package com.dinosauriojuego.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.dinosauriojuego.pantallas.DinosaurioGameScreen;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * ClientThread - Versión CLIENTE
 * Maneja la comunicación del cliente con el servidor
 */
public class ClientThread extends Thread {

    private DatagramSocket socket;
    private int serverPort = 9999;
    private String ipServerStr = "255.255.255.255";
    private InetAddress ipServer;
    private boolean end = false;
    private DinosaurioGameScreen gameController;

    public ClientThread(DinosaurioGameScreen gameController) {
        try {
            this.gameController = gameController;
            ipServer = InetAddress.getByName(ipServerStr);
            socket = new DatagramSocket();
            socket.setSoTimeout(0); // Sin timeout

            System.out.println("🌐 Cliente creado. Servidor: " + ipServerStr + ":" + serverPort);
        } catch (SocketException | UnknownHostException e) {
            System.err.println("❌ Error al crear cliente: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("🔄 Cliente escuchando mensajes del servidor...");

        while (!end) {
            DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
            try {
                socket.receive(packet);
                processMessage(packet);
            } catch (IOException e) {
                if (!end) {
                    System.err.println("❌ Error al recibir paquete: " + e.getMessage());
                }
            }
        }

        System.out.println("🔴 Cliente desconectado");
    }

    private void processMessage(DatagramPacket packet) {
        String message = (new String(packet.getData())).trim();
        String[] parts = message.split(":");

        System.out.println("📨 Servidor: " + message);

        switch (parts[0]) {
            case "AlreadyConnected":
                System.out.println("⚠️ Ya estás conectado al servidor");
                break;

            case "Connected":
                // Connected:numPlayer
                if (parts.length >= 2) {
                    int numPlayer = Integer.parseInt(parts[1]);
                    System.out.println("✅ Conectado como jugador " + numPlayer);
                    this.ipServer = packet.getAddress();
                   // gameController.connect(numPlayer);
                }
                break;

            case "Full":
                System.out.println("❌ Servidor lleno");
                this.end = true;
                break;

            case "Start":
                System.out.println("🎮 ¡Juego iniciado!");
                // gameController.start();
                break;



            case "ForceDisconnect":

                System.out.println("🔴 Servidor forzó desconexión - Volviendo al menú");

                this.end = true; // ✅ Detener el hilo primero

                Gdx.app.postRunnable(() -> {
                    // gameController.backToMenu();
                });
                break;

            case "Disconnect":
                System.out.println("🔌 Servidor desconectado");
                this.end = true; // ✅ Detener el hilo primero

                Gdx.app.postRunnable(() -> {
                    // gameController.backToMenu();
                });
                break;

            case "NotConnected":
                System.out.println("⚠️ No estás conectado al servidor");
                break;



            case "GameOver":
                Gdx.app.postRunnable(() -> {
                  //   gameController.showGameOver();
                });
                break;



            default:
                System.out.println("⚠️ Mensaje desconocido: " + parts[0]);
                break;
        }
    }

    public void sendMessage(String message) {
        if (socket == null || socket.isClosed()) {
            System.err.println("⚠️ Socket cerrado, no se puede enviar: " + message);
            return;
        }

        byte[] byteMessage = message.getBytes();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, ipServer, serverPort);

        try {
            socket.send(packet);
            // System.out.println("📤 Enviado: " + message);
        } catch (IOException e) {
            System.err.println("❌ Error al enviar mensaje: " + e.getMessage());
        }
    }

    public void terminate() {
        System.out.println("🛑 Terminando cliente...");

        this.end = true;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        this.interrupt();
    }

    public void setServerIp(String ip) {
        this.ipServerStr = ip;
        try {
            this.ipServer = InetAddress.getByName(ip);
            System.out.println("🌐 IP del servidor actualizada a: " + ip);
        } catch (UnknownHostException e) {
            System.err.println("❌ IP inválida: " + e.getMessage());
        }
    }

    public void setServerPort(int port) {
        this.serverPort = port;
        System.out.println("🔌 Puerto del servidor actualizado a: " + port);
    }
}