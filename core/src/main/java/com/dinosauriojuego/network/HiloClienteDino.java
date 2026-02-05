package com.dinosauriojuego.network;

import com.badlogic.gdx.Gdx;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Hilo del cliente para comunicación UDP con el servidor del juego
 * Basado en la arquitectura de MatiasLeanza con mejoras
 */
public class HiloClienteDino extends Thread {

    private DatagramSocket socket;
    private InetAddress ipServidor;
    private final int puertoServidor = 8999;

    private volatile boolean finalizar = false;
    private volatile ClienteListener listener;

    /**
     * Constructor - Inicializa el socket UDP y busca el servidor por broadcast
     */
    public HiloClienteDino() {
        try {
            // Usar broadcast para encontrar servidor en la red local
            ipServidor = InetAddress.getByName("255.255.255.255");
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(0); // Sin timeout

            System.out.println("🌐 Cliente UDP creado. Buscando servidor...");

            // Enviar mensaje inicial de conexión
            enviarMensaje("Conexion");
        } catch (Exception e) {
            System.err.println("❌ Error al crear cliente: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Establece el listener para eventos de red
     */
    public void setListener(ClienteListener listener) {
        this.listener = listener;

        // Si ya estamos conectados, notificar inmediatamente
        if (ClienteEstado.conectado) {
            notificarConectadoSeguro();
        }

        if (ClienteEstado.juegoIniciado) {
            notificarJuegoIniciadoSeguro();
        }
    }

    /**
     * Envía un mensaje al servidor
     */
    private void enviarMensaje(String mensaje) {
        try {
            byte[] datos = mensaje.getBytes(StandardCharsets.UTF_8);
            DatagramPacket paquete = new DatagramPacket(datos, datos.length, ipServidor, puertoServidor);
            socket.send(paquete);
            // System.out.println("📤 Enviado: " + mensaje);
        } catch (Exception e) {
            System.err.println("❌ Error al enviar mensaje: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("🔄 Cliente escuchando mensajes del servidor...");

        while (!finalizar) {
            try {
                byte[] buffer = new byte[8192];
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);
                procesarMensaje(paquete);
            } catch (Exception e) {
                if (!finalizar) {
                    System.err.println("⚠️ Error al recibir paquete: " + e.getMessage());
                }
            }
        }

        System.out.println("🔴 Cliente desconectado");
    }

    /**
     * Procesa los mensajes recibidos del servidor
     */
    private void procesarMensaje(DatagramPacket paquete) {
        String mensaje = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8).trim();

        // System.out.println("📨 Servidor: " + mensaje);

        // Mensaje de confirmación de conexión
        if (mensaje.equals("OK")) {
            ipServidor = paquete.getAddress();
            ClienteEstado.conectado = true;
            System.out.println("✅ Conectado al servidor: " + ipServidor);
            notificarConectadoSeguro();
            return;
        }

        // Mensaje de inicio de juego
        if (mensaje.equals("Empieza")) {
            ClienteEstado.juegoIniciado = true;
            System.out.println("🎮 ¡Juego iniciado!");
            notificarJuegoIniciadoSeguro();
            return;
        }

        // Mensaje de desconexión
        if (mensaje.equals("Desconectar") || mensaje.equals("ForceDisconnect")) {
            System.out.println("🔌 Servidor desconectado");
            finalizar = true;
            notificarDesconectadoSeguro();
            return;
        }

        // Snapshot del estado del juego
        if (mensaje.startsWith("SNAP;")) {
            boolean exitoso = parsearSnapshot(mensaje);
            if (exitoso) {
                notificarSnapshotSeguro();
            }
        }
    }

    /**
     * Parsea un snapshot recibido del servidor y actualiza el estado del cliente
     *
     * Formato del snapshot:
     * SNAP;tick;puntuacion;velocidad;juegoIniciado;juegoTerminado;mensajeFinJuego;jugadoresListosReset;
     * j1.y;j1.enSuelo;j1.agachado;j1.vivo;
     * j2.y;j2.enSuelo;j2.agachado;j2.vivo;
     * numObstaculos;[tipo;variante;x;y]...
     */
    private boolean parsearSnapshot(String mensaje) {
        try {
            String[] partes = mensaje.split(";");
            int indice = 0;

            // Verificar que sea un snapshot
            if (!"SNAP".equals(partes[indice++])) {
                return false;
            }

            // Parsear información general
            ClienteEstado.tick = Integer.parseInt(partes[indice++]);
            ClienteEstado.puntuacion = Integer.parseInt(partes[indice++]);

            // Velocidad (no la usamos en el cliente, pero está en el protocolo)
            indice++;

            ClienteEstado.juegoIniciado = Integer.parseInt(partes[indice++]) == 1;
            ClienteEstado.juegoTerminado = Integer.parseInt(partes[indice++]) == 1;

            ClienteEstado.mensajeFinJuego = (indice < partes.length) ? partes[indice++] : "";

            ClienteEstado.jugadoresListosReset = Integer.parseInt(partes[indice++]);

            // Parsear estado del jugador 1
            ClienteEstado.jugador1.y = Float.parseFloat(partes[indice++]);
            ClienteEstado.jugador1.enSuelo = Integer.parseInt(partes[indice++]) == 1;
            ClienteEstado.jugador1.agachado = Integer.parseInt(partes[indice++]) == 1;
            ClienteEstado.jugador1.vivo = Integer.parseInt(partes[indice++]) == 1;

            // Parsear estado del jugador 2
            ClienteEstado.jugador2.y = Float.parseFloat(partes[indice++]);
            ClienteEstado.jugador2.enSuelo = Integer.parseInt(partes[indice++]) == 1;
            ClienteEstado.jugador2.agachado = Integer.parseInt(partes[indice++]) == 1;
            ClienteEstado.jugador2.vivo = Integer.parseInt(partes[indice++]) == 1;

            // Parsear obstáculos
            int numObstaculos = Integer.parseInt(partes[indice++]);

            synchronized (ClienteEstado.obstaculos) {
                ClienteEstado.obstaculos.clear();

                for (int i = 0; i < numObstaculos; i++) {
                    ClienteEstado.EstadoObstaculo obstaculo = new ClienteEstado.EstadoObstaculo();
                    obstaculo.tipo = Integer.parseInt(partes[indice++]);
                    obstaculo.variante = Integer.parseInt(partes[indice++]);
                    obstaculo.x = Float.parseFloat(partes[indice++]);
                    obstaculo.y = Float.parseFloat(partes[indice++]);
                    ClienteEstado.obstaculos.add(obstaculo);
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Error al parsear snapshot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Notifica al listener de forma segura (en el hilo de LibGDX)
     */
    private void notificarConectadoSeguro() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) {
                l.onConectado();
            }
        });
    }

    private void notificarJuegoIniciadoSeguro() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) {
                l.onJuegoIniciado();
            }
        });
    }

    private void notificarSnapshotSeguro() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) {
                l.onSnapshotRecibido();
            }
        });
    }

    private void notificarDesconectadoSeguro() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) {
                l.onDesconectado();
            }
        });
    }

    // ==================== MÉTODOS PÚBLICOS PARA ENVIAR MENSAJES ====================

    /**
     * Envía mensaje indicando que el jugador está listo
     */
    public void enviarListo() {
        enviarMensaje("Listo");
    }

    /**
     * Envía los inputs del jugador al servidor
     * @param saltar true si el jugador quiere saltar
     * @param agachar true si el jugador quiere agacharse
     */
    public void enviarInput(boolean saltar, boolean agachar) {
        String mensaje = "INPUT;" + (saltar ? "1" : "0") + ";" + (agachar ? "1" : "0");
        enviarMensaje(mensaje);
    }

    /**
     * Envía solicitud de reinicio del juego
     */
    public void enviarReset() {
        enviarMensaje("RESET");
    }

    /**
     * Configura la IP del servidor manualmente
     */
    public void setIpServidor(String ip) {
        try {
            this.ipServidor = InetAddress.getByName(ip);
            System.out.println("🌐 IP del servidor actualizada: " + ip);
        } catch (Exception e) {
            System.err.println("❌ IP inválida: " + e.getMessage());
        }
    }

    /**
     * Cierra el cliente y libera recursos
     */
    public void cerrar() {
        System.out.println("🛑 Cerrando cliente...");

        finalizar = true;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        this.interrupt();
    }
}