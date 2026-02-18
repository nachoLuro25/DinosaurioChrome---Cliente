package com.dinosauriojuego.net;

import com.badlogic.gdx.Gdx;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

//hilo cliente para comunicarse con el servidor del juego
public class HiloClienteDino extends Thread {

    private DatagramSocket socket;
    private InetAddress    ipServer;
    private final int      puerto = 8999;
    private volatile boolean fin = false;
    private volatile ClienteListener listener;

    // constructor: crea el socket UDP, hace broadcast buscando el servidor y manda conexion
    public HiloClienteDino() {
        try {
            ipServer = InetAddress.getByName("255.255.255.255");
            socket   = new DatagramSocket();
            socket.setBroadcast(true);
            enviar("Conexion");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //listener para los eventos de red (conectado, empieza, snapshot)
    public void setListener(ClienteListener listener) {
        this.listener = listener;
        if (ClienteEstado.conectado) safeOnConectado();
        if (ClienteEstado.empezo)   safeOnEmpieza();
    }

    //serializa el mensaje a bytes y lo envia por UDP al servidor
    private void enviar(String msg) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, ipServer, puerto));
        } catch (Exception ignored) {}
    }

    @Override
    //loop principal: recibe mensajes y los procesa
    public void run() {
        while (!fin) {
            try {
                byte[] buf = new byte[8192];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);
                procesar(dp);
            } catch (Exception ignored) {}
        }
    }

    //decodifica el mensaje recibido y actua segun su tipo
    private void procesar(DatagramPacket dp) {
        String msg = new String(dp.getData(), 0, dp.getLength(), StandardCharsets.UTF_8).trim();

        if (msg.equals("OK")) {
            // el servidor respondio: guardar su IP real (en caso de que el broadcast llegue por otra interfaz)
            ipServer = dp.getAddress();
            ClienteEstado.conectado = true;
            safeOnConectado();
            return;
        }

        if (msg.equals("Empieza")) {
            ClienteEstado.empezo = true;
            safeOnEmpieza();
            return;
        }

        if (msg.startsWith("SNAP;")) {
            boolean ok = parseSnapshotSeguro(msg);
            if (ok) safeOnSnapshot();
        }
    }

    //parsea el snapshot recibido y actualiza ClienteEstado
    //formato esperado: SNAP;tick;score;vel;started;terminado;mensajeFin;resetCount;
    //                  p1y;p1piso;p1agach;p1vivo;  p2y;p2piso;p2agach;p2vivo;
    //                  cantP1;[type;variant;x;y;]*cantP1
    //                  cantP2;[type;variant;x;y;]*cantP2
    private boolean parseSnapshotSeguro(String msg) {
        try {
            String[] p = msg.split(";");
            int i = 0;

            if (!"SNAP".equals(p[i++])) return false;

            ClienteEstado.tick       = Integer.parseInt(p[i++]);
            ClienteEstado.score      = Integer.parseInt(p[i++]);
            i++; // velocidad (no se usa en el cliente)

            ClienteEstado.empezo    = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.terminado = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.mensajeFin = (i < p.length) ? p[i++] : "";
            ClienteEstado.resetReadyCount = Integer.parseInt(p[i++]);

            // estado p1
            ClienteEstado.p1.y        = Float.parseFloat(p[i++]);
            ClienteEstado.p1.enPiso   = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p1.agachado = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p1.vivo     = Integer.parseInt(p[i++]) == 1;

            // estado p2
            ClienteEstado.p2.y        = Float.parseFloat(p[i++]);
            ClienteEstado.p2.enPiso   = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p2.agachado = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p2.vivo     = Integer.parseInt(p[i++]) == 1;

            // obstaculos de la pista 1
            // synchronized evita que el hilo de render lea la lista mientras se esta modificando
            int n1 = Integer.parseInt(p[i++]);
            synchronized (ClienteEstado.obstaculosP1) {
                ClienteEstado.obstaculosP1.clear();
                for (int k = 0; k < n1; k++) {
                    ClienteEstado.ObstacleState o = new ClienteEstado.ObstacleState();
                    o.type    = Integer.parseInt(p[i++]);
                    o.variant = Integer.parseInt(p[i++]);
                    o.x       = Float.parseFloat(p[i++]);
                    o.y       = Float.parseFloat(p[i++]);
                    ClienteEstado.obstaculosP1.add(o);
                }
            }

            // obstaculos de la pista 2
            int n2 = Integer.parseInt(p[i++]);
            synchronized (ClienteEstado.obstaculosP2) {
                ClienteEstado.obstaculosP2.clear();
                for (int k = 0; k < n2; k++) {
                    ClienteEstado.ObstacleState o = new ClienteEstado.ObstacleState();
                    o.type    = Integer.parseInt(p[i++]);
                    o.variant = Integer.parseInt(p[i++]);
                    o.x       = Float.parseFloat(p[i++]);
                    o.y       = Float.parseFloat(p[i++]);
                    ClienteEstado.obstaculosP2.add(o);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //callbacks ejecutados en el hilo de LibGDX para que sea thread-safe
    private void safeOnConectado() {
        Gdx.app.postRunnable(() -> { ClienteListener l = listener; if (l != null) l.onConectado(); });
    }
    private void safeOnEmpieza() {
        Gdx.app.postRunnable(() -> { ClienteListener l = listener; if (l != null) l.onEmpieza(); });
    }
    private void safeOnSnapshot() {
        Gdx.app.postRunnable(() -> { ClienteListener l = listener; if (l != null) l.onSnapshot(); });
    }

    public void sendListo()                         { enviar("Listo"); }
    public void sendInput(boolean jump, boolean crouch) { enviar("INPUT;" + (jump ? "1" : "0") + ";" + (crouch ? "1" : "0")); }
    public void sendReset()                         { enviar("RESET"); }

    //cierra el hilo y el socket
    public void cerrar() {
        fin = true;
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
    }
}