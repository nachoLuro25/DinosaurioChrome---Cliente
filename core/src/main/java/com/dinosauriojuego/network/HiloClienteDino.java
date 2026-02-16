package com.dinosauriojuego.network;

import com.badlogic.gdx.Gdx;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Hilo del cliente para comunicación UDP con el servidor del juego
 * Mantiene el mismo puerto para todos los mensajes*/
public class HiloClienteDino extends Thread {

    private DatagramSocket socket;
    private InetAddress ipServidor;
    private final int puertoServidor = 8999;
    private int puertoLocal; // ✅ Guardar el puerto local asignado

    private volatile boolean finalizar = false;
    private volatile ClienteListener listener;

    /*** Constructor - Inicializa el socket UDP y busca el servidor*/
    public HiloClienteDino() {
        super("ClienteUDP-Thread");
        try {
            // ✅ Crear socket con puerto fijo
            socket = new DatagramSocket();
            puertoLocal = socket.getLocalPort(); // ✅ Guardar el puerto asignado
            socket.setSoTimeout(100); // Timeout razonable

            System.out.println("🌐 Cliente UDP creado en puerto local: " + puertoLocal);

            // Intentar habilitar broadcast para encontrar servidor
            try {
                socket.setBroadcast(true);
                ipServidor = InetAddress.getByName("255.255.255.255");
                System.out.println("📡 Buscando servidor en broadcast...");
            } catch (java.net.SocketException e) {
                // Si broadcast falla, intentar con localhost
                System.err.println("⚠️ No se pudo habilitar broadcast: " + e.getMessage());
                System.out.println("📡 Buscando servidor en localhost...");
                ipServidor = InetAddress.getByName("localhost");
            }

            // Enviar mensaje inicial de conexión
            enviarMensaje("Conexion");

        } catch (Exception e) {
            System.err.println("❌ Error al crear cliente: " + e.getMessage());
            e.printStackTrace();
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

    /*** Envía un mensaje al servidor usando SIEMPRE el mismo socket*/
    private void enviarMensaje(String mensaje) {
        if (socket == null || socket.isClosed()) {
            System.err.println("⚠️ Socket cerrado, no se puede enviar: " + mensaje);
            return;
        }

        try {
            byte[] datos = mensaje.getBytes(StandardCharsets.UTF_8);
            DatagramPacket paquete = new DatagramPacket(datos, datos.length, ipServidor, puertoServidor);

            socket.send(paquete);

        } catch (Exception e) {
            if (!finalizar) {
                System.err.println("❌ Error al enviar mensaje: " + e.getMessage());
            }
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
            } catch (java.net.SocketTimeoutException e) {
                // Timeout normal, continuar
            } catch (Exception e) {
                if (!finalizar) {
                    System.err.println("⚠️ Error al recibir paquete: " + e.getMessage());
                }
            }
        }

        System.out.println("🔴 Cliente desconectado");
    }

    /*** Procesa los mensajes recibidos del servidor*/
    private void procesarMensaje(DatagramPacket paquete) {
        String mensaje = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8).trim();

        // Mensaje de confirmación de conexión
        if (mensaje.equals("OK")) {
            ipServidor = paquete.getAddress();

            try {
                socket.setBroadcast(false);
            } catch (java.net.SocketException e) {
                System.err.println("⚠️ Error al desactivar broadcast: " + e.getMessage());
            }

            ClienteEstado.conectado = true;
            System.out.println("✅ Conectado al servidor: " + ipServidor + " (desde puerto local " + puertoLocal + ")");
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

    /*** Notifica al listener de forma segura (en el hilo de LibGDX)*/
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


    /*** Envía mensaje indicando que el jugador está listo*/
    public void enviarListo() {
        enviarMensaje("Listo");
        System.out.println("📤 Enviando señal de listo desde puerto " + puertoLocal);
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

    /*** Envía solicitud de reinicio del juego*/
    public void enviarReset() {
        enviarMensaje("RESET");
    }

    /*** Configura la IP del servidor manualmente*/
    public void setIpServidor(String ip) {
        try {
            this.ipServidor = InetAddress.getByName(ip);

            // Desactivar broadcast si se configura IP manual
            if (socket != null) {
                try {
                    socket.setBroadcast(false);
                } catch (java.net.SocketException e) {
                    System.err.println("⚠️ Error al desactivar broadcast: " + e.getMessage());
                }
            }

            System.out.println("🌐 IP del servidor actualizada: " + ip);
        } catch (Exception e) {
            System.err.println("❌ IP inválida: " + e.getMessage());
        }
    }

    /*** Cierra el cliente y libera recursos*/
    public void cerrar() {
        System.out.println("🛑 Cerrando cliente desde puerto " + puertoLocal + "...");

        finalizar = true;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        this.interrupt();
    }
}
