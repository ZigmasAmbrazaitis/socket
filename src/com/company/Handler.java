package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;

public class Handler extends Thread {
    public double posX;
    public double posY;
    private String name;
    private Socket socket;
    private BufferedReader in;
    private ObjectOutputStream out;


    /**
     * Constructs a handler thread, squirreling away the socket.
     * All the interesting work is done in the run method.
     */
    public Handler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Services this thread's client by repeatedly requesting a
     * screen name until a unique one has been submitted, then
     * acknowledges the name and registers the output stream for
     * the client in a global set, then repeatedly gets inputs and
     * broadcasts them.
     */
    public void run() {
        try {

            // Create character streams for the socket.
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new ObjectOutputStream(socket.getOutputStream());
            Main.writers.add(out);
            // Request a name from this client.  Keep requesting until
            // a name is submitted that is not already used.  Note that
            // checking for the existence of a name and adding the name
            // must be done while locking the set of names.

            name = "Player" + new Date().getTime();
            System.out.println("new player: " + name);
            if (name == null) {
                return;
            }
            synchronized (Main.players) {
                boolean isfound = false;
                for (int i = 0; i < Main.players.size(); i++) {
                    if (Main.players.get(i).name.equals(name))
                        isfound = true;

                }
                if (isfound == false) {
                    PlayerVO player = new PlayerVO();
                    player.name = name;
                    int pos = Main.players.size() % 4;
                    posX = 0;
                    posY = 0;
                    switch (pos) {
                        case 0:
                            posX = 1 * Main.CELL_WIDTH;
                            posY = 1 * Main.CELL_HEIGHT;
                            break;
                        case 1:
                            posX = 9 * Main.CELL_WIDTH;
                            posY = 1 * Main.CELL_HEIGHT;
                            break;
                        case 2:
                            posX = 9 * Main.CELL_WIDTH;
                            posY = 9 * Main.CELL_HEIGHT;
                            break;
                        case 3:
                            posX = 1 * Main.CELL_WIDTH;
                            posY = 9 * Main.CELL_HEIGHT;
                            break;
                    }

                    player.X = posX;
                    player.Y = posY;
                    Main.players.add(player);
                    out.writeUTF("myname:" + name);
                    for (PlayerVO syncPlayer : Main.players) {
                        String playerAdd = "add:" + syncPlayer.name + ":" + syncPlayer.X + ":" + syncPlayer.Y;

                        sendToAll(playerAdd);
                    }
                }
            }


            // Now that a successful name has been chosen, add the
            // socket's print writer to the set of all writers so
            // this client can receive broadcast messages.


            // Accept messages from this client and broadcast them.
            // Ignore other clients that cannot be broadcasted to.
            while (true) {
                String input = in.readLine();
                if (input == null) {
                    return;
                }

                //direction:0
                //direction:1
                //direction:2
                //direction:3
                //bomb:Player1516295196

                //explode:Player18368721368712
                //dead:Player18237219873
                System.out.println(input);
                String[] zodziai = input.split(":");
                switch (zodziai[0]) {
                    case "direction":
                        handleDir(zodziai[1]);
                        break;
                    case "addbomb":
                        handleBomb();
                        break;
                    case "explode":
                        break;
                    case "dead":
                        break;
                }


            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            // This client is going down!  Remove its name and its print
            // writer from the sets, and close its socket.
            if (name != null) {
                for (PlayerVO syncPlayer : Main.players) {
                    if (syncPlayer.name.equals(name)) {
                        Main.players.remove(syncPlayer);
                        break;
                    }
                }
            }
            if (out != null) {
                String playerRemove = "";

                sendToAll(playerRemove);
                Main.writers.remove(out);
            }
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    void sendToAll(String value) {
        for (ObjectOutputStream writer : Main.writers) {
            try {
                writer.writeUTF(value);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    void handleDir(String value) {
        switch (value) {
            case "0":
                posY -= Main.PLAYER_SPEED;
                break;
            case "1":
                posX += Main.PLAYER_SPEED;
                break;
            case "2":
                posY += Main.PLAYER_SPEED;
                break;
            case "3":
                posX -= Main.PLAYER_SPEED;
                break;

        }
        for (PlayerVO syncPlayer : Main.players) {
            if (syncPlayer.name.equals(name)) {
                syncPlayer.X = posX;
                syncPlayer.Y = posY;
            }
            String moveAction = "update:" + syncPlayer.name + ":" + syncPlayer.X + ":" + syncPlayer.Y;
            sendToAll(moveAction);
        }
    }

    void handleBomb () {
        Main.bombs.add(new BombVO());
        for (BombVO syncBomb : Main.bombs) {
            if (syncBomb.name.equals(name)) {
                syncBomb.X = posX;
                syncBomb.Y = posY;
            }
            String moveAction = "addbomb:" + syncBomb.name + ":" + syncBomb.X + ":" + syncBomb.Y;
            sendToAll(moveAction);
        }
    }
}
