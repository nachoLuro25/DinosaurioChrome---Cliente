package com.dinosauriojuego.net;
import com.badlogic.gdx.Gdx;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
//hilo cliente para comunicarse con el servidor del juego
public class HiloClienteDino extends Thread {

    private DatagramSocket socket;
    private InetAddress ipServer;
    private final int puerto = 8999;
    private volatile boolean fin = false;
    private volatile ClienteListener listener;

    // constructor del hilo cliente, crea el socket UDP, busca el sv en lan por broadcast y envia mensaje de conexion al sv
    public HiloClienteDino() {
        try {
            ipServer = InetAddress.getByName("255.255.255.255");
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            enviar("Conexion");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //listener para los eventos
    public void setListener(ClienteListener listener) {
        this.listener = listener;

        if (ClienteEstado.conectado) safeOnConectado();
        if (ClienteEstado.empezo) safeOnEmpieza();
    }

    //agarra los mensajes los pasa a bytes y los manda por UDP a la ip y puerto del sv
    private void enviar(String msg) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket dp = new DatagramPacket(data, data.length, ipServer, puerto);
            socket.send(dp);
        } catch (Exception ignored) {}
    }

    @Override
    //loop del hilo, recibe los msjs y los procesa
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

    //procesa mensajes recibidos del sv
    private void procesar(DatagramPacket dp) {
        String msg = new String(dp.getData(), 0, dp.getLength(), StandardCharsets.UTF_8).trim();

        if (msg.equals("OK")) {
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
    //traduce el snapshot recibido del sv y actualiza el estado del cliente
    private boolean parseSnapshotSeguro(String msg) {
        try {
            String[] p = msg.split(";");
            int i = 0;

            if (!"SNAP".equals(p[i++])) return false;

            ClienteEstado.tick = Integer.parseInt(p[i++]);
            ClienteEstado.score = Integer.parseInt(p[i++]);

            // velocidad
            i++;

            ClienteEstado.empezo = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.terminado = Integer.parseInt(p[i++]) == 1;

            ClienteEstado.mensajeFin = (i < p.length) ? p[i++] : "";


            ClienteEstado.resetReadyCount = Integer.parseInt(p[i++]);

            // p1
            ClienteEstado.p1.y = Float.parseFloat(p[i++]);
            ClienteEstado.p1.enPiso = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p1.agachado = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p1.vivo = Integer.parseInt(p[i++]) == 1;

            // p2
            ClienteEstado.p2.y = Float.parseFloat(p[i++]);
            ClienteEstado.p2.enPiso = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p2.agachado = Integer.parseInt(p[i++]) == 1;
            ClienteEstado.p2.vivo = Integer.parseInt(p[i++]) == 1;

            int n = Integer.parseInt(p[i++]);

            synchronized (ClienteEstado.obstacles) { // sincroniza el acceso del hilo del render y el hilo del cliente a la lista de obstaculos para que no crashee
                ClienteEstado.obstacles.clear();
                for (int k = 0; k < n; k++) {
                    ClienteEstado.ObstacleState o = new ClienteEstado.ObstacleState();
                    o.type = Integer.parseInt(p[i++]);
                    o.variant = Integer.parseInt(p[i++]);
                    o.x = Float.parseFloat(p[i++]);
                    o.y = Float.parseFloat(p[i++]);
                    ClienteEstado.obstacles.add(o);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //callbacks (funciones ejecutadas a partir de un evento) para ejecutar en el hilo de libgdx
    private void safeOnConectado() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) l.onConectado();
        });
    }
    private void safeOnEmpieza() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) l.onEmpieza();
        });
    }
    private void safeOnSnapshot() {
        Gdx.app.postRunnable(() -> {
            ClienteListener l = listener;
            if (l != null) l.onSnapshot();
        });
    }

    public void sendListo() {
        enviar("Listo");
    } //msj listo para jugar

    public void sendInput(boolean jump, boolean crouch) { //msj de inputs del jugador
        enviar("INPUT;" + (jump ? "1" : "0") + ";" + (crouch ? "1" : "0"));
    }


    public void sendReset() {
        enviar("RESET");
    } //msj para solicitar reinicio del juego

    public void cerrar() { //cierra el hilo y el socket
        fin = true;
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
    }
}
