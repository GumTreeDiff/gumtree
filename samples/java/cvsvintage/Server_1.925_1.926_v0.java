/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.server.commands.*;
import megamek.common.net.*;
import megamek.common.options.*;
import megamek.common.util.BoardUtilities;
import megamek.common.util.StringUtil;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;
import megamek.common.TagInfo;
import megamek.common.preference.PreferenceManager;

/**
 * @author Ben Mazur
 */
public class Server implements Runnable {
    //    public final static String  LEGAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-";
    public final static String  DEFAULT_BOARD = MapSettings.BOARD_SURPRISE;
    private final static String VERIFIER_CONFIG_FILENAME =
        "data/mechfiles/UnitVerifierOptions.xml";

    // server setup
    private String              password;
    private ServerSocket        serverSocket;
    private String              motd;

    // game info
    private Vector              connections = new Vector(4);
    private Vector              connectionsPending = new Vector(4);
    private Hashtable           connectionIds = new Hashtable();

    private int                 connectionCounter = 0;
    private int                 entityCounter = 0;

    private IGame               game = new Game();

    private Vector              vPhaseReport = new Vector();

    private MapSettings         mapSettings = new MapSettings();

    // commands
    private Hashtable           commandsHash = new Hashtable();

    // listens for and connects players
    private Thread              connector;

    // Track buildings that are affected by an entity's movement.
    private Hashtable           affectedBldgs = new Hashtable();

    // Track Physical Action results, HACK to deal with opposing pushes 
    // canceling each other
    private Vector              physicalResults = new Vector();
    
    private Vector<DynamicTerrainProcessor> terrainProcessors = new Vector();
    
    /* Tracks entities which have been destroyed recently.  Allows refactoring of the
     * damage and kill logic from Server, where it is now, to the Entity subclasses eventually.  
     * This has not been implemented yet -- I am just starting to build the groundwork into Server.
     * It isn't in the execution path and shouldn't cause any bugs */
    //Note from another coder - I have commented out your groundwork
    //for now because it is using HashSet, which isn't available in
    //Java 1.1 unless you import the collections classes.  Since the
    //Server class isn't using any other collecitons classes, there
    //might be a reason we're avoiding them here...if not, feel free
    //to add the import.
    //private HashSet             knownDeadEntities = new HashSet();

    private static EntityVerifier entityVerifier = null;

    private ConnectionListenerAdapter connectionListener = new ConnectionListenerAdapter() {

        /**
         * Called when it is sensed that a connection has terminated.
         */
        public void disconnected(DisconnectedEvent e) {
            Connection conn = e.getConnection();
            
            // write something in the log
            System.out.println("s: connection " + conn.getId()+ " disconnected");

            connections.removeElement(conn);
            connectionsPending.removeElement(conn);
            connectionIds.remove(new Integer(conn.getId()));

            // if there's a player for this connection, remove it too
            Player player = getPlayer(conn.getId());
            if (null != player) {
                Server.this.disconnected( player );
            }
            
        }

        public void packetReceived(PacketReceivedEvent e) {
            Server.this.handle(e.getConnection().getId(),e.getPacket());
        }

    };
    
    /**
     * Construct a new GameHost and begin listening for
     * incoming clients.
     * @param   password the <code>String</code> that is set as a password
     * @param   port the <code>int</code> value that specifies the port that
     *          is used
     */
    public Server(String password, int port) {
        this.password = password.length() > 0 ? password : null;
        // initialize server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch(IOException ex) {
            System.err.println("could not create server socket on port "+port);
        }

        motd = createMotd();

        game.getOptions().initialize();

        changePhase(IGame.PHASE_LOUNGE);

        // display server start text
        System.out.println("s: starting a new server...");

        try {
            String host = InetAddress.getLocalHost().getHostName();
            System.out.print("s: hostname = '" );
            System.out.print( host );
            System.out.print( "' port = " );
            System.out.println( serverSocket.getLocalPort() );
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (int i = 0; i < addresses.length; i++) {
                System.out.println("s: hosting on address = "
                                   + addresses[i].getHostAddress());
            }
        } catch (UnknownHostException  e) {
            // oh well.
        }

        System.out.println("s: password = " + this.password);

        connector = new Thread(this, "Connection Listener");
        connector.start();

        // register commands
        registerCommand(new DefeatCommand(this));
        registerCommand(new HelpCommand(this));
        registerCommand(new KickCommand(this));
        registerCommand(new ResetCommand(this));
        registerCommand(new RollCommand(this));
        registerCommand(new SaveGameCommand(this));
        registerCommand(new SkipCommand(this));
        registerCommand(new VictoryCommand(this));
        registerCommand(new WhoCommand(this));
        registerCommand(new SeeAllCommand(this));
        registerCommand(new LocalSaveGameCommand(this));
        registerCommand(new FixElevationCommand(this));
        
        //register terrain processors
        terrainProcessors.add(new FireProcessor(this));
        terrainProcessors.add(new GeyserProcessor(this));
        terrainProcessors.add(new ElevatorProcessor(this));
    }

    /**
     * Sets the game for this server.  Restores any transient fields, and sets
     * all players as ghosts.
     * This should only be called during server initialization before any
     * players have connected.
     */
    public void setGame(IGame g) {
        this.game = g;

        // reattach the transient fields and ghost the players
        for (Enumeration e = game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = (Entity)e.nextElement();
            ent.setGame(game);
        }
        game.setOutOfGameEntitiesVector(game.getOutOfGameEntitiesVector());
        for (Enumeration e = game.getPlayers(); e.hasMoreElements(); ) {
            Player p = (Player)e.nextElement();
            p.setGame(game);
            p.setGhost(true);
        }

    }

    /** Returns the current game object */
    public IGame getGame() {
        return game;
    }

    /**
     * Make a default message o' the day containing the version string, and
     * if it was found, the build timestamp
     */
    private String createMotd() {
        StringBuffer buf = new StringBuffer();
        buf.append("Welcome to MegaMek.  Server is running version ");
        buf.append(MegaMek.VERSION);
        buf.append(", build date ");
        if (MegaMek.TIMESTAMP > 0L) {
            buf.append(new Date(MegaMek.TIMESTAMP).toString());
        } else {
            buf.append("unknown");
        }
        buf.append(".");

        return buf.toString();
    }

    /**
     * @return true if the server has a password
     */
    public boolean isPassworded() {
        return password != null;
    }

    /**
     * @return true if the password matches
     */
    public boolean isPassword(Object guess) {
        return password.equals(guess);
    }

    /**
     * Registers a new command in the server command table
     */
    private void registerCommand(ServerCommand command) {
        commandsHash.put(command.getName(), command);
    }

    /**
     * Returns the command associated with the specified name
     */
    public ServerCommand getCommand(String name) {
        return (ServerCommand)commandsHash.get(name);
    }

    /**
     * Shuts down the server.
     */
    public void die() {
        // kill thread accepting new connections
        connector = null;

        // close socket
        try {
            serverSocket.close();
        } catch(IOException ex) {}

        // kill pending connnections
        for (Enumeration i=connectionsPending.elements();i.hasMoreElements();){
            final Connection conn = (Connection)i.nextElement();
            conn.close();
        }
        connectionsPending.removeAllElements();

        // Send "kill" commands to all connections
        // N.B. I may be starting a race here.
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            send(conn.getId(), new Packet(Packet.COMMAND_CLOSE_CONNECTION));
        }

        // kill active connnections
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            conn.close();
        }
        connections.removeAllElements();
        connectionIds.clear();
    }

    /**
     * Returns an enumeration of all the command names
     */
    public Enumeration getAllCommandNames() {
        return commandsHash.keys();
    }

    /**
     * Sent when a client attempts to connect.
     */
    private void greeting(int cn) {
        // send server greeting -- client should reply with client info.
        sendToPending(cn, new Packet(Packet.COMMAND_SERVER_GREETING));
    }

    /**
     * Returns a free connection id.
     */
    public int getFreeConnectionId() {
        while (getPendingConnection(connectionCounter) != null
        || getConnection(connectionCounter) != null
        || getPlayer(connectionCounter) != null) {
            connectionCounter++;
        }
        return connectionCounter;
    }

    /**
     * Returns a free entity id.  Perhaps this should be in Game instead.
     */
    public int getFreeEntityId() {
        return game.getNextEntityId();
    }

    /**
     * Allow the player to set whatever parameters he is able to
     */
    private void receivePlayerInfo(Packet packet, int connId) {
        Player player = (Player)packet.getObject(0);
        Player connPlayer = game.getPlayer( connId );
        if ( null != connPlayer ) {
            connPlayer.setColorIndex(player.getColorIndex());
            connPlayer.setStartingPos(player.getStartingPos());
            connPlayer.setTeam(player.getTeam());
            connPlayer.setCamoCategory(player.getCamoCategory());
            connPlayer.setCamoFileName(player.getCamoFileName());
            connPlayer.setNbrMFConventional(player.getNbrMFConventional());
            connPlayer.setNbrMFCommand(player.getNbrMFCommand());
            connPlayer.setNbrMFVibra(player.getNbrMFVibra());
        }
    }

    private String correctDupeName(String oldName) {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            if (player.getName().equals(oldName)) {
                // We need to correct it.
                String newName = oldName;
                int dupNum = 2;
                try {
                    dupNum = Integer.parseInt(oldName.substring(oldName.lastIndexOf(".")+1));
                    dupNum++;
                    newName = oldName.substring(0,oldName.lastIndexOf("."));
                } catch (Exception e) {
                    // If this fails, we don't care much.
                    // Just assume it's the first time for this name.
                    dupNum = 2;
                }
                newName = newName.concat(".").concat(Integer.toString(dupNum));
                return correctDupeName(newName);
            }
        }
        return oldName;
    }

    /**
     * Recieves a player name, sent from a pending connection, and connects
     * that connection.
     */
    private void receivePlayerName(Packet packet, int connId) {
        final Connection conn = getPendingConnection(connId);
        String name = (String)packet.getObject(0);
        boolean returning = false;

        // this had better be from a pending connection
        if (conn == null) {
            System.out.println("server: got a client name from a non-pending" +
                               " connection");
            return;
        }

        // check if they're connecting with the same name as a ghost player
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            if (player.getName().equals(name)) {
                if (player.isGhost()) {
                    returning = true;
                    player.setGhost(false);
                    // switch id
                    connId = player.getId();
                    conn.setId(connId);
                }
            }
        }

        if (!returning) {
            // Check to avoid duplicate names...
            name = correctDupeName(name);
            send(connId, new Packet(Packet.COMMAND_SERVER_CORRECT_NAME, name));
        }

        // right, switch the connection into the "active" bin
        connectionsPending.removeElement(conn);
        connections.addElement(conn);
        connectionIds.put(new Integer(conn.getId()), conn);

        // add and validate the player info
        if (!returning) {
            game.addPlayer(connId, new Player(connId, name));
            validatePlayerInfo(connId);
        }

        // if it is not the lounge phase, this player becomes an observer
        Player player = getPlayer( connId );
        if ( game.getPhase() != IGame.PHASE_LOUNGE
             && null != player
             && game.getEntitiesOwnedBy(player) < 1) {
            player.setObserver(true);
        }

        // send the player the motd
        sendServerChat(connId, motd);

        // send info that the player has connected
        send(createPlayerConnectPacket(connId));

        // tell them their local playerId
        send(connId, new Packet(Packet.COMMAND_LOCAL_PN, new Integer(connId)));

        // send current game info
        sendCurrentInfo(connId);

        try {
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress
                                                               .getLocalHost()
                                                               .getHostName());
            for (int i = 0; i < addresses.length; i++) {
                sendServerChat(connId, "Machine IP is " +
                               addresses[i].getHostAddress());
            }
        } catch (UnknownHostException  e) {
            // oh well.
        }

        // Send the port we're listening on. Only useful for the player
        // on the server machine to check.
        sendServerChat(connId, "Listening on port " + serverSocket
                                                      .getLocalPort());

        // Get the player *again*, because they may have disconnected.
        player = getPlayer( connId );
        if ( null != player ) {
            StringBuffer buff = new StringBuffer();
            buff.append( player.getName() )
                .append( " connected from " )
                .append( getClient(connId).getInetAddress() );
            String who = buff.toString();
            System.out.print( "s: player #" );
            System.out.print( connId );
            System.out.print( ", " );
            System.out.println( who );

            sendServerChat( who );

        } // Found the player
    }

    /**
     * Sends a player the info they need to look at the current phase.
     * This is triggered when a player first connects to the server.
     */
    private void sendCurrentInfo(int connId) {
        //why are these two outside the player != null check below?
        transmitAllPlayerConnects(connId);
        send(connId, createGameSettingsPacket());

        Player player = game.getPlayer(connId);
        if ( null != player ) {
            send(connId, new Packet(Packet.COMMAND_SENDING_MINEFIELDS,
                                    player.getMinefields()));

            switch (game.getPhase()) {
            case IGame.PHASE_LOUNGE :
                send(connId, createMapSettingsPacket());
                // Send Entities *after* the Lounge Phase Change
                send(connId, new Packet(Packet.COMMAND_PHASE_CHANGE,
                                        new Integer(game.getPhase())));
                if (doBlind()) {
                    send(connId, createFilteredFullEntitiesPacket(player));
                }
                else {
                    send(connId, createFullEntitiesPacket());
                }
                break;
            default :
                send(connId, new Packet(Packet.COMMAND_ROUND_UPDATE, new Integer(game.getRoundCount())));
                //send(connId, createReportPacket(player));
                send(connId, createAllReportsPacket(player));

                // Send Entites *before* other phase changes.
                if (doBlind()) {
                    send(connId, createFilteredFullEntitiesPacket(player));
                }
                else {
                    send(connId, createFullEntitiesPacket());
                }
                player.setDone( game.getEntitiesOwnedBy(player) <= 0 );
                send(connId, createBoardPacket());
                send(connId, new Packet(Packet.COMMAND_PHASE_CHANGE,
                                        new Integer(game.getPhase())));
                break;
            }
            if (game.getPhase() == IGame.PHASE_FIRING ||
                game.getPhase() == IGame.PHASE_TARGETING ||
                game.getPhase() == IGame.PHASE_OFFBOARD ||
                game.getPhase() == IGame.PHASE_PHYSICAL) {
                // can't go above, need board to have been sent
                send(connId,createAttackPacket(game.getActionsVector(),0));
                send(connId,createAttackPacket(game.getChargesVector(),1));
                send(connId,createAttackPacket(game.getLayMinefieldActionsVector(),2));
            }
            if (game.phaseHasTurns(game.getPhase())) {
                send(connId, createTurnVectorPacket());
                send(connId, createTurnIndexPacket());
            }
            
            send(connId, createArtilleryPacket(player));
            send(connId, createFlarePacket());

        } // Found the player.

    }

    /**
     * Validates the player info.
     */
    public void validatePlayerInfo(int playerId) {
        final Player player = getPlayer(playerId);

        //        maybe this isn't actually useful
        //        // replace characters we don't like with "X"
        //        StringBuffer nameBuff = new StringBuffer(player.getName());
        //        for (int i = 0; i < nameBuff.length(); i++) {
        //            int chr = nameBuff.charAt(i);
        //            if (LEGAL_CHARS.indexOf(chr) == -1) {
        //                nameBuff.setCharAt(i, 'X');
        //            }
        //        }
        //        player.setName(nameBuff.toString());

        //TODO: check for duplicate or reserved names

        // make sure colorIndex is unique
        boolean[] colorUsed = new boolean[Player.colorNames.length];
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player otherPlayer = (Player)i.nextElement();
            if (otherPlayer.getId() != playerId) {
                colorUsed[otherPlayer.getColorIndex()] = true;
            }
        }
        if (null != player && colorUsed[player.getColorIndex()]) {
            // find a replacement color;
            for (int i = 0; i < colorUsed.length; i++) {
                if (!colorUsed[i]) {
                    player.setColorIndex(i);
                    break;
                }
            }
        }

    }

    /**
     * Called when it's been determined that an actual player
     * disconnected.  Notifies the other players and does the appropriate
     * housekeeping.
     */
    void disconnected(Player player) {
        int phase = game.getPhase();

        // in the lounge, just remove all entities for that player
        if (phase == IGame.PHASE_LOUNGE) {
            removeAllEntitesOwnedBy(player);
        }

        // if a player has active entities, he becomes a ghost
        // except the VICTORY_PHASE when the dosconnected 
        // player is most likely the Bot disconnected after receiving
        // the COMMAND_END_OF_GAME command 
        // see the Bug 1225949.
        // TODO Perhaps there is a better solution to handle the Bot disconnect
        if (game.getEntitiesOwnedBy(player) > 0 && phase != IGame.PHASE_VICTORY) {
            player.setGhost(true);
            player.setDone(true);
            send(createPlayerUpdatePacket(player.getId()));
        } else {
            game.removePlayer(player.getId());
            send(new Packet(Packet.COMMAND_PLAYER_REMOVE,
                 new Integer(player.getId())));
        }

        // make sure the game advances
        if ( game.phaseHasTurns(game.getPhase()) && null != game.getTurn() ) {
            if ( game.getTurn().isValid( player.getId(), game ) ) {
                sendGhostSkipMessage( player );
            }
        } else {
            checkReady();
        }

        // notify other players
        sendServerChat(player.getName() + " disconnected.");

        // log it
        System.out.println("s: removed player " + player.getName());

        // Reset the game after Elvis has left the building.
        if ( 0 == game.getNoOfPlayers() ) {
            resetGame();
        }
    }

    /**
     * Checks each player to see if he has no entities, and if true, sets the
     * observer flag for that player.  An exception is that there are no
     * observers during the lounge phase.
     */
    public void checkForObservers() {
        for (Enumeration e = game.getPlayers(); e.hasMoreElements(); ) {
            Player p = (Player)e.nextElement();
            p.setObserver(game.getEntitiesOwnedBy(p) < 1 &&
                          game.getPhase() != IGame.PHASE_LOUNGE);
        }
    }

    /**
     * Reset the game back to the lounge.
     *
     * TODO: couldn't this be a hazard if there are other things executing at
     *  the same time?
     */
    public void resetGame() {
        // remove all entities
        game.reset();
        send(createEntitiesPacket());
        send(new Packet(Packet.COMMAND_SENDING_MINEFIELDS, new Vector()));

        //TODO: remove ghosts

        // reset all players
        resetPlayersDone();
        transmitAllPlayerDones();

        // Write end of game to stdout so controlling scripts can rotate logs.
        SimpleDateFormat format = new SimpleDateFormat
            ( "yyyy-MM-dd HH:mm:ss z" );
        System.out.print( format.format(new Date()) );
        System.out.println( " END OF GAME" );

        changePhase(IGame.PHASE_LOUNGE);
    }

    
    /**
     * automatically save the game 
     */
    public void autoSave() {
        String fileName = "autosave";
        if (PreferenceManager.getClientPreferences().stampFilenames()) {
            fileName = StringUtil.addDateTimeStamp(fileName);
        }
        saveGame(fileName,game.getOptions().booleanOption("autosave_msg"));
    }

    /**
     * save the game and send it to the sepecified connection
     * @param connId The <code>int</code> connection id to send to
     * @param sFile  The <code>String</code> filename to use
     */
    public void sendSaveGame (int connId, String sFile) {
        saveGame(sFile, false);
        String sFinalFile = sFile;
        if (!sFinalFile.endsWith(".sav")) {
            sFinalFile = sFile + ".sav";
        }
        sFinalFile = "savegames" + File.separator + sFinalFile;
        File f = new File(sFinalFile);
        try {
            ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(f));
            send(connId,new Packet(Packet.COMMAND_SEND_SAVEGAME, new Object[] {sFinalFile, ois.readObject()}));
            sendChat(connId,"***Server","Savegame has been sent to you.");
            ois.close();
        } catch (Exception e) {
            System.err.println("Unable to load file: " + f);
            e.printStackTrace();
        }
    }
    
    /**
     * save the game
     * @param sFile   The <code>String</code> filename to use
     * @param sendChat A <code>boolean</code> value wether or not to announce
     *                 the saving to the server chat. 
     */
    public void saveGame(String sFile, boolean sendChat) {
        String sFinalFile = sFile;
        if (!sFinalFile.endsWith(".sav")) {
            sFinalFile = sFile + ".sav";
        }
        try {
            File sDir = new File("savegames");
            if (!sDir.exists()) {
                sDir.mkdir();
            }
            sFinalFile = sDir + File.separator + sFinalFile;
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(sFinalFile));
            oos.writeObject(game);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            System.err.println("Unable to save file: " + sFinalFile);
            e.printStackTrace();
        }

        if (sendChat) sendChat("MegaMek", "Game saved to " + sFinalFile);
    }

    /**
     * save the game
     * @param sFile   The <code>String</code> filename to use
     */
    public void saveGame(String sFile) {
        saveGame(sFile,true);
    }

    /**
     * load the game
     * @param f The <code>File</code> to load
     * @return A <code>boolean</code> value wether or not the loading was successfull
     */
    public boolean loadGame(File f) {
        System.out.println("s: loading saved game file '"+f+"'");
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(f));
            game = (IGame)ois.readObject();
            ois.close();
        } catch (Exception e) {
            System.err.println("Unable to load file: " + f);
            e.printStackTrace();
            return false;
        }

        // a bit redundant, but there's some initialization code there
        setGame(game);

        return true;
    }

    /**
     * Shortcut to game.getPlayer(id)
     */
    public Player getPlayer(int id) {
        return game.getPlayer(id);
    }

    /**
     * Removes all entities owned by a player.  Should only be called when it
     * won't cause trouble (the lounge, for instance, or between phases.)
     * @param the <code>Player</code> whose entites are to be removed
     */
    private void removeAllEntitesOwnedBy(Player player) {
        Vector toRemove = new Vector();

        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            if (entity.getOwner().equals(player)) {
                toRemove.addElement(entity);
            }
        }

        for (Enumeration e = toRemove.elements(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();
            int id = entity.getId();
            game.removeEntity(id, IEntityRemovalConditions.REMOVE_NEVER_JOINED);
            send(createRemoveEntityPacket(id, IEntityRemovalConditions.REMOVE_NEVER_JOINED));
        }
    }

    /**
     * a shorter name for getConnection()
     */
    private Connection getClient(int connId) {
        return getConnection(connId);
    }

    /**
     * Returns a connection, indexed by id
     */
    public Enumeration getConnections() {
        return connections.elements();
    }

    /**
     * Returns a connection, indexed by id
     */
    public Connection getConnection(int connId) {
        return (Connection)connectionIds.get(new Integer(connId));
    }

    /**
     * Returns a pending connection
     */
    private Connection getPendingConnection(int connId) {
        for (Enumeration i=connectionsPending.elements();i.hasMoreElements();){
            final Connection conn = (Connection)i.nextElement();

            if (conn.getId() == connId) {
                return conn;
            }
        }
        return null;
    }

    /**
     * Called at the beginning of each game round to reset values on this
     * entity that are reset every round
     */
    private void resetEntityRound() {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();

            entity.newRound(game.getRoundCount());
        }
    }

    /**
     * Called at the beginning of each phase.  Sets and resets
     * any entity parameters that need to be reset.
     */
    private void resetEntityPhase(int phase) {
        // first, mark doomed entities as destroyed and flag them
        Vector toRemove = new Vector(0, 10);
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            if (entity.crew.isDoomed()) {
                entity.crew.setDead(true);
                entity.setDestroyed(true);
            }

            if (entity.isDoomed()) {
                entity.setDestroyed(true);

                // Is this unit swarming somebody?  Better let go before
                //  it's too late.
                final int swarmedId = entity.getSwarmTargetId();
                if ( Entity.NONE != swarmedId ) {
                    final Entity swarmed = game.getEntity( swarmedId );
                    swarmed.setSwarmAttackerId( Entity.NONE );
                    entity.setSwarmTargetId( Entity.NONE );
                    Report r = new Report(5165);
                    r.subject = swarmedId;
                    r.addDesc(swarmed);
                    addReport(r);
                    this.entityUpdate( swarmedId );
                }
            }

            if (entity.isDestroyed() || entity.getCrew().isDead()) {
                toRemove.addElement(entity);
            }
        }

        // actually remove all flagged entities
        for (Enumeration e = toRemove.elements(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();
            int condition = IEntityRemovalConditions.REMOVE_SALVAGEABLE;
            if ( !entity.isSalvage() ) {
                condition = IEntityRemovalConditions.REMOVE_DEVASTATED;
            }

            this.entityUpdate(entity.getId());
            game.removeEntity(entity.getId(), condition);
            send( createRemoveEntityPacket(entity.getId(), condition) );
        }

        // do some housekeeping on all the remaining
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            entity.applyDamage();

            entity.reloadEmptyWeapons();

            // reset damage this phase
            entity.damageThisPhase = 0;
            entity.engineHitsThisRound = 0;
            entity.rolledForEngineExplosion = false;
            entity.dodging = false;

            // reset done to false

            if ( phase == IGame.PHASE_DEPLOYMENT ) {
                entity.setDone(!entity.shouldDeploy(game.getRoundCount()));
            } else {
                entity.setDone(false);
            }
            
            // reset spotlights
            entity.setIlluminated(false);
            entity.setUsedSearchlight(false);
        }
    }

    /**
     * Called at the beginning of certain phases to make
     * every player not ready.
     */
    private void resetPlayersDone() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            player.setDone(false);
        }
    }

    /**
     * Called at the beginning of certain phases to make
     * every active player not ready.
     */
    private void resetActivePlayersDone() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            player.setDone(game.getEntitiesOwnedBy(player) <= 0);

        }
        transmitAllPlayerDones();
    }

    /**
     * Writes the victory report
     */
    private void prepareVictoryReport() {
        Report r;

        addReport(new Report(7000, Report.PUBLIC));

        //Declare the victor
        r = new Report(1210);
        r.type = Report.PUBLIC;
        if (game.getVictoryTeam() == Player.TEAM_NONE) {
            Player player = getPlayer( game.getVictoryPlayerId() );
            if ( null == player ) {
                r.messageId = 7005;
            } else {
                r.messageId = 7010;
                r.add(player.getName());
            }
        } else {
            //Team victory
            r.messageId = 7015;
            r.add(game.getVictoryTeam());
        }
        addReport(r);

        //List the survivors
        Enumeration survivors = game.getEntities();
        if ( survivors.hasMoreElements() ) {
            addReport(new Report(7020, Report.PUBLIC));
            while ( survivors.hasMoreElements() ) {
                Entity entity = (Entity) survivors.nextElement();

                if ( !entity.isDeployed() )
                  continue;

                addReport( entity.victoryReport());
            }
        }
        //List units that never deployed
        Enumeration undeployed = game.getEntities();
        if ( undeployed.hasMoreElements() ) {
            boolean wroteHeader = false;

            while ( undeployed.hasMoreElements() ) {
                Entity entity = (Entity) undeployed.nextElement();

                if ( entity.isDeployed() )
                  continue;

                if ( !wroteHeader ) {
                  addReport(new Report(7075, Report.PUBLIC));
                  wroteHeader = true;
                }

                addReport( entity.victoryReport());
            }
        }
        //List units that retreated
        Enumeration retreat = game.getRetreatedEntities();
        if ( retreat.hasMoreElements() ) {
            addReport(new Report(7080, Report.PUBLIC));
            while ( retreat.hasMoreElements() ) {
                Entity entity = (Entity) retreat.nextElement();
                addReport( entity.victoryReport());
            }
        }
        //List destroyed units
        Enumeration graveyard = game.getGraveyardEntities();
        if ( graveyard.hasMoreElements() ) {
            addReport(new Report(7085, Report.PUBLIC));
            while ( graveyard.hasMoreElements() ) {
                Entity entity = (Entity) graveyard.nextElement();
                addReport( entity.victoryReport());
            }
        }
        //List devastated units (not salvagable)
        Enumeration devastated = game.getDevastatedEntities();
        if ( devastated.hasMoreElements() ) {
            addReport(new Report(7090, Report.PUBLIC));

            while ( devastated.hasMoreElements() ) {
                Entity entity = (Entity) devastated.nextElement();
                addReport( entity.victoryReport());
            }
        }
        //Let player know about entitystatus.txt file
        addReport(new Report(7095, Report.PUBLIC));
    }

    /**
     * Generates a detailed report for campaign use
     */
    private String getDetailedVictoryReport() {
        StringBuffer sb = new StringBuffer();

        Vector vAllUnits = new Vector();
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            vAllUnits.addElement(i.nextElement());
        }

        for ( Enumeration i = game.getRetreatedEntities();
              i.hasMoreElements(); ) {
            vAllUnits.addElement(i.nextElement());
        }

        for ( Enumeration i = game.getGraveyardEntities();
              i.hasMoreElements(); ) {
            vAllUnits.addElement(i.nextElement());
        }

        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {

            // Record the player.
            Player p = (Player)i.nextElement();
            sb.append("++++++++++ " )
                .append( p.getName() )
                .append( " ++++++++++");
            sb.append( CommonConstants.NL );

            // Record the player's alive, retreated, or salvageable units.
            for (int x = 0; x < vAllUnits.size(); x++) {
                Entity e = (Entity)vAllUnits.elementAt(x);
                if (e.getOwner() == p) {
                    sb.append(UnitStatusFormatter.format(e));
                }
            }

            // Record the player's devastated units.
            Enumeration devastated = game.getDevastatedEntities();
            if ( devastated.hasMoreElements() ) {
                sb.append("=============================================================");
                sb.append( CommonConstants.NL );
                sb.append("The following utterly destroyed units are not available for salvage:");
                sb.append( CommonConstants.NL );
                while ( devastated.hasMoreElements() ) {
                    Entity e = (Entity) devastated.nextElement();
                    if (e.getOwner() == p) {
                        sb.append( e.getShortName() )
                            .append( ", Pilot: " )
                            .append( e.getCrew().getName() )
                            .append( " (" )
                            .append( e.getCrew().getGunnery() )
                            .append( "/" )
                            .append( e.getCrew().getPiloting() )
                            .append( ")" );
                        sb.append( CommonConstants.NL );
                    }
                } // Handle the next unsalvageable unit for the player
                sb.append("=============================================================");
                sb.append( CommonConstants.NL );
            }

        } // Handle the next player

        return sb.toString();
    }

    /**
     * Forces victory for the specified player, or his/her team at the end of
     * the round.
     */
    public void forceVictory(Player victor) {
        game.setForceVictory(true);
        if (victor.getTeam() == Player.TEAM_NONE) {
            game.setVictoryPlayerId(victor.getId());
            game.setVictoryTeam(Player.TEAM_NONE);
        } else {
            game.setVictoryPlayerId(Player.PLAYER_NONE);
            game.setVictoryTeam(victor.getTeam());
        }

        Vector players = game.getPlayersVector();
        for (int i = 0; i < players.size(); i++) {
            Player player = (Player) players.elementAt(i);
            player.setAdmitsDefeat(false);
        }
    }

    /** Cancels the force victory */
    public void cancelVictory() {
        game.setForceVictory(false);
        game.setVictoryPlayerId(Player.PLAYER_NONE);
        game.setVictoryTeam(Player.TEAM_NONE);
    }

    /**
     * Called when a player declares that he is "done."  Checks to see if all
     * players are done, and if so, moves on to the next phase.
     */
    private void checkReady() {
        // check if all active players are done
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if (!player.isGhost() && !player.isObserver() && !player.isDone()) {
                return;
            }
        }

        // Tactical Genius pilot special ability (lvl 3)
        if (game.getNoOfInitiativeRerollRequests() > 0) {
            resetActivePlayersDone();
            game.rollInitAndResolveTies();

            determineTurnOrder(IGame.PHASE_INITIATIVE);
            clearReports();
            writeInitiativeReport(true);
            sendReport(true);
            return;  // don't end the phase yet, players need to see new report
        }

        // need at least one entity in the game for the lounge phase to end
        if (!game.phaseHasTurns(game.getPhase()) &&
                (game.getPhase() != IGame.PHASE_LOUNGE || game.getNoOfEntities() > 0)) {
            endCurrentPhase();
        }
    }

    /**
     * Called when the current player has done his current turn and the turn
     * counter needs to be advanced.
     * Also enforces the "protos_move_multi" and the "protos_move_multi"
     * option.  If the player has just moved infantry/protos with a "normal"
     * turn, adds up to Game.INF_AND_PROTOS_MOVE_MULTI - 1 more
     * infantry/proto-specific turns after the current turn.
     */
    private void endCurrentTurn(Entity entityUsed) {

        // Enforce "inf_move_multi" and "protos_move_multi" options.
        // The "isNormalTurn" flag is checking to see if any non-Infantry
        // or non-Protomech units can move during the current turn.
        boolean turnsChanged = false;
        GameTurn turn = game.getTurn();
        final int playerId = (null == entityUsed) ?
            Player.PLAYER_NONE : entityUsed.getOwnerId();
        boolean infMoved = entityUsed instanceof Infantry;
        boolean infMoveMulti =
            game.getOptions().booleanOption("inf_move_multi")
            && (game.getPhase() == IGame.PHASE_MOVEMENT
                || game.getPhase() == IGame.PHASE_INITIATIVE);
        boolean protosMoved = entityUsed instanceof Protomech;
        boolean protosMoveMulti =
            game.getOptions().booleanOption("protos_move_multi");

        // If infantry or protos move multi see if any
        // other unit types can move in the current turn.
        int multiMask = 0;
        if ( infMoveMulti ) {
            multiMask += GameTurn.CLASS_INFANTRY;
        }
        if ( protosMoveMulti ) {
            multiMask += GameTurn.CLASS_PROTOMECH;
        }

        // If a proto declared fire and protos don't move
        // multi, ignore whether infantry move or not.
        else if ( protosMoved && game.getPhase() == IGame.PHASE_FIRING ) {
            multiMask = 0;
        }

        // Is this a general move turn?
        boolean isGeneralMoveTurn =
            ( !(turn instanceof GameTurn.SpecificEntityTurn) &&
              !(turn instanceof GameTurn.UnitNumberTurn) &&
              !(turn instanceof GameTurn.UnloadStrandedTurn) &&
              ( !(turn instanceof GameTurn.EntityClassTurn) ||
                ( (turn instanceof GameTurn.EntityClassTurn) &&
                  ( (GameTurn.EntityClassTurn) turn ).isValidClass(~multiMask)
                  )
                )
              );

        // Unless overridden by the "protos_move_multi" option, all Protomechs
        // in a unit declare fire, and they don't mix with infantry.
        if ( protosMoved && !protosMoveMulti && isGeneralMoveTurn &&
             game.getPhase() == IGame.PHASE_FIRING ) {

            // What's the unit number and ID of the entity used?
            final char movingUnit = entityUsed.getUnitNumber();
            final int movingId = entityUsed.getId();

            // How many other Protomechs are in the unit that can fire?
            int protoTurns = game.getSelectedEntityCount
                ( new EntitySelector() {
                        private final int ownerId = playerId;
                        private final int entityId = movingId;
                        private final char unitNum = movingUnit;
                        public boolean accept( Entity entity ) {
                            if ( entity instanceof Protomech &&
                                 entity.isSelectableThisTurn() &&
                                 ownerId == entity.getOwnerId() &&
                                 entityId != entity.getId() &&
                                 unitNum == entity.getUnitNumber() )
                                return true;
                            return false;
                        }
                    } );

            // Add the correct number of turns for the Protomech unit number.
            for (int i = 0; i < protoTurns; i++) {
                GameTurn newTurn = new GameTurn.UnitNumberTurn
                    ( playerId, movingUnit );
                game.insertNextTurn(newTurn);
                turnsChanged = true;
            }
        }

        // Otherwise, we may need to add turns for the "*_move_multi" options.
        else if ( ( (infMoved && infMoveMulti) ||
                    (protosMoved && protosMoveMulti) ) &&
                  isGeneralMoveTurn ) {
            int remaining = 0;

            // Calculate the number of EntityClassTurns need to be added.
            if ( infMoveMulti ) {
                remaining += game.getInfantryLeft(playerId);
            }
            if ( protosMoveMulti ) {
                remaining += game.getProtomechsLeft(playerId);
            }
            int moreInfAndProtoTurns =
                Math.min(game.getOptions().intOption("inf_proto_move_multi") - 1, remaining);

            // Add the correct number of turns for the right unit classes.
            for (int i = 0; i < moreInfAndProtoTurns; i++) {
                GameTurn newTurn =
                    new GameTurn.EntityClassTurn( playerId, multiMask );
                game.insertNextTurn(newTurn);
                turnsChanged = true;
            }
        }
        // brief everybody on the turn update, if they changed
        if (turnsChanged) {
            send(createTurnVectorPacket());
        }

        // move along
        changeToNextTurn();
    }

    /**
     * Changes the current phase, does some bookkeeping and
     * then tells the players.
     * @param phase the <code>int</code> id of the phase to change to
     */
    private void changePhase(int phase) {
        game.setLastPhase(game.getPhase());
        game.setPhase(phase);

        // prepare for the phase
        prepareForPhase(phase);

        if (isPhasePlayable(phase)) {
            // tell the players about the new phase
            send(new Packet(Packet.COMMAND_PHASE_CHANGE, new Integer(phase)));

            // post phase change stuff
            executePhase(phase);
        } else {
            endCurrentPhase();
        }
    }

    /**
     * Prepares for, presumably, the next phase.  This typically involves
     * resetting the states of entities in the game and making sure the client
     * has the information it needs for the new phase.
     * @param phase the <code>int</code> id of the phase to prepare for
     */
    private void prepareForPhase(int phase) {
        switch (phase) {
            case IGame.PHASE_LOUNGE :
                clearReports();
                mapSettings.setBoardsAvailableVector
                    ( scanForBoards(mapSettings.getBoardWidth(),
                                    mapSettings.getBoardHeight()) );
                mapSettings.setNullBoards(DEFAULT_BOARD);
                send(createMapSettingsPacket());
                break;
            case IGame.PHASE_INITIATIVE :
                // remove the last traces of last round
                game.resetActions();
                game.resetTagInfo();
                clearReports();
                resetEntityRound();
                resetEntityPhase(phase);
                checkForObservers();
                // roll 'em
                resetActivePlayersDone();
                rollInitiative();

                if ( !game.shouldDeployThisRound() )
                    incrementAndSendGameRound();

                //setIneligible(phase);
                determineTurnOrder(phase);
                writeInitiativeReport(false);
                System.out.println("Round " + game.getRoundCount() + " memory usage: " + MegaMek.getMemoryUsed());
                break;
            case IGame.PHASE_DEPLOY_MINEFIELDS :
                checkForObservers();
                resetActivePlayersDone();
                setIneligible(phase);

                Enumeration e = game.getPlayers();
                Vector turns = new Vector();
                while (e.hasMoreElements()) {
                    Player p = (Player) e.nextElement();
                    if (p.hasMinefields()) {
                        GameTurn gt = new GameTurn(p.getId());
                        turns.addElement(gt);
                    }
                }
                game.setTurnVector(turns);
                game.resetTurnIndex();

                // send turns to all players
                send(createTurnVectorPacket());
                break;
            case IGame.PHASE_SET_ARTYAUTOHITHEXES :
                // place off board entities actually off-board
                Enumeration i = game.getEntities();
                while (i.hasMoreElements()) {
                    Entity en = (Entity) i.nextElement();
                    en.deployOffBoard();
                }
                checkForObservers();
                resetActivePlayersDone();
                setIneligible(phase);

                i = game.getPlayers();
                Vector turn = new Vector();

                // Walk through the players of the game, and add
                // a turn for all players with artillery weapons.
                while (i.hasMoreElements()) {

                    // Get the next player.
                    final Player p = (Player) i.nextElement();

                    // Does the player have any artillery-equipped units?
                    EntitySelector playerArtySelector = new EntitySelector() {
                            private Player owner = p;
                            public boolean accept (Entity entity) {
                                if ( owner.equals( entity.getOwner() ) &&
                                     entity.isEligibleForTargetingPhase() )
                                    return true;
                                return false;
                            }
                        };
                    if ( game.getSelectedEntities( playerArtySelector )
                         .hasMoreElements() ) {

                        // Yes, the player has arty-equipped units.
                        GameTurn gt = new GameTurn(p.getId());
                        turn.addElement(gt);
                    }
                }
                game.setTurnVector(turn);
                game.resetTurnIndex();

                // send turns to all players
                send(createTurnVectorPacket());
                break;
            case IGame.PHASE_MOVEMENT :
            case IGame.PHASE_DEPLOYMENT :
            case IGame.PHASE_FIRING :
            case IGame.PHASE_PHYSICAL :
            case IGame.PHASE_TARGETING:
            case IGame.PHASE_OFFBOARD:
                resetEntityPhase(phase);
                checkForObservers();
                setIneligible(phase);
                determineTurnOrder(phase);
                resetActivePlayersDone();
                //send(createEntitiesPacket());
                entityAllUpdate();
                clearReports();
                doTryUnstuck();
                break;
            case IGame.PHASE_END :
                resetEntityPhase(phase);
                clearReports();
                resolveHeat();
                //write End Phase header
                addReport(new Report(5005, Report.PUBLIC));
                checkForSuffocation();
                if (game.getOptions().booleanOption("vacuum")) {
                    checkForVacuumDeath();
                }
                for(Enumeration<DynamicTerrainProcessor> tps=terrainProcessors.elements();tps.hasMoreElements();) {
                    DynamicTerrainProcessor tp = tps.nextElement();
                    tp.DoEndPhaseChanges(vPhaseReport);
                }
                addReport( game.ageFlares());
                send(createFlarePacket());
                resolveExtremeTempInfantryDeath();
                resolveAmmoDumps();
                resolveCrewDamage();
                resolveCrewWakeUp();
                resolveMechWarriorPickUp();
                resolveVeeINarcPodRemoval();
                checkForObservers();
                entityAllUpdate();
                break;
            case IGame.PHASE_INITIATIVE_REPORT :
                autoSave();
            case IGame.PHASE_MOVEMENT_REPORT :
            case IGame.PHASE_OFFBOARD_REPORT :
            case IGame.PHASE_FIRING_REPORT :
            case IGame.PHASE_PHYSICAL_REPORT :
            case IGame.PHASE_END_REPORT :
                resetActivePlayersDone();
                sendReport();
                if (game.getOptions().booleanOption("paranoid_autosave")) autoSave();
                break;
            case IGame.PHASE_VICTORY :
                resetPlayersDone();
                clearReports();
                prepareVictoryReport();
                game.addReports(vPhaseReport);
                send(createFullEntitiesPacket());
                send(createReportPacket(null));
                send(createEndOfGamePacket());
                break;
        }
    }

    /**
     * Should we play this phase or skip it?
     */
    private boolean isPhasePlayable(int phase) {
        switch (phase) {
            case IGame.PHASE_INITIATIVE :
            case IGame.PHASE_END :
                return false;
            case IGame.PHASE_SET_ARTYAUTOHITHEXES :
            case IGame.PHASE_DEPLOY_MINEFIELDS :
            case IGame.PHASE_DEPLOYMENT :
            case IGame.PHASE_MOVEMENT :
            case IGame.PHASE_FIRING :
            case IGame.PHASE_PHYSICAL :
            case IGame.PHASE_TARGETING:
            case IGame.PHASE_OFFBOARD :
                return game.hasMoreTurns();
            default :
                return true;
        }
    }

    /**
     * Do anything we seed to start the new phase, such as give a turn to
     * the first player to play.
     */
    private void executePhase(int phase) {
        switch (phase) {
            case IGame.PHASE_EXCHANGE :
                resetPlayersDone();
                // Build teams vector
                game.setupTeams();
                applyBoardSettings();
                game.setupRoundDeployment();
                game.determineWind();
                // If we add transporters for any Magnetic Clamp
                // equiped squads, then update the clients' entities.
                if ( game.checkForMagneticClamp() ) {
                    entityAllUpdate();
                }
                // transmit the board to everybody
                send(createBoardPacket());
                break;
            case IGame.PHASE_MOVEMENT :
                //write Movement Phase header to report
                addReport(new Report(2000, Report.PUBLIC));
            case IGame.PHASE_SET_ARTYAUTOHITHEXES :
            case IGame.PHASE_DEPLOY_MINEFIELDS :
            case IGame.PHASE_DEPLOYMENT :
            case IGame.PHASE_FIRING :
            case IGame.PHASE_PHYSICAL :
            case IGame.PHASE_TARGETING :
            case IGame.PHASE_OFFBOARD :
                changeToNextTurn();
                if (game.getOptions().booleanOption("paranoid_autosave")) autoSave();
                break;
        }
    }

    /**
     * Ends this phase and moves on to the next.
     */
    private void endCurrentPhase() {
        switch (game.getPhase()) {
            case IGame.PHASE_LOUNGE :
                changePhase(IGame.PHASE_EXCHANGE);
                break;
            case IGame.PHASE_EXCHANGE :
                changePhase(IGame.PHASE_SET_ARTYAUTOHITHEXES);
                break;
            case IGame.PHASE_STARTING_SCENARIO :
                changePhase(IGame.PHASE_SET_ARTYAUTOHITHEXES);
                break;
            case IGame.PHASE_SET_ARTYAUTOHITHEXES :
                Enumeration e = game.getPlayers();
                boolean mines = false;
                while (e.hasMoreElements()) {
                    Player p = (Player) e.nextElement();
                    if (p.hasMinefields()) {
                         mines = true;
                    }
                }
                if (mines) {
                    changePhase(IGame.PHASE_DEPLOY_MINEFIELDS);
                } else {
                    changePhase(IGame.PHASE_INITIATIVE);
                }
                break;
            case IGame.PHASE_DEPLOY_MINEFIELDS :
                changePhase(IGame.PHASE_INITIATIVE);
                break;
            case IGame.PHASE_DEPLOYMENT :
                game.clearDeploymentThisRound();
                game.checkForCompleteDeployment();
                Enumeration pls = game.getPlayers();
                while (pls.hasMoreElements()) {
                    Player p = (Player) pls.nextElement();
                    p.adjustStartingPosForReinforcements();
                }

                changePhase(IGame.PHASE_INITIATIVE);
                break;
            case IGame.PHASE_INITIATIVE :
                game.addReports(vPhaseReport);
                changePhase(IGame.PHASE_INITIATIVE_REPORT);
                break;
            case IGame.PHASE_INITIATIVE_REPORT :
                //boolean doDeploy = game.shouldDeployThisRound() && (game.getLastPhase() != IGame.PHASE_DEPLOYMENT);
                if ( game.shouldDeployThisRound() ) {
                    changePhase(IGame.PHASE_DEPLOYMENT);
                } else {
                    changePhase(IGame.PHASE_TARGETING);
                }
                break;
            case IGame.PHASE_MOVEMENT :
                doAllAssaultDrops();
                addMovementHeat();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls(); // Skids cause damage in movement phase
                resolveCrewDamage(); // again, I guess
                checkForFlamingDeath();
                // check phase report
                if (vPhaseReport.size() > 1) {
                    game.addReports(vPhaseReport);
                    changePhase(IGame.PHASE_MOVEMENT_REPORT);
                } else {
                    //just the header, so we'll add the <nothing> label
                    addReport(new Report(1205, Report.PUBLIC));
                    game.addReports(vPhaseReport);
                    sendReport();
                    changePhase(IGame.PHASE_OFFBOARD);
                }
                break;
            case IGame.PHASE_MOVEMENT_REPORT :
                changePhase(IGame.PHASE_OFFBOARD);
                break;
            case IGame.PHASE_FIRING :
                resolveAllButWeaponAttacks();
                assignAMS();
                resolveOnlyWeaponAttacks();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                // check phase report
                if (vPhaseReport.size() > 1) {
                    game.addReports(vPhaseReport);
                    changePhase(IGame.PHASE_FIRING_REPORT);
                } else {
                    //just the header, so we'll add the <nothing> label
                    addReport(new Report(1205, Report.PUBLIC));
                    sendReport();
                    game.addReports(vPhaseReport);
                    changePhase(IGame.PHASE_PHYSICAL);
                }
                break;
            case IGame.PHASE_FIRING_REPORT :
                changePhase(IGame.PHASE_PHYSICAL);
                break;
            case IGame.PHASE_PHYSICAL :
                resolvePhysicalAttacks();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                resolveSinkVees();
                // check phase report
                if (vPhaseReport.size() > 1) {
                    game.addReports(vPhaseReport);
                    changePhase(IGame.PHASE_PHYSICAL_REPORT);
                } else {
                    //just the header, so we'll add the <nothing> label
                    addReport(new Report(1205, Report.PUBLIC));
                    game.addReports(vPhaseReport);
                    sendReport();
                    changePhase(IGame.PHASE_END);
                }
                break;
            case IGame.PHASE_PHYSICAL_REPORT :
                changePhase(IGame.PHASE_END);
                break;
            case IGame.PHASE_TARGETING :
                enqueueIndirectArtilleryAttacks();
                changePhase(IGame.PHASE_MOVEMENT);
                break;
            case IGame.PHASE_OFFBOARD :
                //write Offboard Attack Phase header
                addReport(new Report(1100, Report.PUBLIC));
                resolveAllButWeaponAttacks(); //torso twist or flip arms possible
                resolveOnlyWeaponAttacks(); //should only be TAG at this point
                resolveIndirectArtilleryAttacks();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                //check reports
                if (vPhaseReport.size() > 1) {
                    game.addReports(vPhaseReport);
                    changePhase(IGame.PHASE_OFFBOARD_REPORT);
                } else {
                    //just the header, so we'll add the <nothing> label
                    addReport(new Report(1205, Report.PUBLIC));
                    game.addReports(vPhaseReport);
                    sendReport();
                    changePhase(IGame.PHASE_FIRING);
                }
                break;
            case IGame.PHASE_OFFBOARD_REPORT:
                changePhase(IGame.PHASE_FIRING);
                break;
            case IGame.PHASE_END :
                // check phase report
                //HACK: hardcoded message ID check
                if (vPhaseReport.size() > 3
                    || ((Report)vPhaseReport.elementAt(1)).messageId != 1205) {
                    game.addReports(vPhaseReport);
                    changePhase(IGame.PHASE_END_REPORT);
                } else {
                    //just the heat and end headers, so we'll add
                    // the <nothing> label
                    addReport(new Report(1205, Report.PUBLIC));
                    game.addReports(vPhaseReport);
                    sendReport();
                    if (victory()) {
                        changePhase(IGame.PHASE_VICTORY);
                    } else {
                        changePhase(IGame.PHASE_INITIATIVE);
                    }
                }
                break;
            case IGame.PHASE_END_REPORT :
                if (victory()) {
                    changePhase(IGame.PHASE_VICTORY);
                } else {
                    changePhase(IGame.PHASE_INITIATIVE);
                }
                break;
            case IGame.PHASE_VICTORY :
                resetGame();
                break;
        }
    }

    /**
     * Increment's the server's game round and send it to all the clients
     */
    private void incrementAndSendGameRound() {
        game.incrementRoundCount();
        send(new Packet(Packet.COMMAND_ROUND_UPDATE, new Integer(game.getRoundCount())));
    }

    /**
     * Tries to change to the next turn.  If there are no more turns, ends the
     * current phase.  If the player whose turn it is next is not connected,
     * we allow the other players to skip that player.
     */
    private void changeToNextTurn() {
        // if there aren't any more turns, end the phase
        if (!game.hasMoreTurns()) {
            endCurrentPhase();
            return;
        }

        // okay, well next turn then!
        GameTurn nextTurn = game.changeToNextTurn();
        send(createTurnIndexPacket());

        Player player = getPlayer( nextTurn.getPlayerNum() );
        if ( null != player && player.isGhost() ) {
            sendGhostSkipMessage( player );
        }
        else if ( null == game.getFirstEntity()
                  && null != player
                  && ((game.getPhase() != IGame.PHASE_DEPLOY_MINEFIELDS) && (game.getPhase() != IGame.PHASE_SET_ARTYAUTOHITHEXES))) {
            sendTurnErrorSkipMessage( player );
        }
    }

    /**
     * Sends out a notification message indicating that a ghost player may
     * be skipped.
     *
     * @param   ghost - the <code>Player</code> who is ghosted.
     *          This value must not be <code>null</code>.
     */
    private void sendGhostSkipMessage( Player ghost ) {
        StringBuffer message = new StringBuffer();
        message.append( "Player '" )
            .append( ghost.getName() )
            .append( "' is disconnected.  You may skip his/her current turn with the /skip command." );
        sendServerChat( message.toString() );
    }

    /**
     * Sends out a notification message indicating that the current turn is an
     * error and should be skipped.
     *
     * @param   skip - the <code>Player</code> who is to be skipped.
     *          This value must not be <code>null</code>.
     */
    private void sendTurnErrorSkipMessage( Player skip ) {
        StringBuffer message = new StringBuffer();
        message.append( "Player '" )
            .append( skip.getName() )
            .append( "' has no units to move.  You should skip his/her/your current turn with the /skip command. You may want to report this error.  See the MegaMek homepage (http://megamek.sf.net/) for details." );
        sendServerChat( message.toString() );
    }

    /**
     * Skips the current turn.  This only makes sense in phases that have turns.
     * Operates by finding an entity to move and then doing nothing with it.
     */
    public void skipCurrentTurn() {
        // find an entity to skip...
        Entity toSkip = game.getFirstEntity();

        switch (game.getPhase()) {
            case IGame.PHASE_DEPLOYMENT :
                sendServerChat("Turns cannot be skipped in the deployment phase.");
                break;
            case IGame.PHASE_MOVEMENT :
                if ( toSkip != null ) {
                    processMovement(toSkip, new MovePath(game, toSkip));
                }
                endCurrentTurn(toSkip);
                break;
            case IGame.PHASE_FIRING :
            case IGame.PHASE_PHYSICAL :
            case IGame.PHASE_TARGETING :
            case IGame.PHASE_OFFBOARD :
                if ( toSkip != null ) {
                    processAttack(toSkip, new Vector(0));
                }
                endCurrentTurn(toSkip);
                break;
            default :

        }
    }

    /**
     * Returns true if the current turn may be skipped.  Ghost players' turns
     * are skippable, and a turn should be skipped if there's nothing to move.
     */
    public boolean isTurnSkippable() {
        GameTurn turn = game.getTurn();
        if (null == turn) return false;
        Player player = getPlayer( turn.getPlayerNum() );
        return ( null == player || player.isGhost()
                 || game.getFirstEntity() == null );
    }

    /**
     * Returns true if victory conditions have been met.  Victory conditions
     * are when there is only one player left with mechs or only one team.
     */
    public boolean victory() {
        if (game.isForceVictory()) {
            int victoryPlayerId = game.getVictoryPlayerId();
            int victoryTeam = game.getVictoryTeam();
            Vector players = game.getPlayersVector();
            boolean forceVictory = true;

            // Individual victory.
            if (victoryPlayerId != Player.PLAYER_NONE) {
                for (int i = 0; i < players.size(); i++) {
                    Player player = (Player) players.elementAt(i);

                    if (player.getId() != victoryPlayerId && !player.isObserver()) {
                        if (!player.admitsDefeat()) {
                            forceVictory = false;
                            break;
                        }
                    }
                }
            }
            // Team victory.
            if (victoryTeam != Player.TEAM_NONE) {
                for (int i = 0; i < players.size(); i++) {
                    Player player = (Player) players.elementAt(i);

                    if (player.getTeam() != victoryTeam && !player.isObserver()) {
                        if (!player.admitsDefeat()) {
                            forceVictory = false;
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < players.size(); i++) {
                Player player = (Player) players.elementAt(i);
                player.setAdmitsDefeat(false);
            }

            if (forceVictory) {
                return true;
            }
            cancelVictory();
        }

        if (!game.getOptions().booleanOption("check_victory")) {
            return false;
        }

        // check all players/teams for aliveness
        int playersAlive = 0;
        Player lastPlayer = null;
        boolean oneTeamAlive = false;
        int lastTeam = Player.TEAM_NONE;
        boolean unteamedAlive = false;
        for (Enumeration e = game.getPlayers(); e.hasMoreElements();) {
            Player player = (Player)e.nextElement();
            int team = player.getTeam();
            if (game.getLiveDeployedEntitiesOwnedBy(player) <= 0) {
                continue;
            }
            // we found a live one!
            playersAlive++;
            lastPlayer = player;
            // check team
            if (team == Player.TEAM_NONE) {
                unteamedAlive = true;
            } else if (lastTeam == Player.TEAM_NONE) {
                // possibly only one team alive
                oneTeamAlive = true;
                lastTeam = team;
            } else if (team != lastTeam) {
                // more than one team alive
                oneTeamAlive = false;
                lastTeam = team;
            }
        }

        // check if there's one player alive
        if (playersAlive < 1) {
            game.setVictoryPlayerId( Player.PLAYER_NONE );
            game.setVictoryTeam( Player.TEAM_NONE );
            return true;
        }
        else if ( playersAlive == 1 ) {
            if (lastPlayer.getTeam() == Player.TEAM_NONE) {
                // individual victory
                game.setVictoryPlayerId(lastPlayer.getId());
                game.setVictoryTeam(Player.TEAM_NONE);
                return true;
            }
        }

        // did we only find one live team?
        if (oneTeamAlive && !unteamedAlive) {
            // team victory
            game.setVictoryPlayerId(Player.PLAYER_NONE);
            game.setVictoryTeam(lastTeam);
            return true;
        }

        return false;
    }

    /**
     * Applies board settings.  This loads and combines all the boards that
     * were specified into one mega-board and sets that board as current.
     */
    public void applyBoardSettings() {
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        IBoard[] sheetBoards = new IBoard[mapSettings.getMapWidth() * mapSettings.getMapHeight()];
        for (int i = 0; i < mapSettings.getMapWidth() * mapSettings.getMapHeight(); i++) {
            sheetBoards[i] = new Board();
            String name = (String)mapSettings.getBoardsSelectedVector().elementAt(i);
            boolean isRotated = false;
            if ( name.startsWith( Board.BOARD_REQUEST_ROTATION ) ) {
                isRotated = true;
                name = name.substring( Board.BOARD_REQUEST_ROTATION.length() );
            }
            if (name.startsWith(MapSettings.BOARD_GENERATED)) {
                sheetBoards[i] = BoardUtilities.generateRandom(mapSettings);
            } else {
                sheetBoards[i].load( name + ".board");
                BoardUtilities.flip(sheetBoards[i], isRotated, isRotated );
            }
        }
        IBoard newBoard = BoardUtilities.combine(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(),
                mapSettings.getMapWidth(), mapSettings.getMapHeight(), sheetBoards);
        game.setBoard(newBoard);
    }


    /**
     * Rolls initiative for all the players.
     */
    private void rollInitiative() {
        if(game.getOptions().booleanOption("individual_initiative")) {
            TurnOrdered.rollInitiative(game.getEntitiesVector());
        } else {
            // Roll for initative on the teams.
            TurnOrdered.rollInitiative(game.getTeamsVector());
        }

        transmitAllPlayerUpdates();
    }

    /**
     * Determines the turn oder for a given phase (with individual init)
     * @param phase the <code>int</code> id of the phase
     */
    private void determineTurnOrderIUI(int phase) {
        for (Enumeration loop = game.getEntities(); loop.hasMoreElements();) {
            final Entity entity = (Entity)loop.nextElement();
            entity.resetOtherTurns();
            if (entity.isSelectableThisTurn()) {
                entity.incrementOtherTurns();
            }
        }
        // Now, generate the global order of all teams' turns.
        TurnVectors team_order = TurnOrdered.generateTurnOrder
            ( game.getEntitiesVector(), game );

        // See if there are any loaded units stranded on immobile transports.
        Enumeration strandedUnits = game.getSelectedEntities
            ( new EntitySelector() {
                    public boolean accept( Entity entity ) {
                        if ( Server.this.game.isEntityStranded(entity) )
                            return true;
                        return false;
                    }
                } );

        // Now, we collect everything into a single vector.
        Vector turns;

        if ( strandedUnits.hasMoreElements() &&
             game.getPhase() == IGame.PHASE_MOVEMENT ) {
            // Add a game turn to unload stranded units, if this
            //  is the movement phase.
            turns = new Vector( team_order.getNormalTurns() +
                                team_order.getEvenTurns() + 1);
            turns.addElement( new GameTurn.UnloadStrandedTurn(strandedUnits) );
        } else {
            // No stranded units.
            turns = new Vector( team_order.getNormalTurns() +
                                team_order.getEvenTurns() );
        }
        
        //add the turns (this is easy)
        while(team_order.hasMoreElements()) {
            Entity e = (Entity)team_order.nextElement();
            if(e.isSelectableThisTurn())
                turns.addElement(new GameTurn.SpecificEntityTurn(e.getOwnerId(), e.getId()));
        }
        
        // set fields in game
        game.setTurnVector(turns);
        game.resetTurnIndex();

        // send turns to all players
        send(createTurnVectorPacket());
    }
    /**
     * Determines the turn oder for a given phase
     * @param phase the <code>int</code> id of the phase
     */
    private void determineTurnOrder(int phase) {

        if(game.getOptions().booleanOption("individual_initiative")) {
            determineTurnOrderIUI(phase);
            return;
        }
        // and/or deploy even according to game options.
        boolean infMoveEven =
            ( game.getOptions().booleanOption("inf_move_even") &&
              (game.getPhase() == IGame.PHASE_INITIATIVE ||
               game.getPhase() == IGame.PHASE_MOVEMENT) ) ||
            ( game.getOptions().booleanOption("inf_deploy_even") &&
              game.getPhase() == IGame.PHASE_DEPLOYMENT );
        boolean infMoveMulti =
            ( game.getOptions().booleanOption("inf_move_multi") &&
              (game.getPhase() == IGame.PHASE_INITIATIVE ||
               game.getPhase() == IGame.PHASE_MOVEMENT) );
        boolean protosMoveEven =
            ( game.getOptions().booleanOption("protos_move_even") &&
              (game.getPhase() == IGame.PHASE_INITIATIVE ||
               game.getPhase() == IGame.PHASE_MOVEMENT) ) ||
            ( game.getOptions().booleanOption("protos_deploy_even") &&
              game.getPhase() == IGame.PHASE_DEPLOYMENT );
        boolean protosMoveMulti =
            game.getOptions().booleanOption("protos_move_multi");
        boolean protosFireMulti = !protosMoveMulti &&
            game.getPhase() == IGame.PHASE_FIRING;
        int evenMask = 0;
        if ( infMoveEven ) evenMask += GameTurn.CLASS_INFANTRY;
        if ( protosMoveEven ) evenMask += GameTurn.CLASS_PROTOMECH;

        // Reset all of the Players' turn category counts
        for (Enumeration loop = game.getPlayers(); loop.hasMoreElements();) {
            final Player player = (Player) loop.nextElement();
            player.resetEvenTurns();
            player.resetMultiTurns();
            player.resetOtherTurns();

            // Add turns for protomechs weapons declaration.
            if ( protosFireMulti ) {

                // How many Protomechs does the player have?
                int numPlayerProtos = game.getSelectedEntityCount
                    ( new EntitySelector() {
                            private final int ownerId = player.getId();
                            public boolean accept( Entity entity ) {
                                if ( entity instanceof Protomech &&
                                     ownerId == entity.getOwnerId() )
                                    return true;
                                return false;
                            }
                        } );
                int numProtoUnits =
                    (int) Math.ceil( (numPlayerProtos) / 5.0 );
                for ( int unit = 0; unit < numProtoUnits; unit++ ) {
                    if ( protosMoveEven ) player.incrementEvenTurns();
                    else player.incrementOtherTurns();
                }

            } // End handle-proto-firing-turns

        } // Handle the next player

        // Go through all entities, and update the turn categories of the
        // entity's player.  The teams get their totals from their players.
        // N.B. protomechs declare weapons fire based on their point.
        for (Enumeration loop = game.getEntities(); loop.hasMoreElements();) {
            final Entity entity = (Entity)loop.nextElement();
            if (entity.isSelectableThisTurn()) {
                final Player player = entity.getOwner();
                if ( entity instanceof Infantry ) {
                    if ( infMoveEven ) player.incrementEvenTurns();
                    else if ( infMoveMulti ) player.incrementMultiTurns();
                    else player.incrementOtherTurns();
                }
                else if ( entity instanceof Protomech ) {
                    if ( !protosFireMulti ) {
                        if ( protosMoveEven ) player.incrementEvenTurns();
                        else if ( protosMoveMulti ) player.incrementMultiTurns();
                        else player.incrementOtherTurns();
                    }
                }
                else
                    player.incrementOtherTurns();
            }
        }

        // Generate the turn order for the Players *within*
        // each Team.  Map the teams to their turn orders.
        // Count the number of teams moving this turn.
        int nTeams = game.getNoOfTeams();
        Hashtable allTeamTurns = new Hashtable( nTeams );
        Hashtable evenTrackers = new Hashtable( nTeams );
        int numTeamsMoving = 0;
        for (Enumeration loop = game.getTeams(); loop.hasMoreElements(); ) {
            final Team team = (Team) loop.nextElement();
            allTeamTurns.put( team, team.determineTeamOrder(game) );

            // Track both the number of times we've checked the team for
            // "leftover" turns, and the number of "leftover" turns placed.
            int[] evenTracker = new int[2];
            evenTracker[0] = 0;
            evenTracker[1] = 0;
            evenTrackers.put (team, evenTracker);

            // Count this team if it has any "normal" moves.
            if (team.getNormalTurns(game) > 0)
                numTeamsMoving++;
        }

        // Now, generate the global order of all teams' turns.
        TurnVectors team_order = TurnOrdered.generateTurnOrder
            ( game.getTeamsVector(), game );

        // See if there are any loaded units stranded on immobile transports.
        Enumeration strandedUnits = game.getSelectedEntities
            ( new EntitySelector() {
                    public boolean accept( Entity entity ) {
                        if ( Server.this.game.isEntityStranded(entity) )
                            return true;
                        return false;
                    }
                } );

        // Now, we collect everything into a single vector.
        Vector turns;

        if ( strandedUnits.hasMoreElements() &&
             game.getPhase() == IGame.PHASE_MOVEMENT ) {
            // Add a game turn to unload stranded units, if this
            //  is the movement phase.
            turns = new Vector( team_order.getNormalTurns() +
                                team_order.getEvenTurns() + 1);
            turns.addElement( new GameTurn.UnloadStrandedTurn(strandedUnits) );
        } else {
            // No stranded units.
            turns = new Vector( team_order.getNormalTurns() +
                                team_order.getEvenTurns() );
        }

        // Walk through the global order, assigning turns
        // for individual players to the single vector.
        // Keep track of how many turns we've added to the vector.
        Team prevTeam = null;
        int min = team_order.getMin();
        for ( int numTurn = 0; team_order.hasMoreElements(); numTurn++ ) {
            Team team = (Team) team_order.nextElement();
            TurnVectors withinTeamTurns = (TurnVectors) allTeamTurns.get(team);

            int[] evenTracker = (int[]) evenTrackers.get (team);
            float teamEvenTurns = team.getEvenTurns();

            // Calculate the number of "even" turns to add for this team.
            int numEven = 0;
            if (1 == numTeamsMoving) {
                // The only team moving should move all "even" units.
                numEven += teamEvenTurns;
            }
            else if (prevTeam == null) {
                // Increment the number of times we've checked for "leftovers".
                evenTracker[0]++;

                // The first team to move just adds the "baseline" turns.
                numEven += teamEvenTurns / min;
            }
            else if (!team.equals(prevTeam)) {
                // Increment the number of times we've checked for "leftovers".
                evenTracker[0]++;

                // This wierd equation attempts to spread the "leftover"
                // turns accross the turn's moves in a "fair" manner.
                // It's based on the number of times we've checked for
                // "leftovers" the number of "leftovers" we started with,
                // the number of times we've added a turn for a "leftover",
                // and the total number of times we're going to check.
                numEven += Math.ceil (evenTracker[0] * (teamEvenTurns % min)
                                       / min - 0.5) - evenTracker[1];

                // Update the number of turns actually added for "leftovers".
                evenTracker[1] += numEven;

                // Add the "baseline" number of turns.
                numEven += teamEvenTurns / min;
            }

            // Record this team for the next move.
            prevTeam = team;

            // This may be a "placeholder" for a team without "normal" turns.
            if (withinTeamTurns.hasMoreElements()) {

                // Not a placeholder... get the player who moves next.
                Player player = (Player) withinTeamTurns.nextElement();

                // If we've added all "normal" turns, allocate turns
                // for the infantry and/or protomechs moving even.
                GameTurn turn = null;
                if ( numTurn >= team_order.getNormalTurns() ) {
                    turn = new GameTurn.EntityClassTurn
                        (player.getId(), evenMask);
                }

                // If either Infantry or Protomechs move even, only allow
                // the other classes to move during the "normal" turn.
                else if ( infMoveEven || protosMoveEven ) {
                    turn = new GameTurn.EntityClassTurn
                        (player.getId(), ~evenMask);
                }

                // Otherwise, let *anybody* move.
                else {
                    turn = new GameTurn( player.getId() );
                }
                turns.addElement(turn);

            } // End team-has-"normal"-turns

            // Add the calculated number of "even" turns.
            // Allow the player at least one "normal" turn before the
            // "even" turns to help with loading infantry in deployment.
            while (numEven > 0 && withinTeamTurns.hasMoreEvenElements()) {
                Player evenPlayer = (Player) withinTeamTurns.nextEvenElement();
                turns.addElement
                    (new GameTurn.EntityClassTurn (evenPlayer.getId(),
                                                   evenMask));
                numEven--;
            }
        }

        // set fields in game
        game.setTurnVector(turns);
        game.resetTurnIndex();

        // send turns to all players
        send(createTurnVectorPacket());

    }

    /**
     * Write the initiative results to the report
     */
    private void writeInitiativeReport(boolean abbreviatedReport) {
        // write to report
        Report r;
        boolean deployment = false;
        if (!abbreviatedReport) {
            r = new Report(1210);
            r.type = Report.PUBLIC;
            if ((game.getLastPhase() == IGame.PHASE_DEPLOYMENT) || game.isDeploymentComplete() || !game.shouldDeployThisRound()) {
                r.messageId = 1000;
                r.add(game.getRoundCount());
            } else {
                deployment = true;
                if ( game.getRoundCount() == 0 ) {
                    r.messageId = 1005;
                } else {
                    r.messageId = 1010;
                    r.add(game.getRoundCount());
                }
            }
            addReport(r);

            //write seperator
            addReport(new Report(1200, Report.PUBLIC));
        } else {
            addReport(new Report(1210, Report.PUBLIC)); //newline
        }

        if(game.getOptions().booleanOption("individual_initiative")) {
            r = new Report(1040, Report.PUBLIC);
            addReport(r);
            for(Enumeration<GameTurn> e = game.getTurns();e.hasMoreElements();) {
                GameTurn t = e.nextElement();
                if(t instanceof GameTurn.SpecificEntityTurn) {
                    Entity entity = game.getEntity(((GameTurn.SpecificEntityTurn)t).getEntityNum());
                    r = new Report(1045);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(entity.getInitiative().toString());
                    addReport(r);
                } else {
                    Player player = getPlayer( t.getPlayerNum() );
                    if ( null != player ) {
                        r = new Report(1050, Report.PUBLIC);
                        r.add(player.getName());
                        addReport(r);
                    }                    
                }
            }
        } else {
            for (Enumeration i = game.getTeams(); i.hasMoreElements();) {
                final Team team = (Team)i.nextElement();
    
                // If there is only one player, list them as the 'team', and
                // use the team iniative
                if (team.getSize() == 1) {
                    final Player player = (Player)team.getPlayers().nextElement();
                    r = new Report(1015, Report.PUBLIC);
                    r.add(player.getName());
                    r.add(team.getInitiative().toString());
                    addReport(r);
                } else {
                    // Multiple players.  List the team, then break it down.
                    r = new Report(1015, Report.PUBLIC);
                    r.add(Player.teamNames[team.getId()]);
                    r.add(team.getInitiative().toString());
                    addReport(r);
                    for( Enumeration j = team.getPlayers(); j.hasMoreElements();) {
                        final Player player = (Player)j.nextElement();
                        r = new Report(1015, Report.PUBLIC);
                        r.indent();
                        r.add(player.getName());
                        r.add(player.getInitiative().toString());
                        addReport(r);
                    }
                }
            }
    
            // The turn order is different in movement phase
            // if a player has any "even" moving units.
            r = new Report(1020, Report.PUBLIC);
    
            boolean firstTurn = true;
            boolean hasEven = false;
            for (Enumeration i = game.getTurns(); i.hasMoreElements();) {
                GameTurn turn = (GameTurn)i.nextElement();
                Player player = getPlayer( turn.getPlayerNum() );
                if ( null != player ) {
                    r.add(player.getName());
                    firstTurn = false;
                    if (player.getEvenTurns() > 0)
                        hasEven = true;
                }
            }
            r.newlines = 2;
            addReport(r);
            if (hasEven) {
                r = new Report(1021, Report.PUBLIC);
                if ((game.getOptions().booleanOption("inf_deploy_even")
                    || game.getOptions().booleanOption("protos_deploy_even")) &&
                    !(game.getLastPhase() == IGame.PHASE_END_REPORT))
                    r.choose(true);
                else r.choose(false);
                r.indent();
                r.newlines = 2;
                addReport(r);
            }
        }
        if (!abbreviatedReport) {
            //Wind direction and strength
            r = new Report(1025, Report.PUBLIC);
            r.add(game.getStringWindDirection());
            if (game.getWindStrength() != -1) {
                Report r2 = new Report(1030, Report.PUBLIC);
                r2.add(game.getStringWindStrength());
                r.newlines = 0;
                addReport(r);
                addReport(r2);
            } else {
                addReport(r);
            }

            if (deployment)
                addNewLines();
        }
    }

    /**
     * Marks ineligible entities as not ready for this phase
     */
    private void setIneligible(int phase) {
        Vector assistants = new Vector();
        boolean assistable = false;
        Entity entity = null;
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            entity = (Entity)e.nextElement();
            if (!entity.isEligibleFor(phase)) {
                assistants.addElement(entity);
            } else {
                assistable=true;
            }
        }
        for (int i=0;i<assistants.size();i++) {
            entity = (Entity)assistants.elementAt(i);
            if(!assistable || !(entity.canAssist(phase))) {
                entity.setDone(true);
            }
        }
    }

    /**
     * Have the loader load the indicated unit.
     * The unit being loaded loses its turn.
     *
     * @param   loader - the <code>Entity</code> that is loading the unit.
     * @param   unit - the <code>Entity</code> being loaded.
     */
    private void loadUnit( Entity loader, Entity unit ) {

        // Remove the *last* friendly turn (removing the *first* penalizes
        // the opponent too much, and re-calculating moves is too hard).
        game.removeTurnFor(unit);
        send(createTurnVectorPacket());

        // Load the unit.
        loader.load( unit );

        // The loaded unit is being carried by the loader.
        unit.setTransportId( loader.getId() );

        // Remove the loaded unit from the screen.
        unit.setPosition( null );

        // Update the loaded unit.
        this.entityUpdate( unit.getId() );
    }

    /**
     * Have the unloader unload the indicated unit.
     * The unit being unloaded does *not* gain a turn.
     *
     * @param   unloader - the <code>Entity</code> that is unloading the unit.
     * @param   unloaded - the <code>Targetable</code> unit being unloaded.
     * @param   pos - the <code>Coords</code> for the unloaded unit.
     * @param   facing - the <code>int</code> facing for the unloaded unit.
     * @param   elevation - the <code>int</code> elevation at which to unload,
     *                      if both loader and loaded units use VTOL movement.
     * @return  <code>true</code> if the unit was successfully unloaded,
     *          <code>false</code> if the unit isn't carried in unloader.
     */
    private boolean unloadUnit( Entity unloader, Targetable unloaded,
                             Coords pos, int facing, int elevation ) {

        // We can only unload Entities.
        Entity unit = null;
        if ( unloaded instanceof Entity ) {
            unit = (Entity) unloaded;
        } else {
            return false;
        }

        // Unload the unit.
        if ( !unloader.unload( unit ) ) {
            return false;
        }

        // The unloaded unit is no longer being carried.
        unit.setTransportId( Entity.NONE );

        // Place the unloaded unit onto the screen.
        unit.setPosition( pos );

        // Units unloaded onto the screen are deployed.
        if ( pos != null ) {
            unit.setDeployed( true );
        }

        // Point the unloaded unit in the given direction.
        unit.setFacing( facing );
        unit.setSecondaryFacing( facing );
        
        IHex hex = game.getBoard().getHex(pos);
        boolean isBridge = hex.containsTerrain(Terrains.PAVEMENT);

        if (unloader.getMovementMode() == IEntityMovementMode.VTOL) {
            if (unit.getMovementMode() == IEntityMovementMode.VTOL) {
                // Flying units onload to the same elevation as the flying transport
                unit.setElevation(elevation);
            } else if (game.getBoard().getBuildingAt(pos) != null) {
                // non-flying unit onloaded from a flying onto a building
                // -> sit on the roff
                unit.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
            }
        } else if (game.getBoard().getBuildingAt(pos) != null) {
            // non flying unit unloading units into a building
            // -> sit in the building at the same elevation
            unit.setElevation(elevation);
        } else if (hex.terrainLevel(Terrains.WATER)>0) {
            if (unit.getMovementMode() == IEntityMovementMode.HOVER ||
                unit.getMovementMode() == IEntityMovementMode.HYDROFOIL ||
                unit.getMovementMode() == IEntityMovementMode.NAVAL ||
                unit.getMovementMode() == IEntityMovementMode.SUBMARINE ||
                hex.containsTerrain(Terrains.ICE) ||
                isBridge) {
                // units that can float stay on the surface, or we go on the bridge
                // this means elevation 0, because elevation is relative to the surface
                unit.setElevation(0);
            } 
        } else {
            // default to the floor of the hex.
            // unit elevation is relative to the surface 
            unit.setElevation(hex.floor() - hex.surface());
        }
        doSetLocationsExposure(unit, hex, false, unit.getElevation());

        // Update the unloaded unit.
        this.entityUpdate( unit.getId() );

        // Unloaded successfully.
        return true;
    }

    /**
     * Record that the given building has been affected by the current
     * entity's movement.  At the end of the entity's movement, notify
     * the clients about the updates.
     *
     * @param   bldg - the <code>Building</code> that has been affected.
     * @param   collapse - a <code>boolean</code> value that specifies that
     *          the building collapsed (when <code>true</code>).
     */
    private void addAffectedBldg( Building bldg, boolean collapse ) {

        // If the building collapsed, then the clients have already
        // been notified, so remove it from the notification list.
        if ( collapse ) {
            System.err.print( "Removing building from a list of " + affectedBldgs.size() + "\n" );//killme
            this.affectedBldgs.remove( bldg );
            System.err.print( "... now list of " + affectedBldgs.size() + "\n" );//killme
        }

        // Otherwise, make sure that this building is tracked.
        else {
            this.affectedBldgs.put( bldg, Boolean.FALSE );
        }
    }

    /**
     * Walk through the building hexes that were affected by the recent
     * entity's movement.  Notify the clients about the updates to all
     * affected entities and uncollapsed buildings.  The affected hexes
     * is then cleared for the next entity's movement.
     */
    private void applyAffectedBldgs() {

        // Build a list of Building updates.
        Vector bldgUpdates = new Vector();

        // Only send a single turn update.
        boolean bTurnsChanged = false;

        // Walk the set of buildings.
        Enumeration bldgs = this.affectedBldgs.keys();
        while ( bldgs.hasMoreElements() ) {
            final Building bldg = (Building) bldgs.nextElement();

            // Walk through the building's coordinates.
            Enumeration bldgCoords = bldg.getCoords();
            while ( bldgCoords.hasMoreElements() ) {
                final Coords coords = (Coords) bldgCoords.nextElement();

                // Walk through the entities at these coordinates.
                Enumeration entities = game.getEntities( coords );
                while( entities.hasMoreElements() ) {
                    final Entity entity = (Entity) entities.nextElement();

                    // Is the entity infantry?
                    if ( entity instanceof Infantry ) {

                        // Is the infantry dead?
                        if ( entity.isDoomed() || entity.isDestroyed() ) {

                            // Has the entity taken a turn?
                            if ( !entity.isDone() ) {

                                // Dead entities don't take turns.
                                game.removeTurnFor(entity);
                                bTurnsChanged = true;

                            } // End entity-still-to-move

                            // Clean out the dead entity.
                            entity.setDestroyed(true);
                            game.moveToGraveyard(entity.getId());
                            send(createRemoveEntityPacket(entity.getId()));
                        }

                        // Infantry that aren't dead are damaged.
                        else {
                            this.entityUpdate( entity.getId() );
                        }

                    } // End entity-is-infantry

                } // Check the next entity.

            } // Handle the next hex in this building.

            // Add this building to the report.
            bldgUpdates.addElement( bldg );

        } // Handle the next affected building.

        // Did we update the turns?
        if ( bTurnsChanged ) {
            send(createTurnVectorPacket());
        }

        // Are there any building updates?
        if ( !bldgUpdates.isEmpty() ) {

            // Send the building updates to the clients.
            sendChangedCFBuildings( bldgUpdates );

            // Clear the list of affected buildings.
            this.affectedBldgs.clear();
        }

        // And we're done.
        return;

    } // End private void applyAffectedBldgs()

    /**
     * Receives an entity movement packet, and if valid, executes it and ends
     * the current turn.
     *
     */
    private void receiveMovement(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        MovePath md = (MovePath)packet.getObject(1);

        // is this the right phase?
        if (game.getPhase() != IGame.PHASE_MOVEMENT) {
            System.err.println("error: server got movement packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        if (!game.getTurn().isValid(connId, entity, game)) {
            System.err.println("error: server got invalid movement packet");
            return;
        }

        // looks like mostly everything's okay
        processMovement(entity, md);

        // Notify the clients about any building updates.
        applyAffectedBldgs();

        // Update visibility indications if using double blind.
        if (doBlind()) {
            updateVisibilityIndicator();
        }

        // This entity's turn is over.
        // N.B. if the entity fell, a *new* turn has already been added.
        endCurrentTurn(entity);
    }

    /**
     * Steps through an entity movement packet, executing it.
     */
    private void processMovement(Entity entity, MovePath md) {
        Report r;
        boolean sideslipped = false; // for VTOL sideslipping
        
        // check for fleeing
        if (md.contains(MovePath.STEP_FLEE)) {
            // Unit has fled the battlefield.
            r = new Report(2005, Report.PUBLIC);
            r.addDesc(entity);
            addReport(r);

            // Is the unit carrying passengers?
            final Vector passengers = entity.getLoadedUnits();
            if ( !passengers.isEmpty() ) {
                final Enumeration iter = passengers.elements();
                while ( iter.hasMoreElements() ) {
                    final Entity passenger = (Entity) iter.nextElement();
                    // Unit has fled the battlefield.
                    r = new Report(2010, Report.PUBLIC);
                    r.indent();
                    r.addDesc(passenger);
                    addReport(r);
                    game.removeEntity( passenger.getId(),
                            IEntityRemovalConditions.REMOVE_IN_RETREAT );
                    send( createRemoveEntityPacket(passenger.getId(),
                            IEntityRemovalConditions.REMOVE_IN_RETREAT) );
                }
            }

            // Handle any picked up MechWarriors
            Enumeration iter = entity.getPickedUpMechWarriors().elements();
            while (iter.hasMoreElements() ) {
                Integer mechWarriorId = (Integer)iter.nextElement();
                Entity mw = game.getEntity(mechWarriorId.intValue());

                // Is the MechWarrior an enemy?
                int condition = IEntityRemovalConditions.REMOVE_IN_RETREAT;
                r = new Report(2010);
                if (mw.isCaptured()) {
                    r = new Report(2015);
                    condition = IEntityRemovalConditions.REMOVE_CAPTURED;
                }
                game.removeEntity( mw.getId(), condition );
                send( createRemoveEntityPacket(mw.getId(), condition) );
                r.addDesc(mw);
                r.indent();
                addReport(r);
            }

            // Is the unit being swarmed?
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );

                // Has the swarmer taken a turn?
                if ( !swarmer.isDone() ) {

                    // Dead entities don't take turns.
                    game.removeTurnFor(swarmer);
                    send(createTurnVectorPacket());

                } // End swarmer-still-to-move

                // Unit has fled the battlefield.
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                r = new Report(2015, Report.PUBLIC);
                r.indent();
                r.addDesc(swarmer);
                addReport(r);
                game.removeEntity( swarmerId, IEntityRemovalConditions.REMOVE_CAPTURED );
                send( createRemoveEntityPacket(swarmerId,
                        IEntityRemovalConditions.REMOVE_CAPTURED) );
            }
            game.removeEntity( entity.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT );
            send( createRemoveEntityPacket(entity.getId(),
                    IEntityRemovalConditions.REMOVE_IN_RETREAT) );
            return;
        }

        if (md.contains(MovePath.STEP_EJECT)) {
            if (entity instanceof Mech) {
                r = new Report(2020);
                r.subject = entity.getId();
                r.add(entity.getCrew().getName());
                r.addDesc(entity);
                addReport(r);
            } else if (entity instanceof Tank) {
                r = new Report(2025);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
            }
            addReport( ejectEntity(entity, false));

            return;
        }

        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int curFacing = entity.getFacing();
        int curVTOLElevation = entity.getElevation();
        int distance = 0;
        int mpUsed = 0;
        int moveType = IEntityMovementType.MOVE_NONE;
        int overallMoveType = IEntityMovementType.MOVE_NONE;
        // if the entity already used some MPs,
        // it previously tried to get up and fell,
        // and then got another turn. set moveType
        // and overallMoveType accordingly
        if (entity.mpUsed > 0) {
            moveType = IEntityMovementType.MOVE_WALK;
            overallMoveType = IEntityMovementType.MOVE_WALK;
            if (entity.mpUsed > entity.getWalkMP()) {
                moveType = IEntityMovementType.MOVE_RUN;
                overallMoveType = IEntityMovementType.MOVE_RUN;
            }
        }
        boolean firstStep;
        boolean wasProne;
        boolean fellDuringMovement;
        int prevFacing = curFacing;
        IHex prevHex = null;
        final boolean isInfantry = (entity instanceof Infantry);
        AttackAction charge = null;
        PilotingRollData rollTarget;
        // cache this here, otherwise changing MP in the turn causes 
        // errorneous gravity PSRs
        int cachedGravityLimit = (IEntityMovementType.MOVE_JUMP == moveType)?
            entity.getOriginalJumpMP() : entity.getRunMP(false); 

        // Compile the move
        md.compile(game, entity);

        if (md.contains(MovePath.STEP_CLEAR_MINEFIELD)) {
            ClearMinefieldAction cma = new ClearMinefieldAction(entity.getId());
            entity.setClearingMinefield(true);
            game.addAction(cma);
        }

        // check for MASC failure
        if (entity instanceof Mech) {
            Vector crits = new Vector();
            Vector vReport = new Vector();
            if (((Mech)entity).checkForMASCFailure(md, vReport, crits)) {
                addReport(vReport);
                CriticalSlot cs = null;
                int loc = Entity.LOC_NONE;
                for(Enumeration e = crits.elements();e.hasMoreElements();) {
                    Object o = e.nextElement();
                    if(o instanceof Integer)
                        loc = (Integer) o;
                    else if (o instanceof CriticalSlot) {
                        cs = (CriticalSlot) o;
                        applyCriticalHit(entity, loc, cs, true);
                    }
                }
                // no movement after that
                md.clear();
            }
        }

        overallMoveType = md.getLastStepMovementType();

        //check for starting in liquid magma
        if(game.getBoard().getHex(entity.getPosition()).terrainLevel(Terrains.MAGMA) == 2
            && entity.getElevation() == 0) {
            doMagmaDamage(entity, false);
        }            

        // iterate through steps
        firstStep = true;
        fellDuringMovement = false;
        /* Bug 754610: Revert fix for bug 702735. */
        MoveStep prevStep = null;

        Vector movePath = new Vector();

        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            wasProne = entity.isProne();
            boolean isPavementStep = step.isPavementStep();
            boolean entityFellWhileAttemptingToStand = false;

            // stop for illegal movement
            if (step.getMovementType() == IEntityMovementType.MOVE_ILLEGAL) {
                break;
            }
            
            //stop if the entity already killed itself
            if(entity.isDestroyed() || entity.isDoomed()) {
                break;
            }
            
            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                entity.heatBuildup += 1;
                entity.setProne(false);
                entity.setHullDown(false);
                wasProne = false;
                game.resetPSRs(entity);
                entityFellWhileAttemptingToStand = !doSkillCheckInPlace(entity, rollTarget);
            }
            // did the entity just fall?
            if (entityFellWhileAttemptingToStand) {
                moveType = step.getMovementType();
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }

            if (step.getType() == MovePath.STEP_UNJAM_RAC) {
                entity.setUnjammingRAC(true);
                game.addAction(new UnjamAction(entity.getId()));

                break;
            }
            
            if (step.getType() == MovePath.STEP_LAY_MINE) {
                LayMinefieldAction lma = new LayMinefieldAction(entity.getId(), step.getMineToLay());
                game.addLayMinefieldAction(lma);
                entity.setLayingMines(true);
                break;
            }

            if (step.getType() == MovePath.STEP_SEARCHLIGHT && entity.hasSpotlight()) {
                final boolean SearchOn = !entity.isUsingSpotlight();
                entity.setSpotlightState(SearchOn);
                sendServerChat(entity.getDisplayName() + " switched searchlight "+(SearchOn?"on":"off")+".");
            }

            // set most step parameters
            moveType = step.getMovementType();
            distance = step.getDistance();
            mpUsed = step.getMpUsed();

            // check for charge
            if (step.getType() == MovePath.STEP_CHARGE) {
                if (entity.canCharge()) {
                    checkExtremeGravityMovement(entity, step, curPos, cachedGravityLimit);
                    Targetable target = step.getTarget( game );
                    ChargeAttackAction caa = new ChargeAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                    entity.setDisplacementAttack(caa);
                    game.addCharge(caa);
                    charge = caa;
                } else {
                    sendServerChat("Illegal charge!! I don't think "+entity.getDisplayName() +" should be allowed to charge,"+
                                   " but the client of "+entity.getOwner().getName()+" disagrees.");
                    sendServerChat("Please make sure "+entity.getOwner().getName()+" is running MegaMek "+MegaMek.VERSION+
                                   ", or if that is already the case, submit a bug report at http://megamek.sf.net/");
                    return;
                }
                break;
            }

            // check for dfa
            if (step.getType() == MovePath.STEP_DFA) {
                if (entity.canDFA()) {
                    checkExtremeGravityMovement(entity, step, curPos, cachedGravityLimit);
                    Targetable target = step.getTarget( game );
                    DfaAttackAction daa = new DfaAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                    entity.setDisplacementAttack(daa);
                    game.addCharge(daa);
                    charge = daa;
                } else {
                    sendServerChat("Illegal DFA!! I don't think "+entity.getDisplayName() +" should be allowed to DFA,"+
                                   " but the client of "+entity.getOwner().getName()+" disagrees.");
                    sendServerChat("Please make sure "+entity.getOwner().getName()+" is running MegaMek "+MegaMek.VERSION+
                                   ", or if that is already the case, submit a bug report at http://megamek.sf.net/");
                    return;
                }
                break;
            }

            // set last step parameters
            curPos = step.getPosition();
            if(moveType != IEntityMovementType.MOVE_JUMP || entity.getJumpType() != Mech.JUMP_BOOSTER)
                curFacing = step.getFacing();
            curVTOLElevation = step.getElevation();
            //set elevation in case of collapses
            entity.setElevation(step.getElevation());

            final IHex curHex = game.getBoard().getHex(curPos);

            // check for automatic unstick
            if(entity.canUnstickByJumping() && entity.isStuck() && moveType == IEntityMovementType.MOVE_JUMP) {
                entity.setStuck(false);
                entity.setCanUnstickByJumping(false);
            }

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                                          prevStep, prevFacing, curFacing,
                                          lastPos, curPos, isInfantry,
                                          distance);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                boolean psrPassed = true;
                if ( entity instanceof Mech ) {
                    psrPassed = doSkillCheckWhileMoving( entity, lastPos,
                                                          lastPos, rollTarget,
                                                          true );
                } else {
                    psrPassed = doSkillCheckWhileMoving( entity, lastPos,
                                                          lastPos, rollTarget,
                                                          false );
                }
                // Does the entity skid?
                if ( !psrPassed ){

                    curPos = lastPos;
                    Coords nextPos = curPos;
                    IHex    nextHex = null;
                    int    skidDistance = 0;
                    Enumeration targets = null;
                    Entity target = null;
                    int    curElevation;
                    int    nextElevation;
                    int skidDirection = prevFacing;

                    // All charge damage is based upon
                    // the pre-skid move distance.
                    entity.delta_distance = distance-1;

                    // Attacks against a skidding target have additional +2.
                    moveType = IEntityMovementType.MOVE_SKID;

                    // What is the first hex in the skid?
                    if(step.isThisStepBackwards()) {
                        skidDirection = (skidDirection + 3) % 6;
                    }
                    nextPos = curPos.translated( skidDirection );
                    nextHex = game.getBoard().getHex( nextPos );

                    // Move the entity a number hexes from curPos in the
                    // skidDirection direction equal to half the distance moved
                    // this turn (rounded up), unless something intervenes.
                    for ( skidDistance = 0;
                          skidDistance < (int) Math.ceil(entity.delta_distance / 2.0);
                          skidDistance++ ) {

                        // Is the next hex off the board?
                        if ( !game.getBoard().contains(nextPos) ) {

                            // Can the entity skid off the map?
                            if (game.getOptions().booleanOption("push_off_board")) {
                                // Yup.  One dead entity.
                                game.removeEntity(entity.getId(),
                                        IEntityRemovalConditions.REMOVE_PUSHED);
                                send(createRemoveEntityPacket(entity.getId(),
                                        IEntityRemovalConditions.REMOVE_PUSHED));
                                r = new Report(2030, Report.PUBLIC);
                                r.addDesc(entity);
                                addReport(r);

                                // TODO: remove passengers and swarmers.

                                // The entity's movement is completed.
                                return;

                            } else {
                                // Nope.  Update the report.
                                r = new Report(2035);
                                r.subject = entity.getId();
                                r.indent();
                                addReport(r);
                            }
                            // Stay in the current hex and stop skidding.
                            break;
                        }
                        
                        // Can the skiding entity enter the next hex from this?
                        // N.B. can skid along roads.
                        if ( ( entity.isHexProhibited(curHex) ||
                               entity.isHexProhibited(nextHex) ) &&
                             !Compute.canMoveOnPavement(game, curPos, nextPos)
                             ) {
                            // Update report.
                            r = new Report(2040);
                            r.subject = entity.getId();
                            r.indent();
                            r.add(nextPos.getBoardNum(), true);
                            addReport(r);

                            // N.B. the BMRr pg. 22 says that the unit
                            // "crashes" into the terrain but it doesn't
                            // mention any damage.

                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Hovercraft can "skid" over water.
                        // all units can skid over ice.
                        // TODO: allow entities to occupy different levels of
                        //       buildings.
                        curElevation = curHex.floor();
                        nextElevation = nextHex.floor();
                        if ( entity instanceof Tank &&
                             entity.getMovementMode() ==
                             IEntityMovementMode.HOVER ) {
                            if ( curHex.containsTerrain(Terrains.WATER) ) {
                                curElevation = curHex.surface();
                            }
                            if ( nextHex.containsTerrain(Terrains.WATER) ) {
                                nextElevation += nextHex.surface();
                            }
                        } else {
                            if(curHex.containsTerrain(Terrains.ICE)) {
                                curElevation = curHex.surface();
                            }
                            if(nextHex.containsTerrain(Terrains.ICE)) {
                                nextElevation = nextHex.surface();
                            }
                        }

                        // BMRr pg. 22 - Can't skid uphill,
                        //      but can skid downhill.
                        if ( curElevation < nextElevation ) {
                            r = new Report(2045);
                            r.subject = entity.getId();
                            r.indent();
                            r.add(nextPos.getBoardNum(), true);
                            addReport(r);

                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Have skidding units suffer falls.
                        else if ( curElevation > nextElevation + 1 ) {
                            doEntityFallsInto( entity, curPos, nextPos,
                                               entity.getBasePilotingRoll() );
                            doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Get any building in the hex.
                        Building bldg = game.getBoard().getBuildingAt(nextPos);
                        boolean bldgSuffered = false;
                        boolean stopTheSkid = false;
                        // Does the next hex contain an entities?
                        // ASSUMPTION: hurt EVERYONE in the hex.
                        // TODO: allow entities to occupy different levels of
                        //       buildings, and only skid into a single level.
                        targets = game.getEntities( nextPos );
                        if ( targets.hasMoreElements()) {
                            boolean skidChargeHit = false;
                            while ( targets.hasMoreElements() ) {
                                target = (Entity) targets.nextElement();

                                // TODO : allow ready targets to move out of way

                                // Mechs and vehicles get charged,
                                // but need to make a to-hit roll
                                if ( target instanceof Mech ||
                                     target instanceof Tank ) {
                                    ChargeAttackAction caa = new ChargeAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                                    ToHitData toHit = caa.toHit(game, true);

                                    // Calculate hit location.
                                    if ( entity instanceof Tank
                                         && ((entity.getMovementMode() == IEntityMovementMode.HOVER)
                                         || (entity.getMovementMode() == IEntityMovementMode.NAVAL)
                                         || (entity.getMovementMode() == IEntityMovementMode.HYDROFOIL))
                                         && 0 < nextHex.terrainLevel(Terrains.WATER)
                                         && target.getElevation() < 0) {
                                        if ( 2 <= nextHex.terrainLevel(Terrains.WATER) ||
                                             target.isProne() ) {
                                            // Hovercraft/Naval Craft can't hit the Mek.
                                            continue;
                                        }
                                        else {
                                            toHit.setHitTable(ToHitData.HIT_PUNCH);
                                        }
                                    } else if ( entity.getHeight() <
                                                target.getHeight() ) {
                                        toHit.setHitTable(ToHitData.HIT_KICK);
                                    } else {
                                        toHit.setHitTable(ToHitData.HIT_NORMAL);
                                    }
                                    toHit.setSideTable
                                        (Compute.targetSideTable(entity, target));

                                    // roll
                                    int roll = Compute.d6(2);
                                    // Update report.
                                    r = new Report(2050);
                                    r.subject = entity.getId();
                                    r.indent();
                                    r.add(target.getShortName(), true);
                                    r.add(nextPos.getBoardNum(), true);
                                    r.newlines = 0;
                                    addReport(r);
                                    if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
                                        roll = -12;
                                        r = new Report(2055);
                                        r.subject = entity.getId();
                                        r.add(toHit.getDesc());
                                        r.newlines = 0;
                                        addReport(r);
                                    } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
                                        r = new Report(2060);
                                        r.subject = entity.getId();
                                        r.add(toHit.getDesc());
                                        r.newlines = 0;
                                        addReport(r);
                                        roll = Integer.MAX_VALUE;
                                    } else {
                                        // report the roll
                                        r = new Report(2065);
                                        r.subject = entity.getId();
                                        r.add(toHit.getValue());
                                        r.add(roll);
                                        r.newlines = 0;
                                        addReport(r);
                                    }

                                    // Resolve a charge against the target.
                                    // ASSUMPTION: buildings block damage for
                                    //             *EACH* entity charged.
                                    if (roll < toHit.getValue()) {
                                        r = new Report(2070);
                                        r.subject = entity.getId();
                                        addReport(r);
                                    } else {
                                        // Resolve the charge.
                                        resolveChargeDamage
                                            (entity, target, toHit, skidDirection);
                                        // HACK: set the entity's location
                                        // to the original hex again, for the other targets
                                        if (targets.hasMoreElements()) {
                                            entity.setPosition(curPos);
                                        }
                                        bldgSuffered = true;
                                        skidChargeHit = true;
                                    }
                                    // The skid ends here if the target lives.
                                    if ( !target.isDoomed() &&
                                         !target.isDestroyed() &&
                                         !game.isOutOfGame(target) ) {
                                        stopTheSkid = true;
                                    }

                                    // if we don't do this here,
                                    // we can have a mech without a leg
                                    // standing on the field and moving
                                    // as if it still had his leg after
                                    // getting skid-charged.
                                    if (!target.isDone()) {
                                        resolvePilotingRolls(target);
                                        game.resetPSRs(target);
                                        target.applyDamage();
                                        addNewLines();
                                    }

                                }

                                // Resolve "move-through" damage on infantry.
                                // Infantry inside of a building don't get a
                                // move-through, but suffer "bleed through"
                                // from the building.
                                else if ( target instanceof Infantry &&
                                          bldg != null ) {
                                    // Update report.
                                    r = new Report(2075);
                                    r.subject = entity.getId();
                                    r.indent();
                                    r.add(target.getShortName(), true);
                                    r.add(nextPos.getBoardNum(), true);
                                    r.newlines = 0;
                                    addReport(r);

                                    // Infantry don't have different
                                    // tables for punches and kicks
                                    HitData hit = target.rollHitLocation( ToHitData.HIT_NORMAL, Compute.targetSideTable(entity, target) );

                                    // Damage equals tonnage, divided by 5.
                                    // ASSUMPTION: damage is applied in one hit.
                                    addReport(
                                      damageEntity(target, hit,
                                      Math.round(entity.getWeight()/5)));
                                    addNewLines();
                                }

                                // Has the target been destroyed?
                                if ( target.isDoomed() ) {

                                    // Has the target taken a turn?
                                    if ( !target.isDone() ) {

                                        // Dead entities don't take turns.
                                        game.removeTurnFor(target);
                                        send(createTurnVectorPacket());

                                    } // End target-still-to-move

                                    // Clean out the entity.
                                    target.setDestroyed(true);
                                    game.moveToGraveyard(target.getId());
                                    send(createRemoveEntityPacket(target.getId()));
                                }

                                // Update the target's position,
                                // unless it is off the game map.
                                if ( !game.isOutOfGame(target) ) {
                                    entityUpdate( target.getId() );
                                }

                            } // Check the next entity in the hex.

                            // if we missed all the entities in the hex,
                            // move attacker to side hex
                            if (!skidChargeHit) {
                                Coords src = entity.getPosition();
                                Coords dest = Compute.getMissedChargeDisplacement
                                    (game, entity.getId(), src, skidDirection);
                                doEntityDisplacement(entity, src, dest, null);
                            } else {
                                // HACK: otherwise, set the entities position to that
                                // hex's coords, because we had to move the entity
                                // back earlier for the other targets
                                entity.setPosition(nextPos);
                            }
                        }

                        // Handle the building in the hex.
                        // TODO : BMRr pg. 22, only count buildings that are
                        //      higher than our starting terrain height.
                        // TODO: allow units to skid on top of buildings.
                        if ( bldg != null ) {

                            // Report that the entity has entered the bldg.
                            r = new Report(2080);
                            r.subject = entity.getId();
                            r.indent();
                            r.add(bldg.getName());
                            r.add(nextPos.getBoardNum(), true);
                            addReport(r);

                            // If the building hasn't already suffered
                            // damage, then apply charge damage to the
                            // building and displace the entity inside.
                            // ASSUMPTION: you don't charge the building
                            //             if Tanks or Mechs were charged.
                            int chargeDamage = ChargeAttackAction.getDamageFor
                                ( entity );
                            if ( !bldgSuffered ) {
                                Report buildingReport = damageBuilding( bldg, chargeDamage );
                                buildingReport.indent(2);
                                buildingReport.subject = entity.getId();
                                addReport(buildingReport);

                                // Apply damage to the attacker.
                                int toAttacker = ChargeAttackAction.getDamageTakenBy
                                    ( entity, bldg );
                                HitData hit = entity.rollHitLocation( ToHitData.HIT_NORMAL,
                                                                      entity.sideTable(nextPos)
                                                                      );
                                addReport(
                                                      damageEntity( entity, hit, toAttacker ));
                                addNewLines();

                                entity.setPosition( nextPos );
                                doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                                curPos = nextPos;
                            } // End buildings-suffer-too

                            // Any infantry in the building take damage
                            // equal to the building being charged.
                            // ASSUMPTION: infantry take no damage from the
                            //             building absorbing damage from
                            //             Tanks and Mechs being charged.
                            damageInfantryIn( bldg, chargeDamage );

                            // If a building still stands, then end the skid,
                            // and add it to the list of affected buildings.
                            if ( bldg.getCurrentCF() > 0 ) {
                                stopTheSkid = true;
                                this.addAffectedBldg( bldg, false );
                            }

                        } // End handle-building.

                        // Do we stay in the current hex and stop skidding?
                        if ( stopTheSkid ) {
                            break;
                        }
                        // is the next hex a rubble hex?
                        rollTarget = entity.checkRubbleMove(step, nextHex,
                                                curPos, nextPos);
                        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                            doSkillCheckWhileMoving(entity, curPos, nextPos,
                                                        rollTarget, true);
                            if (entity.isProne()) {
                                // if we fell, stop the skid (see bug 1115608)
                                break;
                            }
                        }
                        
                        //check for breaking magma crust
                        if(curHex.terrainLevel(Terrains.MAGMA) == 1) {
                            int roll = Compute.d6(1);
                            r = new Report(2395);
                            r.addDesc(entity);
                            r.add(roll);
                            r.subject = entity.getId();
                            addReport(r);
                            if(roll == 6) {
                                curHex.removeTerrain(Terrains.MAGMA);
                                curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.MAGMA, 2));
                                sendChangedHex(curPos);
                                for(Enumeration e=game.getEntities(curPos);e.hasMoreElements();) {
                                    Entity en = (Entity)e.nextElement();
                                    if(en != entity)
                                        doMagmaDamage(en, false);
                                }
                            }
                        }

                        //check for entering liquid magma
                        if(curHex.terrainLevel(Terrains.MAGMA) == 2) {
                            doMagmaDamage(entity, false);
                        }            

                        // is the next hex a swamp?
                        rollTarget = entity.checkSwampMove(step, nextHex, 
                                                              curPos, nextPos);
                        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                            if (!doSkillCheckWhileMoving(entity, curPos,
                                                   nextPos, rollTarget, false)){
                                entity.setStuck(true);
                                r = new Report(2081);
                                r.subject = entity.getId();
                                r.add(entity.getDisplayName(), true);
                                // stay here and stop skidding, see bug 1115608
                                break;
                            }
                        }

                        // Update the position and keep skidding.
                        entity.setPosition( nextPos );
                        doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                        curPos = nextPos;
                        r = new Report(2085);
                        r.subject = entity.getId();
                        r.indent();
                        r.add(curPos.getBoardNum(), true);
                        addReport(r);

                        // Get the next hex in the skid?
                        nextPos = nextPos.translated( skidDirection );
                        nextHex = game.getBoard().getHex( nextPos );

                    } // Handle the next skid hex.

                    // If the skidding entity violates stacking,
                    // displace targets until it doesn't.
                    curPos = entity.getPosition();
                    target = Compute.stackingViolation
                        (game, entity.getId(), curPos);
                    while (target != null) {
                        nextPos = Compute.getValidDisplacement
                            (game, target.getId(),
                             target.getPosition(), skidDirection);
                        // ASSUMPTION
                        // There should always be *somewhere* that
                        // the target can go... last skid hex if
                        // nothing else is available.
                        if ( null == nextPos ) {
                            // But I don't trust the assumption fully.
                            // Report the error and try to continue.
                            System.err.println( "The skid of " +
                                                entity.getShortName() +
                                                " should displace " +
                                                target.getShortName() +
                                                " in hex " +
                                                curPos.getBoardNum() +
                                                " but there is nowhere to go."
                                                );
                            break;
                        }
                        // indent displacement
                        r = new Report(1210, Report.PUBLIC);
                        r.indent();
                        r.newlines = 0;
                        addReport(r);
                        doEntityDisplacement(target, curPos, nextPos, null);
                        doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                        target = Compute.stackingViolation( game,
                                                            entity.getId(),
                                                            curPos );
                    }

                    // Mechs suffer damage for every hex skidded.
                    if ( entity instanceof Mech ) {
                        // Calculate one half falling damage times skid length.
                        int damage = skidDistance * (int) Math.ceil(Math.round(entity.getWeight() / 10.0) / 2.0);

                        // report skid damage
                        r = new Report(2090);
                        r.subject = entity.getId();
                        r.indent();
                        r.addDesc(entity);
                        r.add(damage);
                        addReport(r);

                        // standard damage loop
                        // All skid damage is to the front.
                        while (damage > 0) {
                            int cluster = Math.min(5, damage);
                            HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                            addReport(
                                damageEntity(entity, hit, cluster));
                            damage -= cluster;
                        }
                        addNewLines();
                    }

                    // Clean up the entity if it has been destroyed.
                    if ( entity.isDoomed() ) {
                        entity.setDestroyed(true);
                        game.moveToGraveyard(entity.getId());
                        send(createRemoveEntityPacket(entity.getId()));

                        // The entity's movement is completed.
                        return;
                    }

                    // Let the player know the ordeal is over.
                    r = new Report(2095);
                    r.subject = entity.getId();
                    r.indent();
                    addReport(r);

                    // set entity parameters
                    curFacing = entity.getFacing();
                    curPos = entity.getPosition();
                    entity.setSecondaryFacing( curFacing );

                    // skid consumes all movement
                    if (md.hasActiveMASC()) {
                        mpUsed = entity.getRunMP();
                    } else {
                        mpUsed = entity.getRunMPwithoutMASC();
                    }

                    entity.moved = moveType;
                    fellDuringMovement = true;
                    distance = entity.delta_distance;
                    break;

                } // End failed-skid-psr

            } // End need-skid-psr
            if(entity instanceof VTOL) {
                rollTarget = ((VTOL)entity).checkSideSlip(moveType, prevHex, overallMoveType,
                                          prevStep, prevFacing, curFacing,
                                          lastPos, curPos,
                                          distance);
                if(rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    if(!doSkillCheckWhileMoving(entity,lastPos,curPos,rollTarget, false)) {
                        //report sideslip
                        sideslipped = true;
                        r = new Report(2100);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        addReport(r);
                        Coords newPos = lastPos.translated((prevFacing));//does this work for opposing hex?
                        // Is the next hex off the board?
                        if ( !game.getBoard().contains(newPos) ) {
                            // Can the entity skid off the map?
                            if (game.getOptions().booleanOption("push_off_board")) {
                                // Yup.  One dead entity.
                                game.removeEntity(entity.getId(),
                                        IEntityRemovalConditions.REMOVE_PUSHED);
                                send(createRemoveEntityPacket(entity.getId(),
                                        IEntityRemovalConditions.REMOVE_PUSHED));
                                r = new Report(2030);
                                r.subject = entity.getId();
                                r.addDesc(entity);
                                addReport(r);

                                // TODO: remove passengers and swarmers.
                                // The entity's movement is completed.
                                return;
                            } else {
                                // Nope.  Update the report.
                                r = new Report(2035);
                                r.subject = entity.getId();
                                addReport(r);
                            }
                            // Stay in the current hex and stop skidding.
                            break;
                        }
                        IHex hex = game.getBoard().getHex(newPos);
                        int terrainLevel = hex.ceiling() - hex.surface();
                        int newElevation=(entity.calcElevation(game.getBoard().getHex(curPos),game.getBoard().getHex(newPos),curVTOLElevation,step.climbMode()));
                        if(newElevation<=terrainLevel) {                            
                            r = new Report(2105);
                            r.subject = entity.getId();
                            r.add(newPos.getBoardNum(), true);
                            addReport(r);

                            int hitSide=curFacing-prevFacing+6;
                            hitSide=hitSide % 6;
                            int table=0;
                            switch(hitSide) {//quite hackish...I think it ought to work, though.
                                case 0://can this happen?
                                    table=ToHitData.SIDE_FRONT;
                                    break;
                                case 1:
                                case 2:
                                    table=ToHitData.SIDE_LEFT;
                                    break;
                                case 3:
                                    table=ToHitData.SIDE_REAR;
                                    break;
                                case 4:
                                case 5:
                                    table=ToHitData.SIDE_RIGHT;
                                    break;
                            }
                            curPos=newPos;
                            curVTOLElevation=newElevation;
                            addReport(crashVTOL(((VTOL)entity),true,distance,curPos,curVTOLElevation,table));
                            
                            if((hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.ICE))
                                || hex.containsTerrain(Terrains.WOODS)
                                || hex.containsTerrain(Terrains.JUNGLE)) {
                                addReport(destroyEntity(entity,"could not land in crash site"));
                            } else if(newElevation < hex.terrainLevel(Terrains.BLDG_ELEV)){
                                addReport(destroyEntity(entity, "crashed into building"));
                            }
                        } else {
                            r = new Report(2110);
                            r.subject = entity.getId();
                            r.add(newPos.getBoardNum(), true);
                            addReport(r);
                            entity.setElevation(entity.calcElevation(game.getBoard().getHex(curPos),game.getBoard().getHex(newPos),curVTOLElevation,step.climbMode()));
                            curPos=newPos;
                        }

                        if(!entity.isDestroyed() && !entity.isDoomed()) {
                            fellDuringMovement= true; //No, but it should work...
                        }
                        break;
                    }
                }
            }

            // check if we've moved into rubble
            rollTarget = entity.checkRubbleMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                doSkillCheckWhileMoving(entity, lastPos, curPos, rollTarget,
                                         true);
            }

            //check for breaking magma crust
            if(curHex.terrainLevel(Terrains.MAGMA) == 1
                && step.getElevation() == 0
                && step.getMovementType() != IEntityMovementType.MOVE_JUMP) {
                int roll = Compute.d6(1);
                r = new Report(2395);
                r.addDesc(entity);
                r.add(roll);
                r.subject = entity.getId();
                addReport(r);
                if(roll == 6) {
                    curHex.removeTerrain(Terrains.MAGMA);
                    curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.MAGMA, 2));
                    sendChangedHex(curPos);
                    for(Enumeration e=game.getEntities(curPos);e.hasMoreElements();) {
                        Entity en = (Entity)e.nextElement();
                        if(en != entity)
                            doMagmaDamage(en, false);
                    }
                }
            }

            //check for entering liquid magma
            if(curHex.terrainLevel(Terrains.MAGMA) == 2
                && step.getElevation() == 0
                && step.getMovementType() != IEntityMovementType.MOVE_JUMP) {
                doMagmaDamage(entity, false);
            }            

            // check if we've moved into a swamp
            rollTarget = entity.checkSwampMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                if (!doSkillCheckWhileMoving(entity, lastPos, curPos, rollTarget,
                                         false)){
                    entity.setStuck(true);
                    entity.setCanUnstickByJumping(true);
                    r = new Report(2081);
                    r.add(entity.getDisplayName());
                    r.subject = entity.getId();
                    addReport(r);
                    break;
                }
            }

            // check to see if we are a mech and we've moved OUT of fire
            IHex lastHex = game.getBoard().getHex(lastPos);
            if (entity instanceof Mech) {
                if ( !lastPos.equals(curPos)
                    && (lastHex.containsTerrain(Terrains.FIRE)
                            || lastHex.containsTerrain(Terrains.MAGMA))
                    && ( step.getMovementType() != IEntityMovementType.MOVE_JUMP
                         // Bug #828741 -- jumping bypasses fire, but not on the first step
                         //   getMpUsed -- total MP used to this step
                         //   getMp -- MP used in this step
                         //   the difference will always be 0 on the "first step" of a jump,
                         //   and >0 on a step in the midst of a jump
                         || ( 0 == step.getMpUsed() - step.getMp() ) ) )
                {
                    int heat=0;
                    if(lastHex.containsTerrain(Terrains.FIRE))
                        heat+=2;
                    if(lastHex.terrainLevel(Terrains.MAGMA) == 1) {
                        heat+=2;
                    }
                    else if(lastHex.terrainLevel(Terrains.MAGMA) == 2) {
                        heat+=5;
                    }
                    entity.heatBuildup+=heat;
                    r = new Report(2115);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(heat);
                    addReport(r);
                }
            }

            // check to see if we are not a mech and we've moved INTO fire
            if (!(entity instanceof Mech)) {
                if ( game.getBoard().getHex(curPos).containsTerrain(Terrains.FIRE)
                    && !lastPos.equals(curPos)
                    && step.getMovementType() != IEntityMovementType.MOVE_JUMP
                    && step.getElevation() <= 1 ) {
                    if(game.getOptions().booleanOption("vehicle_fires")
                            && entity instanceof Tank) {
                        checkForVehicleFire((Tank)entity, false);
                    } else {
                        doFlamingDeath(entity);
                    }
                }
            }
            // check for extreme gravity movement
            if (!i.hasMoreElements() && !firstStep) {
                checkExtremeGravityMovement(entity, step, curPos, cachedGravityLimit);
            }
            // check for minefields.
            if ((!lastPos.equals(curPos) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP))
                || ((overallMoveType == IEntityMovementType.MOVE_JUMP) && (!i.hasMoreElements()))) {
                checkVibrabombs(entity, curPos, false, lastPos, curPos);
                if (game.containsMinefield(curPos)) {
                    Enumeration minefields = game.getMinefields(curPos).elements();
                    while (minefields.hasMoreElements()) {
                        Minefield mf = (Minefield) minefields.nextElement();

                        boolean isOnGround = (!i.hasMoreElements());
                        isOnGround |= (step.getMovementType() != IEntityMovementType.MOVE_JUMP);
                        isOnGround &= step.getElevation() == 0;
                        if (isOnGround) {
                            enterMinefield(entity, mf, curPos, curPos, true);
                        } else if (mf.getType() == Minefield.TYPE_THUNDER_ACTIVE) {
                            enterMinefield(entity, mf, curPos, curPos, true, 2);
                        }
                    }
                }
            }

            // infantry discovers minefields if they end their move
            // in a minefield.

            if (!lastPos.equals(curPos) &&
                !i.hasMoreElements() &&
                isInfantry) {
                if (game.containsMinefield(curPos)) {
                    Player owner = entity.getOwner();
                    Enumeration minefields = game.getMinefields(curPos).elements();
                    while (minefields.hasMoreElements()) {
                        Minefield mf = (Minefield) minefields.nextElement();
                        if (!owner.containsMinefield(mf)) {
                            r = new Report(2120);
                            r.subject = entity.getId();
                            r.add(entity.getShortName(), true);
                            addReport(r);
                            revealMinefield(owner, mf);
                        }
                    }
                }
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, curHex, lastPos, curPos,
                                               isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Swarmers need special handling.
                final int swarmerId = entity.getSwarmAttackerId();
                boolean swarmerDone = true;
                Entity swarmer = null;
                if (Entity.NONE != swarmerId) {
                    swarmer = game.getEntity( swarmerId );
                    swarmerDone = swarmer.isDone();
                }

                // Now do the skill check.
                doSkillCheckWhileMoving(entity, lastPos, curPos, rollTarget,
                                         true);

                // Swarming infantry platoons may drown.
                if (curHex.terrainLevel(Terrains.WATER) > 1) {
                    drownSwarmer(entity, curPos);
                }

                // Do we need to remove a game turn for the swarmer
                if (!swarmerDone &&
                    ( swarmer.isDoomed() || swarmer.isDestroyed() )) {
                    // We have to diddle with the swarmer's
                    // status to get its turn removed.
                    swarmer.setDone( false );
                    swarmer.setUnloaded( false );

                    // Dead entities don't take turns.
                    game.removeTurnFor( swarmer );
                    send( createTurnVectorPacket() );

                    // Return the original status.
                    swarmer.setDone( true );
                    swarmer.setUnloaded( true );
                }

                // check for inferno wash-off
                checkForWashedInfernos(entity, curPos);
            }
            
            // In water, may or may not be a new hex, neccessary to
            // check during movement, for breach damage, and always
            // set dry if appropriate
            //TODO: possibly make the locations local and set later
            doSetLocationsExposure(entity, curHex, 
                    step.getMovementType() == IEntityMovementType.MOVE_JUMP,
                    step.getElevation());

            //check for breaking ice by breaking through from below
            if(prevHex != null && prevStep != null
                    && prevStep.getElevation() < 0
                    && step.getElevation() == 0
                    && prevHex.containsTerrain(Terrains.ICE)
                    && prevHex.containsTerrain(Terrains.WATER)
                    && step.getMovementType() != IEntityMovementType.MOVE_JUMP
                    && !(lastPos.equals(curPos))) {
                r = new Report(2410);
                r.addDesc(entity);
                addReport(r);
                resolveIceBroken(lastPos);
            }
            //check for breaking ice by stepping on it
            if(curHex.containsTerrain(Terrains.ICE)
                    && curHex.containsTerrain(Terrains.WATER)
                    && step.getMovementType() != IEntityMovementType.MOVE_JUMP
                    && !(lastPos.equals(curPos))) {
                if(step.getElevation() == 0
                        ) {
                    int roll = Compute.d6(1);
                    r = new Report(2118);
                    r.addDesc(entity);
                    r.add(roll);
                    r.subject = entity.getId();
                    addReport(r);
                    if(roll == 6) {
                        resolveIceBroken(curPos);
                        doEntityFallsInto(entity, lastPos, curPos, entity.getBasePilotingRoll(), false);
                    }
                }
                //or intersecting it
                else if(step.getElevation() + entity.height() == 0) {
                    r = new Report(2410);
                    r.addDesc(entity);
                    addReport(r);
                    resolveIceBroken(curPos);
                }
            }
            
            // Handle loading units.
            if ( step.getType() == MovePath.STEP_LOAD ) {

                // Find the unit being loaded.
                Entity loaded = null;
                Enumeration entities = game.getEntities( curPos );
                while ( entities.hasMoreElements() ) {

                    // Is the other unit friendly and not the current entity?
                    loaded = (Entity)entities.nextElement();
                    if ( entity.getOwner() == loaded.getOwner() &&
                         !entity.equals(loaded) ) {

                        // The moving unit should be able to load the other
                        // unit and the other should be able to have a turn.
                        if ( !entity.canLoad(loaded) ||
                             !loaded.isSelectableThisTurn() ) {
                            // Something is fishy in Denmark.
                            System.err.println( entity.getShortName() +
                                                " can not load " +
                                                loaded.getShortName() );
                            loaded = null;
                        }
                        else {
                            // Have the deployed unit load the indicated unit.
                            this.loadUnit( entity, loaded );

                            // Stop looking.
                            break;
                        }

                    } else {
                        // Nope. Discard it.
                        loaded = null;
                    }

                } // Handle the next entity in this hex.

                // We were supposed to find someone to load.
                if ( loaded == null ) {
                    System.err.println( "Could not find unit for " +
                                        entity.getShortName() +
                                        " to load in " + curPos );
                }

            } // End STEP_LOAD

            // Handle unloading units.
            if ( step.getType() == MovePath.STEP_UNLOAD ) {
                Targetable unloaded = step.getTarget( game );
                if ( !this.unloadUnit( entity, unloaded,
                                       curPos, curFacing, step.getElevation() ) ) {
                    System.err.println( "Error! Server was told to unload " +
                                        unloaded.getDisplayName() +
                                        " from " + entity.getDisplayName() +
                                        " into " + curPos.getBoardNum() );
                }
            }

            // Handle non-infantry moving into a building.
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            if (buildingMove > 0) {

                // Get the building being exited.
                Building bldgExited = null;
                if((buildingMove & 1) == 1)
                    bldgExited = game.getBoard().getBuildingAt( lastPos );

                // Get the building being entered.
                Building bldgEntered = null;
                if((buildingMove & 2) == 2)
                    bldgEntered = game.getBoard().getBuildingAt( curPos );
                
                // Get the building being stepped on.
                Building bldgStepped = null;
                if((buildingMove & 4) == 4)
                    bldgStepped = game.getBoard().getBuildingAt( curPos );

                boolean collapsed = false;
                //are we passing through a building wall?
                if(bldgEntered != null || bldgExited != null) {
                    // If we're not leaving a building, just handle the "entered".
                    if ( bldgExited == null) {
                        collapsed = passBuildingWall( entity, bldgEntered,
                                                      lastPos, curPos,
                                                      distance, "entering" );
                        this.addAffectedBldg( bldgEntered, collapsed );
                    }
    
                    // If we're moving withing the same building, just handle
                    // the "within".
                    else if ( bldgExited.equals( bldgEntered ) ) {
                        collapsed = passBuildingWall( entity, bldgEntered,
                                                      lastPos, curPos,
                                                      distance, "moving in" );
                        this.addAffectedBldg( bldgEntered, collapsed );
                    }
    
                    // If we have different buildings, roll for each.
                    else if ( bldgExited != null && bldgEntered != null ) {
                        collapsed = passBuildingWall( entity, bldgExited,
                                                      lastPos, curPos,
                                                      distance, "exiting" );
                        this.addAffectedBldg( bldgExited, collapsed );
                        collapsed = passBuildingWall( entity, bldgEntered,
                                                      lastPos, curPos,
                                                      distance, "entering" );
                        this.addAffectedBldg( bldgEntered, collapsed );
                    }
    
                    // Otherwise, just handle the "exited".
                    else if (bldgExited != null){
                        collapsed = passBuildingWall( entity, bldgExited,
                                                      lastPos, curPos,
                                                      distance, "exiting" );
                        this.addAffectedBldg( bldgExited, collapsed );
                    }
                }
                
                //stepping on roof, no PSR just check for over weight
                if(bldgStepped != null) {
                    collapsed = checkBuildingCollapseWhileMoving(bldgStepped, entity, curPos);
                    this.addAffectedBldg( bldgStepped, collapsed );
                }

                // Clean up the entity if it has been destroyed.
                if ( entity.isDoomed() ) {
                    entity.setDestroyed(true);
                    game.moveToGraveyard(entity.getId());
                    send(createRemoveEntityPacket(entity.getId()));

                    // The entity's movement is completed.
                    return;
                }

                // TODO: what if a building collapses into rubble?
            }

            // did the entity just fall?
            if (!wasProne && entity.isProne()) {
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }

            // dropping prone intentionally?
            if (step.getType() == MovePath.STEP_GO_PRONE) {
                mpUsed = step.getMpUsed();
                rollTarget = entity.checkDislodgeSwarmers(step);
                if (rollTarget.getValue() == TargetRoll.CHECK_FALSE) {
                    // Not being swarmed
                    entity.setProne(true);
                    // check to see if we washed off infernos
                    checkForWashedInfernos(entity, curPos);
                    break;
                } else {
                    // Being swarmed
                    entity.setPosition(curPos);
                    if (doDislodgeSwarmerSkillCheck(entity,
                                                    rollTarget,
                                                    curPos)) {
                        // Entity falls
                        curFacing = entity.getFacing();
                        curPos = entity.getPosition();
                        fellDuringMovement = true;
                        break;
                    }
                }
            }
            
            //going hull down
            if(step.getType() == MovePath.STEP_HULL_DOWN) {
                mpUsed = step.getMpUsed();
                entity.setHullDown(true);
            }

            // Track this step's location.
            movePath.addElement( new UnitLocation( entity.getId(),
                                                   curPos,
                                                   curFacing ) );

            // update lastPos, prevStep, prevFacing & prevHex
            lastPos = new Coords(curPos);
            prevStep = step;
            /* Bug 754610: Revert fix for bug 702735.
            if (prevHex != null && !curHex.equals(prevHex)) {
            */
            if (!curHex.equals(prevHex)) {
                prevFacing = curFacing;
            }
            prevHex = curHex;

            firstStep = false;
        }

        // set entity parameters
        entity.setPosition(curPos);
        entity.setFacing(curFacing);
        entity.setSecondaryFacing(curFacing);
        entity.delta_distance = distance;
        entity.moved = moveType;
        entity.mpUsed = mpUsed;
        if (!sideslipped && !fellDuringMovement) {
            entity.setElevation(curVTOLElevation);
        }
        entity.setClimbMode(md.getFinalClimbMode());
        
        

        // if we ran with destroyed hip or gyro, we need a psr
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            doSkillCheckInPlace(entity, rollTarget);
        }

        // but the danger isn't over yet!  landing from a jump can be risky!
        if (overallMoveType == IEntityMovementType.MOVE_JUMP && !entity.isMakingDfa()) {
            final IHex curHex = game.getBoard().getHex(curPos);
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage();
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                doSkillCheckInPlace(entity, rollTarget);
            }
            // jumped into water?
            int waterLevel = curHex.terrainLevel(Terrains.WATER);
            if(curHex.containsTerrain(Terrains.ICE) && waterLevel > 0) {
                waterLevel = 0;
                //check for breaking ice
                int roll = Compute.d6(1);
                r = new Report(2122);
                r.add(entity.getDisplayName(), true);
                r.add(roll);
                r.subject = entity.getId();
                addReport(r);
                if(roll >= 4) {
                    //oops!
                    resolveIceBroken(curPos);
                    doEntityFallsInto(entity, lastPos, curPos, entity.getBasePilotingRoll(), false);
                }
            }
            rollTarget = entity.checkWaterMove(waterLevel);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                doSkillCheckInPlace(entity, rollTarget);
            }
            if (waterLevel > 1) {
                // Any swarming infantry will be destroyed.
                drownSwarmer(entity, curPos);
            }

            //check for building collapse
            Building bldg = game.getBoard().getBuildingAt(curPos);
            if(bldg != null) {
                checkForCollapse( bldg, game.getPositionMap() );
            }

            //check for breaking magma crust
            if(curHex.terrainLevel(Terrains.MAGMA) == 1) {
                int roll = Compute.d6(1);
                r = new Report(2395);
                r.addDesc(entity);
                r.add(roll);
                r.subject = entity.getId();
                addReport(r);
                if(roll == 6) {
                    curHex.removeTerrain(Terrains.MAGMA);
                    curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.MAGMA, 2));
                    sendChangedHex(curPos);
                    for(Enumeration e=game.getEntities(curPos);e.hasMoreElements();) {
                        Entity en = (Entity)e.nextElement();
                        if(en != entity)
                            doMagmaDamage(en, false);
                    }
                }
            }

            //check for entering liquid magma
            if(curHex.terrainLevel(Terrains.MAGMA) == 2) {
                doMagmaDamage(entity, false);
            }            

            // jumped into swamp? maybe stuck!
            if (curHex.containsTerrain(Terrains.SWAMP)
                || curHex.containsTerrain(Terrains.MAGMA)
                || curHex.containsTerrain(Terrains.SNOW)
                || curHex.containsTerrain(Terrains.MUD)
                || curHex.containsTerrain(Terrains.TUNDRA)) {
                if (entity instanceof Mech) {
                    entity.setStuck(true);
                    r = new Report(2121);
                    r.add(entity.getDisplayName(), true);
                    r.subject = entity.getId();
                    addReport(r);
                } else if (entity instanceof Infantry) {
                    PilotingRollData roll = entity.getBasePilotingRoll();
                    roll.addModifier(5, "infantry jumping into swamp");
                    if (!doSkillCheckWhileMoving(entity, curPos, curPos, roll, false)) {
                        entity.setStuck(true);
                        r = new Report(2081);
                        r.add(entity.getDisplayName());
                        r.subject = entity.getId();
                        addReport(r);
                    }
                }
            }

            // If the entity is being swarmed, jumping may dislodge the fleas.
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );
                final PilotingRollData roll =
                    entity.getBasePilotingRoll();

                entity.addPilotingModifierForTerrain(roll);

                // Add a +4 modifier.
                roll.addModifier( 4, "dislodge swarming infantry" );

                // If the swarmer has Assault claws, give a 1 modifier.
                // We can stop looking when we find our first match.
                for ( Enumeration iter = swarmer.getMisc();
                      iter.hasMoreElements(); ) {
                    Mounted mount = (Mounted) iter.nextElement();
                    EquipmentType equip = mount.getType();
                    if ( BattleArmor.ASSAULT_CLAW.equals
                         (equip.getInternalName()) ) {
                        roll.addModifier( 1, "swarmer has assault claws" );
                        break;
                    }
                }

                // okay, print the info
                r = new Report(2125);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);

                // roll
                final int diceRoll = Compute.d6(2);
                r = new Report(2130);
                r.subject = entity.getId();
                r.add(roll.getValueAsString());
                r.add(roll.getDesc());
                r.add(diceRoll);
                if (diceRoll < roll.getValue()) {
                    r.choose(false);
                    addReport(r);
                } else {
                    // Dislodged swarmers don't get turns.
                    game.removeTurnFor( swarmer );
                    send( createTurnVectorPacket() );

                    // Update the report and the swarmer's status.
                    r.choose(true);
                    addReport(r);
                    entity.setSwarmAttackerId( Entity.NONE );
                    swarmer.setSwarmTargetId( Entity.NONE );

                    // Did the infantry fall into water?
                    if ( curHex.terrainLevel(Terrains.WATER) > 0 ) {
                        // Swarming infantry die.
                        swarmer.setPosition( curPos );
                        r = new Report(2135);
                        r.subject = entity.getId();
                        r.indent();
                        r.addDesc(swarmer);
                        addReport(r);
                        addReport(
                            destroyEntity(swarmer, "a watery grave", false));
                    } else {
                        // Swarming infantry take an 11 point hit.
                        // ASSUMPTION : damage should not be doubled.
                        r = new Report(2140);
                        r.subject = entity.getId();
                        r.indent();
                        r.addDesc(swarmer);
                        addReport(r);
                        addReport(damageEntity(swarmer, swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT), 11));
                        addNewLines();
                        swarmer.setPosition( curPos );
                    }
                    entityUpdate( swarmerId );
                } // End successful-PSR

            } // End try-to-dislodge-swarmers

            // one more check for inferno wash-off
            checkForWashedInfernos(entity, curPos);

        } // End entity-is-jumping
        // update entity's locations' exposure
        doSetLocationsExposure(entity, game.getBoard().getHex(curPos), false, entity.getElevation());

        // should we give another turn to the entity to keep moving?
        if (fellDuringMovement && entity.mpUsed < entity.getRunMP()
        && entity.isSelectableThisTurn() && !entity.isDoomed()) {
            entity.applyDamage();
            entity.setDone(false);
            GameTurn newTurn = new GameTurn.SpecificEntityTurn(entity.getOwner().getId(), entity.getId());
            game.insertNextTurn(newTurn);
            // brief everybody on the turn update
            send(createTurnVectorPacket());
            // let everyone know about what just happened
            send(entity.getOwner().getId(), createSpecialReportPacket());
        } else {
            entity.setDone(true);
        }

        // If the entity is being swarmed, update the attacker's position.
        final int swarmerId = entity.getSwarmAttackerId();
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            swarmer.setPosition( curPos );
            // If the hex is on fire, and the swarming infantry is
            // *not* Battle Armor, it drops off.
            if ( !(swarmer instanceof BattleArmor) &&
                 game.getBoard().getHex(curPos).containsTerrain(Terrains.FIRE) ) {
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                r = new Report(2145);
                r.subject = entity.getId();
                r.indent();
                r.add(swarmer.getShortName(), true);
                addReport(r);
            }
            entityUpdate( swarmerId );
        }

        // Update the entitiy's position,
        // unless it is off the game map.
        if (!game.isOutOfGame(entity)) {
            entityUpdate( entity.getId(), movePath  );
            if (entity.isDoomed()) {
                send(createRemoveEntityPacket(entity.getId(), entity.getRemovalCondition()));
            }
        }

        // if using double blind, update the player on new units he might see
        if (doBlind()) {
            send(entity.getOwner().getId(), createFilteredEntitiesPacket(entity.getOwner()));
        }

        // if we generated a charge attack, report it now
        if (charge != null) {
            send(createAttackPacket(charge, 1));
        }
    }


    /**
     * Delivers a thunder-aug shot to the targetted hex area.
     * Thunder-Augs are 7 hexes, though, so...
     */
    private void deliverThunderAugMinefield( Coords coords,
                                             int playerId, int damage ) {
        Coords mfCoord = null;
        for (int dir=0; dir < 7; dir++) {
            switch (dir) {
            case 6:
                // The targeted hex.
                mfCoord = new Coords(coords);
                break;
            default:
                // The hex in the dir direction from the targeted hex.
                mfCoord = coords.translated(dir);
                break;
            }

            // Only if this is on the board...
            if ( game.getBoard().contains(mfCoord) ) {
                Minefield minefield = null;
                Enumeration minefields = game.getMinefields(mfCoord).elements();
                // Check if there already are Thunder minefields in the hex.
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();
                    if (mf.getType() == Minefield.TYPE_THUNDER) {
                        minefield = mf;
                        break;
                    }
                }

                // Did we find a Thunder minefield in the hex?
                // N.B. damage Thunder minefields equals the number of
                //      missiles, divided by two, rounded up.
                if (minefield == null) {
                    // Nope.  Create a new Thunder minefield
                    minefield = Minefield.createThunderMF
                        ( mfCoord, playerId, (damage/2 + damage%2) );
                    game.addMinefield(minefield);
                    revealMinefield(minefield);
                } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
                    // Yup.  Replace the old one.
                    removeMinefield(minefield);
                    int newDamage = (damage/2 + damage%2);
                    newDamage += minefield.getDamage();

                    // Damage from Thunder minefields are capped.
                    if ( newDamage > Minefield.MAX_DAMAGE ) {
                        newDamage = Minefield.MAX_DAMAGE;
                    }
                    minefield.setDamage(newDamage);
                    game.addMinefield(minefield);
                    revealMinefield(minefield);
                }
            } // End coords-on-board

        } // Handle the next coords

    }

    /**
     * Adds a Thunder minefield to the hex.
     * @param coords
     * @param playerId
     * @param damage
     */
    private void deliverThunderMinefield( Coords coords, int playerId,
                                          int damage ) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder minefield
        if (minefield == null) {
            minefield = Minefield.createThunderMF(coords, playerId, damage);
            // Add to the old one
            game.addMinefield(minefield);
            revealMinefield(minefield);
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
            game.addMinefield(minefield);
            revealMinefield(minefield);
        }
    }

    /**
     * Adds a Thunder Inferno minefield to the hex.
     * @param coords
     * @param playerId
     * @param damage
     */
    private void deliverThunderInfernoMinefield(Coords coords, int playerId, int damage) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER_INFERNO) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder Inferno minefield
        if (minefield == null) {
            minefield = Minefield.createThunderInfernoMF(coords, playerId, damage);
            // Add to the old one
            game.addMinefield(minefield);
            revealMinefield(minefield);
       } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
            game.addMinefield(minefield);
            revealMinefield(minefield);
       }
    }

    /**
     *Delivers a Arrow IV FASCAM shot to the targetted hex area.
     */
    private void deliverFASCAMMinefield( Coords coords, int playerId) {
        // Only if this is on the board...
        if ( game.getBoard().contains(coords) ) {
            Minefield minefield = null;
            Enumeration minefields = game.getMinefields(coords).elements();
            // Check if there already are Thunder minefields in the hex.
            while (minefields.hasMoreElements()) {
                Minefield mf = (Minefield) minefields.nextElement();
                if (mf.getType() == Minefield.TYPE_THUNDER) {
                    minefield = mf;
                    break;
                }
            }
            // Did we find a Thunder minefield in the hex?
            // N.B. damage of FASCAM minefields is 30
            if (minefield == null) minefield = Minefield.createThunderMF( coords, playerId, 30 );
            removeMinefield(minefield);
            minefield.setDamage(30);
            game.addMinefield(minefield);
            revealMinefield(minefield);
        } // End coords-on-board
    }

    /**
     * Adds a Thunder-Active minefield to the hex.
     */
    private void deliverThunderActiveMinefield(Coords coords, int playerId, int damage) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER_ACTIVE) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder-Active minefield
        if (minefield == null) {
            minefield = Minefield.createThunderActiveMF(coords, playerId, damage);
            // Add to the old one
            game.addMinefield(minefield);
            revealMinefield(minefield);
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
            game.addMinefield(minefield);
            revealMinefield(minefield);
        }
    }

    /**
     * Adds a Thunder-Vibrabomb minefield to the hex.
     */
    private void deliverThunderVibraMinefield(Coords coords, int playerId, int damage, int sensitivity) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER_VIBRABOMB) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder-Vibra minefield
        if (minefield == null) {
            minefield = Minefield.createThunderVibrabombMF(coords, playerId, damage, sensitivity);
            // Add to the old one
            game.addVibrabomb(minefield);
            revealMinefield(minefield);
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
            game.addVibrabomb(minefield);
            revealMinefield(minefield);
        }
    }

    /**
     * Creates a flare above the target
     */
    private void deliverFlare(Coords coords, int rackSize) {
        Flare flare = new Flare(coords, Math.max(1, rackSize / 5), 3, 0);
        game.addFlare(flare);
    }
    
    private void deliverArtilleryFlare(Coords coords, int radius) {
        Flare flare = new Flare(coords, 12, radius, Flare.F_DRIFTING);
        game.addFlare(flare);
    }

    private void deliverArtillerySmoke(Coords coords) {
        if(game.getOptions().booleanOption("maxtech_fire")) {
            IHex h = game.getBoard().getHex(coords);
            //Unless there is a heavy smoke in the hex already, add one.
            if ( h.terrainLevel( Terrains.SMOKE ) < 2 ) {
                Report r = new Report(5185, Report.PUBLIC);
                r.indent(2);
                r.add(coords.getBoardNum());
                addReport(r);
                h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 2));
                sendChangedHex(coords);
            }
        }
    }

    private void deliverArtilleryInferno(Coords coords, int subjectId) {
        IHex h = game.getBoard().getHex(coords);
        Report r;
        //Unless there is a fire in the hex already, start one.
        if ( !h.containsTerrain( Terrains.FIRE ) ) {
            r = new Report(3005);
            r.subject = subjectId;
            r.indent(2);
            r.add(coords.getBoardNum());
            addReport(r);
            h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
        }
        game.getBoard().addInfernoTo( coords, InfernoTracker.INFERNO_IV_ROUND, 1 );
        sendChangedHex(coords);
        for(Enumeration impactHexHits = game.getEntities(coords);impactHexHits.hasMoreElements();) {
            Entity entity = (Entity)impactHexHits.nextElement();
            entity.infernos.add( InfernoTracker.INFERNO_IV_ROUND, 1 );
            //entity on fire now
            r = new Report(3205);
            r.indent(2);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(entity.infernos.getTurnsLeftToBurn());
            addReport(r);
        }
        for(int dir=0;dir<=5;dir++) {
            Coords tempcoords=coords.translated(dir);
            if(!game.getBoard().contains(tempcoords)) {
                continue;
            }
            if(coords.equals(tempcoords)) {
                continue;
            }
            h = game.getBoard().getHex(tempcoords);
            // Unless there is a fire in the hex already, start one.
            if ( !h.containsTerrain( Terrains.FIRE ) ) {
                r = new Report(3005);
                r.subject = subjectId;
                r.indent(2);
                r.add(tempcoords.getBoardNum());
                addReport(r);
                h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            }
            game.getBoard().addInfernoTo( tempcoords, InfernoTracker.INFERNO_IV_ROUND, 1 );
            sendChangedHex(tempcoords);
            for (Enumeration splashHexHits = game.getEntities(tempcoords);splashHexHits.hasMoreElements();) {
                Entity entity = (Entity)splashHexHits.nextElement();
                entity.infernos.add( InfernoTracker.INFERNO_IV_ROUND, 1 );
                //entity on fire
                r = new Report(3205);
                r.indent(2);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(entity.infernos.getTurnsLeftToBurn());
                addReport(r);
            }
        }
    }

    /**
     * When an entity enters a conventional or Thunder minefield.
     */
    private void enterMinefield(Entity entity, Minefield mf, Coords src, Coords dest, boolean resolvePSRNow) {
        enterMinefield(entity, mf, src, dest, resolvePSRNow, 0);
    }

    /**
     * When an entity enters a conventional or Thunder minefield.
     * @param entity
     *
     * @param mf
     * @param src
     * @param dest
     * @param resolvePSRNow
     * @param hitMod
     */
    private void enterMinefield(Entity entity, Minefield mf, Coords src, Coords dest, boolean resolvePSRNow, int hitMod) {
        Report r;
        // Bug 954272: Mines shouldn't work underwater
        if (!game.getBoard().getHex(mf.getCoords()).containsTerrain(Terrains.WATER)
                || game.getBoard().getHex(mf.getCoords()).containsTerrain(Terrains.PAVEMENT)
                || game.getBoard().getHex(mf.getCoords()).containsTerrain(Terrains.ICE)) {
        switch (mf.getType()) {
            case (Minefield.TYPE_CONVENTIONAL) :
            case (Minefield.TYPE_THUNDER) :
            case (Minefield.TYPE_THUNDER_ACTIVE) :
                if (mf.getTrigger() != Minefield.TRIGGER_NONE &&
                    Compute.d6(2) < (mf.getTrigger()+hitMod)) {
                    return;
                }

                r = new Report(2150);
                r.subject = entity.getId();
                r.add(entity.getShortName(), true);
                r.add(mf.getCoords().getBoardNum(), true);
                addReport(r);
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                addReport( damageEntity(entity, hit, mf.getDamage()));
                addNewLines();

                if (resolvePSRNow) {
                    resolvePilotingRolls(entity, true, src, dest);
                }

                if (!mf.isOneUse()) {
                    revealMinefield(mf);
                } else {
                    removeMinefield(mf);
                }
                break;

            case (Minefield.TYPE_THUNDER_INFERNO) :
                if (mf.getTrigger() != Minefield.TRIGGER_NONE &&
                    Compute.d6(2) < (mf.getTrigger()+hitMod)) {
                    return;
                }
                entity.infernos.add( InfernoTracker.STANDARD_ROUND, mf.getDamage() );
                //report hitting an inferno mine
                r = new Report(2155);
                r.subject = entity.getId();
                r.add(entity.getShortName(), true);
                r.add(mf.getCoords().getBoardNum(), true);
                r.addDesc(entity);
                r.add(entity.infernos.getTurnsLeftToBurn());
                addReport(r);

                // start a fire in the targets hex
                IHex h = game.getBoard().getHex(dest);

                // Unless there a fire in the hex already, start one.
                if ( !h.containsTerrain( Terrains.FIRE ) ) {
                    r = new Report(3005);
                    r.subject = entity.getId();
                    r.add(dest.getBoardNum(), true);
                    addReport(r);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                }
                game.getBoard().addInfernoTo(dest, InfernoTracker.STANDARD_ROUND, 1);
                sendChangedHex(dest);
                break;
            }
        }
    }

    /**
     * Checks to see if an entity sets off any vibrabombs.
     */
    private void checkVibrabombs(Entity entity, Coords coords, boolean displaced) {
        checkVibrabombs(entity, coords, displaced, null, null);
    }

    private void checkVibrabombs(Entity entity, Coords coords, boolean displaced, Coords lastPos, Coords curPos) {
        // Only mechs can set off vibrabombs.
        if (!(entity instanceof Mech)) {
            return;
        }

        int mass = (int) entity.getWeight();

        Enumeration e = game.getVibrabombs().elements();

        while (e.hasMoreElements()) {
            Minefield mf = (Minefield) e.nextElement();

            // Bug 954272: Mines shouldn't work underwater, and BMRr says Vibrabombs are mines
            if (game.getBoard().getHex(mf.getCoords()).containsTerrain(Terrains.WATER)
                    && !game.getBoard().getHex(mf.getCoords()).containsTerrain(Terrains.PAVEMENT)
                    && !game.getBoard().getHex(mf.getCoords()).containsTerrain(Terrains.ICE)) {
                continue;
            }

            // Mech weighing 10 tons or less can't set off the bomb
            if (mass <= mf.getSetting() - 10) {
                continue;
            }

            int effectiveDistance = (mass - mf.getSetting()) / 10;
            int actualDistance = coords.distance(mf.getCoords());

            if (actualDistance <= effectiveDistance) {
                Report r = new Report(2156);
                r.subject = entity.getId();
                r.add(entity.getShortName(), true);
                r.add(mf.getCoords().getBoardNum(), true);
                addReport(r);
                explodeVibrabomb(mf);
            }

            // Hack; when moving, the Mech isn't in the hex during
            // the movement.
            if (!displaced && actualDistance == 0) {
                //report getting hit by vibrabomb
                Report r = new Report(2160);
                r.subject = entity.getId();
                r.add(entity.getShortName(), true);
                addReport(r);
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                addReport( damageEntity(entity, hit, mf.getDamage()));
                addNewLines();
                resolvePilotingRolls(entity, true, lastPos, curPos);
                // we need to apply Damage now, in case the entity lost a leg,
                // otherwise it won't get a leg missing mod if it hasn't yet
                // moved and lost a leg, see bug 1071434 for an example
                entity.applyDamage();
            }
        }
    }
    /**
     * Remove all minefields in the specified coords from the game
     * @param coords The <code>Coords</code> from which to remove minefields
     */
    private void removeMinefieldsFrom(Coords coords) {
        Vector v = game.getMinefields(coords);
        while (v.elements().hasMoreElements()) {
            Minefield mf = (Minefield)v.elements().nextElement();
            removeMinefield(mf);
        }
        
    }

    /**
     * Removes the minefield from the game.
     * @param mf The <code>Minefield</code> to remove
     */
    private void removeMinefield(Minefield mf) {
        if (game.containsVibrabomb(mf)) {
            game.removeVibrabomb(mf);
        }
        game.removeMinefield(mf);

        Enumeration players = game.getPlayers();
        while (players.hasMoreElements()) {
            Player player = (Player) players.nextElement();
            removeMinefield(player, mf);
        }
    }

    /**
     * Removes the minfield from a player.
     * @param player The <code>Player</code> who's minefield should be removed
     * @param mf The <code>Minefield</code> to be removed
     */
    private void removeMinefield(Player player, Minefield mf) {
        if (player.containsMinefield(mf)) {
            player.removeMinefield(mf);
            send(player.getId(), new Packet(Packet.COMMAND_REMOVE_MINEFIELD, mf));
        }
    }

    /**
     * Reveals a minefield for all players.
     * @param mf The <code>Minefield</code> to be revealed
     */
    private void revealMinefield(Minefield mf) {
        Enumeration players = game.getPlayers();
        while (players.hasMoreElements()) {
            Player player = (Player) players.nextElement();
            revealMinefield(player, mf);
        }
    }

    /**
     * Reveals a minefield for a player.
     * @param player The <code>Player</code> who's minefiled should be revealed
     * @param mf The <code>Minefield</code> to be revealed
     */
    private void revealMinefield(Player player, Minefield mf) {
        if (!player.containsMinefield(mf)) {
            player.addMinefield(mf);
            send(player.getId(), new Packet(Packet.COMMAND_REVEAL_MINEFIELD, mf));
        }
    }

    /**
     * Explodes a vibrabomb.
     * @param mf The <code>Minefield</code> to explode
     */
    private void explodeVibrabomb(Minefield mf) {
        Enumeration targets = game.getEntities(mf.getCoords());
        Report r;

        while (targets.hasMoreElements()) {
            Entity entity = (Entity) targets.nextElement();

            // check for the "no_premove_vibra" option
            // If it's set, and the target has not yet moved,
            // it doesn't get damaged.
            if (!entity.isDone() && game.getOptions().booleanOption("no_premove_vibra")) {
                r = new Report(2157);
                r.subject = entity.getId();
                r.add(entity.getShortName(), true);
                addReport(r);
                continue;
            }
            //report hitting vibrabomb
            r = new Report(2160);
            r.subject = entity.getId();
            r.add(entity.getShortName(), true);
            addReport(r);

            if (mf.getType() == Minefield.TYPE_VIBRABOMB) {
                // normal vibrabombs do all damage in one pack
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                addReport( damageEntity(entity, hit, mf.getDamage()));
                addNewLines();
            } else if (mf.getType() == Minefield.TYPE_THUNDER_VIBRABOMB) {
                int damage = mf.getDamage();
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                addReport( damageEntity(entity, hit, damage));
            }

            resolvePilotingRolls(entity, true, entity.getPosition(), entity.getPosition());
            // we need to apply Damage now, in case the entity lost a leg,
            // otherwise it won't get a leg missing mod if it hasn't yet
            // moved and lost a leg, see bug 1071434 for an example
            game.resetPSRs(entity);
            entity.applyDamage();
            addNewLines();
            entityUpdate(entity.getId());
        }

        if (!mf.isOneUse()) {
            revealMinefield(mf);
        } else {
            removeMinefield(mf);
        }
    }

    /**
     * drowns any units swarming the entity
     * @param entity The <code>Entity</code> that is being swarmed
     * @param pos The <code>Coords</code> the entity is at
     */
    private void drownSwarmer(Entity entity, Coords pos) {
        // Any swarming infantry will be destroyed.
        final int swarmerId = entity.getSwarmAttackerId();
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            // Only *platoons* drown while swarming.
            if (!(swarmer instanceof BattleArmor)) {
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                swarmer.setPosition( pos );
                Report r = new Report(2165);
                r.subject = entity.getId();
                r.indent();
                r.add(entity.getShortName(), true);
                addReport(r);
                addReport( destroyEntity(swarmer, "a watery grave", false));
                entityUpdate( swarmerId );
            }
        }
    }

    /**
     * Checks to see if we may have just washed off infernos.  Call after
     * a step which may have done this.
     *
     * @param entity The <code>Entity</code> that is being checked
     * @param coords The <code>Coords</code> the entity is at
     */
    void checkForWashedInfernos(Entity entity, Coords coords) {
        IHex hex = game.getBoard().getHex(coords);
        int waterLevel = hex.terrainLevel(Terrains.WATER);
        // Mech on fire with infernos can wash them off.
        if (!(entity instanceof Mech) || !entity.infernos.isStillBurning()) {
            return;
        }
        // Check if entering depth 2 water or prone in depth 1.
        if (waterLevel > 0 && entity.absHeight() < 0) {
            washInferno(entity, coords);
        }
    }

    /**
     * Washes off an inferno from a mech and adds it to the (water) hex.
     *
     * @param entity The <code>Entity</code> that is taking a bath
     * @param coords The <code>Coords</code> the entity is at
     */
    void washInferno(Entity entity, Coords coords) {
        game.getBoard().addInfernoTo( coords, InfernoTracker.STANDARD_ROUND, 1 );
        entity.infernos.clear();

        // Start a fire in the hex?
        IHex hex = game.getBoard().getHex(coords);
        Report r = new Report(2170);
        r.subject = entity.getId();
        r.addDesc(entity);
        if ( hex.containsTerrain(Terrains.FIRE) ) {
        } else {
            r.messageId = 2175;
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
        }
        addReport(r);
        sendChangedHex(coords);
    }

    /**
     * Add heat from the movement phase
     */
    public void addMovementHeat() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            // build up heat from movement
            if (entity.moved == IEntityMovementType.MOVE_NONE) {
                entity.heatBuildup += entity.getStandingHeat();
            } else if (entity.moved == IEntityMovementType.MOVE_WALK
                       || entity.moved == IEntityMovementType.MOVE_VTOL_WALK) {
                entity.heatBuildup += entity.getWalkHeat();
            } else if (entity.moved == IEntityMovementType.MOVE_RUN
                       || entity.moved == IEntityMovementType.MOVE_VTOL_RUN
                       || entity.moved == IEntityMovementType.MOVE_SKID) {
                entity.heatBuildup += entity.getRunHeat();
            } else if (entity.moved == IEntityMovementType.MOVE_JUMP) {
                entity.heatBuildup += entity.getJumpHeat(entity.delta_distance);
            }
        }
    }

    /**
     * Set the locationsexposure of an entity
     *
     * @param entity The <code>Entity</code> who's exposure is being set
     * @param hex The <code>IHex</code> the entity is in
     * @param isJump a <code>boolean</code> value wether the entity is jumping
     * @param elevation the elevation the entity should be at.
     */

    public void doSetLocationsExposure(Entity entity, IHex hex, boolean isJump, int elevation) {
        if ( hex.terrainLevel(Terrains.WATER) > 0
                && !isJump
                && elevation < 0) {
            if (entity instanceof Mech
                    && !entity.isProne()
                    && hex.terrainLevel(Terrains.WATER) == 1) {
                for (int loop = 0; loop < entity.locations(); loop++) {
                    if (game.getOptions().booleanOption("vacuum"))
                        entity.setLocationStatus(loop, ILocationExposureStatus.VACUUM);
                    else entity.setLocationStatus(loop, ILocationExposureStatus.NORMAL);
                }
                entity.setLocationStatus(Mech.LOC_RLEG, ILocationExposureStatus.WET);
                entity.setLocationStatus(Mech.LOC_LLEG, ILocationExposureStatus.WET);
                addReport(
                                      breachCheck(entity, Mech.LOC_RLEG, hex));
                addReport(
                                      breachCheck(entity, Mech.LOC_LLEG, hex));
                if (entity instanceof QuadMech) {
                    entity.setLocationStatus(Mech.LOC_RARM, ILocationExposureStatus.WET);
                    entity.setLocationStatus(Mech.LOC_LARM, ILocationExposureStatus.WET);
                    addReport(
                               breachCheck(entity, Mech.LOC_RARM, hex));
                    addReport(
                               breachCheck(entity, Mech.LOC_LARM, hex));
                }
            } else {
                for (int loop = 0; loop < entity.locations(); loop++) {
                    entity.setLocationStatus(loop, ILocationExposureStatus.WET);
                    addReport( breachCheck(entity, loop, hex));
                }
            }
        } else {
            for (int loop = 0; loop < entity.locations(); loop++) {
                if (game.getOptions().booleanOption("vacuum"))
                    entity.setLocationStatus(loop, ILocationExposureStatus.VACUUM);
                else entity.setLocationStatus(loop, ILocationExposureStatus.NORMAL);
            }
        }

    }

    /**
     * Do a piloting skill check while standing still (during the
     *  movement phase).
     *
     * @param entity The <code>Entity</code> that should make the PSR
     * @param roll   The <code>PilotingRollData</code> to be used for this PSR.
     *
     *@param Returns true if check succeeds, false otherwise.
     *
     */
    private boolean doSkillCheckInPlace(Entity entity, PilotingRollData roll) {
        if (roll.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            return true;
        }

        // non-mechs should never get here
        if (! (entity instanceof Mech) || entity.isProne()) {
            return true;
        }

        // okay, print the info
        Report r = new Report(2180);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(roll.getLastPlainDesc(), true);
        addReport(r);

        // roll
        final int diceRoll = Compute.d6(2);
        r = new Report(2185);
        r.subject = entity.getId();
        r.add(roll.getValueAsString());
        r.add(roll.getDesc());
        r.add(diceRoll);
        boolean suc;
        if (diceRoll < roll.getValue()) {
            r.choose(false);
            addReport(r);
            doEntityFall(entity, roll);
            suc = false;
        } else {
            r.choose(true);
            addReport(r);
            suc = true;
        }

        return suc;
    }

    /**
     * Do a Piloting Skill check to dislogde swarming infantry.
     *
     * @param   entity The <code>Entity</code> that is doing the dislodging.
     * @param   roll The <code>PilotingRollData</code> for this PSR.
     * @param   curPos The <code>Coords</code> the entity is at.
     * @return  <code>true</code> if the dislodging is successful.
     */
    private boolean doDislodgeSwarmerSkillCheck
        (Entity entity, PilotingRollData roll, Coords curPos)
    {
        // okay, print the info
        Report r = new Report(2180);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(roll.getLastPlainDesc(), true);
        addReport(r);

        // roll
        final int diceRoll = Compute.d6(2);
        r = new Report(2190);
        r.subject = entity.getId();
        r.add(roll.getValueAsString());
        r.add(roll.getDesc());
        r.add(diceRoll);
        if (diceRoll < roll.getValue()) {
            r.choose(false);
            addReport(r);
            return false;
        } else {
            // Dislodged swarmers don't get turns.
            int swarmerId = entity.getSwarmAttackerId();
            final Entity swarmer = game.getEntity( swarmerId );
            game.removeTurnFor( swarmer );
            send( createTurnVectorPacket() );

            // Update the report and cause a fall.
            r.choose(true);
            addReport(r);
            entity.setPosition( curPos );
            doEntityFallsInto(entity, curPos, curPos, roll, false);
            return true;
        }
    }

    /**
     * Do a piloting skill check while moving.
     *
     * @param   entity - the <code>Entity</code> that must roll.
     * @param   src - the <code>Coords</code> the entity is moving from.
     * @param   dest - the <code>Coords</code> the entity is moving to.
     *          This value can be the same as src for in-place checks.
     * @param   reason - the <code>PilotingRollData</code> that is causing
     *          this check.
     * @param   isFallRoll - a <code>boolean</code> flag that indicates that
     *          failure will result in a fall or not.  Falls will be processed.
     * @return  <code>true</code> if the pilot passes the skill check.
     */
    private boolean doSkillCheckWhileMoving( Entity entity,
                                             Coords src,
                                             Coords dest,
                                             PilotingRollData roll,
                                             boolean isFallRoll ) {
        boolean result = true;
        boolean fallsInPlace;

        // Start the info for this roll.
        Report r = new Report(1210);
        r.subject = entity.getId();
        r.addDesc(entity);

        // Will the entity fall in the source or destination hex?
        if ( src.equals(dest) ) {
            fallsInPlace = true;
            r.messageId = 2195;
            r.add(src.getBoardNum(), true);
        } else {
            fallsInPlace = false;
            r.messageId = 2200;
            r.add(src.getBoardNum(), true);
            r.add(dest.getBoardNum(), true);
        }

        // Finish the info.
        r.add(roll.getLastPlainDesc(), true);
        addReport(r);

        // roll
        final int diceRoll = Compute.d6(2);
        r = new Report(2185);
        r.subject = entity.getId();
        r.add(roll.getValueAsString());
        r.add(roll.getDesc());
        r.add(diceRoll);
        if (diceRoll < roll.getValue()) {
            // Does failing the PSR result in a fall.
            if ( isFallRoll ) {
                r.choose(false);
                addReport(r);
                doEntityFallsInto( entity,
                                   (fallsInPlace ? dest : src),
                                   (fallsInPlace ? src : dest),
                                   roll );
            } else {
                r.messageId = 2190;
                r.choose(false);
                addReport(r);
                entity.setPosition( fallsInPlace ? src : dest );
            }
            result = false;
        } else {
            r.choose(true);
            addReport(r);
        }
        return result;
    }


    /**
     * The entity falls into the hex specified.  Check for any conflicts and
     * resolve them.  Deal damage to faller.
     *
     * @param entity The <code>Entity</code> that is falling.
     * @param src The <code>Coords</code> of the source hex.
     * @param dest The <code>Coords</code> of the destination hex.
     * @param roll The <code>PilotingRollData</code> to be used for PSRs induced
     * by the falling.
     */
    private void doEntityFallsInto(Entity entity, Coords src, Coords dest, PilotingRollData roll) {
        doEntityFallsInto(entity, src, dest, roll, true);
    }

    /**
    * The entity falls into the hex specified.  Check for any conflicts and
    * resolve them.  Deal damage to faller.
    *
    * @param entity The <code>Entity</code> that is falling.
    * @param src The <code>Coords</code> of the source hex.
    * @param dest The <code>Coords</code> of the destination hex.
    * @param roll The <code>PilotingRollData</code> to be used for PSRs induced
    * by the falling.
    * @param causeAffa The <code>boolean</code> value wether this fall should
    * be able to cause an accidental fall from above
    */
    private void doEntityFallsInto(Entity entity, Coords src, Coords dest, PilotingRollData roll, boolean causeAffa) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final int srcHeightAboveFloor = entity.getElevation() + srcHex.depth(); 
        final int fallElevation = Math.max(0, srcHex.floor() + srcHeightAboveFloor - destHex.floor());
        int direction = src.direction(dest);
        Report r;
        // check entity in target hex
        Entity affaTarget = game.getAffaTarget(dest, entity);
        // falling mech falls
        r = new Report(2205);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(fallElevation);
        r.add(dest.getBoardNum(), true);
        addReport(r);

        // if hex was empty, deal damage and we're done
        if (affaTarget == null) {
            doEntityFall(entity, dest, fallElevation, roll);
            return;
        }

        // hmmm... somebody there... problems.
        if (fallElevation >= 2 && causeAffa && affaTarget != null) {
            // accidental fall from above: havoc!
            r = new Report(2210);
            r.subject = entity.getId();
            r.addDesc(affaTarget);
            addReport(r);

            // determine to-hit number
            ToHitData toHit = new ToHitData(7, "base");
            if (affaTarget instanceof Tank ) {
                toHit = new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target is a Tank");
            } else {
                toHit.append(Compute.getTargetMovementModifier(game, affaTarget.getId()));
                toHit.append(Compute.getTargetTerrainModifier(game, affaTarget));
            }

            if (toHit.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                // collision roll
                final int diceRoll = Compute.d6(2);
                r = new Report(2215);
                r.subject = entity.getId();
                r.add(toHit.getValue());
                r.add(diceRoll);
                if (diceRoll >= toHit.getValue()) {
                    r.choose(true);
                    addReport(r);
                    // deal damage to target
                    int damage = Compute.getAffaDamageFor(entity);
                    r = new Report(2220);
                    r.subject = affaTarget.getId();
                    r.addDesc(affaTarget);
                    r.add(damage);
                    addReport(r);
                    while (damage > 0) {
                        int cluster = Math.min(5, damage);
                        HitData hit = affaTarget.rollHitLocation(ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                        addReport(
                                              damageEntity(affaTarget, hit, cluster));
                        damage -= cluster;
                    }
                    addNewLines();

                    // attacker falls as normal, on his back
                    // only given a modifier, so flesh out into a full piloting roll
                    PilotingRollData pilotRoll = entity.getBasePilotingRoll();
                    pilotRoll.append(roll);
                    entity.addPilotingModifierForTerrain(pilotRoll, dest);
                    doEntityFall(entity, dest, fallElevation, 3, pilotRoll);
                    doEntityDisplacementMinefieldCheck(entity, src, dest);

                    // defender pushed away, or destroyed, if there is a stacking violation
                    Entity violation = Compute.stackingViolation(game, entity.getId(), dest);
                    if (violation != null) {
                        Coords targetDest = Compute.getValidDisplacement(game, violation.getId(), dest, direction);
                        if (targetDest != null) {
                            doEntityDisplacement(affaTarget, dest, targetDest, new PilotingRollData(violation.getId(), 2, "fallen on"));
                            // Update the violating entity's postion on the client.
                            entityUpdate( affaTarget.getId() );
                        } else {
                            // ack!  automatic death!  Tanks
                            // suffer an ammo/power plant hit.
                            // TODO : a Mech suffers a Head Blown Off crit.
                            addReport(
                                                  destroyEntity(affaTarget, "impossible displacement", (violation instanceof Mech), (violation instanceof Mech)));
                        }
                    }
                    return;
                } else {
                    r.choose(false);
                    addReport(r);
                }
            } else {
                //automatic miss
                r = new Report(2225);
                r.add(toHit.getDesc());
                addReport(r);
            }
            // ok, we missed, let's fall into a valid other hex and not cause an AFFA while doing so
            Coords targetDest = Compute.getValidDisplacement(game, entity.getId(), dest, direction);
            if (targetDest != null) {
                doEntityFallsInto(entity, src, targetDest, new PilotingRollData(entity.getId(), PilotingRollData.IMPOSSIBLE, "pushed off a cliff"), false);
                // Update the entity's postion on the client.
                entityUpdate( entity.getId() );
            } else {
                // ack!  automatic death!  Tanks
                // suffer an ammo/power plant hit.
                // TODO : a Mech suffers a Head Blown Off crit.
                addReport( destroyEntity(entity, "impossible displacement", (entity instanceof Mech), (entity instanceof Mech)));
            }
        } else {
            // damage as normal
            doEntityFall(entity, dest, fallElevation, roll);
            Entity violation = Compute.stackingViolation(game, entity.getId(), dest);
            if(violation != null) {
                // target gets displaced, because of low elevation
                Coords targetDest = Compute.getValidDisplacement(game, entity.getId(), dest, direction);
                doEntityDisplacement(violation, dest, targetDest, new PilotingRollData(violation.getId(), 0, "domino effect"));
                // Update the violating entity's postion on the client.
                entityUpdate( violation.getId() );
            }
        }
    }

    /**
     * Displace a unit in the direction specified.  The unit moves in that
     * direction, and the piloting skill roll is used to determine if it
     * falls.  The roll may be unnecessary as certain situations indicate an
     * automatic fall.  Rolls are added to the piloting roll list.
     */
    private void doEntityDisplacement(Entity entity, Coords src, Coords dest,
                                      PilotingRollData roll) {
        Report r;
        if (!game.getBoard().contains(dest)) {
            // set position anyway, for pushes moving through and stuff like
            // that
            entity.setPosition(dest);
            if (!entity.isDoomed()) {
                game.removeEntity(entity.getId(),
                        IEntityRemovalConditions.REMOVE_PUSHED);
                send(createRemoveEntityPacket(entity.getId(),
                        IEntityRemovalConditions.REMOVE_PUSHED));
                //entity forced from the field
                r = new Report(2230);
                r.addDesc(entity);
                addReport(r);
                // TODO: remove passengers and swarmers.
            }
            return;
        }
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final int direction = src.direction(dest);

        // Handle null hexes.
        if ( srcHex == null || destHex == null ) {
            System.err.println( "Can not displace " + entity.getShortName() +
                                " from " + src +
                                " to " + dest + "." );
            return;
        }
        int fallElevation = entity.elevationOccupied(srcHex) - entity.elevationOccupied(destHex);
        if (fallElevation > 1) {
            if(roll == null)
                roll = entity.getBasePilotingRoll();
            doEntityFallsInto(entity, src, dest, roll);
            return;
        } else {
            //move the entity into the new location gently
            entity.setPosition(dest);
            entity.setElevation(entity.elevationOccupied(destHex) - destHex.surface());
            Entity violation = Compute.stackingViolation(game, entity.getId(), dest);
            if (violation == null) {
                // move and roll normally
                r = new Report(2235);
                r.indent();
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(dest.getBoardNum(), true);
                addReport(r);
            } else {
                // domino effect: move & displace target
                r = new Report(2240);
                r.indent();
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(dest.getBoardNum(), true);
                r.addDesc(violation);
                addReport(r);
            }
            // trigger any special things for moving to the new hex
            doEntityDisplacementMinefieldCheck(entity, src, dest);
            doSetLocationsExposure(entity, destHex, false, entity.getElevation());
            if (roll != null) {
                game.addPSR(roll);
            }
            // Update the entity's postion on the client.
            entityUpdate( entity.getId() );
            
            if(violation != null) {
                doEntityDisplacement(violation, dest, dest.translated(direction), new PilotingRollData(violation.getId(), 0, "domino effect"));
                // Update the violating entity's postion on the client,
                // if it didn't get displaced off the board.
                if ( !game.isOutOfGame(violation) ) {
                    entityUpdate( violation.getId() );
                }
            }
        }
    }

    private void doEntityDisplacementMinefieldCheck(Entity entity, Coords src, Coords dest) {
        if (game.containsMinefield(dest)) {
            Enumeration minefields = game.getMinefields(dest).elements();
            while (minefields.hasMoreElements()) {
                Minefield mf = (Minefield) minefields.nextElement();
                enterMinefield(entity, mf, src, dest, false);
            }
        }
        checkVibrabombs(entity, dest, true);
    }

    /**
     * Receive a deployment packet.  If valid, execute it and end the current
     * turn.
     */
    private void receiveDeployment(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        Coords coords = (Coords)packet.getObject(1);
        int nFacing = packet.getIntValue(2);

        // Handle units that deploy loaded with other units.
        int loadedCount = packet.getIntValue(3);
        Vector loadVector = new Vector();
        for ( int i = 0; i < loadedCount; i++ ){
            int loadedId = packet.getIntValue( 5 + i );
            loadVector.addElement(game.getEntity( loadedId ));
        }

        // is this the right phase?
        if (game.getPhase() != IGame.PHASE_DEPLOYMENT) {
            System.err.println("error: server got deployment packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        final boolean assaultDrop = packet.getBooleanValue(4);
        if ( !game.getTurn().isValid(connId, entity, game)
             || !(game.getBoard().isLegalDeployment(coords, entity.getOwner()) 
                     ||(assaultDrop && game.getOptions().booleanOption("assault_drop") && entity.canAssaultDrop()))) {
            System.err.println("error: server got invalid deployment packet");
            return;
        }

        // looks like mostly everything's okay
        processDeployment(entity, coords, nFacing, loadVector, assaultDrop);

        // Update visibility indications if using double blind.
        if (doBlind()) {
            updateVisibilityIndicator();
        }

        endCurrentTurn(entity);
    }

    /**
     * Process a deployment packet by... deploying the entity!  We load any
     * other specified entities inside of it too.  Also, check that the
     * deployment is valid.
     */
    private void processDeployment(Entity entity, Coords coords, int nFacing, Vector loadVector, boolean assaultDrop) {
        for (Enumeration i = loadVector.elements(); i.hasMoreElements();) {
            Entity loaded = (Entity)i.nextElement();
            if ( loaded == null || loaded.getPosition() != null ||
                 loaded.getTransportId() != Entity.NONE ) {
                // Something is fishy in Denmark.
                System.err.println("error: " + entity + " can not load entity #" + loaded );
                break;
            }
            else {
                // Have the deployed unit load the indicated unit.
                this.loadUnit( entity, loaded );
            }
        }

        entity.setPosition(coords);
        entity.setFacing(nFacing);
        entity.setSecondaryFacing(nFacing);
        IHex hex = game.getBoard().getHex(coords);
        if(assaultDrop) {
            entity.setElevation(hex.ceiling() - hex.surface() + 100); //falling from the sky!
            entity.setAssaultDropInProgress(true);
        } else if (entity instanceof VTOL) {
            // We should let players pick, but this simplifies a lot.
            // Only do it for VTOLs, though; assume everything else is on the ground.
            entity.setElevation(hex.ceiling()-hex.surface()+1);
            while ((Compute.stackingViolation(game, entity, coords, null) != null) && (entity.getElevation() <= 50)) {
                entity.setElevation(entity.getElevation() + 1);
            }
            if (entity.getElevation() > 50) {
                throw new IllegalStateException("Entity #" + entity.getId() + " appears to be in an infinite loop trying to get a legal elevation.");
            }
        } else if (entity.getMovementMode() == IEntityMovementMode.SUBMARINE) {
            // TODO: Submarines should have a selectable height.
            // For now, pretend they're regular naval.
            entity.setElevation(0);
        } else if ((entity.getMovementMode() == IEntityMovementMode.HOVER)
                || (entity.getMovementMode() == IEntityMovementMode.NAVAL)
                || (entity.getMovementMode() == IEntityMovementMode.HYDROFOIL)) {
            // For now, assume they're on the surface.
            // entity elevation is relative to hex surface
            entity.setElevation(0);
        } else if (hex.containsTerrain(Terrains.ICE)
                || hex.containsTerrain(Terrains.BRIDGE)) {
            entity.setElevation(0);
        } else {
            // For anything else, assume they're on the floor.
            // entity elevation is relative to hex surface
            entity.setElevation(hex.floor()-hex.surface());
        }
        entity.setDone(true);
        entity.setDeployed(true);
        entityUpdate(entity.getId());
    }

    private void receiveArtyAutoHitHexes(Packet packet, int connId) {
        Vector artyAutoHitHexes = (Vector) packet.getObject(0);

        Integer playerId = (Integer)artyAutoHitHexes.firstElement();
        artyAutoHitHexes.removeElementAt(0);

        // is this the right phase?
        if (game.getPhase() != IGame.PHASE_SET_ARTYAUTOHITHEXES) {
            System.err.println("error: server got set artyautohithexespacket in wrong phase");
            return;
        }
        game.getPlayer(playerId.intValue()).setArtyAutoHitHexes(artyAutoHitHexes);
        endCurrentTurn(null);
    }

    private void receiveDeployMinefields(Packet packet, int connId) {
        Vector minefields = (Vector) packet.getObject(0);

        // is this the right phase?
        if (game.getPhase() != IGame.PHASE_DEPLOY_MINEFIELDS) {
            System.err.println("error: server got deploy minefields packet in wrong phase");
            return;
        }

        // looks like mostly everything's okay
        processDeployMinefields(minefields);
        endCurrentTurn(null);
    }

    private void processDeployMinefields(Vector minefields) {
        int playerId = Player.PLAYER_NONE;
        for (int i = 0; i < minefields.size(); i++) {
            Minefield mf = (Minefield) minefields.elementAt(i);
            playerId = mf.getPlayerId();

            game.addMinefield(mf);
            if (mf.getType() == Minefield.TYPE_VIBRABOMB) {
                game.addVibrabomb(mf);
            }
        }

        Player player = game.getPlayer( playerId );
        if ( null != player ) {
            int teamId = player.getTeam();

            if (teamId != Player.TEAM_NONE) {
                Enumeration teams = game.getTeams();
                while (teams.hasMoreElements()) {
                    Team team = (Team) teams.nextElement();
                    if (team.getId() == teamId) {
                        Enumeration players = team.getPlayers();
                        while (players.hasMoreElements()) {
                            Player teamPlayer = (Player) players.nextElement();
                            if (teamPlayer.getId() != player.getId()) {
                                send(teamPlayer.getId(), new Packet(Packet.COMMAND_DEPLOY_MINEFIELDS, minefields));
                            }
                            teamPlayer.addMinefields(minefields);
                        }
                        break;
                    }
                }
            } else {
                player.addMinefields(minefields);
            }
        }
    }
    /**
     * Gets a bunch of entity attacks from the packet.  If valid, processess
     * them and ends the current turn.
     */
    private void receiveAttack(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        Vector vector = (Vector)packet.getObject(1);

        // is this the right phase?
        if (game.getPhase() != IGame.PHASE_FIRING
        && game.getPhase() != IGame.PHASE_PHYSICAL
        && game.getPhase() != IGame.PHASE_TARGETING
        && game.getPhase() != IGame.PHASE_OFFBOARD) {
            System.err.println("error: server got attack packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        if (!game.getTurn().isValid(connId, entity, game)) {
            System.err.println("error: server got invalid attack packet");
            return;
        }

        // looks like mostly everything's okay
        processAttack(entity, vector);

        // Update visibility indications if using double blind.
        if (doBlind()) {
            updateVisibilityIndicator();
        }

        endCurrentTurn(entity);
    }

    /**
     * Process a batch of entity attack (or twist) actions by adding them to
     * the proper list to be processed later.
     */
    private void processAttack(Entity entity, Vector vector) {

        // Not **all** actions take up the entity's turn.
        boolean setDone =
            !(game.getTurn() instanceof GameTurn.TriggerAPPodTurn);
        for (Enumeration i = vector.elements(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();

            // is this the right entity?
            if (ea.getEntityId() != entity.getId()) {
                System.err.println("error: attack packet has wrong attacker");
                continue;
            }

            // Anti-mech and pointblank attacks from
            // hiding may allow the target to respond.
            if ( ea instanceof WeaponAttackAction ) {
                final WeaponAttackAction waa = (WeaponAttackAction) ea;
                final String weaponName = entity.getEquipment
                    ( waa.getWeaponId() ).getType().getInternalName();

                if ( Infantry.SWARM_MEK.equals(weaponName) ||
                     Infantry.LEG_ATTACK.equals(weaponName) ) {

                    // Does the target have any AP Pods available?
                    final Entity target = game.getEntity( waa.getTargetId() );
                    Enumeration misc = target.getMisc();
                    while ( misc.hasMoreElements() ) {
                        final Mounted equip = (Mounted) misc.nextElement();
                        if ( equip.getType().hasFlag(MiscType.F_AP_POD) &&
                             equip.canFire()) {

                            // Yup.  Insert a game turn to handle AP pods.
                            // ASSUMPTION : AP pod declarations come
                            // immediately after the attack declaration.
                            game.insertNextTurn( new GameTurn.TriggerAPPodTurn
                                  ( target.getOwnerId(), target.getId() ) );
                            send(createTurnVectorPacket());

                            // We can stop looking.
                            break;

                        } // end found-available-ap-pod

                    } // Check the next piece of equipment on the target.

                } // End check-for-available-ap-pod
            }

            // The equipment type of a club needs to be restored.
            if (ea instanceof ClubAttackAction) {
                ClubAttackAction caa = (ClubAttackAction) ea;
                Mounted club = caa.getClub();
                club.restore();
            }

            if (ea instanceof PushAttackAction) {
                // push attacks go the end of the displacement attacks
                PushAttackAction paa = (PushAttackAction)ea;
                entity.setDisplacementAttack(paa);
                game.addCharge(paa);
            } else if (ea instanceof DodgeAction) {
                entity.dodging = true;
            } else if (ea instanceof SpotAction) {
                entity.setSpotting(true);
            } else {
                // add to the normal attack list.
                game.addAction(ea);
            }

            // Mark any AP Pod as used in this turn.
            if ( ea instanceof TriggerAPPodAction ) {
                TriggerAPPodAction tapa = (TriggerAPPodAction) ea;
                Mounted pod = entity.getEquipment( tapa.getPodId() );
                pod.setUsedThisRound( true );
            }
        }

        // Unless otherwise stated,
        // this entity is done for the round.
        if ( setDone ) {
            entity.setDone(true);
        }
        entityUpdate(entity.getId());

        // update all players on the attacks.  Don't worry about pushes being a
        // "charge" attack.  It doesn't matter to the client.
        send(createAttackPacket(vector, 0));
    }

    /**
     * Auto-target active AMS systems
     */
    private void assignAMS() {

        // sort all missile-based attacks by the target
        Hashtable htAttacks = new Hashtable();
        for (Enumeration i = game.getActions(); i.hasMoreElements(); ) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());

                // Only entities can have AMS.
                if ( Targetable.TYPE_ENTITY != waa.getTargetType() ) {
                    continue;
                }

                // Can only use AMS versus missles.
                if (((WeaponType)weapon.getType()).getDamage() == WeaponType.DAMAGE_MISSILE) {
                    Entity target = game.getEntity(waa.getTargetId());
                    Vector v = (Vector)htAttacks.get(target);
                    if (v == null) {
                        v = new Vector();
                        htAttacks.put(target, v);
                    }
                    v.addElement(waa);
                }
            }
        }

        // let each target assign its AMS
        for (Enumeration i = htAttacks.keys(); i.hasMoreElements(); ) {
            Entity e = (Entity)i.nextElement();
            Vector vAttacks = (Vector)htAttacks.get(e);
            e.assignAMS(vAttacks);
        }
    }

    /** 
     * Called during the weapons fire phase.  Resolves anything other than
     * weapons fire that happens.  Torso twists, for example.
     */
    private void resolveAllButWeaponAttacks() {
        if (game.getPhase()==IGame.PHASE_FIRING) {
            //Phase report header
            addReport(new Report(3000, Report.PUBLIC));
            Report r;
            for (Enumeration e = game.getLayMinefieldActions(); e.hasMoreElements();) {
                LayMinefieldAction lma = (LayMinefieldAction)e.nextElement();
                Entity ent = game.getEntity(lma.getEntityId());
                Mounted mine = ent.getEquipment(lma.getMineId());
                if (!mine.isMissing()) {
                    switch (mine.getMineType()) {
                        case 0:
                            deliverThunderMinefield(ent.getPosition(), ent.getOwnerId(), 10);
                            mine.setMissing(true);
                            r = new Report(3500);
                            r.subject = ent.getId();
                            r.addDesc(ent);
                            r.add(ent.getPosition().getBoardNum());
                            addReport(r);
                            break;
                        case 1:
                            deliverThunderVibraMinefield(ent.getPosition(), ent.getOwnerId(), 10, mine.getVibraSetting());
                            mine.setMissing(true);
                            r = new Report(3505);
                            r.subject = ent.getId();
                            r.addDesc(ent);
                            r.add(ent.getPosition().getBoardNum());
                            addReport(r);
                            break;
                        //TODO: command-detonated mines
                        // case 2:
                    }
                }
            }
            game.resetLayMinefieldActions();
        }

        Vector clearAttempts = new Vector();
        Vector triggerPodActions = new Vector();
        // loop thru actions and handle everything we expect except attacks
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            Entity entity = game.getEntity(ea.getEntityId());
            if (ea instanceof TorsoTwistAction) {
                TorsoTwistAction tta = (TorsoTwistAction)ea;
                if ( entity.canChangeSecondaryFacing() ) {
                    entity.setSecondaryFacing(tta.getFacing());
                }
            }
            else if (ea instanceof FlipArmsAction) {
                FlipArmsAction faa = (FlipArmsAction)ea;
                entity.setArmsFlipped(faa.getIsFlipped());
            }
            else if (ea instanceof FindClubAction) {
                resolveFindClub(entity);
            }
            else if (ea instanceof UnjamAction) {
                resolveUnjam(entity);
            }
            else if (ea instanceof ClearMinefieldAction) {
                clearAttempts.addElement(entity);
            }
            else if (ea instanceof TriggerAPPodAction) {
                TriggerAPPodAction tapa = (TriggerAPPodAction) ea;

                // Don't trigger the same pod twice.
                if ( !triggerPodActions.contains( tapa ) ) {
                    triggerAPPod(entity, tapa.getPodId());
                    triggerPodActions.addElement( tapa );
                } else {
                    System.err.print( "AP Pod #" );
                    System.err.print( tapa.getPodId() );
                    System.err.print( " on " );
                    System.err.print( entity.getDisplayName() );
                    System.err.println(" was already triggered this round!!");
                }
            }
            else if (ea instanceof SearchlightAttackAction) {
                SearchlightAttackAction saa = (SearchlightAttackAction)ea;
                addReport(
                                      saa.resolveAction(game));
            }
        }

        resolveClearMinefieldAttempts(clearAttempts);
    }

    private void resolveClearMinefieldAttempts(Vector clearAttempts) {

        for (int i = 0; i < clearAttempts.size(); i++) {
            Vector temp = new Vector();
            Entity e = (Entity) clearAttempts.elementAt(i);
            Coords pos = e.getPosition();
            temp.addElement(e);

            for (int j = i + 1; j < clearAttempts.size(); j++) {
                Entity ent = (Entity) clearAttempts.elementAt(j);
                if (ent.getPosition().equals(pos)) {
                    temp.addElement(ent);
                    clearAttempts.removeElement(ent);
                }
            }

            boolean accident = false;
            boolean cleared = false;
            for (int j = 0; j < temp.size(); j++) {
                Entity ent = (Entity) temp.elementAt(j);
                int roll = Compute.d6(2);
                int clear = Minefield.CLEAR_NUMBER_INFANTRY;
                int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;

                // Does the entity has a minesweeper?
                Enumeration equip = ent.getMisc();
                while ( equip.hasMoreElements() ) {
                    Mounted mounted = (Mounted) equip.nextElement();
                    if ( mounted.getType().hasFlag(MiscType.F_TOOLS)
                     && mounted.getType().hasSubType(MiscType.S_MINESWEEPER) ) {
                        int sweeperType = mounted.getType().getToHitModifier();
                        clear = Minefield.CLEAR_NUMBER_SWEEPER[sweeperType];
                        boom = Minefield.CLEAR_NUMBER_SWEEPER_ACCIDENT[sweeperType];
                        break;
                    }
                }
                //mine clearing roll
                Report r = new Report(2245);
                r.subject = ent.getId();
                r.add(ent.getShortName(), true);
                r.add(pos.getBoardNum(), true);
                r.add(clear);
                r.add(roll);
                r.newlines = 0;
                addReport(r);

                if (roll >= clear) {
                    //success
                    r = new Report(2250);
                    r.subject = ent.getId();
                    addReport(r);
                    cleared = true;
                } else if (roll <= boom) {
                    //"click"...oops!
                    r = new Report(2255);
                    r.subject = ent.getId();
                    addReport(r);
                    accident = true;
                } else {
                    //failure
                    r = new Report(2260);
                    r.subject = ent.getId();
                    addReport(r);
                }
            }
            if (accident) {
                Enumeration minefields = game.getMinefields(pos).elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();
                    switch (mf.getType()) {
                        case (Minefield.TYPE_CONVENTIONAL) :
                        case (Minefield.TYPE_THUNDER) :
                            for (int j = 0; j < temp.size(); j++) {
                                Entity entity = (Entity) temp.elementAt(j);
                                Report r = new Report(2265);
                                r.subject = entity.getId();
                                r.add(entity.getShortName(), true);
                                addReport(r);
                                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                                addReport( damageEntity(entity, hit, mf.getDamage()));
                                addNewLines();
                            }
                            break;
                        case (Minefield.TYPE_VIBRABOMB) :
                            explodeVibrabomb(mf);
                            break;
                    }
                }
            }
            if (cleared) {
                removeMinefieldsFrom(pos);
            }
        }
    }

    /**
     * Called during the fire phase to resolve all (and only) weapon attacks
     */
    private void resolveOnlyWeaponAttacks() {
        Vector results = new Vector(game.actionsSize());

        // loop thru received attack actions, getting weapon results
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                results.addElement(preTreatWeaponAttack(waa));
            }
        }

        // loop through weapon results and resolve
        int cen = Entity.NONE;
        for (Enumeration i = results.elements(); i.hasMoreElements();) {
            WeaponResult wr = (WeaponResult)i.nextElement();
            resolveWeaponAttack(wr, cen);
            cen = wr.waa.getEntityId();
        }

        // and clear the attacks Vector
        game.resetActions();
    }

    /**
     * Trigger the indicated AP Pod of the entity.
     *
     * @param   entity the <code>Entity</code> triggering the AP Pod.
     * @param   podId the <code>int</code> ID of the AP Pod.
     */
    private void triggerAPPod( Entity entity, int podId ) {

        // Get the mount for this pod.
        Mounted mount = entity.getEquipment( podId );

        // Confirm that this is, indeed, an AP Pod.
        if ( null == mount ) {
            System.err.print( "Expecting to find an AP Pod at " );
            System.err.print( podId );
            System.err.print( " on the unit, " );
            System.err.print( entity.getDisplayName() );
            System.err.println( " but found NO equipment at all!!!" );
            return;
        }
        EquipmentType equip = mount.getType();
        if ( !(equip instanceof MiscType) ||
             !equip.hasFlag(MiscType.F_AP_POD) ) {
            System.err.print( "Expecting to find an AP Pod at " );
            System.err.print( podId );
            System.err.print( " on the unit, " );
            System.err.print( entity.getDisplayName() );
            System.err.print( " but found " );
            System.err.print( equip.getName() );
            System.err.println( " instead!!!" );
            return;
        }

        // Now confirm that the entity can trigger the pod.
        // Ignore the "used this round" flag.
        boolean oldFired = mount.isUsedThisRound();
        mount.setUsedThisRound( false );
        boolean canFire = mount.canFire();
        mount.setUsedThisRound( oldFired );
        if ( !canFire ) {
            System.err.print( "Can not trigger the AP Pod at " );
            System.err.print( podId );
            System.err.print( " on the unit, " );
            System.err.print( entity.getDisplayName() );
            System.err.println( "!!!" );
            return;
        }

        Report r;

        // Mark the pod as fired and log the action.
        mount.setFired( true );
        r = new Report(3010);
        r.newlines = 0;
        r.subject = entity.getId();
        r.addDesc(entity);
        addReport(r);

        // Walk through ALL entities in the triggering entity's hex.
        Enumeration targets = game.getEntities( entity.getPosition() );
        while ( targets.hasMoreElements() ) {
            final Entity target = (Entity) targets.nextElement();

            // Is this an unarmored infantry platoon?
            if ( target instanceof Infantry &&
                 !(target instanceof BattleArmor) ) {

                // Roll d6-1 for damage.
                final int damage = Compute.d6() - 1;

                // If the platoon took no damage, log it and go no further
                if ( 0 == damage ) {
                    r = new Report(3015);
                    r.indent(2);
                    r.subject = target.getId();
                    r.addDesc(target);
                    addReport(r);
                }
                else {
                    // Damage the platoon.
                    addReport( damageEntity( target, new HitData(Infantry.LOC_INFANTRY),damage ));

                    // Damage from AP Pods is applied immediately.
                    target.applyDamage();
                }

            } // End target-is-unarmored

            // Nope, the target is immune.
            // Don't make a log entry for the triggering entity.
            else if ( !entity.equals( target ) ) {
                r = new Report(3020);
                r.indent(2);
                r.subject = target.getId();
                r.addDesc(target);
                addReport(r);
            }

        } // Check the next entity in the triggering entity's hex.
    }

    /**
     * Resolve an Unjam Action object
     */
    private void resolveUnjam(Entity entity) {
        Report r;
        final int TN = entity.getCrew().getGunnery() + 3;
        r = new Report(3025);
        r.subject = entity.getId();
        r.addDesc(entity);
        addReport(r);
        for (Enumeration i = entity.getWeapons(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if(mounted.isJammed()) {
                WeaponType wtype = (WeaponType)mounted.getType();
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    int roll = Compute.d6(2);
                    r = new Report(3030);
                    r.indent();
                    r.subject = entity.getId();
                    r.add(wtype.getName());
                    r.add(TN);
                    r.add(roll);
                    if(roll >= TN) {
                        r.choose(true);
                        mounted.setJammed(false);
                    } else {
                        r.choose(false);
                    }
                    addReport(r);
                }
            }
        }
    }

    private void resolveFindClub(Entity entity) {
        EquipmentType clubType = null;

        entity.setFindingClub(true);

        // Get the entity's current hex.
        Coords coords = entity.getPosition();
        IHex curHex = game.getBoard().getHex( coords );

        Report r;

        // Is there a blown off arm in the hex?
        if (curHex.terrainLevel(Terrains.ARMS) > 0) {
            clubType = EquipmentType.get("Limb Club");
            curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, curHex.terrainLevel(Terrains.ARMS)-1));
            sendChangedHex(entity.getPosition());
            r = new Report(3035);
            r.subject = entity.getId();
            r.addDesc(entity);
            addReport(r);
        }
        // Is there a blown off leg in the hex?
        else if (curHex.terrainLevel(Terrains.LEGS) > 0) {
            clubType = EquipmentType.get("Limb Club");
            curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, curHex.terrainLevel(Terrains.LEGS)-1));
            sendChangedHex(entity.getPosition());
            r = new Report(3040);
            r.subject = entity.getId();
            r.addDesc(entity);
            addReport(r);
        }

        // Is there the rubble of a medium, heavy,
        // or hardened building in the hex?
        else if ( Building.LIGHT < curHex.terrainLevel( Terrains.RUBBLE ) ) {

            // Finding a club is not guaranteed.  The chances are
            // based on the type of building that produced the
            // rubble.
            boolean found = false;
            int roll = Compute.d6(2);
            switch ( curHex.terrainLevel( Terrains.RUBBLE ) ) {
            case Building.MEDIUM:
                if ( roll >= 7 ) {
                    found = true;
                }
                break;
            case Building.HEAVY:
                if ( roll >= 6 ) {
                    found = true;
                }
                break;
            case Building.HARDENED:
                if ( roll >= 5 ) {
                    found = true;
                }
                break;
            }

            // Let the player know if they found a club.
            if ( found ) {
                clubType = EquipmentType.get("Girder Club");
                r = new Report(3045);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
            } else {
                // Sorry, no club for you.
                clubType = null;
                r = new Report(3050);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
            }
        }

        // Are there woods in the hex?
        else if ( curHex.containsTerrain( Terrains.WOODS )
                || curHex.containsTerrain( Terrains.JUNGLE ) ) {
            clubType = EquipmentType.get("Tree Club");
            r = new Report(3055);
            r.subject = entity.getId();
            r.addDesc(entity);
            addReport(r);
        }

        // add the club
        try {
            if (clubType != null) {
                entity.addEquipment(clubType, Mech.LOC_NONE);
            }
        } catch (LocationFullException ex) {
            // unlikely...
            r = new Report(3060);
            r.subject = entity.getId();
            r.addDesc(entity);
            addReport(r);
        }
    }

    /**
     * Generates a WeaponResult object for a WeaponAttackAction.  Adds heat,
     * depletes ammo, sets weapons used.
     */
    private WeaponResult preTreatWeaponAttack(WeaponAttackAction waa) {
        final Entity ae = game.getEntity(waa.getEntityId());
        final Mounted weapon = ae.getEquipment(waa.getWeaponId());
        final WeaponType wtype = (WeaponType)weapon.getType();
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !wtype.hasFlag(WeaponType.F_INFANTRY);

        Mounted ammo = null;
        if (usesAmmo) {
            if (waa.getAmmoId() > -1) {
                ammo = ae.getEquipment(waa.getAmmoId());
                weapon.setLinked(ammo);
            } else {
                ammo = weapon.getLinked();
            }
        }
        boolean streakMiss;

        WeaponResult wr = new WeaponResult();
        wr.waa = waa;

        // has this weapon fired already?
        if (weapon.isUsedThisRound()) {
            wr.toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon has already been used this round");
            return wr;
        }
        // is the weapon functional?
        if (weapon.isDestroyed()) {
            wr.toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon was destroyed in a previous round");
            return wr;
        }
        // is it jammed?
        if (weapon.isJammed()) {
            wr.toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon is jammed");
            return wr;
        }
        // make sure ammo is loaded
        if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0 || ammo.isDumping())) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinked();
        }

        // store the ammo type for later use (needed for artillery attacks)
        waa.setAmmoId(ae.getEquipmentNum(ammo));

        // compute to-hit
        wr.toHit = waa.toHit(game);
        
        if (waa.isNemesisConfused()) {
            wr.toHit.addModifier(1, "iNarc Nemesis pod");
        }
        // roll dice
        wr.roll = Compute.d6(2);

        // if the shot is possible and not a streak miss
        // and not a nemesis-confused shot, add heat and use ammo
        streakMiss = (((wtype.getAmmoType() == AmmoType.T_SRM_STREAK)
                || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK))
                && wr.roll < wr.toHit.getValue());
        if (wr.toHit.getValue() != TargetRoll.IMPOSSIBLE
                && (!streakMiss || Compute.isAffectedByAngelECM(ae, ae.getPosition(), waa.getTarget(game).getPosition()))
                && !waa.isNemesisConfused()) {
            wr = addHeatUseAmmoFor(waa, wr);
        }

        // set the weapon as having fired
        weapon.setUsedThisRound(true);

        // if not streak miss, resolve any AMS attacks on this attack
        if (!streakMiss) {
            wr = resolveAmsFor(waa, wr);
        }

        return wr;
    }

    /**
     * Adds heat and uses ammo appropriate for a single attack of this weapon.
     * Call only on a valid attack (and with a streak weapon, only on hits.)
     *
     * @returns modified WeaponResult
     */
    private WeaponResult addHeatUseAmmoFor(WeaponAttackAction waa, WeaponResult wr) {
        if (waa.isSwarmingMissiles()) return wr;
        
        final Entity ae = game.getEntity(waa.getEntityId());
        final Mounted weapon = ae.getEquipment(waa.getWeaponId());
        final WeaponType wtype = (WeaponType)weapon.getType();
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !wtype.hasFlag(WeaponType.F_INFANTRY);

        Mounted ammo = weapon.getLinked();

        // how many shots are we firing?
        int nShots = weapon.howManyShots();

        // do we need to revert to single shot?
        if (usesAmmo && nShots > 1) {
            int nAvail = ae.getTotalAmmoOfType(ammo.getType());
            if (nAvail < nShots) {
                wr.revertsToSingleShot = true;
                nShots = 1;
            }
        }

        // use up ammo
        if (usesAmmo) {
            for (int i = 0; i < nShots; i++) {
                if (ammo.getShotsLeft() <= 0) {
                    ae.loadWeaponWithSameAmmo(weapon);
                    ammo = weapon.getLinked();
                }
                ammo.setShotsLeft(ammo.getShotsLeft() - 1);
            }
        }

        // build up some heat
        ae.heatBuildup += (wtype.getHeat() * nShots);

        return wr;
    }

    /**
     * Resolves any AMS fire for this weapon attack, adding AMS heat, depleting
     * AMS ammo.
     * @returns the appropriately modified WeaponResult
     */
    private WeaponResult resolveAmsFor(WeaponAttackAction waa, WeaponResult wr) {
        final Entity te = game.getEntity(waa.getTargetId());

        // any AMS attacks by the target?
        Vector vCounters = waa.getCounterEquipment();
        if (null != vCounters) {
            // resolve AMS counter-fire
            wr.amsShotDown = new int[vCounters.size()];
            for (int x = 0; x < vCounters.size(); x++) {
                wr.amsShotDown[x] = 0;

                Mounted counter = (Mounted)vCounters.elementAt(x);
                Mounted mAmmo = counter.getLinked();
                if ((!(counter.getType() instanceof WeaponType))
                        || (!(counter.getType().hasFlag(WeaponType.F_AMS)))
                        || (!counter.isReady())
                        || (counter.isMissing())) {
                    continue;
                }
                // roll hits
                int amsHits = Compute.d6(((WeaponType)counter.getType()).getDamage());

                // build up some heat (assume target is ams owner)
                if (counter.getType().hasFlag(WeaponType.F_HEATASDICE))
                    te.heatBuildup += Compute.d6(((WeaponType)counter.getType()).getHeat());
                else
                    te.heatBuildup += ((WeaponType)counter.getType()).getHeat();

                // decrement the ammo
                if (mAmmo != null)
                    mAmmo.setShotsLeft(Math.max(0, mAmmo.getShotsLeft() - amsHits));

                // set the ams as having fired
                counter.setUsedThisRound(true);

                wr.amsShotDown[x]    = amsHits;
                wr.amsShotDownTotal += amsHits;
            }
        }

        return wr;
    }

    /**
     * Try to ignite the hex, taking into account exisiting fires and the
     * effects of Inferno rounds.
     *
     * @param   c - the <code>Coords</code> of the hex being lit.
     * @param   bInferno - <code>true</code> if the weapon igniting the
     *          hex is an Inferno round.  If some other weapon or ammo
     *          is causing the roll, this should be <code>false</code>.
     * @param   nTargetRoll - the <code>int</code> target number for the
     *          ignition roll.
     * @param   nTargetRoll - the <code>int</code> roll target for the attempt.
     * @param   bReportAttempt - <code>true</code> if the attempt roll should
     *          be added to the report.
     */
    private boolean tryIgniteHex( Coords c, int entityId, boolean bInferno,
                                  int nTargetRoll, boolean bReportAttempt ) {

        IHex hex = game.getBoard().getHex(c);
        boolean bAnyTerrain = false;
        Report r;

        // Ignore bad coordinates.
        if ( hex == null ) {
            return false;
        }

        // inferno always ignites
        if (bInferno) {
            game.getBoard().addInfernoTo(c, InfernoTracker.STANDARD_ROUND, 1);
            nTargetRoll = 0;
            bAnyTerrain = true;
        }

        // The hex may already be on fire.
        if ( hex.containsTerrain( Terrains.FIRE ) ) {
            if ( bReportAttempt ) {
                r = new Report(3065);
                r.indent(3);
                r.subject = entityId;
                addReport(r);
            }
            return true;
        }
        else if ( ignite(hex, nTargetRoll, bAnyTerrain, entityId) ) {
            //hex ignites
            r = new Report(3070);
            r.indent(3);
            r.subject = entityId;
            addReport(r);
            sendChangedHex(c);
            return true;
        }
        return false;
    }

    /**
     * Try to ignite the hex, taking into account exisiting fires and the
     * effects of Inferno rounds.  This version of the method will not report
     * the attempt roll.
     *
     * @param   c - the <code>Coords</code> of the hex being lit.
     * @param   bInferno - <code>true</code> if the weapon igniting the
     *          hex is an Inferno round.  If some other weapon or ammo
     *          is causing the roll, this should be <code>false</code>.
     * @param   nTargetRoll - the <code>int</code> roll target for the attempt.
     */
   private boolean tryIgniteHex(Coords c, int entityId, boolean bInferno,
                                int nTargetRoll) {
       return tryIgniteHex(c, entityId, bInferno, nTargetRoll, false);
   }

    private void tryClearHex(Coords c, int nTarget, int entityId) {
        IHex h = game.getBoard().getHex(c);
        int woods = h.terrainLevel(Terrains.WOODS);
        int jungle = h.terrainLevel(Terrains.JUNGLE);
        boolean ice = h.containsTerrain(Terrains.ICE);
        Report r;
        if (woods == ITerrain.LEVEL_NONE && jungle == ITerrain.LEVEL_NONE && !ice) {
            //woods already cleared
            r = new Report(3075);
            r.indent(3);
            r.subject = entityId;
            addReport(r);
        } else {
            int woodsRoll = Compute.d6(2);
            r = new Report(3080);
            r.indent(3);
            r.subject = entityId;
            r.add(nTarget);
            r.add(woodsRoll);
            r.newlines = 0;
            addReport(r);
            if(woodsRoll >= nTarget) {
                if(woods > 2) {
                    h.removeTerrain(Terrains.WOODS);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.WOODS, woods - 1));
                    //ultra heavy converted to heavy
                    r = new Report(3082);
                    r.subject = entityId;
                    addReport(r);
                }
                else if(woods == 2) {
                    h.removeTerrain(Terrains.WOODS);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.WOODS, woods - 1));
                    //heavy converted to light
                    r = new Report(3085);
                    r.subject = entityId;
                    addReport(r);
                }
                else if(woods == 1) {
                    h.removeTerrain(Terrains.WOODS);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ROUGH, 1));
                    //light converted to rough
                    r = new Report(3090);
                    r.subject = entityId;
                    addReport(r);
                }
                else if(jungle > 2) {
                    h.removeTerrain(Terrains.JUNGLE);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.JUNGLE, jungle - 1));
                    //ultra heavy converted to heavy
                    r = new Report(3083);
                    r.subject = entityId;
                    addReport(r);
                }
                else if(jungle == 2) {
                    h.removeTerrain(Terrains.JUNGLE);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.JUNGLE, jungle - 1));
                    //heavy converted to light
                    r = new Report(3086);
                    r.subject = entityId;
                    addReport(r);
                }
                else if(jungle == 1) {
                    h.removeTerrain(Terrains.JUNGLE);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ROUGH, 1));
                    //light converted to rough
                    r = new Report(3091);
                    r.subject = entityId;
                    addReport(r);
                }
                else if(ice) {
                    h.removeTerrain(Terrains.ICE);
                    r = new Report(3092);
                    r.subject = entityId;
                    addReport(r);
                    resolveIceBroken(c);
                }
                sendChangedHex(c);
            } else {
                //fails to clear woods
                r = new Report(3095);
                r.subject = entityId;
                addReport(r);
            }
        }
    }

    private void resolveWeaponAttack(WeaponResult wr, int lastEntityId) {
        resolveWeaponAttack(wr, lastEntityId, false);
    }
    
    private boolean resolveWeaponAttack(WeaponResult wr, int lastEntityId, boolean isNemesisConfused) {
        return resolveWeaponAttack(wr, lastEntityId, isNemesisConfused, 0);
    }
    
    /**
     * Resolve a single Weapon Attack object
     * @param wr The <code>WeaponResult</code> to resolve
     * @param lastEntityId The <code>int</code> ID of the last
     *        resolved weaponattack's attacking entity
     * @param isNemesisConfused The <code>boolean</code> value of wether
     *        this attack is one caused by homing in on a iNarc Nemesis pod
     *        and so should not be further diverted
     * @param swarmMissilesLeft The <code>int</code> number of remaining swarm
     *        missiles this attack has, 0 if this is not a remaining swarm
     *        missile attack 
     * @return wether we hit or not, only needed for nemesis pod stuff
     */
    private boolean resolveWeaponAttack(WeaponResult wr, int lastEntityId, boolean isNemesisConfused, int swarmMissilesLeft) {
      // If it's an artillery shot, the shooting entity
      // might have died in the meantime
      Entity ae = game.getEntity( wr.waa.getEntityId() );
      if (ae == null) {
          ae = game.getOutOfGameEntity( wr.waa.getEntityId() );
      }
      final Targetable target = game.getTarget(wr.waa.getTargetType(),
                                               wr.waa.getTargetId());
      Report r;
      boolean throughFront;
      if (target instanceof Mech) {
          throughFront = Compute.isThroughFrontHex(game, wr.waa.getEntityId(), (Entity)target); 
      } else {
          throughFront = true;
      }
       
      int subjectId = Entity.NONE;
      Entity entityTarget = null;
      if (target.getTargetType() == Targetable.TYPE_ENTITY) {
          entityTarget = (Entity) target;
          // The target of the attack should definately see the report.
          // The attacker usually will, but they might not if the attack
          // was indirect without a spotter.
          subjectId = entityTarget.getId();
      } else {
          //The target is not an entity, so we will show the report to
          // the attacker instead.
          subjectId = ae.getId();
      }
      final Mounted weapon = ae.getEquipment(wr.waa.getWeaponId());
      final WeaponType wtype = (WeaponType) weapon.getType();
      final boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
      // 2002-09-16 Infantry weapons have unlimited ammo.
      final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
          wtype.getAmmoType() != AmmoType.T_BA_MG &&
          wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
          !isWeaponInfantry;
      //retrieve ammo from the WeaponAttackAction rather than weapon.getLinked, because selected ammo may have changed
      //in the case of artillery attacks
      Mounted ammo = usesAmmo ? ae.getEquipment(wr.waa.getAmmoId()) : null;
      final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();
      Infantry platoon = null;
      final boolean isBattleArmorAttack = wtype.hasFlag(WeaponType.F_BATTLEARMOR);
      ToHitData toHit = wr.toHit;
      boolean bInferno = (usesAmmo
                        && ((atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_BA_INFERNO))
                        && atype.getMunitionType() == AmmoType.M_INFERNO);
      boolean bFragmentation = (usesAmmo
                                && ((atype.getAmmoType() == AmmoType.T_LRM) || (atype.getAmmoType() == AmmoType.T_SRM))
                                && atype.getMunitionType() == AmmoType.M_FRAGMENTATION);
      boolean bAcidHead = (usesAmmo
                            && atype.getAmmoType() == AmmoType.T_SRM
                            && atype.getMunitionType() == AmmoType.M_AX_HEAD);
      boolean bFlechette = (usesAmmo && (atype.getAmmoType() == AmmoType.T_AC)
                            && atype.getMunitionType() == AmmoType.M_FLECHETTE);
      boolean bArtillery = target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY;
      boolean bArtilleryFLAK = target.getTargetType() == Targetable.TYPE_ENTITY
                               && wtype.hasFlag(WeaponType.F_ARTILLERY) 
                               && (usesAmmo && atype.getMunitionType() == AmmoType.M_STANDARD)
                               && entityTarget.getMovementMode() == IEntityMovementMode.VTOL
                               && entityTarget.getElevation() > 0;
      boolean bIncendiary = (usesAmmo && atype.getAmmoType() == AmmoType.T_AC &&
              (atype.getMunitionType() == AmmoType.M_INCENDIARY_AC));
      boolean bTracer = (usesAmmo && atype.getAmmoType() == AmmoType.T_AC &&
              (atype.getMunitionType() == AmmoType.M_TRACER));
      boolean bAntiTSM = (usesAmmo
                            && ((atype.getAmmoType() == AmmoType.T_LRM) || (atype.getAmmoType() == AmmoType.T_SRM))
                             && atype.getMunitionType() == AmmoType.M_ANTI_TSM);
      boolean bSwarm = (usesAmmo
                            && (atype.getAmmoType() == AmmoType.T_LRM)
                            && atype.getMunitionType() == AmmoType.M_SWARM);
      boolean bSwarmI = (usesAmmo
                            && (atype.getAmmoType() == AmmoType.T_LRM)
                            && atype.getMunitionType() == AmmoType.M_SWARM_I);
      boolean isIndirect = ((wtype.getAmmoType() == AmmoType.T_LRM) || (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO))
                               && weapon.curMode().equals("Indirect");
      boolean isHotLoaded = ((wtype.getAmmoType() == AmmoType.T_LRM) ||                 
              (wtype.getAmmoType() == AmmoType.T_LRM_STREAK) ||
              (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO) ||
              (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO_COMBO))
               && weapon.curMode().equals("Hot Load");
      boolean isAngelECMAffected = Compute.isAffectedByAngelECM(ae, ae.getPosition(), target.getPosition());
      if (isIndirect && game.getOptions().booleanOption("indirect_fire") &&
          !game.getOptions().booleanOption("indirect_always_possible") &&
          LosEffects.calculateLos(game, ae.getId(), target).canSee()) {
          r = new Report(3470);
          r.subject = subjectId;
          r.addDesc(ae);
          addReport(r);
          return false;
      }
      boolean bGlancing = false; // For Glancing Hits Rule
      int swarmMissilesNowLeft = 0;
      int hits = 1, glancingMissileMod = 0;
      int glancingCritMod = 0;

      if (!bInferno) {
        // also check for inferno infantry
        bInferno = (isWeaponInfantry && wtype.hasFlag(WeaponType.F_INFERNO));
      }
      final boolean targetInBuilding =
          Compute.isInBuilding(game, entityTarget);
      if ((bArtillery||bArtilleryFLAK) && game.getPhase()==IGame.PHASE_FIRING) { //if direct artillery
          wr.artyAttackerCoords=ae.getPosition();
      }
      if ( (bSwarm || bSwarmI) && entityTarget != null) {
          entityTarget.addTargetedBySwarm(ae.getId(), wr.waa.getWeaponId());
      }

      // Which building takes the damage?
      Building bldg = game.getBoard().getBuildingAt(target.getPosition());
      
      // Are we iNarc Nemesis Confusable?
      boolean isNemesisConfusable = false;
      Mounted mLinker = weapon.getLinkedBy();
      if ( wtype.getAmmoType() == AmmoType.T_ATM ||
           ( mLinker != null &&
             mLinker.getType() instanceof MiscType &&
             !mLinker.isDestroyed() && !mLinker.isMissing() && !mLinker.isBreached() &&
             mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) ) {
          if ((!weapon.getType().hasModes() ||
               !weapon.curMode().equals("Indirect")) &&
              ( ((atype.getAmmoType() == AmmoType.T_ATM) &&
                 (atype.getMunitionType() == AmmoType.M_STANDARD ||
                  atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE ||
                  atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) ) ||
                ((atype.getAmmoType() == AmmoType.T_LRM ||
                 atype.getAmmoType() == AmmoType.T_SRM) &&
                atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE))) {
              isNemesisConfusable = true;
          }
      } else if (wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_SRM) {
          if (usesAmmo && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
              isNemesisConfusable = true;
          }
      }

      if (lastEntityId != ae.getId()) {
          //report who is firing
          r = new Report(3100);
          r.subject = subjectId;
          r.addDesc(ae);
          addReport(r);
      }

      // Swarming infantry can stop during any weapons phase after start.
      if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
          // ... but only as their *only* attack action.
          if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
              r = new Report(3105);
              r.subject = subjectId;
              r.add(toHit.getDesc());
              addReport(r);
              return true;
          } else {
              //swarming ended succesfully
              r = new Report(3110);
              r.subject = subjectId;
              addReport(r);
              // Only apply the "stop swarm 'attack'" to the swarmed Mek.
              if (ae.getSwarmTargetId() != target.getTargetId()) {
                  Entity other = game.getEntity(ae.getSwarmTargetId());
                  other.setSwarmAttackerId(Entity.NONE);
              }
              else {
                  entityTarget.setSwarmAttackerId(Entity.NONE);
              }
              ae.setSwarmTargetId(Entity.NONE);
              return true;
          }
      }

      // Report weapon attack and its to-hit value.
      r = new Report(3115);
      r.indent();
      r.newlines = 0;
      r.subject = subjectId;
      r.add(wtype.getName());
      if (entityTarget != null) {
          r.addDesc(entityTarget);
      } else {
          r.messageId = 3120;
          r.add(target.getDisplayName(), true);
      }
      addReport(r);

      boolean shotAtNemesisTarget = false;
      // check for nemesis
      if (isNemesisConfusable && !isNemesisConfused) {
          // loop through nemesis targets
          for (Enumeration e = game.getNemesisTargets(ae, target.getPosition());e.hasMoreElements();) {
              Entity entity = (Entity)e.nextElement();
              //friendly unit with attached iNarc Nemesis pod standing in the way
              r = new Report(3125);
              r.subject = subjectId;
              addReport(r);
              weapon.setUsedThisRound(false);
              WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                  entity.getTargetId(), wr.waa.getWeaponId());
              newWaa.setNemesisConfused(true);
              WeaponResult newWr = preTreatWeaponAttack(newWaa);
              // attack the new target, and if we hit it, return;
              if (resolveWeaponAttack(newWr, ae.getId(), true)) return true;
              shotAtNemesisTarget = true;
          }
      }
      if (shotAtNemesisTarget) {
          //back to original target
          r = new Report(3130);
          r.subject = subjectId;
          r.newlines = 0;
          addReport(r);
      }
      if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
          r = new Report(3135);
          r.subject = subjectId;
          r.add(toHit.getDesc());
          addReport(r);
          return false;
      }
      else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
          r = new Report(3140);
          r.newlines = 0;
          r.subject = subjectId;
          r.add(toHit.getDesc());
          addReport(r);
      }
      else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
          r = new Report(3145);
          r.newlines = 0;
          r.subject = subjectId;
          r.add(toHit.getDesc());
          addReport(r);
      }
      else {
          //roll to hit
          r = new Report(3150);
          r.newlines = 0;
          r.subject = subjectId;
          r.add(toHit.getValue());
          addReport(r);
      }

      // if firing an HGR unbraced, schedule a PSR
      if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY && ae.mpUsed > 0) {
          // the mod is weight-based
          int nMod;
          switch (ae.getWeightClass()) {
          case EntityWeightClass.WEIGHT_LIGHT:
              nMod = 2;
              break;
          case EntityWeightClass.WEIGHT_MEDIUM:
              nMod = 1;
              break;
          case EntityWeightClass.WEIGHT_HEAVY:
              nMod = 0;
              break;
          default:
              nMod = -1;
          }
          PilotingRollData psr = new PilotingRollData(ae.getId(), nMod,
                                          "fired HeavyGauss unbraced");
          psr.setCumulative(false);
          game.addPSR(psr);
      }

      // dice have been rolled, thanks
      r = new Report(3155);
      r.newlines = 0;
      r.subject = subjectId;
      r.add(wr.roll);
      addReport(r);

      // check for AC or Prototype jams
      int nShots = weapon.howManyShots();
      if (nShots > 1 || 
          (wtype.hasFlag(WeaponType.F_PROTOTYPE) &&
           wtype.getAmmoType() != AmmoType.T_NA) ) {
          int jamCheck = 0;
          if ((((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                && weapon.curMode().equals("Ultra")) ||
              wtype.hasFlag(WeaponType.F_PROTOTYPE)) {
              jamCheck = 2;
              if (weapon.getType().hasModes() &&
                      weapon.curMode().equals("Ultra") &&
                      wtype.hasFlag(WeaponType.F_PROTOTYPE)) {
                  jamCheck = 4;
              }
          } else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
              if (nShots == 2) {
                  jamCheck = 2;
              } else if (nShots == 4) {
                  jamCheck = 3;
              } else if (nShots == 6) {
                  jamCheck = 4;
              }
          }

          if (jamCheck > 0 && wr.roll <= jamCheck) {
              r = new Report(1210);
              // ultras and prototypes are destroyed by jamming
              if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                  r.messageId = 3160;
                  weapon.setJammed(true);
                  weapon.setHit(true);
              } else if (wtype.hasFlag(WeaponType.F_PROTOTYPE)) {
                  r.messageId = 3165;
                  weapon.setJammed(true);
                  weapon.setHit(true);
              } else {
                  r.messageId = 3170;
                  weapon.setJammed(true);
              }
              r.subject = subjectId;
              addReport(r);
              return true;
          }
      }

      // Resolve roll for disengaged field inhibitors on PPCs, if needed
      if (game.getOptions().booleanOption("maxtech_ppc_inhibitors")
          && wtype.hasModes()
          && weapon.curMode().equals("Field Inhibitor OFF") ) {
          int rollTarget = 0;
          int dieRoll = Compute.d6(2);
          int distance = Compute.effectiveDistance(game, ae, target);

          if (distance>=3) {
              rollTarget = 3;
          } else if (distance == 2) {
              rollTarget = 6;
          } else if (distance == 1) {
              rollTarget = 10;
          }
          //roll to avoid damage
          r = new Report(3175);
          r.subject = ae.getId();
          r.indent();
          addReport(r);
          r = new Report(3180);
          r.subject = ae.getId();
          r.indent();
          r.add(rollTarget);
          r.add(dieRoll);
          if (dieRoll<rollTarget) {
              // Oops, we ruined our day...
              int wlocation = weapon.getLocation();
              int wid = ae.getEquipmentNum(weapon);
              int slot = 0;
              weapon.setDestroyed (true);
              for (int i=0; i<ae.getNumberOfCriticals(wlocation); i++) {
                  CriticalSlot slot1 = ae.getCritical (wlocation, i);
                  if (slot1 == null || slot1.getType() != CriticalSlot.TYPE_SYSTEM) {
                      continue;
                  }
                  Mounted mounted = ae.getEquipment(slot1.getIndex());
                  if (mounted.equals(weapon)) {
                      ae.hitAllCriticals(wlocation,i);
                  }
              }
              // Bug 1066147 : damage is *not* like an ammo explosion,
              //        but it *does* get applied directly to the IS.
              r.choose(false);
              addReport(r);
              addReport( damageEntity(ae, new HitData(wlocation), 10, false, 0, true));
              r = new Report(3185);
              r.subject = ae.getId();
              addReport(r);
          } else {
              r.choose(true);
              addReport(r);
          }
      }

      // do we hit?
      boolean bMissed = wr.roll < toHit.getValue();
      if (game.getOptions().booleanOption("maxtech_glancing_blows")) {
          if (wr.roll == toHit.getValue()) {
              bGlancing = true;
              glancingMissileMod = -4;
              glancingCritMod = -2;
              r = new  Report(3186);
              r.subject = ae.getId();
              r.newlines = 0;
              addReport(r);
          } else {
              bGlancing = false;
              glancingMissileMod = 0;
              glancingCritMod = 0;
          }
      } else {
          bGlancing = false;
          glancingMissileMod = 0;
          glancingCritMod = 0;
      }

      // special case TAG.  No damage, but target is tagged until end of turn
      if (wtype.hasFlag(WeaponType.F_TAG)) {
          if (entityTarget == null) {
              r = new Report(3187);
              r.subject = ae.getId();
              addReport(r);
          } else {
              int priority = 1;
              EquipmentMode mode = (weapon.curMode());
              if (mode != null) {
                  if(mode.getName() == "1-shot") {
                      priority=1;
                  } else if(mode.getName() == "2-shot") {
                      priority=2;
                  } else if(mode.getName() == "3-shot") {
                      priority=3;
                  } else if(mode.getName() == "4-shot") {
                      priority=4;
                  }
              }
              if (priority < 1) priority = 1;
              //add even misses, as they waste homing missiles.
              //it is possible for 2 or more tags to hit the same entity, 
              //but this only matters in the offboard phase
              TagInfo info = new TagInfo(ae.getId(), entityTarget.getId(), priority, bMissed);
              game.addTagInfo(info);
              if (!bMissed) {
                  entityTarget.setTaggedBy(ae.getId());
                  r = new Report(3188);
                  r.subject = ae.getId();
                  addReport(r);
              } else {
                  r = new Report(3220);
                  r.subject = ae.getId();
                  addReport(r);
              }
          }
          return !bMissed;
      }
      // special case Artillery FLAK
      if (bArtilleryFLAK) {
          Coords coords = target.getPosition();
          int targEl = target.getElevation();
          // absolute height of target, so we can check units in adjacent hexes
          int absEl = targEl + game.getBoard().getHex(coords).surface();
          if (!bMissed) {
              r = new Report(3191);
              r.subject = subjectId;
              r.add(coords.getBoardNum());
              addReport(r);
          } else {
              coords = Compute.scatter(coords, game.getOptions().booleanOption("margin_scatter_distance")?toHit.getValue()-wr.roll:-1);
              if (game.getBoard().contains(coords)) {
                  r = new Report(3192);
                  r.subject = subjectId;
                  r.add(coords.getBoardNum());
                  addReport(r);
              }
              else {
                  r = new Report(3193);
                  r.subject = subjectId;
                  addReport(r);
                  return !bMissed;
              }
          }
          artilleryDamageArea(coords, wr.artyAttackerCoords, atype, subjectId, ae, true, absEl);
          return !bMissed;
      } // End artillery FLAK
      // special case BA micro bombs
      if (target.getTargetType() == Targetable.TYPE_HEX_BOMB) {
          Coords coords = target.getPosition();
          if (!bMissed) {
              r = new Report(3190);
              r.subject = subjectId;
              r.add(coords.getBoardNum());
              addReport(r);
          } else {
              coords = Compute.scatter(coords, 1);
              if (game.getBoard().contains(coords)) {
                  r = new Report(3195);
                  r.subject = subjectId;
                  r.add(coords.getBoardNum());
                  addReport(r);
              }
              else {
                  r = new Report(3200);
                  r.subject = subjectId;
                  addReport(r);
                  return !bMissed;
              }
          }
          int nCluster = 5;
          Infantry ba = (Infantry)ae;
          int ratedDamage = ba.getShootingStrength();
          artilleryDamageArea(coords, wr.artyAttackerCoords, atype, subjectId, ae, ratedDamage*2, ratedDamage, false, 0);
          return !bMissed;          
      } //end ba-micro-bombs

      // special case minefield delivery, no damage and scatters if misses.
      if (target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER
          || target.getTargetType() == Targetable.TYPE_FLARE_DELIVER) {
          Coords coords = target.getPosition();
          if (!bMissed) {
              r = new Report(3190);
              r.subject = subjectId;
              r.add(coords.getBoardNum());
              addReport(r);
          }
          else {
              coords = Compute.scatter(coords, game.getOptions().booleanOption("margin_scatter_distance")?toHit.getValue()-wr.roll:-1);
              if (game.getBoard().contains(coords)) {
                  //misses and scatters to another hex
                  r = new Report(3195);
                  r.subject = subjectId;
                  r.add(coords.getBoardNum());
                  addReport(r);
              }
              else {
                  //misses and scatters off-board
                  r = new Report(3200);
                  r.subject = subjectId;
                  addReport(r);
                  return !bMissed;
              }
          }

          // Handle the thunder munitions.
          if ((atype.getAmmoType() == AmmoType.T_LRM)
                && atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED) {
              deliverThunderAugMinefield(coords, ae.getOwner().getId(),
                                         atype.getRackSize());
          }
          else if ((atype.getAmmoType() == AmmoType.T_LRM)
                    && atype.getMunitionType() == AmmoType.M_THUNDER) {
              deliverThunderMinefield(coords, ae.getOwner().getId(),
                                      atype.getRackSize());
          }
          else if ((atype.getAmmoType() == AmmoType.T_LRM)
                    && atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO)
              deliverThunderInfernoMinefield(coords, ae.getOwner().getId(),
                                             atype.getRackSize());
          else if ((atype.getAmmoType() == AmmoType.T_LRM)
                    && atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)
              deliverThunderVibraMinefield(coords, ae.getOwner().getId(),
                                           atype.getRackSize(),
                                           wr.waa.getOtherAttackInfo());
          else if ((atype.getAmmoType() == AmmoType.T_LRM)
                    && atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)
              deliverThunderActiveMinefield(coords, ae.getOwner().getId(),
                                        atype.getRackSize());
          else if ((atype.getAmmoType() == AmmoType.T_LRM)
                    && atype.getMunitionType() == AmmoType.M_FLARE)
              deliverFlare(coords, atype.getRackSize());
          //else
          //{
          //...This is an error, but I'll just ignore it for now.
          //}
          return !bMissed;
      }
      //special case artillery
      if ((target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
          && !(usesAmmo && atype.getMunitionType() == AmmoType.M_HOMING)) { 
          Coords coords = target.getPosition();
          if (!bMissed) {
              r = new Report(3190);
              r.subject = subjectId;
              r.add(coords.getBoardNum());
              addReport(r);
          }
          else {
              coords = Compute.scatter(coords, (game.getOptions().booleanOption("margin_scatter_distance"))?(toHit.getValue()-wr.roll):-1);
              if (game.getBoard().contains(coords)) {
                  //misses and scatters to another hex
                  r = new Report(3195);
                  r.subject = subjectId;
                  r.add(coords.getBoardNum());
                  addReport(r);
              }
              else {
                  //misses and scatters off-board
                  r = new Report(3200);
                  r.subject = subjectId;
                  addReport(r);
                  return !bMissed;
              }
          }

          if (usesAmmo) {
              //Check for various non-explosive types
              if(atype.getMunitionType() == AmmoType.M_FLARE) {
                  int radius;
                  if(atype.getAmmoType() == AmmoType.T_ARROW_IV)
                      radius = 4;
                  else if(atype.getAmmoType() == AmmoType.T_LONG_TOM)
                      radius = 3;
                  else radius = Math.max(1,atype.getRackSize() / 5);
                      deliverArtilleryFlare(coords,radius);
                  return !bMissed;
              }
              if(atype.getMunitionType() == AmmoType.M_INFERNO_IV) {
                  deliverArtilleryInferno(coords, subjectId);
                  return !bMissed;
              }
              if(atype.getMunitionType() == AmmoType.M_FASCAM) {
                  deliverFASCAMMinefield(coords, ae.getOwner().getId());
                  return !bMissed;
              }
              if(atype.getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
                  deliverThunderVibraMinefield(coords, ae.getOwner().getId(), 20,
                                               wr.waa.getOtherAttackInfo());
                  return !bMissed;
              }
              if(atype.getMunitionType() == AmmoType.M_SMOKE) {
                  deliverArtillerySmoke(coords);
                  return !bMissed;
              }
          }

          artilleryDamageArea(coords, wr.artyAttackerCoords, atype, subjectId, ae, false, 0);

          return !bMissed;
      } // End artillery
      if(bMissed && usesAmmo && atype.getMunitionType() == AmmoType.M_HOMING) {
          //Arrow IV homing missed, splash the hex
          artilleryDamageHex(target.getPosition(), wr.artyAttackerCoords, 5, atype, subjectId, ae, null, false, 0);
      }
      
      int ammoUsage=0;
      int nDamPerHit = wtype.getDamage();
      if (bMissed) {
          // Report the miss.
          // MGs in rapidfire do heat even when they miss.
          if (weapon.isRapidfire() &&
              !(target instanceof Infantry &&
              !(target instanceof BattleArmor)) ){
                // Check for rapid fire Option. Only MGs can be rapidfire.
                nDamPerHit = Compute.d6();
                ammoUsage = 3*nDamPerHit;
                if (ae.getTotalAmmoOfType(ammo.getType())>0) {
                    for (int i=0; i<ammoUsage; i++) {
                        if (ammo.getShotsLeft() <= 0) {
                            ae.loadWeapon(weapon);
                            ammo = weapon.getLinked();
                        }
                        ammo.setShotsLeft(ammo.getShotsLeft()-1);
                    }
                    if (ae instanceof Mech) {
                        // Apply heat
                        ae.heatBuildup += nDamPerHit;
                    }
                } else {
                    hits = 0;
                }
            }
            if ((( wtype.getAmmoType() == AmmoType.T_SRM_STREAK )
                    || ( wtype.getAmmoType() == AmmoType.T_LRM_STREAK ))
                    && !isAngelECMAffected) {
                //no lock
                r = new Report(3215);
                r.subject = subjectId;
                addReport(r);
            } else {
                //miss
                r = new Report(3220);
                r.subject = subjectId;
                if (weapon.isRapidfire() &&
                    !(target instanceof Infantry &&
                    !(target instanceof BattleArmor)) ){
                  r.messageId = 3225;
                  r.add(ammoUsage);
              }
              addReport(r);
          }

          // Report any AMS action.
          for (int i=0; i < wr.amsShotDown.length; i++) {
              if (wr.amsShotDown[i] > 0) {
                  r = new Report(3230);
                  r.indent();
                  r.subject = subjectId;
                  r.add(wr.amsShotDown[i]);
                  addReport(r);
              }
          }

            // Figure out the maximum number of missile hits.
            // TODO: handle this in a different place.
            int maxMissiles = 0;
            if ( usesAmmo ) {
                maxMissiles = wtype.getRackSize();
                if ( wtype.hasFlag(WeaponType.F_DOUBLE_HITS) ) {
                    maxMissiles *= 2;
                }
                if ( ae instanceof BattleArmor ) {
                    platoon = (Infantry) ae;
                    maxMissiles *= platoon.getShootingStrength();
                }
            }
            if (bSwarm || bSwarmI) {
                swarmMissilesNowLeft = swarmMissilesLeft > 0 ? swarmMissilesLeft : maxMissiles;
                maxMissiles = swarmMissilesLeft > 0 ? swarmMissilesLeft : maxMissiles;
            }

            // If the AMS shot down *all* incoming missiles, if
            // the shot is an automatic failure, or if it's from
            // a Streak rack, then Infernos can't ignite the hex
            // and any building is safe from damage.
            if ( (usesAmmo && wr.amsShotDownTotal >= maxMissiles)
                    || toHit.getValue() == TargetRoll.AUTOMATIC_FAIL
                    || ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK 
                    || wtype.getAmmoType() == AmmoType.T_LRM_STREAK)
                    && !isAngelECMAffected)) {
                return !bMissed;
            }
            // If we're using swarm munition, set the number of missiles that
            // are left
            if ((bSwarm || bSwarmI) && entityTarget != null) {
                swarmMissilesNowLeft -= wr.amsShotDownTotal;
                Entity swarmTarget = Compute.getSwarmTarget(game, ae.getId(), entityTarget, wr.waa.getWeaponId());
                if (swarmTarget != null) {
                    r = new Report(3420);
                    r.subject = ae.getId();
                    r.indent();
                    r.add(swarmMissilesNowLeft);
                    addReport(r);
                    weapon.setUsedThisRound(false);
                    WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                        swarmTarget.getTargetId(), wr.waa.getWeaponId());
                    newWaa.setSwarmingMissiles(true);
                    newWaa.setOldTargetId(target.getTargetId());
                    newWaa.setAmmoId(wr.waa.getAmmoId());
                    WeaponResult newWr = preTreatWeaponAttack(newWaa);
                    resolveWeaponAttack(newWr, ae.getId(), false, swarmMissilesNowLeft);
                } else {
                    r = new Report(3425);
                    r.subject = ae.getId();
                    r.indent();
                    addReport(r);
                }
            }

            // Shots that miss an entity can set fires.
            // Infernos always set fires.  Otherwise
            // Buildings can't be accidentally ignited,
            // and some weapons can't ignite fires.
            if ( entityTarget != null &&
                 ( bInferno ||
                   ( bldg == null &&
                     wtype.getFireTN() != TargetRoll.IMPOSSIBLE ) ) ) {
                tryIgniteHex(target.getPosition(), ae.getId(), bInferno, 11);
            }

            // BMRr, pg. 51: "All shots that were aimed at a target inside
            // a building and miss do full damage to the building instead."
            // BMRr, pg. 77: If the spotting unit successfully designates the target but
            // the missile misses, it still detonates in the hex and causes 5
            // points of artillery damage each to all units in the target hex
            if ( !targetInBuilding) {
                return !bMissed;
            }
        }

        // special case NARC hits.  No damage, but a beacon is appended
        if (!bMissed &&
            wtype.getAmmoType() == AmmoType.T_NARC &&
            atype.getMunitionType() != AmmoType.M_NARC_EX) {

            if (wr.amsShotDownTotal > 0) {
                r = new Report(3235);
                r.subject = subjectId;
                addReport(r);
                for (int i=0; i < wr.amsShotDown.length; i++) {
                    r = new Report(3230);
                    r.indent(1);
                    r.subject = subjectId;
                    r.add(wr.amsShotDown[i]);
                    addReport(r);
                }
                r = new Report(3240);
                r.subject = subjectId;
                addReport(r);
            } else if (entityTarget == null) {
                r = new Report(3245);
                r.subject = subjectId;
                addReport(r);
            } else {
                entityTarget.setNarcedBy(ae.getOwner().getTeam());
                //narced
                r = new Report(3250);
                r.subject = subjectId;
                addReport(r);
            }
            return !bMissed;
        }
        
        // special case iNARC hits.  No damage, but a beacon is appended
        if (!bMissed &&
            wtype.getAmmoType() == AmmoType.T_INARC &&
            atype.getMunitionType() != AmmoType.M_EXPLOSIVE) {

            if (wr.amsShotDownTotal > 0) {
                r = new Report(3235);
                r.subject = subjectId;
                addReport(r);
                for (int i=0; i < wr.amsShotDown.length; i++) {
                    r = new Report(3230);
                    r.indent(1);
                    r.subject = subjectId;
                    r.add(wr.amsShotDown[i]);
                    addReport(r);
                }
                r = new Report(3240);
                r.subject = subjectId;
                addReport(r);
            } else if (entityTarget == null) {
                r = new Report(3245);
                r.subject = subjectId;
                addReport(r);
            } else {
                INarcPod pod = null;
                if (atype.getMunitionType() == AmmoType.M_ECM) {
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.ECM );
                    r = new Report(3251);
                    r.subject = subjectId;
                    addReport(r);
                } else if (atype.getMunitionType() == AmmoType.M_HAYWIRE) {
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.HAYWIRE );
                    r = new Report(3252);
                    r.subject = subjectId;
                    addReport(r);
                } else if (atype.getMunitionType() == AmmoType.M_NEMESIS) {
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.NEMESIS );
                    r = new Report(3253);
                    r.subject = subjectId;
                    addReport(r);
                } else {
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.HOMING );
                    r = new Report(3254);
                    r.subject = subjectId;
                    addReport(r);
                }
                entityTarget.attachINarcPod(pod);
            }
            return !bMissed;
        }

        // attempt to clear minefield by LRM/MRM fire.
        if (!bMissed && target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR) {
            int clearAttempt = Compute.d6(2);

            if (clearAttempt >= Minefield.CLEAR_NUMBER_WEAPON) {
                //minefield cleared
                r = new Report(3255);
                r.indent(1);
                r.subject = subjectId;
                addReport(r);
                Coords coords = target.getPosition();

                Enumeration minefields = game.getMinefields(coords).elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();

                    removeMinefield(mf);
                }
            } else {
                //fails to clear
                r = new Report(3260);
                r.indent(1);
                r.subject = subjectId;
                addReport(r);
            }
            return !bMissed;
        }

        // yeech.  handle damage. . different weapons do this in very different ways
        int nCluster = 1, nSalvoBonus = 0;
        boolean bSalvo = false;
        // ecm check is heavy, so only do it once
        boolean bCheckedECM = false;
        boolean bECMAffected = false;
        boolean bMekStealthActive = false;
        String sSalvoType = " shot(s) ";
        boolean bAllShotsHit = false;
        int nRange = ae.getPosition().distance(target.getPosition());
        int nMissilesModifier = 0;
        boolean maxtechmissiles = game.getOptions().booleanOption("maxtech_mslhitpen");
        if (maxtechmissiles) {
            if (nRange<=1) {
                nMissilesModifier = +1;
            } else if (nRange <= wtype.getShortRange()) {
                nMissilesModifier = 0;
            } else if (nRange <= wtype.getMediumRange()) {
                nMissilesModifier = -1;
            } else {
                nMissilesModifier = -2;
            }
       }
        // All shots fired by a Streak SRM weapon, during
        // a Mech Swarm hit, or at an adjacent building.
        if (((wtype.getAmmoType() == AmmoType.T_SRM_STREAK
                || wtype.getAmmoType() == AmmoType.T_LRM_STREAK)
                && !isAngelECMAffected)
                || wtype.getAmmoType() == AmmoType.T_NARC
                || ae.getSwarmTargetId() == wr.waa.getTargetId()
                || ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE
                || target.getTargetType() == Targetable.TYPE_BUILDING)
                && ae.getPosition().distance(target.getPosition()) <= 1)) {
            bAllShotsHit = true;
        }

        // Mek swarms attach the attacker to the target.
        if ( !bMissed && Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            // Is the target already swarmed?
            if ( Entity.NONE != entityTarget.getSwarmAttackerId() ) {
                r = new Report(3265);
                r.subject = subjectId;
                addReport(r);
            }
            // Did the target get destroyed by weapons fire?
            else if ( entityTarget.isDoomed() || entityTarget.isDestroyed() ||
                      entityTarget.getCrew().isDead() ) {
                r = new Report(3270);
                r.subject = subjectId;
                addReport(r);
            } else {
                //success
                r = new Report(3275);
                r.subject = subjectId;
                addReport(r);
                ae.setSwarmTargetId( wr.waa.getTargetId() );
                entityTarget.setSwarmAttackerId( wr.waa.getEntityId() );
            }
            return !bMissed;
        }

        // Magnetic Mine Launchers roll number of hits on battle armor
        // hits table but use # mines firing instead of men shooting.
        else if ( wtype.getInternalName().equals(BattleArmor.MINE_LAUNCHER) ) {
            hits = nShots;
            if ( !bAllShotsHit ) {
                hits = Compute.getBattleArmorHits( hits );
            }
            bSalvo = true;
            sSalvoType = " mine(s) ";
        }

        // Other battle armor attacks use # of men firing to determine hits.
        // Each hit can be in a new location. The damage per shot comes from
        // the "racksize", or from the ammo, for ammo weapons
        else if ( isBattleArmorAttack ) {
            bSalvo = true;
            platoon = (Infantry) ae;
            nCluster = 1;
            if (usesAmmo) {
                nDamPerHit = atype.getDamagePerShot();
            }
            nDamPerHit = wtype.getRackSize();
            hits = platoon.getShootingStrength();
            // All attacks during Mek Swarms hit; all
            // others use the Battle Armor hits table.
            if ( !bAllShotsHit ) {
                hits = Compute.getBattleArmorHits( hits );
            }

            // Handle Inferno SRM squads.
            if (bInferno) {
                nCluster = hits;
                nDamPerHit = 0;
                sSalvoType = " Inferno missle(s) ";
                bSalvo = false;
            }
            if (ae.getSwarmTargetId() == wr.waa.getTargetId())
                nDamPerHit += ((BattleArmor)ae).getVibroClawDamage();
        }

        // Infantry damage depends on # men left in platoon.
        else if (isWeaponInfantry) {
            bSalvo = true;
            platoon = (Infantry)ae;
            nCluster = 5;
            nDamPerHit = 1;
            hits = platoon.getDamage(platoon.getShootingStrength());
            //TODO: Hmm, this should be localizable
            sSalvoType = " damage are inflicted by the shots that ";

            // Handle Inferno SRM infantry.
            if (bInferno) {
                nCluster = hits;
                nDamPerHit = 0;
                sSalvoType = " Inferno missile(s) ";
                bSalvo = false;
            }
        } else if (wtype.getDamage() == WeaponType.DAMAGE_MISSILE ||
                   wtype.hasFlag(WeaponType.F_MISSILE_HITS) ) {
            bSalvo = true;

            // Weapons with ammo type T_BA_MG or T_BA_SMALL_LASER
            // don't have an atype object.
            if ( wtype.getAmmoType() == AmmoType.T_BA_MG ||
                 wtype.getAmmoType() == AmmoType.T_BA_SMALL_LASER ) {
                nDamPerHit = Math.abs( wtype.getAmmoType() );
            } else {
                sSalvoType = " missile(s) ";
                // Get the damage from the linked ammo.
                nDamPerHit = atype.getDamagePerShot();
                if ((wtype.getAmmoType() == AmmoType.T_TBOLT5
                        || wtype.getAmmoType() == AmmoType.T_TBOLT10
                        || wtype.getAmmoType() == AmmoType.T_TBOLT15
                        || wtype.getAmmoType() == AmmoType.T_TBOLT20
                        ) && nRange <= wtype.getMinimumRange()) {
                    nDamPerHit /= 2;
                } 
            }

            if ( wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_LRM_STREAK ||
                 wtype.getAmmoType() == AmmoType.T_MRM ||
                 wtype.getAmmoType() == AmmoType.T_ATM ||
                 wtype.getAmmoType() == AmmoType.T_EXLRM ||
                 wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER ) {
                nCluster = 5;
            }

            // calculate # of missiles hitting
            if ( wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_SRM ||
                 wtype.getAmmoType() == AmmoType.T_ATM ) {

                // check for artemis, else check for narc and similar things
                mLinker = weapon.getLinkedBy();
                if ( wtype.getAmmoType() == AmmoType.T_ATM ||
                     ( mLinker != null &&
                       mLinker.getType() instanceof MiscType &&
                       !mLinker.isDestroyed() && !mLinker.isMissing() && !mLinker.isBreached() &&
                       mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) &&
                       atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {

                    // check ECM interference
                    if (!bCheckedECM) {
                        // Attacking Meks using stealth suffer ECM effects.
                        if ( ae instanceof Mech ) {
                            bMekStealthActive = ae.isStealthActive();
                        }
                        //if the attacker is effected by ECM or the target is protected by ECM then
                        //act as if effected.
                        if (Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition()) || 
                                Compute.isAffectedByAngelECM(ae, ae.getPosition(), target.getPosition()))
                            bECMAffected = true;
                        else if ( Compute.isProtectedByECM((Entity)target,target.getPosition(),ae.getPosition()) ||
                                Compute.isProtectedByAngelECM((Entity)target,target.getPosition(),ae.getPosition()))
                            bECMAffected = true;
                        else
                            bECMAffected = false;
                        bCheckedECM = true;
                    }
                    // also no artemis for IDF, and only use standard ammo (excepot for ATMs)
                    if (!bECMAffected && !bMekStealthActive
                        && (!weapon.getType().hasModes()
                            || !weapon.curMode().equals("Indirect"))
                        && ( (atype.getAmmoType() == AmmoType.T_ATM &&
                              (atype.getMunitionType() == AmmoType.M_STANDARD||
                               atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE ||
                               atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)) ||
                             ( (atype.getAmmoType() == AmmoType.T_LRM ||
                                atype.getAmmoType() == AmmoType.T_SRM) &&
                               atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE))) {
                        nSalvoBonus += 2;
                    }
                } else if (entityTarget != null && 
                        (entityTarget.isNarcedBy(ae.getOwner().getTeam()) || 
                         entityTarget.isINarcedBy(ae.getOwner().getTeam()))) {
                    // check ECM interference
                    if (!bCheckedECM) {
                        // Attacking Meks using stealth suffer ECM effects.
                        if ( ae instanceof Mech ) {
                            bMekStealthActive = ae.isStealthActive();
                        }
                        
                        //if the attacker is effected by ECM or the target is protected by ECM then
                        //act as if effected.
                        if (Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition()) || 
                                Compute.isAffectedByAngelECM(ae, ae.getPosition(), target.getPosition()))
                            bECMAffected = true;
                        else if ( Compute.isProtectedByECM((Entity)target,target.getPosition(),ae.getPosition()) ||
                                Compute.isProtectedByAngelECM((Entity)target,target.getPosition(),ae.getPosition()))
                            bECMAffected = true;
                        else
                            bECMAffected = false;
                        bCheckedECM = true;
                    }
                    // only apply Narc bonus if we're not suffering ECM effect
                    // and we are using narc ammo.
                    if (!bECMAffected
                            && !bMekStealthActive
                            && ((atype.getAmmoType() == AmmoType.T_LRM) || (atype.getAmmoType() == AmmoType.T_SRM))
                            && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
                        nSalvoBonus += 2;
                    }
                }
            }

            // If dealing with Inferno rounds set damage to zero and reset
            // all salvo bonuses (cannot mix with other special munitions).
            if (bInferno) {
                    nDamPerHit = 0;
                    nSalvoBonus = 0;
                    sSalvoType = " inferno missile(s) ";
                    bSalvo = false;
            }
            if (bSwarm) {
                sSalvoType = " swarm missile(s) ";
            }
            if (bSwarmI) {
                sSalvoType = " swarm-I missile(s) ";
            }
            if (bAntiTSM) {
                sSalvoType = " anti-TSM missile(s) ";
            }

            // If dealing with fragmentation missiles,
            // it does double damage to infantry...
            if (bFragmentation) {
                sSalvoType = " fragmentation missile(s) ";
            }

            // Acid-heads, like infernos, can't mix with any other munitions type.
            if (bAcidHead) {
                nDamPerHit = 1;
                nSalvoBonus = -2;
                sSalvoType = " acid-head missile(s) ";
            }

            // Large MRM missile racks roll twice.
            // MRM missiles never recieve hit bonuses.
            if ( wtype.getRackSize() == 30 || wtype.getRackSize() == 40 ) {
                hits = Compute.missilesHit(wtype.getRackSize() / 2, nMissilesModifier+glancingMissileMod, maxtechmissiles | bGlancing) +
                    Compute.missilesHit(wtype.getRackSize() / 2, nMissilesModifier+glancingMissileMod, maxtechmissiles | bGlancing);
            }

            // Battle Armor units multiply their racksize by the number
            // of men shooting and they can't get missile hit bonuses.
            else if ( ae instanceof BattleArmor ) {
                platoon = (Infantry) ae;
                int temp = wtype.getRackSize() * platoon.getShootingStrength();

                // Do all shots hit?
                if ( bAllShotsHit ) {
                    hits = temp;
                } else {
                    // Account for more than 20 missles hitting.
                    hits = 0;
                    while ( temp > 20 ) {
                        hits += Compute.missilesHit( 20, nMissilesModifier+glancingMissileMod, maxtechmissiles | bGlancing );
                        temp -= 20;
                    }
                    hits += Compute.missilesHit( temp, nMissilesModifier+glancingMissileMod, maxtechmissiles | bGlancing );
                } // End not-all-shots-hit
            }

            // If all shots hit, use the full racksize.
            else if ( bAllShotsHit ) {
                hits = wtype.getRackSize();
            }
            // In all other circumstances, roll for hits.
            else {
                hits = Compute.missilesHit(wtype.getRackSize(), nSalvoBonus + nMissilesModifier + glancingMissileMod, maxtechmissiles | bGlancing);
                // swarm missiles that didn't hit continue 
                if ( (bSwarm || bSwarmI) && swarmMissilesLeft == 0) {
                    swarmMissilesNowLeft = wtype.getRackSize() - hits;
                }
            }
            // anti TSM missiles hit with half the number, round up
            if (bAntiTSM) {
                hits = (int)Math.ceil((double)hits/2);
            }
            // swarm or swarm-I shots may just hit with the remaining missiles
            if ((bSwarm || bSwarmI) && (swarmMissilesLeft > 0)) {
                int swarmsForHitTable = 5;
                if (swarmMissilesLeft > 5 && swarmMissilesLeft <= 10)
                    swarmsForHitTable = 10;
                else if (swarmMissilesLeft > 10 && swarmMissilesLeft <= 15)
                    swarmsForHitTable = 15;
                else if (swarmMissilesLeft > 15 && swarmMissilesLeft <= 20)
                    swarmsForHitTable = 20;
                hits = Compute.missilesHit(swarmsForHitTable, nSalvoBonus + nMissilesModifier + glancingMissileMod, maxtechmissiles | bGlancing);
                if (hits > swarmMissilesLeft) {
                    hits = swarmMissilesLeft;
                }
                swarmMissilesNowLeft = swarmMissilesLeft - hits;
            }

            // Advanced SRMs may get additional missiles
            if ( usesAmmo &&
                 atype.getAmmoType() == AmmoType.T_SRM_ADVANCED) {
                int tmp = wtype.getRackSize() * platoon.getShootingStrength();
                if (hits%2 == 1 && hits < tmp) {
                    hits++;
                }
            }

        } else if (usesAmmo
                && ((atype.getAmmoType() == AmmoType.T_AC_LBX) 
                || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB))
                && atype.getMunitionType() == AmmoType.M_CLUSTER) {
            // Cluster shots break into single point clusters.
            bSalvo = true;
            hits = wtype.getRackSize();
            // war of 3039 prototype LBXs get -1 mod on missile chart
            int nMod = wtype.hasFlag(WeaponType.F_PROTOTYPE) ? 0 : -1;
            if ( !bAllShotsHit ) {
                if (!bGlancing) {
                    hits = Compute.missilesHit( hits, nMod );
                } else {
                    // if glancing blow, half the number of missiles that hit,
                    // that halves damage. do this, and not adjust number of 
                    // pellets, because maxtech only talks about missile weapons
                    hits = Compute.missilesHit(hits, nMod)/2;
                }
            }
            nDamPerHit = 1;
        } else if (nShots > 1) {
            // this should handle multiple attacks from ultra and rotary ACs
            bSalvo = true;
            hits = nShots;
            if ( !bAllShotsHit ) {
                hits = Compute.missilesHit( hits );
            }
        }
        else if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY) {
            // HGR does range-dependent damage
            if (nRange <= wtype.getShortRange()) {
                nDamPerHit = 25;
            } else if (nRange <= wtype.getMediumRange()) {
                nDamPerHit = 20;
            } else {
                nDamPerHit = 10;
            }
        } else if (wtype.hasFlag(WeaponType.F_ENERGY)) {
            // Check for Altered Damage from Energy Weapons (MTR, pg.22)
            nDamPerHit = wtype.getDamage();
            if (game.getOptions().booleanOption("maxtech_altdmg")) {
                if (nRange<=1) {
                    nDamPerHit++;
                } else if (nRange <= wtype.getMediumRange()) {
                    // Do Nothing for Short and Medium Range
                } else if (nRange <= wtype.getLongRange()) {
                    nDamPerHit--;
                } else if (nRange <= wtype.getExtremeRange()) {
                    nDamPerHit = (int)Math.floor(nDamPerHit/2.0);
                }
            }
        } else if (weapon.isRapidfire() &&
                   !(target instanceof Infantry &&
                     !(target instanceof BattleArmor)) ){
            // Check for rapid fire Option. Only MGs can be rapidfire.
            nDamPerHit = Compute.d6();
            ammoUsage = 3*nDamPerHit;
            if (ae.getTotalAmmoOfType(ammo.getType())>0) {
                for (int i=0; i<ammoUsage; i++) {
                    if (ammo.getShotsLeft() <= 0) {
                        ae.loadWeapon(weapon);
                        ammo = weapon.getLinked();
                    } 
                    ammo.setShotsLeft(ammo.getShotsLeft()-1);
                }
                if (ae instanceof Mech) {
                    // Apply heat
                    ae.heatBuildup += nDamPerHit;
                }
            } else {
                hits = 0;
            }
        } 
        // laser prototype weapons get 1d6 of extra heat
        if (wtype.hasFlag(WeaponType.F_LASER) &&
            wtype.hasFlag(WeaponType.F_PROTOTYPE)) {
            ae.heatBuildup += Compute.d6();
        }

        // tracer rounds do -1 damage.
        if(bTracer) {
            nDamPerHit--;
        }

        // only halve damage for non-missiles and non-cluster,
        // because cluster lbx gets handled above.
        if (bGlancing && !wtype.hasFlag(WeaponType.F_MISSILE) && !wtype.hasFlag(WeaponType.F_MISSILE_HITS)
                && !(usesAmmo
                && ((atype.getAmmoType() == AmmoType.T_AC_LBX) 
                || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB))
                && atype.getMunitionType() == AmmoType.M_CLUSTER)) {
            nDamPerHit = (int)Math.floor(nDamPerHit/2.0);
        }

        // Some weapons double the number of hits scored.
        if ( wtype.hasFlag(WeaponType.F_DOUBLE_HITS) ) {
            hits *= 2;
        }

        //Arrow IV homing hits single location, like an AC20
        if(usesAmmo && atype.getMunitionType() == AmmoType.M_HOMING) {
            nDamPerHit = wtype.getRackSize();
            if (entityTarget!=null && entityTarget.getTaggedBy() != -1) {
                if(wr.artyAttackerCoords != null) {
                    toHit.setSideTable(entityTarget.sideTable(wr.artyAttackerCoords));
                } else {
                    Entity tagger = game.getEntity(entityTarget.getTaggedBy());
                    if(tagger != null) { 
                        toHit.setSideTable(Compute.targetSideTable(tagger, entityTarget));
                    }
                }
            }
        }

        // We've calculated how many hits.  At this point, any missed
        // shots damage the building instead of the target.
        if ( bMissed ) {
            if ( targetInBuilding && bldg != null && !(bSwarm || bSwarmI)) {

                // Reduce the number of hits by AMS hits.
                if (wr.amsShotDownTotal > 0) {
                    for (int i=0; i < wr.amsShotDown.length; i++) {
                        int shotDown = Math.min(wr.amsShotDown[i], hits);
                        r = new Report(3280);
                        r.indent(1);
                        r.subject = subjectId;
                        r.add(shotDown);
                        addReport(r);
                    }
                    hits -= wr.amsShotDownTotal;
                }

                // Is the building hit by Inferno rounds?
                if ( bInferno && hits > 0 ) {

                    // start a fire in the targets hex
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);

                    // Is there a fire in the hex already?
                    if ( h.containsTerrain( Terrains.FIRE ) ) {
                        r = new Report(3285);
                        r.indent(2);
                        r.subject = subjectId;
                        r.add(hits);
                        r.add(c.getBoardNum());
                        addReport(r);
                    } else {
                        r = new Report(3290);
                        r.indent(2);
                        r.subject = subjectId;
                        r.add(hits);
                        r.add(c.getBoardNum());
                        addReport(r);
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.getBoard().addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                }

                // Damage the building in one big lump.
                else {

                    // Only report if damage was done to the building.
                    int toBldg = hits * nDamPerHit;
                    if ( toBldg > 0 ) {
                        Report buildingReport = damageBuilding( bldg, toBldg );
                        buildingReport.indent(2);
                        buildingReport.newlines = 1;
                        buildingReport.subject = subjectId;
                        addReport(buildingReport);
                    }

                } // End rounds-hit

            } // End missed-target-in-building
            return !bMissed;

        } // End missed-target

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if ( targetInBuilding && bldg != null ) {
            bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
        }

        // All attacks (except from infantry weapons)
        // during Mek Swarms hit the same location.
        if ( !isWeaponInfantry &&
             ae.getSwarmTargetId() == wr.waa.getTargetId() ) {
            nCluster = hits;
        }

        // Battle Armor MGs do one die of damage per hit to PBI.
        if ( wtype.getAmmoType() == AmmoType.T_BA_MG &&
             (target instanceof Infantry) &&
             !(target instanceof BattleArmor) ) {

            // ASSUMPTION: Building walls protect infantry from BA MGs.
            if ( bldgAbsorbs > 0 ) {
                int toBldg = nDamPerHit * hits;
                r = new Report(3295);
                r.newlines = 0;
                r.subject = subjectId;
                r.add(hits);
                r.add(sSalvoType);
                addReport(r);

                Report buildingReport =
                    damageBuilding( bldg,
                                    Math.min( toBldg, bldgAbsorbs ),
                                    " absorbs the shots, taking "  );
                buildingReport.newlines = 1;
                buildingReport.subject = subjectId;
                addReport(buildingReport);
                return !bMissed;
            }
            nDamPerHit = Compute.d6(hits);
            r = new Report(3300);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(nDamPerHit);
            r.add(sSalvoType);
            addReport(r);
            hits = 1;
        }

        // Mech and Vehicle MGs do *DICE* of damage to PBI.
        else if (usesAmmo && atype.hasFlag(AmmoType.F_MG) &&
                  !isWeaponInfantry && (target instanceof Infantry) &&
                  !(target instanceof BattleArmor) && !weapon.isRapidfire()) {

            int dice = wtype.getDamage();

            // A building may absorb the entire shot.
            if ( nDamPerHit <= bldgAbsorbs ) {
                int toBldg = nDamPerHit * hits;
                int curCF = bldg.getCurrentCF();
                curCF = Math.min( curCF, toBldg );
                bldg.setCurrentCF( curCF );
                if ( bSalvo ) {
                    r = new Report(3305);
                    r.subject = subjectId;
                    r.add(hits);
                    r.add(sSalvoType);
                    addReport(r);
                } else{
                    r = new Report(3310);
                    r.subject = subjectId;
                    addReport(r);
                }
                r = new Report(3315);
                r.indent(2);
                r.subject = subjectId;
                addReport(r);
                Report buildingReport =
                    damageBuilding( bldg,
                                    Math.min( toBldg, bldgAbsorbs ),
                                    " absorbs the shots, taking "  );
                buildingReport.newlines = 1;
                buildingReport.subject = subjectId;
                addReport(buildingReport);
                return !bMissed;
            }

            // If a building absorbs partial damage, reduce the dice of damage.
            else if ( bldgAbsorbs > 0 ) {
                dice -= bldgAbsorbs;
            }

            nDamPerHit = Compute.d6( dice );
            r = new Report(3320);
            r.subject = subjectId;
            r.add(nDamPerHit);
            r.add(sSalvoType);
            addReport(r);
            bSalvo = true;

            // If a building absorbed partial damage, report it now
            // instead of later and then clear the variable.
            if ( bldgAbsorbs > 0 ) {
                Report buildingReport = damageBuilding( bldg, bldgAbsorbs );
                buildingReport.indent(2);
                buildingReport.subject = subjectId;
                addReport(buildingReport);
                bldgAbsorbs = 0;
            }

        }

        // Report the number of hits.  Infernos have their own reporting
        else if (bSalvo && !bInferno) {
            r = new Report(3325);
            r.subject = subjectId;
            r.add(hits);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            addReport(r);
            if (bECMAffected) {
                //ECM prevents bonus
                r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                addReport(r);
            }
            else if (bMekStealthActive) {
                //stealth prevents bonus
                r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                addReport(r);
            }
            if (nSalvoBonus > 0) {
                r = new Report(3340);
                r.subject = subjectId;
                r.add(nSalvoBonus);
                r.newlines = 0;
                addReport(r);
            }
            r = new Report(3345);
            r.subject = subjectId;
            r.newlines = 0;
            addReport(r);

            if (wr.amsShotDownTotal > 0) {
                for (int i=0; i < wr.amsShotDown.length; i++) {
                    int shotDown = Math.min(wr.amsShotDown[i], hits);
                    r = new Report(3350);
                    r.indent();
                    r.subject = subjectId;
                    r.add(wr.amsShotDown[i]);
                    r.add(shotDown);
                    r.newlines = 0;
                    addReport(r);
                }
                hits -= wr.amsShotDownTotal;

                addNewLines();
                if (hits < 1) {
                    //all missiles shot down
                    r = new Report(3355);
                    r.indent();
                    r.subject = subjectId;
                    r.newlines = 0;
                    addReport(r);
                } else {
                    r = new Report(3360);
                    r.indent();
                    r.subject = subjectId;
                    r.add(hits);
                    r.newlines = 0;
                    addReport(r);
                }
            }
        }

        // convert the ATM missile damages to LRM type 5 point cluster damage
        // done here after AMS has been performed
        if (wtype.getAmmoType() == AmmoType.T_ATM) {
            hits = nDamPerHit * hits;
            nDamPerHit = 1;
        }

        // Make sure the player knows when his attack causes no damage.
        if ( hits == 0 ) {
            r = new Report(3365);
            r.subject = subjectId;
            addReport(r);
        }

        // for each cluster of hits, do a chunk of damage
        while (hits > 0) {
            int nDamage;

            // If the attack was with inferno rounds then
            // do heat and fire instead of damage.
            if ( bInferno ) {
                // AMS can shoot down infernos, too.
                if (wr.amsShotDownTotal > 0) {
                    for (int i=0; i < wr.amsShotDown.length; i++) {
                        int shotDown = Math.min(wr.amsShotDown[i], hits);
                        r = new Report(3350);
                        r.indent();
                        r.subject = subjectId;
                        r.add(wr.amsShotDown[i]);
                        r.add(shotDown);
                        r.newlines = 0;
                        addReport(r);
                    }
                    hits -= wr.amsShotDownTotal;

                    addNewLines();
                    if (hits < 1) {
                        //all missiles shot down
                        r = new Report(3355);
                        r.indent();
                        r.subject = subjectId;
                        r.newlines = 0;
                        addReport(r);
                    } else {
                        r = new Report(3360);
                        r.indent();
                        r.subject = subjectId;
                        r.add(hits);
                        r.newlines = 0;
                        addReport(r);
                    }
                    if ( hits <= 0 ) {
                        continue;
                    }
                }

                // targeting a hex for ignition
                if( target.getTargetType() == Targetable.TYPE_HEX_IGNITE ||
                    target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {

                    //inferno missiles hit
                    r = new Report(3370);
                    r.subject = subjectId;
                    r.add(hits);
                    addReport(r);

                    // Unless there a fire in the hex already, start one.
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);
                    if ( !h.containsTerrain( Terrains.FIRE ) ) {
                        r = new Report(3005);
                        r.subject = subjectId;
                        r.add(c.getBoardNum());
                        addReport(r);
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.getBoard().addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                    return !bMissed;
                }

                // Targeting an entity
                if (entityTarget != null ) {
                    r = new Report(3375);
                    r.subject = subjectId;
                    r.add(hits);
                    addReport(r);

                    if(game.getOptions().booleanOption("vehicle_fires")
                            && entityTarget instanceof Tank) {
                        checkForVehicleFire((Tank)entityTarget, true);
                    } else {
                        entityTarget.infernos.add( InfernoTracker.STANDARD_ROUND,
                                hits );
                        r = new Report(3205);
                        r.indent(2);
                        r.subject = subjectId;
                        r.addDesc(entityTarget);
                        r.add(entityTarget.infernos.getTurnsLeftToBurn());
                        addReport(r);
                    }

                    // Start a fire in the targets hex, unless already on fire.
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);

                    // Unless there a fire in the hex already, start one.
                    if ( !h.containsTerrain( Terrains.FIRE ) ) {
                        r = new Report(3005);
                        r.subject = subjectId;
                        r.add(c.getBoardNum());
                        addReport(r);
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.getBoard().addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, 1 );
                    sendChangedHex(c);

                    return !bMissed;
                }

            } // End is-inferno

            // targeting a hex for igniting
            if( target.getTargetType() == Targetable.TYPE_HEX_IGNITE ||
                target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
                if ( !bSalvo ) {
                    //hits!
                    r = new Report(2270);
                    r.subject = subjectId;
                    r.newlines = 0;
                    addReport(r);
                }
                // We handle Inferno rounds above.
                int tn = wtype.getFireTN();
                if(bIncendiary) {
                    tn = 5; // Incendiary AC and LRM
                }
                if (tn != TargetRoll.IMPOSSIBLE) {
                    if ( bldg != null ) {
                        tn += bldg.getType() - 1;
                    }
                    addNewLines();
                    tryIgniteHex( target.getPosition(), ae.getId(), bInferno, tn, true );
                }
                return !bMissed;
            }

            // targeting a hex for clearing
            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {

                nDamage = nDamPerHit * hits;
                if ( !bSalvo ) {
                    //hits!
                    r = new Report(2270);
                    r.subject = subjectId;
                    r.newlines = 0;
                    addReport(r);
                }
                if (ae instanceof Infantry) {
                    //infantry cannot clear hexes
                    r = new Report(3380);
                    r.indent();
                    r.subject = subjectId;
                    addReport(r);
                    return !bMissed;
                }


                //report that damge was "applied" to terrain
                r = new Report(3385);
                r.indent();
                r.subject = subjectId;
                r.add(nDamage);
                addReport(r);

                // Any clear attempt can result in accidental ignition, even
                // weapons that can't normally start fires.  that's weird.
                // Buildings can't be accidentally ignited.
                if ( bldg == null) {
                    boolean alreadyIgnited = game.getBoard().getHex(target.getPosition()).containsTerrain(Terrains.FIRE);
                    boolean ignited = tryIgniteHex(target.getPosition(), subjectId, bInferno, 9);
                    if (!alreadyIgnited && ignited) return !bMissed;
                }

                int tn = 14 - nDamage;
                tryClearHex(target.getPosition(), tn, ae.getId());

                return !bMissed;
            }

            // Targeting a building.
            if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

                // Is the building hit by Inferno rounds?
                if ( bInferno ) {

                    // start a fire in the targets hex
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);

                    // Is there a fire in the hex already?
                    if ( h.containsTerrain( Terrains.FIRE ) ) {
                        r = new Report(3285);
                        r.subject = subjectId;
                        r.add(hits);
                        r.add(c.getBoardNum());
                        addReport(r);
                    } else {
                        r = new Report(3290);
                        r.subject = subjectId;
                        r.add(hits);
                        r.add(c.getBoardNum());
                        addReport(r);
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.getBoard().addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                } else {
                    // The building takes the full brunt of the attack.
                    nDamage = nDamPerHit * hits;
                    if ( !bSalvo ) {
                        //hits!
                        r = new Report(3390);
                        r.subject = subjectId;
                        addReport(r);
                    }
                    addNewLines();
                    Report buildingReport = damageBuilding( bldg, nDamage );
                    buildingReport.indent(2);
                    buildingReport.newlines = 1;
                    buildingReport.subject = subjectId;
                    addReport(buildingReport);
    
                    // Damage any infantry in the hex.
                    this.damageInfantryIn( bldg, nDamage );
                }

                // And we're done!
                return !bMissed;
            }

            // Battle Armor squads equipped with fire protection
            // gear automatically avoid flaming death.
            if ( wtype.hasFlag(WeaponType.F_FLAMER) &&
                 target instanceof BattleArmor ) {

                for ( Enumeration iter = entityTarget.getMisc();
                      iter.hasMoreElements(); ) {
                    Mounted mount = (Mounted) iter.nextElement();
                    EquipmentType equip = mount.getType();
                    if ( BattleArmor.FIRE_PROTECTION.equals
                         (equip.getInternalName()) ) {
                        if ( !bSalvo ) {
                            //hits
                            r = new Report(3390);
                            r.subject = subjectId;
                            addReport(r);
                        }
                        r = new Report(3395);
                        r.indent(2);
                        r.subject = subjectId;
                        r.addDesc(entityTarget);
                        addReport(r);

                        // A building may be damaged, even if the squad is not.
                        if ( bldgAbsorbs > 0 ) {
                            int toBldg = nDamPerHit * Math.min( bldgAbsorbs,
                                                                hits );
                            Report buildingReport = damageBuilding( bldg, toBldg );
                            buildingReport.indent(2);
                            buildingReport.newlines = 1;
                            buildingReport.subject = subjectId;
                            addReport(buildingReport);
                        }

                     return !bMissed;
                    }
                }
            } // End target-may-be-immune

            // Flamers do heat to mechs instead damage if the option is
            // available and the mode is set.
            if (entityTarget != null
                    && (entityTarget instanceof Mech)
                    && wtype.hasFlag(WeaponType.F_FLAMER)
                    && game.getOptions().booleanOption("flamer_heat")
                    && wtype.hasModes()
                    && weapon.curMode().equals("Heat")) {
                nDamage = nDamPerHit * hits;
                if ( !bSalvo ) {
                    //hits
                    r = new Report(3390);
                    r.subject = subjectId;
                    addReport(r);
                }
                r = new Report(3400);
                r.subject = subjectId;
                r.indent(2);
                r.add(nDamage);
                r.newlines = 0;
                addReport(r);
                entityTarget.heatBuildup += nDamage;
                hits = 0;
            } else if (entityTarget != null) {
                 HitData hit = entityTarget.rollHitLocation
                     ( toHit.getHitTable(),
                       toHit.getSideTable(),
                       wr.waa.getAimedLocation(),
                       wr.waa.getAimingMode() );

                if (wtype.hasFlag(WeaponType.F_PLASMA_MFUK)
                        && (entityTarget instanceof Mech)) {
                    nDamage = nDamPerHit * hits;
                    if (!bSalvo) {
                        //hits
                        r = new Report(3390);
                        r.subject = subjectId;
                        addReport(r);
                    }
                    r = new Report(3400);
                    r.subject = subjectId;
                    r.indent(2);
                    r.add(5);
                    r.newlines = 0;
                    addReport(r);
                    entityTarget.heatBuildup += 5;
                }

                // If a leg attacks hit a leg that isn't
                // there, then hit the other leg.
                if ( wtype.getInternalName().equals("LegAttack") &&
                     entityTarget.getInternal(hit) <= 0 ) {
                    if ( hit.getLocation() == Mech.LOC_RLEG ) {
                        hit = new HitData( Mech.LOC_LLEG );
                    }
                    else {
                        hit = new HitData( Mech.LOC_RLEG );
                    }
                }

                // Mine Launchers automatically hit the
                // CT of a Mech or the front of a Tank.
                if ( wtype.getInternalName()
                     .equals(BattleArmor.MINE_LAUNCHER) ) {
                    if ( target instanceof Mech ) {
                        hit = new HitData( Mech.LOC_CT );
                    }
                    else { // te instanceof Tank
                        hit = new HitData( Tank.LOC_FRONT );
                    }
                }

                // Each hit in the salvo get's its own hit location.
                if (!bSalvo) {
                    r = new Report(3405);
                    r.subject = subjectId;
                    r.add(toHit.getTableDesc());
                    r.add(entityTarget.getLocationAbbr(hit));
                    r.newlines = 0;
                    addReport(r);
                    if (hit.hitAimedLocation()) {
                        r = new Report(3410);
                        r.subject = subjectId;
                        r.newlines = 0;
                        addReport(r);
                    }
                }

                // Special weapons do criticals instead of damage.
                if ( nDamPerHit == WeaponType.DAMAGE_SPECIAL ) {
                    // Do criticals.
                    //String specialDamage = criticalEntity( entityTarget, hit.getLocation() );
                    Vector specialDamageReport = criticalEntity( entityTarget, hit.getLocation() );

                    // Replace "no effect" results with 4 points of damage.
                    //if ( specialDamage.endsWith(" no effect.") ) {
                    if (((Report)specialDamageReport.lastElement()).messageId == 6005) {
                        int damage = 4;
                        if (ae instanceof BattleArmor)
                            damage += ((BattleArmor)ae).getVibroClawDamage();
                        // ASSUMPTION: buildings CAN'T absorb *this* damage.
                        //specialDamage = damageEntity(entityTarget, hit, damage);
                        specialDamageReport = damageEntity(entityTarget, hit, damage, false, 0, false, false, throughFront);
                    }
                    else {
                        //add newline _before_ last report
                        try {
                            ((Report) specialDamageReport.elementAt(specialDamageReport.size() - 2)).newlines++;
                        } catch (ArrayIndexOutOfBoundsException aiobe) {
                            System.err.println("ERROR: no previous report when trying to add newline");
                        }
                    }

                    // Report the result
                    addReport( specialDamageReport);
                }
                else if(game.getOptions().booleanOption("maxtech_partial_cover") &&
                  toHit.getHitTable() == ToHitData.HIT_PARTIAL_COVER &&
                  entityTarget.removePartialCoverHits(hit.getLocation(), toHit.getCover(), toHit.getSideTable())) {
                    r = new Report(3460);
                    r.subject = entityTarget.getId();
                    r.indent(2);
                    r.add(entityTarget.getDisplayName());
                    r.add(entityTarget.getLocationAbbr(hit));
                    r.newlines = 0;
                    addReport(r);
                }
                else {
                    // Resolve damage normally.
                    nDamage = nDamPerHit * Math.min(nCluster, hits);

                    // A building may be damaged, even if the squad is not.
                    if ( bldgAbsorbs > 0 ) {
                        int toBldg = Math.min( bldgAbsorbs, nDamage );
                        nDamage -= toBldg;
                        addNewLines();
                        Report buildingReport = damageBuilding( bldg, toBldg );
                        buildingReport.indent(2);
                        buildingReport.subject = subjectId;
                        addReport(buildingReport);
                    }

                    // A building may absorb the entire shot.
                    if ( nDamage == 0 ) {
                        r = new Report(3415);
                        r.subject = subjectId;
                        r.indent(2);
                        r.addDesc(entityTarget);
                        r.newlines = 0;
                        addReport(r);
                    } else if (bFragmentation) {
                        // If it's a frag missile...
                        if (bGlancing) {
                            hit.makeGlancingBlow();
                        }
                        addReport(
                          damageEntity(entityTarget, hit, nDamage, false, 1, false, false, throughFront));
                    } else if (bFlechette) {
                        // If it's a frag missile...
                        if (bGlancing) {
                            hit.makeGlancingBlow();
                        }
                        addReport(
                          damageEntity(entityTarget, hit, nDamage, false, 2, false, false, throughFront));
                    } else if (bAcidHead) {
                        // If it's an acid-head warhead...
                        if (bGlancing) {
                            hit.makeGlancingBlow();
                        }
                        addReport(
                                              damageEntity(entityTarget, hit, nDamage, false, 3, false, false, throughFront) );
                    } else if(bIncendiary && usesAmmo && atype.getAmmoType() == AmmoType.T_AC) {
                        //incendiary AC ammo
                        if (bGlancing) {
                            hit.makeGlancingBlow();
                        }
                        addReport(
                                              damageEntity(entityTarget, hit, nDamage, false, 4, false, false, throughFront));
                    } else {
                        if (usesAmmo
                                && (atype.getAmmoType() == AmmoType.T_AC)
                                && (atype.getMunitionType() == AmmoType.M_ARMOR_PIERCING)
                                && !(entityTarget.getArmorType() == EquipmentType.T_ARMOR_HARDENED))
                            hit.makeArmorPiercing(atype);
                        if (bGlancing) {
                            hit.makeGlancingBlow();
                        }
                        if (bAntiTSM) {
                            entityTarget.hitThisRoundByAntiTSM = true;
                        }
                        addReport(
                            damageEntity(entityTarget, hit, nDamage, false, 0, false, false, throughFront));
                    }
                }
                hits -= nCluster;
                creditKill(entityTarget, ae);
            } else {
                System.err.println("Unable to resolve hit against "+target.getDisplayName());
                if(entityTarget == null) {
                    System.err.println("   entityTarget is null");
                }
                hits = 0; //prevents server lock-up
            }
        } // Handle the next cluster.

        //deal with splash damage from Arrow IV homing
        if(atype != null && atype.getMunitionType() == AmmoType.M_HOMING) {
            artilleryDamageHex(target.getPosition(), wr.artyAttackerCoords, 5, atype, subjectId, ae, entityTarget, false, 0);
        }

        addNewLines();

        if (swarmMissilesNowLeft > 0 && entityTarget != null) {
            Entity swarmTarget = Compute.getSwarmTarget(game, ae.getId(), entityTarget, wr.waa.getWeaponId());
            if (swarmTarget != null) {
                //missiles keep swarming
                r = new Report(3420);
                r.subject = swarmTarget.getId();
                r.indent();
                r.add(swarmMissilesNowLeft);
                addReport(r);
                weapon.setUsedThisRound(false);
                WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                    swarmTarget.getTargetId(), wr.waa.getWeaponId());
                newWaa.setSwarmingMissiles(true);
                newWaa.setOldTargetId(target.getTargetId());
                newWaa.setAmmoId(wr.waa.getAmmoId());
                WeaponResult newWr = preTreatWeaponAttack(newWaa);
                resolveWeaponAttack(newWr, ae.getId(), false, swarmMissilesNowLeft);
            } else {
                //missiles can't find another target
                r = new Report(3425);
                r.subject = ae.getId();
                r.indent();
                addReport(r);
            }
        }

        return !bMissed;
    }

    /**
     * Handle all physical attacks for the round
     */
    private void resolvePhysicalAttacks() {
        //Physical phase header
        addReport(new Report(4000, Report.PUBLIC));

        // add any pending charges
        for (Enumeration i = game.getCharges(); i.hasMoreElements();) {
            game.addAction((EntityAction)i.nextElement());
        }
        game.resetCharges();

        // remove any duplicate attack declarations
        cleanupPhysicalAttacks();

        // loop thru received attack actions
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();

            // verify that the attacker is still active
            AttackAction aa = (AttackAction)o;
            if (!(game.getEntity(aa.getEntityId()).isActive())
            && !(o instanceof DfaAttackAction)) {
                continue;
            }
            AbstractAttackAction aaa = (AbstractAttackAction)o;
            // do searchlights immediately
            if (aaa instanceof SearchlightAttackAction) {
                SearchlightAttackAction saa = (SearchlightAttackAction)aaa;
                addReport(
                                      saa.resolveAction(game));
            } else {
                physicalResults.addElement(preTreatPhysicalAttack(aaa));
            }
        }
        int cen = Entity.NONE;
        for (Enumeration i = physicalResults.elements(); i.hasMoreElements();) {
            PhysicalResult pr = (PhysicalResult)i.nextElement();
            resolvePhysicalAttack(pr, cen);
            cen = pr.aaa.getEntityId();
        }
        physicalResults.removeAllElements();
    }

    /**
     * Cleans up the attack declarations for the physical phase by removing
     * all attacks past the first for any one mech.  Also clears out attacks
     * by dead or disabled mechs.
     */
    private void cleanupPhysicalAttacks() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            removeDuplicateAttacks(entity.getId());
        }
        removeDeadAttacks();
    }

    /**
     * Removes any actions in the attack queue beyond the first by the
     * specified entity.
     */
    private void removeDuplicateAttacks(int entityId) {
        boolean attacked = false;
        Vector toKeep = new Vector(/*game.actionsSize()*/);

        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction action = (EntityAction)i.nextElement();
            if (action.getEntityId() != entityId) {
                toKeep.addElement(action);
            } else if (!attacked) {
                toKeep.addElement(action);
                if(!(action instanceof SearchlightAttackAction)) {
                    attacked = true;
                }
            } else {
                System.err.println("server: removing duplicate phys attack for id#" + entityId);
                System.err.println("        action was "+ action.toString());
            }
        }

        // reset actions and re-add valid elements
        game.resetActions();
        for (Enumeration i = toKeep.elements(); i.hasMoreElements();) {
            game.addAction((EntityAction)i.nextElement());
        }
    }

    /**
     * Removes all attacks by any dead entities.  It does this by going through
     * all the attacks and only keeping ones from active entities.  DFAs are
     * kept even if the pilot is unconscious, so that he can fail.
     */
    private void removeDeadAttacks() {
        Vector toKeep = new Vector(game.actionsSize());

        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction action = (EntityAction)i.nextElement();
            Entity entity = game.getEntity(action.getEntityId());
            if (entity != null && !entity.isDestroyed()
            && (entity.isActive() || action instanceof DfaAttackAction)) {
                toKeep.addElement(action);
            }
        }

        // reset actions and re-add valid elements
        game.resetActions();
        for (Enumeration i = toKeep.elements(); i.hasMoreElements();) {
            game.addAction((EntityAction)i.nextElement());
        }
    }

    /**
     * Handle a punch attack
     */
    private void resolvePunchAttack(PhysicalResult pr, int lastEntityId) {
        final PunchAttackAction paa = (PunchAttackAction)pr.aaa;
        final Entity ae = game.getEntity(paa.getEntityId());
        final Targetable target = game.getTarget(paa.getTargetType(), paa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        boolean throughFront = true;
        if (te!=null) {
            throughFront = Compute.isThroughFrontHex(game, paa.getEntityId(), te);
        }
        final String armName = paa.getArm() == PunchAttackAction.LEFT
        ? "Left Arm" : "Right Arm";
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = paa.getArm() == PunchAttackAction.LEFT
        ? pr.damage : pr.damageRight;
        final ToHitData toHit = paa.getArm() == PunchAttackAction.LEFT
        ? pr.toHit : pr.toHitRight;
        int roll = paa.getArm() == PunchAttackAction.LEFT
        ? pr.roll : pr.rollRight;
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));
        Report r;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( target.getPosition() );

        if (lastEntityId != paa.getEntityId()) {
            //report who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4010);
        r.subject = ae.getId();
        r.indent();
        r.add(armName);
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4015);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(4020);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);
            if (glancing) {
                r = new Report(4030);
                r.subject = ae.getId();
                r.newlines = 0;
                addReport(r);
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            //nope
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    Report buildingReport = damageBuilding( bldg, damage );
                    buildingReport.indent();
                    buildingReport.subject = ae.getId();
                    addReport(buildingReport);
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            addReport(r);
            Report buildingReport = damageBuilding( bldg, damage );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        r = new Report(4045);
        r.subject = ae.getId();
        r.add(toHit.getTableDesc());
        r.add(te.getLocationAbbr(hit));
        r.newlines = 0;
        addReport(r);

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            addNewLines();
            Report buildingReport = damageBuilding( bldg, toBldg );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            r = new Report(4050);
            r.subject = ae.getId();
            r.add(te.getShortName());
            r.add(te.getOwner().getName());
            r.newlines = 0;
            addReport(r);
        } else {
            if (glancing) {
                damage = (int)Math.floor(damage/2.0);
            }
            addReport(damageEntity(te, hit, damage, false, 0, false, false, throughFront));
        }

        addNewLines();
    }

    /**
     * Handle a kick attack
     */
    private void resolveKickAttack(PhysicalResult pr, int lastEntityId) {
        KickAttackAction kaa = (KickAttackAction)pr.aaa;
        final Entity ae = game.getEntity(kaa.getEntityId());
        final Targetable target = game.getTarget(kaa.getTargetType(), kaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        boolean throughFront = true;
        if (te!=null) {
            throughFront = Compute.isThroughFrontHex(game, kaa.getEntityId(), te);
        }
        String legName = ( (kaa.getLeg() == KickAttackAction.LEFT) ||
                                 (kaa.getLeg() == KickAttackAction.LEFTMULE) )
        ? "Left "
        : "Right ";
        if (game.getOptions().booleanOption("maxtech_mulekicks")) {
            if ( (kaa.getLeg() == KickAttackAction.LEFTMULE) || 
                 (kaa.getLeg() == KickAttackAction.RIGHTMULE) ) {
                legName.concat(" rear ");
            } else {
                legName.concat(" front ");
            }
        }
        legName.concat("leg");
        Report r;

        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( target.getPosition() );

        if (lastEntityId != ae.getId()) {
            //who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4055);
        r.subject = ae.getId();
        r.indent();
        r.add(legName);
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4060);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(4065);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);
            if (glancing) {
                r = new Report(4030);
                r.subject = ae.getId();
                r.newlines = 0;
                addReport(r);
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);
            game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    Report buildingReport = damageBuilding( bldg, damage );
                    buildingReport.indent();
                    buildingReport.subject = ae.getId();
                    addReport(buildingReport);
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            addReport(r);
            Report buildingReport = damageBuilding( bldg, damage );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        r = new Report(4045);
        r.subject = ae.getId();
        r.add(toHit.getTableDesc());
        r.add(te.getLocationAbbr(hit));
        r.newlines = 0;
        addReport(r);

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            addNewLines();
            Report buildingReport = damageBuilding( bldg, toBldg );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            r = new Report(4050);
            r.subject = ae.getId();
            r.add(te.getShortName());
            r.add(te.getOwner().getName());
            r.newlines = 0;
            addReport(r);
        } else {
            if (glancing) {
                damage = (int)Math.floor(damage/2.0);
            }
            addReport(damageEntity(te, hit, damage, false, 0, false, false, throughFront));
        }

        if (te.getMovementMode() == IEntityMovementMode.BIPED || te.getMovementMode() == IEntityMovementMode.QUAD) {
            PilotingRollData kickPRD = new PilotingRollData(te.getId(), getKickPushPSRMod(ae, te, 0), "was kicked");
            kickPRD.setCumulative(false); // see Bug# 811987 for more info
            game.addPSR(kickPRD);
        }

        addNewLines();
    }

    /**
     * Handle a Protomech physicalattack
     */

    private void resolveProtoAttack(PhysicalResult pr, int lastEntityId) {
        final ProtomechPhysicalAttackAction ppaa = (ProtomechPhysicalAttackAction)pr.aaa;
        final Entity ae = game.getEntity(ppaa.getEntityId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final Targetable target = game.getTarget(ppaa.getTargetType(), ppaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        boolean throughFront = true;
        if (te!=null) {
            throughFront = Compute.isThroughFrontHex(game, ppaa.getEntityId(), te);
        }
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));
        Report r;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( target.getPosition() );

        if (lastEntityId != ae.getId()) {
            //who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4070);
        r.subject = ae.getId();
        r.indent();
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4075);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(4080);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);
            if (glancing) {
                r = new Report(4030);
                r.subject = ae.getId();
                r.newlines = 0;
                addReport(r);
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    Report buildingReport = damageBuilding( bldg, damage );
                    buildingReport.indent();
                    buildingReport.subject = ae.getId();
                    addReport(buildingReport);
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            addReport(r);
            Report buildingReport = damageBuilding( bldg, damage );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        r = new Report(4045);
        r.subject = ae.getId();
        r.add(toHit.getTableDesc());
        r.add(te.getLocationAbbr(hit));
        r.newlines = 0;
        addReport(r);

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            addNewLines();
            Report buildingReport = damageBuilding( bldg, toBldg );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            r = new Report(4050);
            r.subject = ae.getId();
            r.add(te.getShortName());
            r.add(te.getOwner().getName());
            r.newlines = 0;
            addReport(r);
        } else {
            if (glancing) {
                damage = (int)Math.floor(damage/2.0);
            }
            addReport(damageEntity(te, hit, damage));
        }

        addNewLines();
    }


    /**
     * Handle a brush off attack
     */
    private void resolveBrushOffAttack( PhysicalResult pr,
                                        int lastEntityId ) {
        final BrushOffAttackAction baa = (BrushOffAttackAction)pr.aaa;
        final Entity ae = game.getEntity(baa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target
        // of a "brush off", but iNarc pods **are**.
        Targetable target = game.getTarget( baa.getTargetType(),
                                            baa.getTargetId() );
        Entity te = null;
        final String armName = baa.getArm() == BrushOffAttackAction.LEFT
            ? "Left Arm" : "Right Arm";
        Report r;

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = game.getEntity(baa.getTargetId());
        }

        // get damage, ToHitData and roll from the PhysicalResult
        // ASSUMPTION: buildings can't absorb *this* damage.
        int damage = baa.getArm() == BrushOffAttackAction.LEFT
        ? pr.damage : pr.damageRight;
        final ToHitData toHit = baa.getArm() == BrushOffAttackAction.LEFT
        ? pr.toHit : pr.toHitRight;
        int roll = baa.getArm() == BrushOffAttackAction.LEFT
        ? pr.roll : pr.rollRight;

        if (lastEntityId != baa.getEntityId()) {
            //who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4085);
        r.subject = ae.getId();
        r.indent();
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4090);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            return;
        }

        // report the roll
        r = new Report(4025);
        r.subject = ae.getId();
        r.add(toHit.getValue());
        r.add(roll);
        r.newlines = 0;
        addReport(r);

        // do we hit?
        if (roll < toHit.getValue()) {
            //miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);

            // Missed Brush Off attacks cause punch damage to the attacker.
            toHit.setHitTable( ToHitData.HIT_PUNCH );
            toHit.setSideTable( ToHitData.SIDE_FRONT );
            HitData hit = ae.rollHitLocation( toHit.getHitTable(),
                                              toHit.getSideTable() );
            r = new Report(4095);
            r.subject = ae.getId();
            r.addDesc(ae);
            r.add(ae.getLocationAbbr(hit));
            r.newlines = 0;
            addReport(r);
            addReport(
                                  damageEntity(ae, hit, damage));
            addNewLines();
            return;
        }

        // Different target types get different handling.
        switch ( target.getTargetType() ) {
        case Targetable.TYPE_ENTITY:
            // Handle Entity targets.
            HitData hit = te.rollHitLocation( toHit.getHitTable(),
                                              toHit.getSideTable() );
            r = new Report(4045);
            r.subject = ae.getId();
            r.add(toHit.getTableDesc());
            r.add(te.getLocationAbbr(hit));
            r.newlines = 0;
            addReport(r);
            addReport(
                                  damageEntity(te, hit, damage));
            addNewLines();

            // Dislodge the swarming infantry.
            ae.setSwarmAttackerId( Entity.NONE );
            te.setSwarmTargetId( Entity.NONE );
            r = new Report(4100);
            r.subject = ae.getId();
            r.add(te.getDisplayName());
            addReport(r);
            break;
        case Targetable.TYPE_INARC_POD:
            // Handle iNarc pod targets.
            // TODO : check the return code and handle false appropriately.
            ae.removeINarcPod( (INarcPod) target );
//             // TODO : confirm that we don't need to update the attacker. //killme
//             entityUpdate( ae.getId() ); // killme
            r = new Report(4105);
            r.subject = ae.getId();
            r.add(target.getDisplayName());
            addReport(r);
            break;
            // TODO : add a default: case and handle it appropriately.
        }
    }

    /**
     * Handle a thrash attack
     */
    private void resolveThrashAttack( PhysicalResult pr,
                                        int lastEntityId ) {
        final ThrashAttackAction taa = (ThrashAttackAction)pr.aaa;
        final Entity ae = game.getEntity(taa.getEntityId());

        // get damage, ToHitData and roll from the PhysicalResult
        int hits = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // PLEASE NOTE: buildings are *never* the target of a "thrash".
        final Entity te = game.getEntity(taa.getTargetId());
        Report r;

        if (lastEntityId != taa.getEntityId()) {
            //who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4110);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4115);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            return;
        }

        // Thrash attack may hit automatically
        if ( toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS ) {
            r = new Report(4120);
            r.subject = ae.getId();
            r.newlines = 0;
            addReport(r);
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);

            // do we hit?
            if (roll < toHit.getValue()) {
                //miss
                r = new Report(4035);
                r.subject = ae.getId();
                addReport(r);
                return;
            }
            r = new Report(4125);
            r.subject = ae.getId();
            r.newlines = 0;
            addReport(r);
        }

        // Standard damage loop in 5 point clusters.
        if (glancing) {
            hits = (int)Math.floor(hits/2.0);
        }

        r = new Report(4130);
        r.subject = ae.getId();
        r.add(hits);
        r.newlines = 0;
        addReport(r);
        if (glancing) {
            r = new Report(4030);
            r.subject = ae.getId();
            r.newlines = 0;
            addReport(r);
        }
        while ( hits > 0 ) {
            int damage = Math.min(5, hits);
            hits -= damage;
            HitData hit = te.rollHitLocation( toHit.getHitTable(),
                                              toHit.getSideTable() );
            r = new Report(4135);
            r.subject = ae.getId();
            r.add(te.getLocationAbbr(hit));
            r.newlines = 0;
            addReport(r);
            addReport(
                                  damageEntity(te, hit, damage));
        }

        addNewLines();

        // Thrash attacks cause PSRs.  Failed PSRs cause falling damage.
        // This fall damage applies even though the Thrashing Mek is prone.
        PilotingRollData rollData = ae.getBasePilotingRoll();
        ae.addPilotingModifierForTerrain(rollData);
        rollData.addModifier( 0, "thrashing at infantry" );
        r = new Report(4140);
        r.subject = ae.getId();
        r.addDesc(ae);
        addReport(r);
        final int diceRoll = Compute.d6(2);
        r = new Report(2190);
        r.subject = ae.getId();
        r.addDesc(ae);
        r.add(rollData.getValueAsString());
        r.add(rollData.getDesc());
        r.add(diceRoll);
        if (diceRoll < rollData.getValue()) {
            r.choose(false);
            addReport(r);
            doEntityFall( ae, rollData );
        } else {
            r.choose(true);
            addReport(r);
        }

    }

    /**
     * Handle a club attack
     */
    private void resolveClubAttack(PhysicalResult pr, int lastEntityId) {
        final ClubAttackAction caa = (ClubAttackAction)pr.aaa;
        final Entity ae = game.getEntity(caa.getEntityId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final Targetable target = game.getTarget(caa.getTargetType(), caa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        boolean throughFront = true;
        if (te!=null) {
            throughFront = Compute.isThroughFrontHex(game, caa.getEntityId(), te);
        }
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));
        Report r;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( target.getPosition() );

        // restore club attack
        caa.getClub().restore();

        if (lastEntityId != caa.getEntityId()) {
            //who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4145);
        r.subject = ae.getId();
        r.indent();
        r.add(caa.getClub().getName());
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4075);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            if (((MiscType)(caa.getClub().getType())).hasSubType(MiscType.S_MACE_THB)) {
                game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a mace attack"));
            }
            if (((MiscType)(caa.getClub().getType())).hasSubType(MiscType.S_MACE)) {
                game.addPSR(new PilotingRollData(ae.getId(), 2, "missed a mace attack"));
            }
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(4080);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);
            if (glancing) {
                r = new Report(4030);
                r.subject = ae.getId();
                r.newlines = 0;
                addReport(r);
            }
       }

        // do we hit?
        if (roll < toHit.getValue()) {
            //miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);
            if (((MiscType)(caa.getClub().getType())).hasSubType(MiscType.S_MACE_THB)) {
                game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a mace attack"));
            }
            if (((MiscType)(caa.getClub().getType())).hasSubType(MiscType.S_MACE)) {
                game.addPSR(new PilotingRollData(ae.getId(), 2, "missed a mace attack"));
            }

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    Report buildingReport = damageBuilding( bldg, damage );
                    buildingReport.indent();
                    buildingReport.subject = ae.getId();
                    addReport(buildingReport);
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            addReport(r);
            Report buildingReport = damageBuilding( bldg, damage );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        r = new Report(4045);
        r.subject = ae.getId();
        r.add(toHit.getTableDesc());
        r.add(te.getLocationAbbr(hit));
        r.newlines = 0;
        addReport(r);

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            addNewLines();
            Report buildingReport = damageBuilding( bldg, toBldg );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            r = new Report(4050);
            r.subject = ae.getId();
            r.add(te.getShortName());
            r.add(te.getOwner().getName());
            r.newlines = 0;
            addReport(r);
        } else {
            if (glancing) {
                damage = (int)Math.floor(damage/2.0);
            }
            addReport(damageEntity(te, hit, damage, false, 0, false, false, throughFront));
        }

        addNewLines();

        if (((MiscType)caa.getClub().getType()).hasSubType(MiscType.S_TREE_CLUB)) {
            //the club breaks
            r = new Report(4150);
            r.subject = ae.getId();
            r.add(caa.getClub().getName());
            addReport(r);
            ae.removeMisc(caa.getClub().getName());
        }
    }

    /**
     * Handle a push attack
     */
    private void resolvePushAttack(PhysicalResult pr, int lastEntityId) {
        final PushAttackAction paa = (PushAttackAction)pr.aaa;
        final Entity ae = game.getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "push".
        final Entity te = game.getEntity(paa.getTargetId());
        // get roll and ToHitData from the PhysicalResult
        int roll = pr.roll;
        final ToHitData toHit = pr.toHit;
        Report r;

        // was this push resolved earlier?
        if (pr.pushBackResolved) {
            return;
        }

        if (lastEntityId != paa.getEntityId()) {
            //who is making the attack
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        r = new Report(4155);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        addReport(r);

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(4160);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            return;
        }

        // report the roll
        r = new Report(4025);
        r.subject = ae.getId();
        r.add(toHit.getValue());
        r.add(roll);
        r.newlines = 0;
        addReport(r);

        // check if our target has a push against us, too, and get it
        PhysicalResult targetPushResult = null;
        for (Enumeration i = physicalResults.elements(); i.hasMoreElements();) {
            PhysicalResult tpr = (PhysicalResult)i.nextElement();
            if (tpr.aaa.getEntityId() == te.getId() &&
                tpr.aaa instanceof PushAttackAction &&
                tpr.aaa.getTargetId() == ae.getId() ) {
                targetPushResult = tpr;
            }
        }
        // if our target has a push against us, we need to resolve both now
        if (targetPushResult != null) {
            // do both hit?
            if (targetPushResult.roll >= targetPushResult.toHit.getValue() &&
                roll >= toHit.getValue()) {
                r = new Report(4165);
                r.subject = ae.getId();
                r.addDesc(te);
                r.addDesc(te);
                r.addDesc(ae);
                r.add(toHit.getValue());
                r.add(roll);
                r.addDesc(ae);
                addReport(r);
                PilotingRollData targetPushPRD = new PilotingRollData(te.getId(), getKickPushPSRMod(ae, te, 0), "was pushed");
                targetPushPRD.setCumulative(false); // see Bug# 811987 for more info
                PilotingRollData pushPRD = new PilotingRollData(ae.getId(), getKickPushPSRMod(ae, te, 0), "was pushed");
                pushPRD.setCumulative(false); // see Bug# 811987 for more info
                game.addPSR(pushPRD);
                game.addPSR(targetPushPRD);
                targetPushResult.pushBackResolved = true;
                return;
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            //miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);
            return;
        }

        // we hit...
        int direction = ae.getFacing();

        Coords src = te.getPosition();
        Coords dest = src.translated(direction);

        PilotingRollData pushPRD = new PilotingRollData(te.getId(), getKickPushPSRMod(ae, te, 0), "was pushed");
        pushPRD.setCumulative(false); // see Bug# 811987 for more info

        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            r = new Report(4170);
            r.subject = ae.getId();
            r.newlines = 0;
            addReport(r);
            if (game.getBoard().contains(dest)) {
                r = new Report(4175);
                r.subject = ae.getId();
                r.add(dest.getBoardNum(), true);
                addReport(r);
            } else {
                //uh-oh, pushed off board
                r = new Report(4180);
                r.subject = ae.getId();
                addReport(r);
            }

            doEntityDisplacement(te, src, dest, pushPRD);

            // if push actually moved the target, attacker follows thru
            if (!te.getPosition().equals(src)) {
                ae.setPosition(src);
            }
        } else {
            //targe imovable
            r = new Report(4185);
            r.subject = ae.getId();
            addReport(r);
            game.addPSR(pushPRD);
        }

        addNewLines();
    }

    /**
     * Handle a charge attack
     */
    private void resolveChargeAttack(PhysicalResult pr, int lastEntityId) {
        final ChargeAttackAction caa = (ChargeAttackAction)pr.aaa;
        final Entity ae = game.getEntity(caa.getEntityId());
        final Targetable target = game.getTarget(caa.getTargetType(), caa.getTargetId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        Entity te = null;
        if (target != null && target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        boolean throughFront = true;
        if (te!=null) {
            throughFront = Compute.isThroughFrontHex(game, caa.getEntityId(), te);
        }
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));
        Report r;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( caa.getTargetPos() );

        // is the attacker dead?  because that sure messes up the calculations
        if (ae == null) {
            return;
        }

        final int direction = ae.getFacing();

        // entity isn't charging any more
        ae.setDisplacementAttack(null);

        if (lastEntityId != caa.getEntityId()) {
            //who is making the attack
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        // should we even bother?
        if (target == null || (target.getTargetType() == Targetable.TYPE_ENTITY
             && (te.isDestroyed() || te.isDoomed() || te.crew.isDead()))) {
            r = new Report(4190);
            r.subject = ae.getId();
            r.indent();
            addReport(r);
            // doEntityDisplacement(ae, ae.getPosition(), caa.getTargetPos(), null);
            // Randall said that if a charge fails because of target destruction,
            // the attacker stays in the hex he was in at the end of the movement phase
            // See Bug 912094
            return;
        }

        // attacker fell down?
        if (ae.isProne()) {
            r = new Report(4195);
            r.subject = ae.getId();
            r.indent();
            addReport(r);
            return;
        }

        // attacker immobile?
        if (ae.isImmobile()) {
            r = new Report(4200);
            r.subject = ae.getId();
            r.indent();
            addReport(r);
            return;
        }

        if (te.isProne()) {
            r = new Report(4205);
            r.subject = ae.getId();
            r.indent();
            addReport(r);
            return;
        }

        r = new Report(4210);
        r.subject = ae.getId();
        r.indent();
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        // target still in the same position?
        if (!target.getPosition().equals(caa.getTargetPos())) {
            r = new Report(4215);
            r.subject = ae.getId();
            addReport(r);
            doEntityDisplacement(ae, ae.getPosition(), caa.getTargetPos(), null);
            return;
        }

        // if the attacker's prone, fudge the roll
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            roll = -12;
            r = new Report(4220);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            roll = Integer.MAX_VALUE;
            r = new Report(4225);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);
            if (glancing) {
                r = new Report(4030);
                r.subject = ae.getId();
                r.newlines = 0;
                addReport(r);
            }
       }

        // do we hit?
        if (roll < toHit.getValue()) {
            Coords src = ae.getPosition();
            Coords dest = Compute.getMissedChargeDisplacement(game, ae.getId(), src, direction);

            // TODO: handle movement into/out of/through a building.  Do it here?

            //miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);
            // move attacker to side hex
            doEntityDisplacement(ae, src, dest, null);
        }

        // Targeting a building.
        else if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            addReport(r);
            Report buildingReport = damageBuilding( bldg, damage );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // Apply damage to the attacker.
            int toAttacker = ChargeAttackAction.getDamageTakenBy( ae, bldg );
            HitData hit = ae.rollHitLocation( ToHitData.HIT_NORMAL,
                                              ae.sideTable(target.getPosition())
                                                  );
            addReport(
                                  damageEntity( ae, hit, toAttacker, false, 0, false, false, throughFront));
            addNewLines();
            entityUpdate( ae.getId() );

            // TODO: Does the attacker enter the building?
            // TODO: What if the building collapses?
        }
        else {
            // Resolve the damage.
            resolveChargeDamage( ae, te, toHit, direction, glancing, throughFront );
        }
        return;
    }

    /**
     * Handle a charge's damage
     */
    private void resolveChargeDamage(Entity ae, Entity te, ToHitData toHit, int direction) {
        resolveChargeDamage (ae, te, toHit, direction, false, true);
    }

    private void resolveChargeDamage(Entity ae, Entity te, ToHitData toHit, int direction, boolean glancing, boolean throughFront) {

        // we hit...
        int damage = ChargeAttackAction.getDamageFor(ae);
        int damageTaken = ChargeAttackAction.getDamageTakenBy(ae, te, game.getOptions().booleanOption("maxtech_charge_damage"));
        PilotingRollData chargePSR = null;
        if (glancing) {
            // Glancing Blow rule doesn't state whether damage to attacker on charge
            // or DFA is halved as well, assume yes. TODO: Check with PM
            damage = (int)Math.floor(damage/2.0);
            damageTaken = (int)Math.floor(damageTaken/2.0);
        }
        // Is the target inside a building?
        final boolean targetInBuilding = Compute.isInBuilding( game, te );

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( te.getPosition() );

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if ( targetInBuilding && bldg != null ) {
            bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
        }

        // If we're upright, we may fall down.
        if ( !ae.isProne() ) {
            chargePSR = new PilotingRollData(ae.getId(), 2, "charging");
        }

        Report r;


        r = new Report(4230);
        r.subject = ae.getId();
        r.add(damage);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        addReport(r);
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            damage -= cluster;
            if ( bldgAbsorbs > 0 ) {
                int toBldg = Math.min( bldgAbsorbs, cluster );
                cluster -= toBldg;
                addNewLines();
                Report buildingReport = damageBuilding( bldg, toBldg );
                buildingReport.indent();
                buildingReport.subject = ae.getId();
                addReport(buildingReport);
            }

            // A building may absorb the entire shot.
            if ( cluster == 0 ) {
                r = new Report(4235);
                r.subject = ae.getId();
                r.addDesc(te);
                r.newlines = 0;
                addReport(r);
            } else {
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                addReport(
                                      damageEntity(te, hit, cluster, false, 0, false, false, throughFront));
            }
        }

        //damage to attacker
        r = new Report(4240);
        r.subject = ae.getId();
        r.add(damageTaken);
        r.newlines = 0;
        addReport(r);
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = ae.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            addReport(
                                      damageEntity(ae, hit, cluster));
            damageTaken -= cluster;
        }
        // move attacker and target, if possible
        Coords src = te.getPosition();
        Coords dest = src.translated(direction);

        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            addNewLines();
            doEntityDisplacement(te, src, dest, new PilotingRollData(te.getId(), 2, "was charged"));
            doEntityDisplacement(ae, ae.getPosition(), src, chargePSR);
        }

        addNewLines();

    } // End private void resolveChargeDamage( Entity, Entity, ToHitData )

    private void resolveLayExplosivesAttack(PhysicalResult pr, int lastEntityId) {
        final LayExplosivesAttackAction laa = (LayExplosivesAttackAction)pr.aaa;
        final Entity ae = game.getEntity(laa.getEntityId());
        final Targetable target = game.getTarget(laa.getTargetType(), laa.getTargetId());
        if(ae instanceof Infantry) {
            Infantry inf = (Infantry) ae;
            if(inf.turnsLayingExplosives < 0) {
                inf.turnsLayingExplosives = 0;
                Report r = new Report(4270);
                r.subject = inf.getId();
                r.addDesc(inf);
                addReport(r);
            } else {
                Building building = game.getBoard().getBuildingAt(ae.getPosition());
                if(building != null) {
                    building.addDemolitionCharge(ae.getOwner().getId(), pr.damage);
                    Report r = new Report(4275);
                    r.subject = inf.getId();
                    r.addDesc(inf);
                    r.add(pr.damage);
                    addReport(r);
                }
                inf.turnsLayingExplosives = -1;
            }
        }
    }

    /**
     * Handle a death from above attack
     */
    private void resolveDfaAttack(PhysicalResult pr, int lastEntityId) {
        final DfaAttackAction daa = (DfaAttackAction)pr.aaa;
        final Entity ae = game.getEntity(daa.getEntityId());
        final Targetable target = game.getTarget(daa.getTargetType(), daa.getTargetId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        Entity te = null;
        if (target != null && target.getTargetType() == Targetable.TYPE_ENTITY) {
            // Lets re-write around that horrible hack that was here before.
            // So instead of asking if a specific location is wet and praying
            // that it won't cause an NPE...
            // We'll check 1) if the hex has water, and 2) if it's deep enough
            // to cover the unit in question at its current elevation.
            // It's especially important to make sure it's done this way,
            // because some units (Sylph, submarines) can be at ANY elevation
            // underwater, and VTOLs can be well above the surface.
            te = (Entity)target;
            IHex hex = game.getBoard().getHex(te.getPosition());
            if (hex.containsTerrain(Terrains.WATER)) {
                if (te.absHeight() < hex.getElevation())
                    damage = (int)Math.ceil(damage * 0.5f);
            }
        }
        boolean throughFront = true;
        if (te!=null) {
            throughFront = Compute.isThroughFrontHex(game, daa.getEntityId(), te);
        }
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));
        Report r;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt( daa.getTargetPos() );

        // is the attacker dead?  because that sure messes up the calculations
        if (ae == null) {
            return;
        }

        final int direction = ae.getFacing();

        if (lastEntityId != daa.getEntityId()) {
            //who is making the attack
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            addReport(r);
        }

        // entity isn't DFAing any more
        ae.setDisplacementAttack(null);

        // should we even bother?
        if (target == null || (target.getTargetType() == Targetable.TYPE_ENTITY
             && (te.isDestroyed() || te.isDoomed() || te.crew.isDead()))) {
            r = new Report(4245);
            r.subject = ae.getId();
            r.indent();
            addReport(r);
            if (ae.isProne()) {
                // attacker prone during weapons phase
                doEntityFall(ae, daa.getTargetPos(), 2, 3, ae.getBasePilotingRoll());

            } else {
                // same effect as successful DFA
                doEntityDisplacement(ae, ae.getPosition(), daa.getTargetPos(), new PilotingRollData(ae.getId(), 4, "executed death from above"));
            }
            return;
        }

        r = new Report(4246);
        r.subject = ae.getId();
        r.indent();
        r.add(target.getDisplayName());
        r.newlines = 0;
        addReport(r);

        // target still in the same position?
        if ( !target.getPosition().equals(daa.getTargetPos()) ) {
            r = new Report(4215);
            r.subject = ae.getId();
            addReport(r);
            doEntityFallsInto(ae, ae.getPosition(), daa.getTargetPos(), ae.getBasePilotingRoll());
            return;
        }

        // hack: if the attacker's prone, or incapacitated, fudge the roll
        if (ae.isProne() || !ae.isActive()) {
            roll = -12;
            r = new Report(4250);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
        } else if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            roll = -12;
            r = new Report(4255);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(4260);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit.getValue());
            r.add(roll);
            r.newlines = 0;
            addReport(r);
            if (glancing) {
                r = new Report(4030);
                r.subject = ae.getId();
                r.newlines = 0;
                addReport(r);
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            Coords dest = te.getPosition();
            Coords targetDest = Compute.getPreferredDisplacement(game, te.getId(), dest, direction);
            //miss
            r = new Report(4035);
            r.subject = ae.getId();
            addReport(r);
            if (targetDest != null) {
                // attacker falls into destination hex
                r = new Report(4265);
                r.subject = ae.getId();
                r.addDesc(ae);
                r.add(dest.getBoardNum(), true);
                addReport(r);
                doEntityFall(ae, dest, 2, 3, ae.getBasePilotingRoll());

                // move target to preferred hex
                doEntityDisplacement(te, dest, targetDest, null);
            } else {
                // attacker destroyed  Tanks
                // suffer an ammo/power plant hit.
                // TODO : a Mech suffers a Head Blown Off crit.
                addReport(
                                      destroyEntity(ae, "impossible displacement", (ae instanceof Mech), (ae instanceof Mech)));
            }
            return;
        }

        // we hit...
        // Can't DFA a target inside of a building.
        int damageTaken = DfaAttackAction.getDamageTakenBy(ae);

        r = new Report(4040);
        r.subject = ae.getId();
        addReport(r);

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            Report buildingReport = damageBuilding( bldg, damage );
            buildingReport.indent();
            buildingReport.subject = ae.getId();
            addReport(buildingReport);

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

        }

        // Target isn't building.
        else {
            if (glancing) {
                damage = (int)Math.floor(damage/2.0);
            }
            //damage target
            r = new Report(4230);
            r.subject = ae.getId();
            r.add(damage);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            addReport(r);
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                addReport(
                                      damageEntity(te, hit, cluster, false, 0, false, false, throughFront));
                damage -= cluster;
            }
        }

        if (glancing) {
            // Glancing Blow rule doesn't state whether damage to attacker on charge
            // or DFA is halved as well, assume yes. TODO: Check with PM
            damageTaken = (int)Math.floor(damageTaken/2.0);
        }

        //damage attacker
        r = new Report(4240);
        r.subject = ae.getId();
        r.add(damageTaken);
        r.newlines = 0;
        addReport(r);
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = ae.rollHitLocation(ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
            addReport(
                                      damageEntity(ae, hit, cluster));
            damageTaken -= cluster;
        }

        addNewLines();

        // That's it for target buildings.
        // TODO: where do I put the attacker?!?
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return;
        }

        // Target entities are pushed away or destroyed.
        Coords dest = te.getPosition();
        Coords targetDest = Compute.getValidDisplacement(game, te.getId(), dest, direction);
        if (targetDest != null) {
            doEntityDisplacement(te, dest, targetDest, new PilotingRollData(te.getId(), 2, "hit by death from above"));
        } else {
            // ack!  automatic death!  Tanks
            // suffer an ammo/power plant hit.
            // TODO : a Mech suffers a Head Blown Off crit.
            addReport(destroyEntity(te, "impossible displacement", (te instanceof Mech), (te instanceof Mech)));
        }
        // HACK: to avoid automatic falls, displace from dest to dest
        doEntityDisplacement(ae, dest, dest, new PilotingRollData(ae.getId(), 4, "executed death from above"));
    }

    /**
     * Get the modifier for a Kick or Push PSR
     * @param attacker The attacking <code>Entity></code>
     * @param target   The target <code>Entity</code>
     * @param def      The <code>int</code> default modifier 
     * @return         The <code>int</code> modifier to the PSR 
     */
    private int getKickPushPSRMod(Entity attacker, Entity target, int def) {
        int mod = def;
        
        if ( game.getOptions().booleanOption("maxtech_physical_psr") ) {
            int attackerMod = 0;
            int targetMod = 0;
            
            switch ( attacker.getWeightClass() ) {
            case EntityWeightClass.WEIGHT_LIGHT:
                attackerMod = 1;
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                attackerMod = 2;
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                attackerMod = 3;
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
                attackerMod = 4;
                break;
            }
            switch ( target.getWeightClass() ) {
            case EntityWeightClass.WEIGHT_LIGHT:
                targetMod = 1;
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                targetMod = 2;
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                targetMod = 3;
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
                targetMod = 4;
                break;
            }
            mod = attackerMod - targetMod;
        }
        return mod;
    }

    /**
     * Each mech sinks the amount of heat appropriate to its current heat
     * capacity.
     */
    private void resolveHeat() {
        Report r;
        //Heat phase header
        addReport(new Report(5000, Report.PUBLIC));
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ) {
                continue;
            }
            IHex entityHex = game.getBoard().getHex(entity.getPosition());

            // heat doesn't matter for non-mechs
            if (!(entity instanceof Mech)) {
                entity.heatBuildup = 0;

                if(game.getOptions().booleanOption("vehicle_fires")
                        && entity instanceof Tank) {
                    resolveVehicleFire((Tank)entity, true);
                }
                // If the unit is hit with an Inferno, do flaming death test.
                else if ( entity.infernos.isStillBurning()) {
                    doFlamingDeath(entity);
                }
                continue;
            }
            else {
                // Meks gain heat from inferno hits.
                if ( entity.infernos.isStillBurning() ) {
                    int infernoHeat = entity.infernos.getHeat();
                    entity.heatBuildup += infernoHeat;
                    r = new Report(5010);
                    r.subject = entity.getId();
                    r.add(infernoHeat);
                    addReport(r);
                }
            }

            // should we even bother?
            if ( entity.isDestroyed() || entity.isDoomed() ||
                 entity.crew.isDoomed() || entity.crew.isDead() ) {
                continue;
            }

            // engine hits add a lot of heat, provided the engine is on
            entity.heatBuildup += entity.getEngineCritHeat();

            // If a Mek had an active Stealth suite, add 10 heat.
            if ( entity instanceof Mech && entity.isStealthActive() ) {
                entity.heatBuildup += 10;
                r = new Report(5015);
                r.subject = entity.getId();
                addReport(r);
            }

            // If a Mek is in extreme Temperatures, add or subtract one
            // heat per 10 degrees (or fraction of 10 degrees) above or
            // below 50 or -30 degrees Celsius
            if ( entity instanceof Mech && game.getTemperatureDifference() != 0
                 && !((Mech)entity).hasLaserHeatSinks()) {
                if (game.getOptions().intOption("temperature") > 50) {
                    entity.heatBuildup += game.getTemperatureDifference();
                    r = new Report(5020);
                    r.subject = entity.getId();
                    r.add(game.getTemperatureDifference());
                    addReport(r);
                }
                else {
                    entity.heatBuildup -= game.getTemperatureDifference();
                    r = new Report(5025);
                    r.subject = entity.getId();
                    r.add(game.getTemperatureDifference());
                    addReport(r);
                }
            }

            // Add +5 Heat if the hex you're in is on fire
            // and was on fire for the full round.
            if (entityHex != null) {
                if (entityHex.terrainLevel(Terrains.FIRE) == 2) {
                    entity.heatBuildup += 5;
                    r = new Report(5030);
                    r.subject = entity.getId();
                    addReport(r);
                }
                int magma = entityHex.terrainLevel(Terrains.MAGMA);
                if(magma > 0) {
                    entity.heatBuildup += 5 * magma;
                    r = new Report(5032);
                    r.subject = entity.getId();
                    r.add(5 * magma);
                    addReport(r);                    
                }
            }
            
            // if heatbuildup is negative due to temperature, set it to 0
            // for prettier turnreports
            if (entity.heatBuildup < 0) {
                entity.heatBuildup = 0;
            }

            // add the heat we've built up so far.
            entity.heat += entity.heatBuildup;

            // how much heat can we sink?
            int tosink = entity.getHeatCapacityWithWater();

            // should we use a coolant pod?
            int safeHeat = entity.hasInfernoAmmo() ? 9 : 13;
            int possibleSinkage = ((Mech)entity).getNumberOfSinks();
            for(Enumeration equip=entity.getEquipment();equip.hasMoreElements();) {
                Mounted m = (Mounted)equip.nextElement();
                if(m.getType() instanceof AmmoType) {
                    AmmoType at = (AmmoType)(m.getType());
                    if(at.getAmmoType() == AmmoType.T_COOLANT_POD && m.isAmmoUsable()) {
                        EquipmentMode mode = m.curMode();
                        if(mode.equals("dump")) {
                            r = new Report(5260);
                            r.subject = entity.getId();
                            addReport(r);
                            m.setShotsLeft(0);
                            tosink += possibleSinkage;
                            break;
                        }
                        if(mode.equals("safe") && entity.heat - tosink > safeHeat) {
                            r = new Report(5265);
                            r.subject = entity.getId();
                            addReport(r);
                            m.setShotsLeft(0);
                            tosink += possibleSinkage;
                            break;
                        }
                        if(mode.equals("efficient") && entity.heat - tosink >= possibleSinkage) {
                            r = new Report(5270);
                            r.subject = entity.getId();
                            addReport(r);
                            m.setShotsLeft(0);
                            tosink += possibleSinkage;
                            break;
                        }
                    }
                }
            }

            tosink = Math.min( tosink, entity.heat );
            entity.heat -= tosink;
            r = new Report(5035);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(entity.heatBuildup);
            r.add(tosink);
            r.add(entity.heat);
            addReport(r);
            entity.heatBuildup = 0;

            // Does the unit have inferno ammo?
            if( entity.hasInfernoAmmo() ) {

                // Roll for possible inferno ammo explosion.
                if (entity.heat >= 10) {
                    int boom = 4 + (entity.heat >= 14 ? 2 : 0) +
                        (entity.heat >= 19 ? 2 : 0) +
                        (entity.heat >= 23 ? 2 : 0) +
                        (entity.heat >= 28 ? 2 : 0);
                    int boomroll = Compute.d6(2);
                    r = new Report(5040);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(boom);
                    r.add(boomroll);

                    if (boomroll >= boom) {
                        //avoided
                        r.choose(true);
                        addReport(r);
                    } else {
                        r.choose(false);
                        addReport(r);
                        addReport( explodeInfernoAmmoFromHeat(entity));
                    }
                }
            } // End avoid-inferno-explosion
            int autoShutDownHeat;
            boolean mtHeat;
            
            if (game.getOptions().booleanOption("maxtech_heat")) {
                autoShutDownHeat = 50;
                mtHeat = true;
            } else {
                autoShutDownHeat = 30;
                mtHeat = false;
            }
            // heat effects: start up
            if (entity.heat < autoShutDownHeat && entity.isShutDown()) {
                if (entity.heat < 14) {
                    //automatically starts up again
                    entity.setShutDown(false);
                    r = new Report(5045);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    addReport(r);
                } else {
                    //roll for startup
                    int startup = 4 + (((entity.heat - 14) / 4) * 2);
                    if (mtHeat) {
                        startup = entity.crew.getPiloting() + startup - 8;
                    }
                    int suroll = Compute.d6(2);
                    r = new Report(5050);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(startup);
                    r.add(suroll);
                    if (suroll >= startup) {
                        //start 'er back up
                        entity.setShutDown(false);
                        r.choose(true);
                    } else {
                        r.choose(false);
                    }
                    addReport(r);
                }
            }

            // heat effects: shutdown!
            // 2003-01-26 JAD - Don't shut down if you just restarted.
            else if (entity.heat >= 14 && !entity.isShutDown()) {
                if (entity.heat >= autoShutDownHeat) {
                    r = new Report(5055);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    addReport(r);
                    // add a piloting roll and resolve immediately
                    game.addPSR(new PilotingRollData
                        ( entity.getId(), 3, "reactor shutdown" ));
                    resolvePilotingRolls();
                    // okay, now mark shut down
                    entity.setShutDown(true);
                } else if (entity.heat >= 14) {
                    int shutdown = 4 + (((entity.heat - 14) / 4) * 2);
                    if (mtHeat) {
                        shutdown = entity.crew.getPiloting() + shutdown - 8;
                    }
                    int sdroll = Compute.d6(2);
                    r = new Report(5060);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(shutdown);
                    r.add(sdroll);
                    if (sdroll >= shutdown) {
                        //avoided
                        r.choose(true);
                        addReport(r);
                    } else {
                        //shutting down...
                        r.choose(false);
                        addReport(r);
                        // add a piloting roll and resolve immediately
                        game.addPSR(new PilotingRollData
                            ( entity.getId(), 3, "reactor shutdown" ));
                        resolvePilotingRolls();
                        // okay, now mark shut down
                        entity.setShutDown(true);
                    }
                }
            }

            // heat effects: ammo explosion!
            if (entity.heat >= 19) {
                int boom = 4 + (entity.heat >= 23 ? 2 : 0) +
                    (entity.heat >= 28 ? 2 : 0);
                if (mtHeat) {
                    boom += 
                        (entity.heat >= 35 ? 2 : 0) +
                        (entity.heat >= 40 ? 2 : 0) +
                        (entity.heat >= 45 ? 2 : 0);
                    // Last line is a crutch; 45 heat should be no roll
                    // but automatic explosion.
                }
                if(entity instanceof Mech && ((Mech)entity).hasLaserHeatSinks())
                    boom--;
                int boomroll = Compute.d6(2);
                r = new Report(5065);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(boom);
                r.add(boomroll);
                if (boomroll >= boom) {
                    //mech is ok
                    r.choose(true);
                    addReport(r);
                } else {
                    //boom!
                    r.choose(false);
                    addReport(r);
                    addReport( explodeAmmoFromHeat(entity));
                }
            }

            // heat effects: mechwarrior damage
            // N.B. The pilot may already be dead.
            int lifeSupportCritCount = 0;
            boolean torsoMountedCockpit = ((Mech)entity).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED;
            if ((entity instanceof Mech)
                    && torsoMountedCockpit) {
                lifeSupportCritCount = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_LIFE_SUPPORT,
                    Mech.LOC_RT);
                lifeSupportCritCount += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_LIFE_SUPPORT,
                    Mech.LOC_LT);
            } else {
                lifeSupportCritCount = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_LIFE_SUPPORT,
                        Mech.LOC_HEAD);
            }
            if (lifeSupportCritCount > 0
                    && ((entity.heat >= 15) || (torsoMountedCockpit && (entity.heat >= 0)))
                    && !entity.crew.isDead() && !entity.crew.isDoomed()
                    && !entity.crew.isEjected()) {
                int heatLimitDesc = 1;
                int damageToCrew = 0;
                if (entity.heat >= 47 && mtHeat) {
                    // mechwarrior takes 5 damage
                    heatLimitDesc = 47;
                    damageToCrew = 5;
                } else if (entity.heat >= 39 && mtHeat) {
                    // mechwarrior takes 4 damage
                    heatLimitDesc = 39;
                    damageToCrew = 4;
                } else if (entity.heat >= 32 && mtHeat) {
                    // mechwarrior takes 3 damage
                    heatLimitDesc = 32;
                    damageToCrew = 3;
                } else if (entity.heat >= 25) {
                    // mechwarrior takes 2 damage
                    heatLimitDesc = 25;
                    damageToCrew = 2;
                } else if (entity.heat >= 15) {
                    // mechwarrior takes 1 damage
                    heatLimitDesc = 15;
                    damageToCrew = 1;
                }
                if (entity.heat > 0
                        && (entity instanceof Mech)
                        && ((Mech)entity).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)
                    damageToCrew += 1;
                r = new Report(5070);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(heatLimitDesc);
                r.add(damageToCrew);
                addReport(r);
                damageCrew(entity, damageToCrew);
            } else if (mtHeat && entity.heat >= 32
                       && !entity.crew.isDead() && !entity.crew.isDoomed() ) {
                // Pilot may take damage from heat if MaxTech option is set
                int heatroll = Compute.d6(2);
                int avoidNumber = -1;
                if (entity.heat >= 47) {
                    avoidNumber = 12;
                } else if (entity.heat >= 39) {
                    avoidNumber = 10;
                } else if (entity.heat >= 32) {
                    avoidNumber = 8;
                }
                r = new Report(5075);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(avoidNumber);
                r.add(heatroll);
                if (heatroll >= avoidNumber) {
                    //damage avoided
                    r.choose(true);
                } else {
                    damageCrew(entity, 1);
                    r.choose(false);
                }
                addReport(r);
            }

            // The pilot may have just expired.
            if ( (entity.crew.isDead() || entity.crew.isDoomed() )
                 && !entity.crew.isEjected() ) {
                r = new Report(5080);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.newlines = 0;
                addReport(r);
                addReport( destroyEntity(entity, "crew death", true));
            }

            // With MaxTech Heat Scale, there may occur critical damage
            if (mtHeat) {
                if (entity.heat >= 36) {
                    int damageroll = Compute.d6(2);
                    int damageNumber = -1;
                    if (entity.heat >= 44) {
                        damageNumber = 10;
                    } else if (entity.heat >= 36) {
                        damageNumber = 8;
                    }
                    r = new Report(5085);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(damageNumber);
                    r.add(damageroll);
                    r.newlines = 0;
                    if (damageroll >= damageNumber) {
                        r.choose(true);
                        addReport(r);
                    } else {
                        r.choose(false);
                        addReport(r);
                        addReport(oneCriticalEntity(entity, Compute.d6(2)));
                        //add an empty report, for linebreaking
                        r = new Report(1210);
                        addReport(r);
                    }
                }
            }
        }
        if (vPhaseReport.size() == 1) {
            //I guess nothing happened...
            addReport(new Report(1205, Report.PUBLIC));
        }
    }

    /**
     * check to see if unarmored infantry is outside in extreme temperatures
     * (crude fix because infantry shouldn't be able to be deployed
     * outside of vehicles or buildings, but we can't do that because
     * we don't know wether the map has buildings or not or wether the
     * player has an apc
     */
    private void resolveExtremeTempInfantryDeath() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ) {
                continue;
            }
            IHex entityHex = game.getBoard().getHex(entity.getPosition());
            if (entity instanceof Infantry &&
                    !(entity instanceof BattleArmor) &&
                    game.getTemperatureDifference() > 0 &&
                    !(entityHex.containsTerrain(Terrains.BUILDING)) && 
                    (entity.getTransportId() == Entity.NONE) ) {
                Report r = new Report(5090);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
                addReport(destroyEntity(entity, "heat/cold", false, false));
            }
        }
    }

    /**
     * Resolve Flaming Death for the given Entity
     * @param entity The <code>Entity</code> that may experience flaming death.
     */
    private void doFlamingDeath(Entity entity) {
        Report r;
        int boomroll = Compute.d6(2);
        // Infantry are unaffected by fire while they're still swarming.
        if ( Entity.NONE != entity.getSwarmTargetId() ) {
            return;
        }
        if (entity.getMovementMode() == IEntityMovementMode.VTOL
                && !(entity.infernos.isStillBurning())) {
            // VTOLs don't check as long as they are flying higher than
            // the burning terrain. TODO: Check for rules conformity (ATPM?)
            // according to maxtech, elevation 0 or 1 should be affected,
            // this makes sense for level 2 as well
            Coords c = entity.getPosition();
            IHex h = game.getBoard().getHex(c.x, c.y);
            if (entity.getElevation() > 1) {
                return;
            }
        }
        // Battle Armor squads equipped with fire protection
        // gear automatically avoid flaming death.
        for ( Enumeration iter = entity.getMisc(); iter.hasMoreElements(); ) {
            Mounted mount = (Mounted) iter.nextElement();
            EquipmentType equip = mount.getType();
            if ( BattleArmor.FIRE_PROTECTION.equals(equip.getInternalName()) ) {
                r = new Report(5095);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
                return;
            }
        }

        //Must roll 8+ to survive...
        r = new Report(5100);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(boomroll);
        if (boomroll >= 8) {
            //phew!
            r.choose(true);
            addReport(r);
        } else {
            //eek
            r.choose(false);
            addReport(r);
            addReport( destroyEntity(entity, "fire", false, false));
        }
    }

    /**
     * Checks to see if any entity has takes 20 damage.  If so, they need a
     * piloting skill roll.
     */
    private void checkFor20Damage() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (entity instanceof Mech) {
                // if this mech has 20+ damage, add another roll to the list.
                if (entity.damageThisPhase >= 20) {
                    if ( game.getOptions().booleanOption("maxtech_round_damage") ) {
                      int damMod = (entity.damageThisPhase / 20);
                      int weightMod = 0;
                      StringBuffer reportStr = new StringBuffer();
                      reportStr.append(entity.damageThisPhase)
                          .append(" damage +").append(damMod);

                      switch ( entity.getWeightClass() ) {
                        case EntityWeightClass.WEIGHT_LIGHT:
                          weightMod = 1;
                          break;

                        case EntityWeightClass.WEIGHT_MEDIUM:
                          weightMod = 0;
                          break;

                        case EntityWeightClass.WEIGHT_HEAVY:
                          weightMod = -1;
                          break;

                        case EntityWeightClass.WEIGHT_ASSAULT:
                          weightMod = -2;
                          break;
                      }

                      PilotingRollData damPRD = new PilotingRollData(entity.getId(), damMod + weightMod, reportStr.toString());
                      damPRD.setCumulative(false);  // see Bug# 811987 for more info
                      game.addPSR(damPRD);
                    } else {
                      game.addPSR(new PilotingRollData(entity.getId(), 1, "20+ damage"));
                    }
                }
            }
        }
    }

    /**
     * Checks to see if any non-mech units are standing in fire.  Called at the
     * end of the movement phase
     */
    public void checkForFlamingDeath() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ||
                 entity instanceof Mech ||
                 entity.isDoomed() ||
                 entity.isDestroyed() ||
                 entity.isOffBoard()) {
                continue;
            }
            final IHex curHex = game.getBoard().getHex(entity.getPosition());
            if (curHex.containsTerrain(Terrains.FIRE)
                    && entity.getElevation() <= 1) {
                if(game.getOptions().booleanOption("vehicle_fires")
                        && entity instanceof Tank) {
                    checkForVehicleFire((Tank)entity, false);
                } else {
                    doFlamingDeath(entity);
                }
            }
        }
    }

    /**
     * Check to see if anyone dies due to being in a vacuum.
     */
    private void checkForVacuumDeath() {
        Report r;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (null == entity.getPosition() || entity.isOffBoard()) {
                // If it's not on the board - aboard something else, for example...
                continue;
            }
            if (entity.doomedInVacuum()) {
                r = new Report(6015);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
                addReport(destroyEntity(entity, "being in a vacuum where it can't survive", true, true));
            }
        }
    }

    /**
     * Checks to see if any entities are underwater with damaged life support.
     * Called during the end phase.
     */
    private void checkForSuffocation() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ||
                 entity.isOffBoard()) {
                continue;
            }
            final IHex curHex = game.getBoard().getHex(entity.getPosition());
            if (entity.getElevation()<0 && (curHex.terrainLevel(Terrains.WATER) > 1
            || (curHex.terrainLevel(Terrains.WATER) == 1 && entity.isProne()))
            && entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, Mech.LOC_HEAD) > 0) {
                Report r = new Report(6020);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
                addReport(damageCrew(entity, 1));

            }
        }
    }

    /**
     * Resolves all built up piloting skill rolls.
     * Used at end of weapons, physical phases.
     */
    private void resolvePilotingRolls() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            resolvePilotingRolls((Entity)i.nextElement());
        }
        game.resetPSRs();
    }

    /**
     * Resolves and reports all piloting skill rolls for a single mech.
     */
    void resolvePilotingRolls(Entity entity) {
        resolvePilotingRolls(entity, false, null, null);
    }
    void resolvePilotingRolls( Entity entity, boolean moving,
                               Coords src, Coords dest ) {
        // dead units don't need to.
        if ( entity.isDoomed() || entity.isDestroyed() ) {
            return;
        }
        Report r;

        // first, do extreme gravity PSR, because non-mechs do these, too
        PilotingRollData rollTarget = null;
        for (Enumeration i = game.getExtremeGravityPSRs();i.hasMoreElements();) {
            final PilotingRollData roll = (PilotingRollData)i.nextElement();
            if (roll.getEntityId() != entity.getId()) {
                continue;
            }
            // found a roll, use it (there can be only 1 per entity)
            rollTarget = roll;
            game.resetExtremeGravityPSRs(entity);
        }
        if (rollTarget != null &&
            rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            // okay, print the info
            r = new Report(2180);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(rollTarget.getLastPlainDesc());
            addReport(r);
            // roll
            final int diceRoll = Compute.d6(2);
            r = new Report(2190);
            r.subject = entity.getId();
            r.add(rollTarget.getValueAsString());
            r.add(rollTarget.getDesc());
            r.add(diceRoll);
            if (diceRoll < rollTarget.getValue()) {
                r.choose(false);
                addReport(r);
                // walking and running, 1 damage per MP used more than we would
                // have normally
                if (entity.moved == IEntityMovementType.MOVE_WALK
                        || entity.moved == IEntityMovementType.MOVE_VTOL_WALK
                        || entity.moved == IEntityMovementType.MOVE_RUN
                        || entity.moved == IEntityMovementType.MOVE_VTOL_RUN) {
                    if (entity instanceof Mech) {
                        int j = entity.mpUsed;
                        int damage = 0;
                        while (j > entity.getRunMP(false)) {
                            j--;
                            damage++;
                        }
                        // Wee, direct internal damage
                        doExtremeGravityDamage(entity, damage);
                    } else if (entity instanceof Tank) {
                        // if we got a pavement bonus, take care of it
                        int k = entity.gotPavementBonus ? 1 : 0;  
                        if (!entity.gotPavementBonus) {
                            int j = entity.mpUsed;
                            int damage = 0;
                            while (j > entity.getRunMP(false) + k) {
                                j--;
                                damage++;
                            }
                            doExtremeGravityDamage(entity, damage);
                        }                      
                    }
                }
                // jumping
                if (entity.moved == IEntityMovementType.MOVE_JUMP && entity instanceof Mech) {
                    // low g, 1 damage for each hex jumped further than
                    // possible normally
                    if (game.getOptions().floatOption("gravity") < 1) {
                        int j = entity.mpUsed;
                        int damage = 0;
                        while (j > entity.getOriginalJumpMP()) {
                            j--;
                            damage++;
                        }
                        // Wee, direct internal damage
                        doExtremeGravityDamage(entity, damage);
                    }
                    // high g, 1 damage for each MP we have less than normally
                    else if (game.getOptions().floatOption("gravity") > 1) {
                            int damage = entity.getWalkMP(false) - entity.getWalkMP();
                            // Wee, direct internal damage
                            doExtremeGravityDamage(entity, damage);                    
                    }
                }                
            } else {
                r.choose(true);
                addReport(r);
            }
        }
        // non mechs and prone mechs can now return
        if ( !(entity instanceof Mech) || entity.isProne()) {
            return;
        }
        // add all cumulative rolls, count all rolls
        Vector rolls = new Vector();
        StringBuffer reasons = new StringBuffer();
        PilotingRollData base = entity.getBasePilotingRoll();
        entity.addPilotingModifierForTerrain(base);
        for (Enumeration i = game.getPSRs(); i.hasMoreElements();) {
            final PilotingRollData modifier = (PilotingRollData)i.nextElement();
            if (modifier.getEntityId() != entity.getId()) {
                continue;
            }
            // found a roll, add it
            rolls.addElement(modifier);
            if (reasons.length() > 0) {
                reasons.append(", ");
            }
            reasons.append(modifier.getPlainDesc());
            // only cumulative rolls get added to the base roll
            if (modifier.isCumulative()) {
                base.append(modifier);
            }
        }
        // any rolls needed?
        if (rolls.size() == 0) {
            return;
        }
        // is our base roll impossible?
        if (base.getValue() == PilotingRollData.AUTOMATIC_FAIL || base.getValue() == PilotingRollData.IMPOSSIBLE) {
            r = new Report(2275);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(rolls.size());
            r.add(reasons.toString()); //international issue
            r.add(base.getDesc()); //international issue
            addReport(r);
            if (moving) {
                doEntityFallsInto( entity, src, dest, base );
            } else {
                doEntityFall(entity, base);
            }
            return;
        }
        // loop thru rolls we do have to make...
        r = new Report(2280);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(rolls.size());
        r.add(reasons.toString()); //international issue
        addReport(r);
        r = new Report(2285);
        r.subject = entity.getId();
        r.add(base.getValueAsString());
        r.add(base.getDesc()); //international issue
        addReport(r);
        for (int i = 0; i < rolls.size(); i++) {
            PilotingRollData modifier = (PilotingRollData)rolls.elementAt(i);
            PilotingRollData target = base;
            r = new Report(2290);
            r.subject = entity.getId();
            r.indent();
            r.newlines = 0;
            r.add(i+1);
            r.add(modifier.getPlainDesc()); //international issue
            addReport(r);
            if (!modifier.isCumulative()) {
                // non-cumulative rolls only happen due to weight class adj.
                r = new Report(2295);
                r.subject = entity.getId();
                r.newlines = 0;
                r.add(modifier.getValueAsString()); //international issue
                target = new PilotingRollData(entity.getId());
                target.append(base);
                target.append(modifier);
            }
            int diceRoll = Compute.d6(2);
            r = new Report(2300);
            r.subject = entity.getId();
            r.add(target.getValueAsString());
            r.add(diceRoll);
            if (diceRoll < target.getValue()) {
                r.choose(false);
                addReport(r);
                if (moving) {
                    doEntityFallsInto( entity, src, dest, base );
                } else {
                    doEntityFall(entity, base);
                }
                return;
            } else {
                r.choose(true);
                addReport(r);
            }
        }
    }

    /**
     * Inflict damage on a pilot
     *
     * @param en     The <code>Entity</code> who's pilot gets damaged.
     * @param damage The <code>int</code> amount of damage.
     */
    private Vector damageCrew(Entity en, int damage) {
        Vector vDesc = new Vector();
        Pilot crew = en.getCrew();

        if (!crew.isDead() && !crew.isEjected() && !crew.isDoomed()) {
            crew.setHits( crew.getHits() + damage );
            Report r = new Report(6025);
            r.subject = en.getId();
            r.indent(2);
            r.addDesc(en);
            r.add(crew.getName());
            r.add(damage);
            r.newlines = 0;
            vDesc.addElement(r);
            if ( Pilot.DEATH > crew.getHits() ) {
                crew.setRollsNeeded( crew.getRollsNeeded() + damage );
            } else if ( !crew.isDoomed() ) {
                crew.setDoomed(true);
                vDesc.addAll( destroyEntity(en, "pilot death", true));
            }
        }

        return vDesc;
    }

    /**
     * This checks if the mech pilot goes unconscious from the damage he has
     * taken this phase.
     */
    private void resolveCrewDamage() {
        boolean anyRolls = false;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();
            if (resolveCrewDamage(e, anyRolls)) {
                anyRolls = true;
            }
        }
        if (anyRolls) {
            addNewLines();
        }
    }

    /**
     * resolves consciousness rolls for one entity
     */
    private boolean resolveCrewDamage(Entity e, boolean anyRolls) {
        final int totalHits = e.getCrew().getHits();
        final int rollsNeeded = e.getCrew().getRollsNeeded();
        e.crew.setRollsNeeded(0);
        if (!e.isTargetable() || !e.getCrew().isActive() || rollsNeeded == 0) {
            return false;
        }
        for (int hit = totalHits - rollsNeeded + 1; hit <= totalHits; hit++) {
            int rollTarget = Compute.getConsciousnessNumber( hit );
            boolean edgeUsed = false;
            do {
                if (edgeUsed)
                    e.crew.decreaseEdge();
                int roll = Compute.d6(2);
                if ( e.getCrew().getOptions().booleanOption("pain_resistance") )
                  roll = Math.min(12, roll + 1);
                Report r = new Report(6030);
                r.subject = e.getId();
                r.addDesc(e);
                r.add(e.getCrew().getName());
                r.add(rollTarget);
                r.add(roll);
                if (roll >= rollTarget) {
                    e.crew.setKoThisRound(false);
                    r.choose(true);
                } else {
                    e.crew.setKoThisRound(true);
                    r.choose(false);
                    if (e.crew.hasEdgeRemaining()
                            && e.crew.getOptions().booleanOption("edge_when_ko")) {
                        edgeUsed = true;
                        vPhaseReport.addElement(r);
                        r = new Report(6520);
                        r.subject = e.getId();
                        r.addDesc(e);
                        r.add(e.getCrew().getName());
                        r.add(e.crew.getOptions().intOption("edge"));
                    } // if
                    //return true;
                } // else
                addReport(r);
            } while (e.crew.hasEdgeRemaining() && e.crew.isKoThisRound()
                    && e.crew.getOptions().booleanOption("edge_when_ko"));
            // end of do-while
            if (e.crew.isKoThisRound()) {
                e.crew.setUnconscious(true);  
                return true;
            }
        }
        return true;
    }

    /**
     * Make the rolls indicating whether any unconscious crews wake up
     */
    private void resolveCrewWakeUp() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();

            // only unconscious pilots of mechs and protos and MechWarrirs
            // can roll to wake up
            if ( !e.isTargetable() || !e.crew.isUnconscious() ||
                 e.crew.isKoThisRound() ||
                !(e instanceof Mech || e instanceof Protomech || e instanceof MechWarrior)) {
                continue;
            }
            int roll = Compute.d6(2);

            if ( e.getCrew().getOptions().booleanOption("pain_resistance") )
              roll = Math.min(12, roll + 1);

            int rollTarget = Compute.getConsciousnessNumber( e.crew.getHits() );
            Report r = new Report(6029);
            r.subject = e.getId();
            r.addDesc(e);
            r.add(e.getCrew().getName());
            r.add(rollTarget);
            r.add(roll);
            if (roll >= rollTarget) {
                r.choose(true);
                e.crew.setUnconscious(false);
            } else {
                r.choose(false);
            }
            addReport(r);
        }
    }

    public Vector damageEntity(Entity te, HitData hit, int damage, boolean ammoExplosion) {
        return damageEntity(te, hit, damage, ammoExplosion, 0, false, false);
    }

    public Vector damageEntity(Entity te, HitData hit, int damage) {
        return damageEntity(te, hit, damage, false, 0, false, false);
    }

    public Vector damageEntity(Entity te, HitData hit, int damage,
                               boolean ammoExplosion, int bFrag) {
        return damageEntity(te, hit, damage, ammoExplosion, bFrag,
                            false, false);
    }

    public Vector damageEntity(Entity te, HitData hit, int damage,
                               boolean ammoExplosion, int bFrag,
                               boolean damageIS) {
        return damageEntity(te, hit, damage, ammoExplosion, bFrag,
                            damageIS, false);
    }

    public Vector damageEntity(Entity te, HitData hit, int damage,
                               boolean ammoExplosion, int bFrag,
                               boolean damageIS, boolean areaSatArty) {
        return damageEntity (te, hit, damage, ammoExplosion, bFrag, damageIS,
                             false, true);
    }
    
    /**
     * Deals the listed damage to an entity.  Returns a vector of Reports
     * for the phase report
     *
     * @param te the target entity
     * @param hit the hit data for the location hit
     * @param damage the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied
     *          directly to the IS, hurts the pilot, causes auto-ejects,
     *          and can blow the unit to smithereens
     * @param bFrag If 0, nothing; if 1, Fragmentation; if 2, Flechette.
     * @param damageIS Should the target location's internal structure be
     *          damaged directly?
     * @param areaSatArty Is the damage from an area saturating artillery
     *          attack?
     * @param throughFront Is the damage coming through the hex the unit
     *          is facing?
     * @return a <code>Vector</code> of <code>Report</code>s
     */
    private Vector damageEntity(Entity te, HitData hit, int damage,
                                boolean ammoExplosion, int bFrag,
                                boolean damageIS, boolean areaSatArty,
                                boolean throughFront) {

        Vector vDesc = new Vector();
        Report r;
        int te_n = te.getId();
        //This is good for shields if a shield absorps the hit it shouldn't
        //effect the pilot.
        boolean isHeadHit = (te instanceof Mech
            && ((Mech)te).getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED
            && hit.getLocation() == Mech.LOC_HEAD);
        
        // show Locations which have rerolled with Edge
        HitData undoneLocation = hit.getUndoneLocation();
        while (undoneLocation != null) {
            r = new Report(6500);
            r.subject = te_n;
            r.indent(2);
            r.newlines = 0;
            r.addDesc(te);
            r.add(te.getLocationAbbr(undoneLocation));
            vDesc.addElement(r);            
            undoneLocation = undoneLocation.getUndoneLocation();
        } // while
        // if edge was uses, give at end overview of remainings
        if (hit.getUndoneLocation() != null) {
            r = new Report(6510);
            r.subject = te_n;
            r.indent(2);
            r.newlines = 0;
            r.addDesc(te);
            r.add(te.crew.getOptions().intOption("edge"));
            vDesc.addElement(r);            
        } // if
        
        boolean autoEject = false;
        if (ammoExplosion) {
            if (te instanceof Mech) {
                Mech mech = (Mech)te;
                if (mech.isAutoEject()) {
                    autoEject = true;
                    vDesc.addAll(ejectEntity(te, true));
                }
            }
        }
        boolean isBattleArmor = (te instanceof BattleArmor);
        boolean isPlatoon = !isBattleArmor && (te instanceof Infantry);
        boolean isFerroFibrousTarget = false;
        boolean wasDamageIS = false;
        boolean tookInternalDamage = damageIS;
        IHex te_hex = null;

        boolean hardenedArmor = false;
        if ((te instanceof Mech)
                && (te.getArmorType() == EquipmentType.T_ARMOR_HARDENED))
            hardenedArmor = true;
        int crits = ((hit.getEffect() == HitData.EFFECT_CRITICAL) && (!hardenedArmor)) ? 1 : 0;
        int specCrits = ((hit.getEffect() == HitData.EFFECT_CRITICAL) && (hardenedArmor)) ? 1 : 0;
        HitData nextHit = null;

        // Some "hits" on a Protomech are actually misses.
        if( te instanceof Protomech &&
            hit.getLocation() == Protomech.LOC_NMISS ) {
            r = new Report(6035);
            r.subject = te_n;
            r.indent(2);
            r.newlines = 0;
            vDesc.addElement(r);
            return vDesc;
        }

        // check for critical hit/miss vs. a BA
        if( crits > 0 && te instanceof BattleArmor) {
            //possible critical miss if the rerolled location isn't alive
            if(hit.getLocation() >= te.locations()
            || te.getInternal(hit.getLocation()) <= 0) {
                r = new Report(6037);
                r.add(hit.getLocation());
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
                return vDesc;
            }
            //otherwise critical hit
            r = new Report(6225);
            r.add(te.getLocationAbbr(hit));
            r.subject = te_n;
            r.indent(2);
            r.newlines = 0;
            vDesc.addElement(r);

            crits = 0;
            damage = Math.max(te.getInternal(hit.getLocation()) + 
                              te.getArmor(hit.getLocation()),
                              damage);
        }

        if ((te != null) &&
            (te.getArmor(hit) > 0) &&
            ((te.getArmorType() == EquipmentType.T_ARMOR_FERRO_FIBROUS) ||
             (te.getArmorType() == EquipmentType.T_ARMOR_LIGHT_FERRO) ||
             (te.getArmorType() == EquipmentType.T_ARMOR_HEAVY_FERRO))) {
            isFerroFibrousTarget = true;
        }

        // Is the infantry in the open?
        if ( isPlatoon && !te.isDestroyed() && !te.isDoomed() ) {
            te_hex = game.getBoard().getHex( te.getPosition() );
            if ( te_hex != null &&
                 !te_hex.containsTerrain( Terrains.WOODS ) &&
                 !te_hex.containsTerrain( Terrains.JUNGLE ) &&
                 !te_hex.containsTerrain( Terrains.ROUGH ) &&
                 !te_hex.containsTerrain( Terrains.RUBBLE ) &&
                 !te_hex.containsTerrain( Terrains.SWAMP ) &&
                 !te_hex.containsTerrain( Terrains.BUILDING ) &&
                 !te_hex.containsTerrain(Terrains.FORTIFIED)) {
                // PBI.  Damage is doubled.
                damage = damage * 2;
                r = new Report(6040);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
        }
        // Is the infantry in vacuum?
        if ( (isPlatoon || isBattleArmor ) && !te.isDestroyed()
                && !te.isDoomed() && game.getOptions().booleanOption("vacuum")) {
            // PBI. Double damage.
            damage = damage * 2;
            r = new Report(6041);
            r.subject = te_n;
            r.indent(2);
            r.newlines = 0;
            vDesc.addElement(r);
        }
        // If dealing with fragmentation missiles,
        // it does double damage to infantry...
        // We're actually going to abuse this for AX-head warheads, too, so as to not add another parameter.
        switch (bFrag)
        {
        case 1:
            if (isPlatoon) {
                damage *= 2;
                r = new Report(6045);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            else if (te != null) {
                damage = 0;
                r = new Report(6050);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            break;
        case 2:
            if (isPlatoon) {
                damage *= 2;
                r = new Report(6055);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            else if ((te != null) && (!isBattleArmor)) {
                damage /= 2;
                r = new Report(6060);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            break;
        case 3:
            if (isFerroFibrousTarget) {
                damage = te.getArmor(hit) >=3?3:te.getArmor(hit);
                r = new Report(6061);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                r.add(damage);
                vDesc.addElement(r);
            } else if (te != null) {
                r = new Report(6062);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            break;
        case 4:
            //Incendiary AC ammo does +2 damage to unarmoured infantry
            if(isPlatoon ) {
                damage += 2;
            }
            break;

        default:
            // We can ignore this.
            break;
        }

        //save EI status, in case sensors crit destroys it
        final boolean eiStatus = te.hasActiveEiCockpit();
        // BA using EI implants receive +1 damage from attacks
        if (!(te instanceof Mech) && !(te instanceof Protomech) && eiStatus) {
            damage += 1;
        }

        // Allocate the damage
        while (damage > 0) {

            // let's resolve some damage!
            r = new Report(6065);
            r.subject = te_n;
            r.indent(2);
            r.newlines = 0;
            r.addDesc(te);
            r.add(damage);
            if (damageIS) r.messageId = 6070;
            r.add(te.getLocationAbbr(hit));
            vDesc.addElement(r);

            // was the section destroyed earlier this phase?
            if (te.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                crits = 0;
            }

            //here goes the fun :)
            //Shields take damage first then cowls then armor whee
            //Shield does not protect from ammo explosions or falls.
            if ( !ammoExplosion && !hit.isFallDamage() && te.hasShield() ){
                Mech me = (Mech)te;
                int damageNew = me.shieldAbsorptionDamage(damage,hit.getLocation(),hit.isRear());
                //if a shield absorbed the damage then lets tell the world about it.
                if ( damageNew != damage){
                    int damageDiff = damage - damageNew;
                    damage = damageNew;
                    
                    r = new Report (3530);
                    r.subject = te_n;
                    r.indent(3);
                    r.newlines=0;
                    r.add(damageDiff);
                    vDesc.addElement(r);
                    
                    if ( damage <= 0 ){
                        crits = 0;
                        specCrits = 0;
                        isHeadHit = false;
                    }
                        
                }
            }
            
            // Armored Cowl may absorb some damage from hit
            if (te instanceof Mech) {
                Mech me = (Mech)te;
                if (me.hasCowl() && hit.getLocation()==Mech.LOC_HEAD &&
                    !throughFront ) {
                    int damageNew = me.damageCowl(damage);
                    int damageDiff = damage-damageNew;
                    damage = damageNew;
                    
                    r = new Report (3520);
                    r.subject = te_n;
                    r.indent(3);
                    r.newlines=0;
                    r.add(damageDiff);
                    vDesc.addElement(r);
                }
            }
            
            // Destroy searchlights on 7+ (torso hits on mechs)
            boolean spotlightHittable = false;
            if  (te.hasSpotlight()) {
                spotlightHittable = true;
                int loc = hit.getLocation();
                if (te instanceof Mech) {
                    if (loc != Mech.LOC_CT && loc != Mech.LOC_LT && loc != Mech.LOC_RT) {
                        spotlightHittable = false;
                    }
                } else if (te instanceof Tank) {
                    if (loc != Tank.LOC_FRONT && loc != Tank.LOC_RIGHT && loc != Tank.LOC_LEFT) {
                        spotlightHittable = false;
                    }
                }
                if (spotlightHittable) {
                    int spotroll = Compute.d6(2);
                    r = new Report(6072);
                    r.indent(2);
                    r.subject = te_n;
                    r.add(spotroll);
                    vDesc.addElement(r);
                    if (spotroll >= 7) {
                        r = new Report(6071);
                        r.subject = te_n;
                        r.indent(2);
                        vDesc.addElement(r);
                        te.setSpotlightState(false);
                        te.setSpotlight(false);
                    }
                }
            }

            // Does an exterior passenger absorb some of the damage?
            if (!damageIS) {
                int nLoc = hit.getLocation();
                Entity passenger = te.getExteriorUnitAt( nLoc, hit.isRear() );
                // Does an exterior passenger absorb some of the damage?
                if ( !ammoExplosion && null != passenger
                        && !passenger.isDoomed() ) {
                    // Yup.  Roll up some hit data for that passenger.
                    r = new Report(6075);
                    r.subject = passenger.getId();
                    r.indent(3);
                    r.addDesc(passenger);
                    vDesc.addElement(r);

                    HitData passHit = passenger.getTrooperAtLocation
                        ( hit, te);

                    // How much damage will the passenger absorb?
                    int absorb = 0;
                    HitData nextPassHit = passHit;
                    do {
                        if ( 0 < passenger.getArmor( nextPassHit ) ) {
                            absorb += passenger.getArmor( nextPassHit );
                        }
                        if ( 0 < passenger.getInternal( nextPassHit ) ) {
                            absorb += passenger.getInternal( nextPassHit );
                        }
                        nextPassHit = passenger.getTransferLocation( nextPassHit );
                    } while ( damage > absorb && nextPassHit.getLocation() >= 0 );

                    // Damage the passenger.
                    vDesc.addAll( damageEntity(passenger, passHit, damage));

                    // Did some damage pass on?
                    if ( damage > absorb ) {
                        // Yup.  Remove the absorbed damage.
                        damage -= absorb;
                        r = new Report(6080);
                        r.subject = te_n;
                        r.indent(1);
                        r.add(damage);
                        r.addDesc(te);
                        vDesc.addElement(r);
                    } else {
                        // Nope.  Return our description.
                        return vDesc;
                    }

                } // End nLoc-has-exterior-passenger

                // is this a mech dumping ammo being hit in the rear torso?
                boolean bTorso = (nLoc == Mech.LOC_CT || nLoc == Mech.LOC_RT ||
                                  nLoc == Mech.LOC_LT);
                if (te instanceof Mech && hit.isRear() && bTorso) {
                    for (Enumeration e = te.getAmmo(); e.hasMoreElements(); ) {
                        Mounted mAmmo = (Mounted)e.nextElement();
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed() &&
                            !mAmmo.isHit()) {
                            // doh.  explode it
                            vDesc.addAll( explodeEquipment(te, mAmmo.getLocation(), mAmmo) );
                            mAmmo.setHit(true);
                        }
                    }
                }
            }

            // is there armor in the location hit?
            if (!ammoExplosion && te.getArmor(hit) > 0 && !damageIS) {
                int tmpDamageHold = -1;

                // If the target has hardened armor, we need to adjust damage.
                if (hardenedArmor) {
                    tmpDamageHold = damage;
                    damage /= 2;
                    damage += (tmpDamageHold%2);
                }
                
                if (te.getArmor(hit) > damage) {
                    // armor absorbs all damage
                    te.setArmor(te.getArmor(hit) - damage, hit);
                    if (tmpDamageHold >= 0)
                        te.damageThisPhase += tmpDamageHold;
                    else
                        te.damageThisPhase += damage;
                    damage = 0;
                    r = new Report(6085);
                    r.subject = te_n;
                    r.newlines = 0;
                    if(spotlightHittable)r.indent(3);
                    r.add(te.getArmor(hit));
                    vDesc.addElement(r);
                } else {
                    // damage goes on to internal
                    int absorbed = Math.max(te.getArmor(hit), 0);
                    te.setArmor(IArmorState.ARMOR_DESTROYED, hit);
                    if (tmpDamageHold >= 0)
                        te.damageThisPhase += 2*absorbed;
                    else
                        te.damageThisPhase += absorbed;
                    damage -= absorbed;
                    r = new Report(6090);
                    r.subject = te_n;
                    r.newlines = 0;
                    if(spotlightHittable)r.indent(3);
                    vDesc.addElement(r);
                    if (te instanceof GunEmplacement) {
                        // gun emplacements have no internal,
                        // destroy the section
                        destroyLocation(te, hit.getLocation());
                        r = new Report(6115);
                        r.subject = te_n;
                        r.newlines = 0;
                        vDesc.addElement(r);

                        if (te.getTransferLocation(hit).getLocation() ==
                            Entity.LOC_DESTROYED) {
                            vDesc.addAll(
                                                  destroyEntity(te,
                                                                "damage",
                                                                false));
                        }
                    }
                }
                
                // If it has hardened armor, now we need to "correct" any remaining damage.
                if (tmpDamageHold > 0) {
                    if (hardenedArmor) {
                        damage *= 2;
                        damage -= (tmpDamageHold%2);
                    }
                }
            }

            // is there damage remaining?
            if (damage > 0) {
                tookInternalDamage = true;
                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {
                    // Triggers a critical hit on Vehicles and Mechs.
                    if ( !isPlatoon && !isBattleArmor ) {
                        crits++;
                    }

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if ((te instanceof Mech) && (((Mech)te).hasCompositeStructure())) {
                        tmpDamageHold = damage;
                        damage *= 2;
                    }
                    if ((te instanceof Mech) && (((Mech)te).hasReinforcedStructure())) {
                        tmpDamageHold = damage;
                        damage /= 2;
                        damage += (tmpDamageHold%2);
                    }

                    if (te.getInternal(hit) > damage) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        te.damageThisPhase += damage;
                        damage = 0;
                        r = new Report(1210);
                        r.subject = te_n;
                        r.newlines = 0;
                        // Infantry platoons have men not "Internals".
                        if ( isPlatoon ) {
                            r.messageId = 6095;
                        } else {
                            r.messageId = 6100;
                        }
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else {
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

                        // Handle Protomech pilot damage
                        // due to location destruction
                        if ( te instanceof Protomech ) {
                            int hits = Protomech.POSSIBLE_PILOT_DAMAGE[hit.getLocation()] -
                                ((Protomech)te).getPilotDamageTaken(hit.getLocation());
                            if ( hits > 0 ) {
                                vDesc.addAll( damageCrew( te, hits ));
                                ((Protomech)te).setPilotDamageTaken
                                     (hit.getLocation(),
                                      Protomech.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]);
                            }
                        }

                        // Platoon, Trooper, or Section destroyed message
                        r = new Report(1210);
                        r.subject = te_n;
                        r.newlines = 0;
                        if ( isPlatoon ) {
                            // Infantry have only one section, and
                            //  are therefore destroyed.
                            r.messageId = 6105;
                        } else if ( isBattleArmor ) {
                            r.messageId = 6110;
                        } else {
                            r.messageId = 6115;
                        }
                        vDesc.addElement(r);

                        // If a sidetorso got destroyed, and the
                        // corresponding arm is not yet destroyed, add
                        // it as a club to that hex (p.35 BMRr)
                        if (te instanceof Mech &&
                            ((hit.getLocation() == Mech.LOC_RT &&
                              te.getInternal(Mech.LOC_RARM) > 0) ||
                             (hit.getLocation() == Mech.LOC_LT &&
                              te.getInternal(Mech.LOC_LARM) > 0))) {
                            int blownOffLocation = -1; //good initial value?
                            if (hit.getLocation() == Mech.LOC_RT) {
                                blownOffLocation = Mech.LOC_RARM;
                            } else {
                                blownOffLocation = Mech.LOC_LARM;
                            }
                            r = new Report(6120);
                            r.subject = te_n;
                            r.add(te.getLocationName(blownOffLocation));
                            r.newlines = 0;
                            vDesc.addElement(r);
                            IHex h = game.getBoard().getHex(te.getPosition());
                            if (te instanceof BipedMech) {
                                if (!h.containsTerrain( Terrains.ARMS)) {
                                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, 1));
                                }
                                else h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, h.terrainLevel(Terrains.ARMS)+1));
                            } else if (!h.containsTerrain( Terrains.LEGS)) {
                                h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, 1));
                            } else h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, h.terrainLevel(Terrains.LEGS)+1));
                            sendChangedHex(te.getPosition());
                        }

                        // Level 3 mechanized BA, troopers riding on a location 
                        // all die when the location is destroyed.
                        if(game.getOptions().booleanOption("maxtech_mechanized_ba") &&
                           te instanceof Mech) {
                            Entity passenger = te.getExteriorUnitAt( hit.getLocation(), hit.isRear() );
                            if(null != passenger && !passenger.isDoomed()) {
                                HitData passHit = passenger.getTrooperAtLocation( hit, te);
                                passHit.setEffect(HitData.EFFECT_CRITICAL); //ensures a kill
                                if(passenger.getInternal(passHit) > 0) {
                                    vDesc.addAll(damageEntity(passenger, passHit, damage));
                                }
                                passHit = new HitData(hit.getLocation(), !(hit.isRear()));
                                passHit = passenger.getTrooperAtLocation( passHit, te);
                                passHit.setEffect(HitData.EFFECT_CRITICAL); //ensures a kill
                                if(passenger.getInternal(passHit) > 0) {
                                    vDesc.addAll(damageEntity(passenger, passHit, damage));
                                }
                            }
                        }

                        // Destroy the location.
                        destroyLocation(te, hit.getLocation());
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;

                        // Now we need to consider alternate structure types!
                        if (tmpDamageHold > 0) {
                            if (((Mech)te).hasCompositeStructure()) {
                                // If there's a remainder, we can actually ignore it.
                                damage /= 2;
                            } else if (((Mech)te).hasReinforcedStructure()) {
                                damage *= 2;
                                damage -= (tmpDamageHold%2);
                            }
                        }

                        if (te instanceof Mech &&
                            (hit.getLocation() == Mech.LOC_RT ||
                             hit.getLocation() == Mech.LOC_LT)) {

                            boolean engineExploded = false;


                            int numEngineHits = 0;
                            numEngineHits +=
                                te.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                                   Mech.SYSTEM_ENGINE,
                                                   Mech.LOC_CT);
                            numEngineHits +=
                                te.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                                   Mech.SYSTEM_ENGINE,
                                                   Mech.LOC_RT);
                            numEngineHits +=
                                te.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                                   Mech.SYSTEM_ENGINE,
                                                   Mech.LOC_LT);

                            engineExploded = checkEngineExplosion(te, vDesc, numEngineHits);
                            if ( !engineExploded && numEngineHits > 2  ) {
                                // third engine hit
                                vDesc.addAll( destroyEntity(te, "engine destruction"));
                                if ( game.getOptions().booleanOption("auto_abandon_unit") )
                                    vDesc.addAll(abandonEntity(te));

                            }
                        }
                        
                        if (te instanceof VTOL && hit.getLocation() == VTOL.LOC_ROTOR) {
                            //if rotor is destroyed, movement goes bleh.
                            //I think this will work?
                            hit.setEffect(HitData.EFFECT_VEHICLE_MOVE_DESTROYED);
                            
                        }
                    }
                } if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        if (te instanceof Mech) {
                            // add all non-destroyed engine crits
                            te.engineHitsThisRound += te.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, hit.getLocation());
                            // and substract those that where hit previously this round
                            // hackish, but works.
                            te.engineHitsThisRound -= te.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, hit.getLocation());
                        }

                        boolean engineExploded = false;

                        engineExploded = checkEngineExplosion(te, vDesc, te.engineHitsThisRound);

                        if ( !engineExploded && !((te instanceof VTOL) && hit.getLocation() == VTOL.LOC_ROTOR)) {
                            // Entity destroyed.  Ammo explosions are
                            // neither survivable nor salvagable.
                            // Only ammo explosions in the CT are devastating.
                            vDesc.addAll( destroyEntity( te, "damage",
                                                      !ammoExplosion,
                                                      !( (ammoExplosion || areaSatArty) &&
                                                         hit.getLocation() ==
                                                         Mech.LOC_CT ) ) );
                            // If the head is destroyed, kill the crew.
                            if (hit.getLocation() == Mech.LOC_HEAD ||
                                (hit.getLocation() == Mech.LOC_CT && ((ammoExplosion && !autoEject) || areaSatArty))) {
                                te.getCrew().setDoomed(true);
                            }
                            if (game.getOptions().booleanOption("auto_abandon_unit")) {
                                vDesc.addAll(abandonEntity(te));
                            }
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if ( nextHit.getLocation() == Entity.LOC_NONE ) {
                        // Rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && te.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented by CASE
                        r = new Report(6125);
                        r.subject = te_n;
                        r.add(damage);
                        r.indent(3);
                        r.newlines = 0;
                        vDesc.addElement(r);

                        // ... but page 21 of the Ask The Precentor Martial FAQ
                        // www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf
                        // says that the damage counts for making PSRs.
                        te.damageThisPhase += damage;

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        r = new Report(6130);
                        r.subject = te_n;
                        r.indent(2);
                        r.newlines = 0;
                        r.add(damage);
                        r.add(te.getLocationAbbr(nextHit));
                        vDesc.addElement(r);

                        // If there are split weapons in this location, mark it as
                        // destroyed, even if it took no criticals.
                        for (Enumeration weapons=te.getWeapons(); weapons.hasMoreElements(); ) {
                            Mounted m = (Mounted)weapons.nextElement();
                            if (m.isSplit()) {
                                if (m.getLocation() == hit.getLocation() ||
                                    m.getLocation() == nextHit.getLocation()) {
                                    te.setWeaponDestroyed(m);
                                }
                            }
                        }
                    }
                }
            } else if (hit.getSpecCritMod() < 0) {
                // If there ISN'T any armor left but we did damage, then there's a chance of a crit, using Armor Piercing.
                specCrits++;
            }
            // check for breaching
            vDesc.addAll( breachCheck(te, hit.getLocation(), null));

            // resolve special results
            if (hit.getEffect() == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
                r = new Report(6135);
                r.subject = te_n;
                r.indent(3);
                vDesc.addElement(r);
                int nMP = te.getOriginalWalkMP();
                if (nMP > 0) {
                    te.setOriginalWalkMP(nMP - 1);

                    if (te.getOriginalWalkMP()==0) {
                        // From http://www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf
                        // page 19, tanks are only immobile if they take that critical hit.
                        // ((Tank)te).immobilize();

                        // Hovercraft reduced to 0MP over water sink
                        if ( te.getMovementMode() == IEntityMovementMode.HOVER &&
                             game.getBoard().getHex( te.getPosition() ).terrainLevel(Terrains.WATER) > 0 
                             &&!(game.getBoard().getHex( te.getPosition() ).containsTerrain(Terrains.ICE))) {
                            vDesc.addAll( destroyEntity(te, "a watery grave", false) );
                        }
                    }
                }
            } else if (hit.getEffect() == HitData.EFFECT_VEHICLE_MOVE_DESTROYED) {
                r = new Report(6140);
                r.subject = te_n;
                r.indent(3);
                vDesc.addElement(r);
                ((Tank)te).immobilize();
                // Does the hovercraft sink?
                te_hex = game.getBoard().getHex( te.getPosition() );
                if ( te.getMovementMode() == IEntityMovementMode.HOVER &&
                     te_hex.terrainLevel(Terrains.WATER) > 0 &&
                     !(te_hex.containsTerrain(Terrains.ICE))) {
                    vDesc.addAll( destroyEntity(te, "a watery grave", false) );
                }
                if(te instanceof VTOL) {
                    Report.addNewline(vDesc);
                    //report problem: add tab
                    vDesc.addAll( crashVTOL((VTOL)te));
                }
            } else if (hit.getEffect() == HitData.EFFECT_VEHICLE_TURRETLOCK) {
                r = new Report(6145);
                r.subject = te_n;
                r.indent(3);
                vDesc.addElement(r);
                ((Tank)te).lockTurret();
            }
            else if (hit.getEffect() ==
                     HitData.EFFECT_GUN_EMPLACEMENT_WEAPONS) {
                r = new Report(6146);
                r.subject = te_n;
                r.indent(3);
                vDesc.addElement(r);
                Enumeration weapons = te.getWeapons();
                while (weapons.hasMoreElements()) {
                    ((Mounted) weapons.nextElement()).setDestroyed(true);
                }
            }
            else if (hit.getEffect() == HitData.EFFECT_GUN_EMPLACEMENT_TURRET) {
                r = new Report(6147);
                r.subject = te_n;
                r.indent(3);
                vDesc.addElement(r);
                ((GunEmplacement)te).setTurretLocked(true);
            }
            else if (hit.getEffect() == HitData.EFFECT_GUN_EMPLACEMENT_CREW) {
                r = new Report(6148);
                r.subject = te_n;
                r.indent(3);
                vDesc.addElement(r);
                ((GunEmplacement)te).getCrew().setDoomed(true);
            }
            // roll all critical hits against this location
            // unless the section destroyed in a previous phase?
            if (te.getInternal(hit) != IArmorState.ARMOR_DESTROYED) {
                for (int i = 0; i < crits; i++) {
                    ((Report) vDesc.elementAt(vDesc.size() - 1)).newlines++;
                    vDesc.addAll( criticalEntity(te, hit.getLocation(), hit.glancingMod()) );
                }
                crits = 0;

                for (int i = 0; i < specCrits; i++) {
                    ((Report) vDesc.elementAt(vDesc.size() - 1)).newlines++;
                    vDesc.addAll( criticalEntity(te, hit.getLocation(), (hardenedArmor?-2:hit.getSpecCritMod())+hit.glancingMod()) );
                }
                specCrits = 0;
            }

            if (isHeadHit) {
                Report.addNewline(vDesc);
                vDesc.addAll( damageCrew(te, 1) );
            }

            // loop to next location
            hit = nextHit;
            if (damageIS) {
                wasDamageIS = true;
                damageIS = false;
            }
        }
        //Mechs using EI implants take pilot damage each time a hit 
        //inflicts IS damage
        if (tookInternalDamage
                && ((te instanceof Mech) || (te instanceof Protomech))
                && te.hasActiveEiCockpit()) {
            Report.addNewline(vDesc);
            int roll = Compute.d6(2);
            r = new Report(5075);
            r.subject = te.getId();
            r.addDesc(te);
            r.add(7);
            r.add(roll);
            r.choose(roll>=7);
            r.indent(2);
            vDesc.add(r);
            if(roll < 7) {
                vDesc.addAll( damageCrew(te, 1) );
            }
        }

        //damage field guns on infantry platoons if there arent enough men left to man it
        if(isPlatoon) {
            float tons = 0.0f;
            for(Enumeration<Mounted> weapons = te.getWeapons();weapons.hasMoreElements();) {
                Mounted weap = weapons.nextElement();
                WeaponType wtype = (WeaponType)weap.getType();
                if(!wtype.hasFlag(WeaponType.F_INFANTRY)) {
                    tons += wtype.getTonnage(te);
                    if(tons > te.getInternal(Infantry.LOC_INFANTRY)) {
                        weap.setDestroyed(true);
                    }
                }
            }
        }

        //This flag indicates the hit was directly to IS
        if (wasDamageIS) {
            Report.addNewline(vDesc);
        }
        return vDesc;
    }

    /**
     * Check to see if the entity's engine explodes.  
     * Rules for ICE explosions are different to fusion engines.
     * @param   en - the <code>Entity</code> in question.
     *          This value must not be <code>null</code>.
     * @param   vDesc - the <code>Vector</code> that this function should
     *          add its <code>Report<code>s to.  It may be empty, but not
     *          <code>null</code>.
     * @param   hits - the number of criticals on the engine
     * @return  <code>true</code> if the unit's engine exploded,
     *          <code>false</code> if not.
     */
    private boolean checkEngineExplosion(Entity en, Vector vDesc, int hits) {
        if (!(en instanceof Mech)
                && !(en instanceof QuadMech)
                && !(en instanceof BipedMech)) {
            return false;
        }
        if(en.isDoomed() || en.isDestroyed())
            return false;
        Mech mech = (Mech)en;

        //ICE can always explode and roll every time hit
        if (mech.getEngine().isFusion()
                && (!game.getOptions().booleanOption("engine_explosions")
                    || en.rolledForEngineExplosion
                    || en.engineHitsThisRound < 2) )
            return false;
        int explosionBTH = 12;
        if(!mech.getEngine().isFusion()) {
            switch (hits) {
                case 0: 
                    return false;
                case 1:
                    explosionBTH = 10;
                    break;
                case 2:
                    explosionBTH = 7;
                    break;
                case 3:
                default:
                    explosionBTH = 4;
                    break;
            }
        }
        int explosionRoll = Compute.d6(2);
        boolean didExplode = explosionRoll >= explosionBTH;

        Report r;
        r = new Report(6150);
        r.subject = en.getId();
        r.indent(2);
        r.addDesc(en);
        r.add(en.engineHitsThisRound);
        vDesc.addElement(r);
        r = new Report(6155);
        r.subject = en.getId();
        r.indent(2);
        r.add(explosionBTH);
        r.add(explosionRoll);
        vDesc.addElement(r);
        en.rolledForEngineExplosion = true;

        if ( !didExplode ) {
            //whew!
            r = new Report(6160);
            r.subject = en.getId();
            r.indent(2);
            vDesc.addElement(r);
        } else {
            r = new Report(6165, Report.PUBLIC);
            r.subject = en.getId();
            r.indent(2);
            vDesc.addElement(r);
            vDesc.addAll( destroyEntity(en, "engine explosion", false, false));
            //kill the crew
            en.getCrew().setDoomed(true);

            //This is a hack so MM.NET marks the mech as not salvageable
            if ( en instanceof Mech )
                destroyLocation(en, Mech.LOC_CT);

            //Light our hex on fire
            final IHex curHex = game.getBoard().getHex(en.getPosition());

            if ((null != curHex)
                    && !curHex.containsTerrain(Terrains.FIRE)
                    && (curHex.containsTerrain(Terrains.WOODS)
                    || curHex.containsTerrain(Terrains.JUNGLE))) {
                curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                r = new Report(6170, Report.PUBLIC);
                r.subject = en.getId();
                r.indent(2);
                r.add(en.getPosition().getBoardNum());
                vDesc.addElement(r);
                sendChangedHex(en.getPosition());
            }
          
            //ICE explosions don't hurt anyone else, but fusion do
            if (mech.getEngine().isFusion()) {
                //Nuke anyone that is in our hex
                Enumeration entitesWithMe = game.getEntities(en.getPosition());
                Hashtable entitesHit = new Hashtable();

                entitesHit.put(en, en);
    
                while (entitesWithMe.hasMoreElements()) {
                    Entity entity = (Entity)entitesWithMe.nextElement();
                    if ( entity.equals(en) )
                        continue;
                    vDesc.addAll( destroyEntity(entity, "engine explosion proximity", false, false));
                    // Kill the crew
                    entity.getCrew().setDoomed(true);
                    entitesHit.put(entity, entity);
                }
    
                //Now we damage people near us
                int engineRating = en.getEngine().getRating();
                int[] damages = { 999, (engineRating / 10), (engineRating / 20), (engineRating / 40) };
                Vector entites = game.getEntitiesVector();
                for (int i = 0; i < entites.size(); i++) {
                    Entity entity = (Entity)entites.elementAt(i);
    
                    if (entitesHit.containsKey(entity))
                        continue;
    
                    if ( entity.isDoomed() || entity.isDestroyed() || !entity.isDeployed() )
                        continue;

                    int range = en.getPosition().distance(entity.getPosition());

                    if ( range > 3 )
                        continue;

                    int damage = damages[range];

                    r = new Report(6175);
                    r.subject = entity.getId();
                    r.indent(2);
                    r.addDesc(entity);
                    r.add(damage);
                    r.newlines = 0;
                    vDesc.addElement(r);

                    while (damage > 0) {
                        int cluster = Math.min(5, damage);
                        HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, Compute.targetSideTable(en, entity));
                        vDesc.addAll( damageEntity(entity, hit, cluster));
                        damage -= cluster;
                    }
                    Report.addNewline(vDesc);
                }
            }
        }

        return didExplode;
    }

    /**
     * Apply a single critical hit.
     *
     * The following private member of Server are accessed from this function,
     * preventing it from being factored out of the Server class:
     * destroyEntity()
     * destroyLocation()
     * checkEngineExplosion()
     * damageCrew()
     * explodeEquipment()
     * game
     *
     * @param   en the <code>Entity</code> that is being damaged.
     *          This value may not be <code>null</code>.
     * @param   loc the <code>int</code> location of critical hit.
     *          This value may be <code>Entity.NONE</code> for hits
     *          to <code>Tank</code>s and for hits to a <code>Protomech</code>
     *          torso weapon.
     * @param   cs the <code>CriticalSlot</code> being damaged.
     *          This value may not be <code>null</code>.
     *          For critical hits on a <code>Tank</code>, the index of
     *          the slot should be the index of the critical hit table.
     * @param   secondaryEffects the <code>boolean</code> flag that indicates
     *          whether to allow critical hits to cause secondary effects (such
     *          as triggering an ammo explosion, sending hovercraft to watery
     *          graves, or damaging Protomech torso weapons). This value is
     *          normally <code>true</code>, but it will be <code>false</code>
     *          when the hit is being applied from a saved game or scenario.
     */
    public Vector applyCriticalHit( Entity en, int loc, CriticalSlot cs,
                                     boolean secondaryEffects ) {
        Vector vDesc = new Vector();
        Report r;

        // Handle hits on "critical slots" of tanks.
        if ( en instanceof Tank ) {
            Tank tank = (Tank)en;
            VTOL vtol = null;
            if (en instanceof VTOL)
                vtol = (VTOL)en;
            r = new Report(6180);
            r.subject = en.getId();
            r.indent(3);
            r.newlines = 0;
            vDesc.addElement(r);
            switch ( cs.getIndex() ) {
                case 1 : //crew stunned, or killed if VTOL
                    if (vtol == null) {
                        r = new Report(6185);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                        // Carried units can't unload from a stunned transport.
                        // Units that escape a transport don't need to un-stun.
                        tank.stunCrew();
                    } else { //VTOL's suffer crew death instead
                        r = new Report(6190);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                        vDesc.addAll(
                                              destroyEntity(vtol, "crew death", true));
                        en.getCrew().setDoomed(true);
                        vDesc.addAll( crashVTOL(vtol));
                    }
                    break;
                case 2 : //this one's ridiculous.  the 'main weapon' jams.
                    Mounted mWeap = tank.getMainWeapon();
                    if (mWeap == null) { //no main weapon, no crit
                        r = new Report(6195);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    }
                    else {
                        r = new Report(6200);
                        r.subject = en.getId();
                        r.add(mWeap.getName());
                        int jamTurns = tank.getJammedTurns() + 1;
                        if ( jamTurns > 1 ) {
                            r.messageId = 6205;
                            r.add(jamTurns);
                        }
                        r.newlines = 0;
                        vDesc.addElement(r);
                        tank.setJammedTurns( jamTurns );
                    }
                    break;
                case 3 : //engine destroyed
                    r = new Report(6210);
                    r.subject = en.getId();
                    r.newlines = 0;
                    vDesc.addElement(r);
                    tank.immobilize();
                    // Does the hovercraft sink?
                    // Sinking immobile hovercraft is a secondary effect
                    // and does not occur when loading from a scenario.
                    if ( secondaryEffects ) {
                        IHex te_hex = game.getBoard().getHex( en.getPosition() );
                        if (vtol == null) {
                            if ( en.getMovementMode() == IEntityMovementMode.HOVER
                                 && te_hex.terrainLevel(Terrains.WATER) > 0
                                 && !(te_hex.containsTerrain(Terrains.ICE))) {
                                vDesc.addAll(
                                                      destroyEntity(en,"a watery grave", false));
                            }
                        } else { //VTOLs may land or crash
                            //TODO: Nothing if VTOL is landed.  If over clear,
                            // paved, rough, or building, VTOL must make PSR
                            // to land or crash.  Other terrain is automatic
                            // crash.
                            Report.addNewline(vDesc);
                            //report problem: add 3 tabs
                            vDesc.addAll( crashVTOL(vtol));
                        }
                    }
                    break;
                case 4 : //crew killed
                    r = new Report(6190);
                    r.subject = en.getId();
                    r.newlines = 0;
                    vDesc.addElement(r);
                    vDesc.addAll(
                        destroyEntity(en, "crew death", true));
                    en.getCrew().setDoomed(true);
                    if (vtol != null) { //VTOL's crash too
                        Report.addNewline(vDesc);
                        //report problem: add 3 tabs
                        vDesc.addAll( crashVTOL(vtol));
                    }
                    break;
                case 5 : //fuel tank/engine shielding, vehicle explodes
                    r = new Report(6215);
                    r.subject = en.getId();
                    r.newlines = 0;
                    vDesc.addElement(r);
                    if (vtol == null) {
                        vDesc.addAll(destroyEntity(en, "fuel tank explosion", false, false));
                    } else { //VTOL's explode and scatter burning fuel
                        Report.addNewline(vDesc);
                        //report problem: add 3 tabs
                        vDesc.addAll( explodeVTOL(vtol));
                    }
                    en.getCrew().setDoomed(true);
                    break;
                case 6 : //power plant hit, vehicle explodes
                    r = new Report(6220);
                    r.subject = en.getId();
                    r.newlines = 0;
                    vDesc.addElement(r);
                    boolean hasCASE = en.locationHasCase(Tank.LOC_BODY);
                    if (vtol == null) {
                        vDesc.addAll(destroyEntity(en, "power plant destruction", hasCASE, hasCASE));
                    } else { //VTOL's explode and scatter burning fuel
                        Report.addNewline(vDesc);
                        //report problem: add 3 tabs
                        vDesc.addAll( explodeVTOL(vtol));
                    }
                    en.getCrew().setDoomed(!hasCASE);
                    break;
            }

        } // End entity-is-tank

        // Handle critical hits on system slots.
        else if ( CriticalSlot.TYPE_SYSTEM == cs.getType() ) {
            cs.setHit(true);
            if (en instanceof Protomech) {
                int numHit=((Protomech)en).getCritsHit(loc);
                if ( cs.getIndex() != Protomech.SYSTEM_TORSO_WEAPON_A &&
                     cs.getIndex() != Protomech.SYSTEM_TORSO_WEAPON_B ) {
                    r = new Report(6225);
                    r.subject = en.getId();
                    r.indent(3);
                    r.newlines = 0;
                    r.add(Protomech.systemNames[cs.getIndex()]);
                    vDesc.addElement(r);
                }
                switch (cs.getIndex()) {
                case Protomech.SYSTEM_HEADCRIT:
                    if (2==numHit) {
                        r = new Report(6230);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                        destroyLocation(en, loc);
                    }
                    break;
                case Protomech.SYSTEM_ARMCRIT:
                    if (2==numHit) {
                        r = new Report(6235);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                        destroyLocation(en,loc);
                    }
                    break;
                case Protomech.SYSTEM_LEGCRIT:
                    if (3==numHit) {
                        r = new Report(6240);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                        destroyLocation(en,loc);
                    }
                    break;
                case Protomech.SYSTEM_TORSOCRIT:
                    if (3==numHit) {
                        vDesc.addAll(
                            destroyEntity(en, "torso destruction"));
                    }
                    // Torso weapon hits are secondary effects and
                    // do not occur when loading from a scenario.
                    else if ( secondaryEffects ) {
                        int tweapRoll=Compute.d6(1);
                        CriticalSlot newSlot = null;
                        switch (tweapRoll) {
                        case 1:
                        case 2:
                            newSlot = new CriticalSlot
                                ( CriticalSlot.TYPE_SYSTEM,
                                  Protomech.SYSTEM_TORSO_WEAPON_A );
                            vDesc.addAll(
                                applyCriticalHit(en, Entity.NONE,
                                                  newSlot, secondaryEffects));
                            break;
                        case 3:
                        case 4:
                            newSlot = new CriticalSlot
                                ( CriticalSlot.TYPE_SYSTEM,
                                  Protomech.SYSTEM_TORSO_WEAPON_B );
                            vDesc.addAll(
                                applyCriticalHit(en, Entity.NONE,
                                                  newSlot, secondaryEffects));
                            break;
                        case 5:
                        case 6:
                            //no effect
                        }
                    }
                    break;
                case Protomech.SYSTEM_TORSO_WEAPON_A:
                    Mounted weaponA =( (Protomech) en ).getTorsoWeapon(true);
                    if ( null != weaponA ) {
                        weaponA.setHit(true);
                        r = new Report(6245);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    }
                    break;
                case Protomech.SYSTEM_TORSO_WEAPON_B:
                    Mounted weaponB = ( (Protomech) en ).getTorsoWeapon(false);
                    if ( null != weaponB ) {
                        weaponB.setHit(true);
                        r = new Report(6250);
                        r.subject = en.getId();
                        r.newlines = 0;
                        vDesc.addElement(r);
                    }
                    break;


                } // End switch( cs.getType() )

                // Shaded hits cause pilot damage.
                if ( ((Protomech)en).shaded(loc, numHit) ) {
                    // Destroyed Protomech sections have
                    // already damaged the pilot.
                    int pHits =
                        Protomech.POSSIBLE_PILOT_DAMAGE[ loc ] -
                        ((Protomech)en).getPilotDamageTaken( loc );
                    if (  Math.min(1, pHits) > 0 ) {
                        Report.addNewline(vDesc);
                        vDesc.addAll(
                                              damageCrew(en, 1));
                        pHits = 1 + ((Protomech)en)
                            .getPilotDamageTaken( loc );
                        ((Protomech)en).setPilotDamageTaken
                            ( loc, pHits );
                    }
                } // End have-shaded-hit

            } // End entity-is-protomech
            else {
                r = new Report(6225);
                r.subject = en.getId();
                r.indent(3);
                r.add(((Mech)en).getSystemName(cs.getIndex()));
                r.newlines = 0;
                vDesc.addElement(r);
                switch(cs.getIndex()) {
                case Mech.SYSTEM_COCKPIT :
                    // Don't kill a pilot multiple times.
                    if ( Pilot.DEATH > en.getCrew().getHits() ) {
                        // boink!
                        en.getCrew().setDoomed(true);
                        Report.addNewline(vDesc);
                        vDesc.addAll(
                            destroyEntity(en, "pilot death", true));
                    }
                    break;
                case Mech.SYSTEM_ENGINE :
                    en.engineHitsThisRound++;

                    boolean engineExploded = false;

                    int numEngineHits = 0;
                    numEngineHits += en.getHitCriticals
                        (CriticalSlot.TYPE_SYSTEM,
                         Mech.SYSTEM_ENGINE, Mech.LOC_CT);
                    numEngineHits += en.getHitCriticals
                        (CriticalSlot.TYPE_SYSTEM,
                         Mech.SYSTEM_ENGINE, Mech.LOC_RT);
                    numEngineHits += en.getHitCriticals
                        (CriticalSlot.TYPE_SYSTEM,
                         Mech.SYSTEM_ENGINE, Mech.LOC_LT);

                    engineExploded = checkEngineExplosion(en, vDesc, numEngineHits);
                    if ( !engineExploded && numEngineHits > 2 ) {
                        // third engine hit
                        vDesc.addAll(
                            destroyEntity(en, "engine destruction"));
                        if (game.getOptions().booleanOption("auto_abandon_unit")) {
                            vDesc.addAll(abandonEntity(en));
                        }
                    }
                    break;
                case Mech.SYSTEM_GYRO :
                    if (en.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                           Mech.SYSTEM_GYRO, loc) > 1) {
                                // gyro destroyed
                        game.addPSR( new PilotingRollData
                            (en.getId(), PilotingRollData.AUTOMATIC_FAIL,
                             3, "gyro destroyed") );
                    } else {
                                // first gyro hit
                        game.addPSR( new PilotingRollData
                            (en.getId(), 3, "gyro hit") );
                    }
                    break;
                case Mech.ACTUATOR_UPPER_LEG :
                case Mech.ACTUATOR_LOWER_LEG :
                case Mech.ACTUATOR_FOOT :
                    // leg/foot actuator piloting roll
                    game.addPSR( new PilotingRollData
                        (en.getId(), 1, "leg/foot actuator hit") );
                    break;
                case Mech.ACTUATOR_HIP :
                    // hip piloting roll
                    game.addPSR( new PilotingRollData
                        (en.getId(), 2, "hip actuator hit") );
                    break;
                }

            } // End entity-is-mek

        } // End crit-on-system-slot

        // Handle critical hits on equipment slots.
        else if ( CriticalSlot.TYPE_EQUIPMENT == cs.getType() ) {
            cs.setHit(true);
            Mounted mounted = en.getEquipment(cs.getIndex());
            EquipmentType eqType = mounted.getType();
            boolean hitBefore = mounted.isHit();

            r = new Report(6225);
            r.subject = en.getId();
            r.indent(3);
            r.add(mounted.getDesc());
            r.newlines = 0;
            vDesc.addElement(r);
            
            //Shield objects are not useless when they take one crit.
            //Shields can be critted and still be usable.
            if (eqType instanceof MiscType && ((MiscType)eqType).isShield())
                mounted.setHit(false);
            else
                mounted.setHit(true);
            
            if (eqType instanceof MiscType && eqType.hasFlag(MiscType.F_HARJEL)) {
                r = new Report(6254);
                r.subject = en.getId();
                r.indent(2);
                breachLocation(en, loc, null, true);
            }

            // If the item is the ECM suite of a Mek Stealth system
            // then it's destruction turns off the stealth.
            if ( !hitBefore && eqType instanceof MiscType &&
                 eqType.hasFlag(MiscType.F_ECM) &&
                 mounted.getLinkedBy() != null ) {
                Mounted stealth = mounted.getLinkedBy();
                r = new Report(6255);
                r.subject = en.getId();
                r.indent(2);
                r.add(stealth.getType().getName());
                r.newlines = 0;
                vDesc.addElement(r);
                stealth.setMode( "Off" );
            }

            // Handle equipment explosions.
            // Equipment explosions are secondary effects and
            // do not occur when loading from a scenario.
            if ( secondaryEffects && eqType.isExplosive() && !hitBefore ) {
                vDesc.addAll(explodeEquipment(en, loc, mounted));
            }

            // Make sure that ammo in this slot is exhaused.
            if ( mounted.getShotsLeft() > 0 ) {
                mounted.setShotsLeft(0);
            }

        } // End crit-on-equipment-slot
        // mechs with TSM hit by anti-tsm missiles this round get another crit
        if (en instanceof Mech && en.hitThisRoundByAntiTSM) {
            Mech mech = (Mech)en;
            if (mech.hasTSM()) {
                r = new Report(6430);
                r.subject = en.getId();
                r.indent(2);
                r.addDesc(en);
                r.newlines = 0;
                vDesc.addElement(r);
                vDesc.addAll(oneCriticalEntity(en, Compute.d6(2)));
            }
            en.hitThisRoundByAntiTSM = false;
        }

        // Return the results of the damage.
        return vDesc;
    }

    /**
     * Rolls and resolves critical hits with no die roll modifiers.
     */
    private Vector criticalEntity(Entity en, int loc) {
        return criticalEntity(en, loc, 0, true);
    }

    private Vector criticalEntity(Entity en, int loc, int critMod) {
        return criticalEntity(en, loc, critMod, true);
    }

    /**
     * Rolls one critical hit
     */
    private Vector oneCriticalEntity(Entity en, int loc) {
        return criticalEntity(en, loc, 0, false);
    }

    private Vector crashVTOL(VTOL en,Coords crashPos,int curElevation) {
        return crashVTOL(en, false, 0,crashPos,curElevation,0);
    }
    private Vector crashVTOL(VTOL en) {
        return crashVTOL(en, false, 0 , en.getPosition(),en.getElevation(),0);
    }

    /**
     * Crash a VTOL
     * @param en             The <code>VTOL</code> to crash
     * @param sideSlipCrash  A <code>boolean</code> value indicating wether this
     *                       is a sideslip crash or not.
     * @param hexesMoved     The <code>int</code> number of hexes moved.
     * @param crashPos       The <code>Coords</code> of the crash
     * @param crashElevation The <code>int</code> elevation of the VTOL
     * @param impactSide     The <code>int</code> describing the side on which
     *                       the VTOL falls
     * @return               a <code>Vector</code> of Reports.
     */
    private Vector crashVTOL(VTOL en, boolean sideSlipCrash, int hexesMoved, Coords crashPos, int crashElevation,int impactSide) {
        Vector vDesc = new Vector();
        Report r;

        if(!sideSlipCrash) {
            //report lost movement and crashing
            r = new Report(6260);
            r.subject = en.getId();
            r.newlines = 0;
            r.addDesc(en);
            vDesc.addElement(r);
            int newElevation = 0;
            IHex fallHex = game.getBoard().getHex(crashPos);

            //May land on roof of building or bridge
            if(fallHex.containsTerrain(Terrains.BLDG_ELEV))
                newElevation = fallHex.terrainLevel(Terrains.BLDG_ELEV);
            else if(fallHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                newElevation = fallHex.terrainLevel(Terrains.BRIDGE_ELEV);
                if(newElevation > crashElevation)
                    newElevation = 0; //vtol was under bridge already
            }

            int fall = crashElevation - newElevation;
            if(fall==0) {
                //already on ground, no harm done
                r = new Report(6265);
                r.subject = en.getId();
                vDesc.addElement(r);
            } else {
                //set elevation 1st to avoid multiple crashes
                en.setElevation(newElevation);

                //plummets to ground
                r = new Report(6270);
                r.subject = en.getId();
                r.add(fall);
                vDesc.addElement(r);

                // facing after fall
                String side;
                int table;
                int facing = Compute.d6();
                switch(facing) {
                    case 1:
                    case 2:
                        side = "right side";
                        table = ToHitData.SIDE_RIGHT;
                        break;
                    case 3:
                        side = "rear";
                        table = ToHitData.SIDE_REAR;
                        break;
                    case 4:
                    case 5:
                        side = "left side";
                        table = ToHitData.SIDE_LEFT;
                        break;
                    case 0:
                    default:
                        side = "front";
                        table = ToHitData.SIDE_FRONT;
                }

                if(newElevation <= 0) {
                    boolean waterFall= fallHex.containsTerrain(Terrains.WATER);
                    if(waterFall && fallHex.containsTerrain(Terrains.ICE)) {
                        int roll = Compute.d6(1);
                        r = new Report(2118);
                        r.subject = en.getId();
                        r.add(en.getDisplayName(), true);
                        r.add(roll);
                        r.subject = en.getId();
                        addReport(r);
                        if(roll == 6) {
                            resolveIceBroken(crashPos);
                        } else {
                            waterFall = false; //saved by ice
                        }
                    }
                    if(waterFall) {
                        //falls into water and is destroyed
                        r = new Report(6275);
                        r.subject = en.getId();
                        vDesc.addElement(r);
                        en.destroy("Fell into water",false, false);//not sure, is this salvagable?
                    }
                }

                // calculate damage for hitting the surface
                int damage = (int)Math.round(en.getWeight() / 10.0) * (fall + 1);

                // adjust damage for gravity
                damage = Math.round(damage * game.getOptions().floatOption("gravity"));
                // report falling
                r = new Report(6280);
                r.subject = en.getId();
                r.indent();
                r.addDesc(en);
                r.add(side);
                r.add(damage);
                r.newlines = 0;
                vDesc.addElement(r);

                en.setFacing((en.getFacing() + (facing - 1)) % 6);

                boolean exploded=false;
    
                // standard damage loop
                while (damage > 0) {
                    int cluster = Math.min(5, damage);
                    HitData hit = en.rollHitLocation(ToHitData.HIT_NORMAL, table);
                    int ISBefore[]={en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT), en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR)};//hack?
                    vDesc.addAll(
                                          damageEntity(en, hit, cluster));
                    int ISAfter[]={en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT), en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR)};
                    for(int x=0;x<=3;x++) {
                        if(ISBefore[x]!=ISAfter[x]) {
                            exploded=true;
                        }
                    }
                    damage -= cluster;
                }
                if (exploded) {
                    r = new Report(6285);
                    r.subject = en.getId();
                    r.addDesc(en);
                    vDesc.addElement(r);
                    vDesc.addAll( explodeVTOL(en));
                }

                //check for location exposure
                doSetLocationsExposure(en, fallHex, false, newElevation);
                
            }
        } else {
            en.setElevation(0);//considered landed in the hex.
            //crashes into ground thanks to sideslip
            r = new Report(6290);
            r.subject = en.getId();
            r.addDesc(en);
            vDesc.addElement(r);
            int damage = (int)Math.round(en.getWeight() / 10.0) * (hexesMoved + 1);
            boolean exploded=false;

            // standard damage loop
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = en.rollHitLocation(ToHitData.HIT_NORMAL, impactSide);
                int ISBefore[]={en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT), en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR)};//hack?
                vDesc.addAll( damageEntity(en, hit, cluster));
                int ISAfter[]={en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT), en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR)};
                for(int x=0;x<=3;x++) {
                    if(ISBefore[x]!=ISAfter[x]) {
                        exploded=true;
                    }
                }
                damage -= cluster;
            }
            if(exploded) {
                r = new Report(6295);
                r.subject = en.getId();
                r.addDesc(en);
                vDesc.addElement(r);
                vDesc.addAll( explodeVTOL(en));
            }

        }
        return vDesc;

    }

    /**
     * Explode a VTOL
     * @param en  The <code>VTOL</code> to explode.
     * @return a <code>Vector</code> of reports
     */
    private Vector explodeVTOL(VTOL en) {
        Vector vDesc = new Vector();
        Report r;

        if(en.getEngine().isFusion()) {
            //fusion engine, no effect
            r = new Report(6300);
            r.subject = en.getId();
            vDesc.addElement(r);
        } else {
            Coords pos=en.getPosition();
            IHex hex = game.getBoard().getHex(pos);
            if(hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE)) {
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            } else {
                game.getBoard().addInfernoTo(pos, InfernoTracker.STANDARD_ROUND, 1);
                ((InfernoTracker)(game.getBoard().getInfernos().get(pos))).setTurnsLeftToBurn(game.getBoard().getInfernoBurnTurns(pos)-game.getBoard().getInfernoIVBurnTurns(pos)-2);  //massive hack
            }
        }

        return vDesc;
    }

    /**
     * Rolls and resolves critical hits on mechs or vehicles.
     * if rollNumber is false, a single hit is applied - needed for
     * MaxTech Heat Scale rule.
     */
    private Vector criticalEntity(Entity en, int loc, int critMod, boolean rollNumber) {
        CriticalSlot slot = null;
        Vector vDesc = new Vector();
        Report r;
        Coords coords = en.getPosition();
        IHex hex = null;
        int hits;
        if (rollNumber) {
            if (null != coords) hex = game.getBoard().getHex(coords);
            r = new Report(6305);
            r.subject = en.getId();
            r.indent(2);
            r.add(en.getLocationAbbr(loc));
            r.newlines = 0;
            vDesc.addElement(r);
            hits = 0;
            int roll = Compute.d6(2);
            r = new Report(6310);
            r.subject = en.getId();
            String rollString = new String();
            if ( critMod != 0 ) {
                rollString = "(" + roll;
                if ( critMod > 0 ) {
                    rollString += "+";
                }
                rollString += critMod + ") = ";
                roll += critMod;
            }
            rollString += roll;
            r.add(rollString);
            r.newlines = 0;
            vDesc.addElement(r);
            if (roll <= 7) {
                //no effect
                r = new Report(6005);
                r.subject = en.getId();
                r.newlines = 0;
                vDesc.addElement(r);
                return vDesc;
            } else if (roll >= 8 && roll <= 9) {
                hits = 1;
                r = new Report(6315);
                r.subject = en.getId();
                r.newlines = 0;
                vDesc.addElement(r);
            } else if (roll >= 10 && roll <= 11) {
                hits = 2;
                r = new Report(6320);
                r.subject = en.getId();
                r.newlines = 0;
                vDesc.addElement(r);
            } else if (roll == 12) {
                if (en instanceof Tank || en instanceof Protomech) {
                    hits = 3;
                    r = new Report(6325);
                    r.subject = en.getId();
                    r.newlines = 0;
                    vDesc.addElement(r);
                } else if (en.locationIsLeg(loc)) {
                    //limb blown off
                    r = new Report(6120);
                    r.subject = en.getId();
                    r.add(en.getLocationName(loc));
                    r.newlines = 0;
                    vDesc.addElement(r);
                    if (en.getInternal(loc) > 0) {
                        destroyLocation(en, loc);
                    }
                    if (null != hex) {
                        if (!hex.containsTerrain (Terrains.LEGS)) {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, 1));
                        }
                        else {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain
                                            (Terrains.LEGS,
                                             hex.terrainLevel(Terrains.LEGS)+1));
                        }
                    }
                    sendChangedHex(en.getPosition());
                    return vDesc;
                } else if (loc == Mech.LOC_RARM || loc == Mech.LOC_LARM) {
                    //limb blown off
                    r = new Report(6120);
                    r.subject = en.getId();
                    r.add(en.getLocationName(loc));
                    r.newlines = 0;
                    vDesc.addElement(r);
                    destroyLocation(en, loc);
                    if (null != hex) {
                        if (!hex.containsTerrain( Terrains.ARMS)) {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, 1));
                        }
                        else {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain
                                            (Terrains.ARMS,
                                             hex.terrainLevel(Terrains.ARMS)+1));
                        }
                    }
                    sendChangedHex(en.getPosition());
                    return vDesc;
                } else if (loc == Mech.LOC_HEAD) {
                    //head blown off
                    r = new Report(6330);
                    r.subject = en.getId();
                    r.add(en.getLocationName(loc));
                    r.newlines = 0;
                    vDesc.addElement(r);
                    destroyLocation(en, loc);
                    // Don't kill a pilot multiple times.
                    if ( Pilot.DEATH > en.getCrew().getHits() ) {
                        en.crew.setDoomed(true);
                        Report.addNewline(vDesc);
                        vDesc.addAll( destroyEntity(en, "pilot death", true));
                    }
                    return vDesc;
                } else {
                    // torso hit
                    hits = 3;
                    r = new Report(6325);
                    r.subject = en.getId();
                    r.newlines = 0;
                    vDesc.addElement(r);
                }
            }
        } else {
            hits = 1;
        }

        // vehicle handle crits in their own 'special' way
        if (en instanceof Tank) {
            Tank tank = (Tank)en;
            for (int x = 0; x < hits; x++) {
                slot = new CriticalSlot( CriticalSlot.TYPE_SYSTEM,
                                         Compute.d6(1) );
                vDesc.addAll( applyCriticalHit(en, Entity.NONE, slot, true));
            }
        }
        else {
            // transfer criticals, if needed
            while (hits > 0 && en.canTransferCriticals(loc)
                   && en.getTransferLocation(loc) != Entity.LOC_DESTROYED
                   && en.getTransferLocation(loc) != Entity.LOC_NONE) {
                loc = en.getTransferLocation(loc);
                r = new Report(6335);
                r.subject = en.getId();
                r.indent(3);
                r.add(en.getLocationAbbr(loc));
                r.newlines = 0;
                vDesc.addElement(r);
            }

            // Roll critical hits in this location.
            while (hits > 0) {

                // Have we hit all available slots in this location?
                if (en.getHittableCriticals(loc) <= 0) {
                    r = new Report(6340);
                    r.subject = en.getId();
                    r.indent(3);
                    r.newlines = 0;
                    vDesc.addElement(r);
                    break;
                }

                // Randomly pick a slot to be hit.
                int slotIndex = Compute.randomInt
                    ( en.getNumberOfCriticals(loc) );
                slot = en.getCritical(loc, slotIndex);

                // Ignore empty or unhitable slots (this
                // includes all previously hit slots).
                
                if (slot != null && slot.isHittable()) {
                    // if explosive use edge
                    if ((en instanceof Mech) 
                       && ( en.crew.hasEdgeRemaining()
                            && en.crew.getOptions().booleanOption("edge_when_explosion"))
                       && en.getEquipment(slot.getIndex()).getType().isExplosive()) {
                       en.crew.decreaseEdge();
                       r = new Report(6530);
                       r.subject = en.getId();
                       r.indent(3);
                       r.newlines = 0;
                       r.add(en.crew.getOptions().intOption("edge"));
                       vDesc.addElement(r);
                       continue;
                    }
                    vDesc.addAll( applyCriticalHit(en, loc, slot, true));
                    hits--;
                }

            } // Hit another slot in this location.
        }

        return vDesc;
    }

    /**
     * Checks for location breach and returns phase logging.
     * <p/>
     * Please note that dependent locations ARE NOT considered breached!
     *
     * @param   entity the <code>Entity</code> that needs to be checked.
     * @param   loc the <code>int</code> location on the entity that needs
     *          to be checked for a breach.
     * @param   hex the <code>IHex</code> the enitity occupies when checking
     *          This value will be <code>null</code> if the check is the
     *          result of an attack, and non-null if it occurs during movement.
     */
    private Vector breachCheck(Entity entity, int loc, IHex hex) {
        Vector vDesc = new Vector();
        Report r;

        // BattleArmor does not breach
        if (entity instanceof Infantry) {
            return vDesc;
        }

        if (entity instanceof VTOL) {
            return vDesc;
        }
        
        // functional HarJel prevents breach
        if (entity instanceof Mech && ((Mech)entity).hasHarJelIn(loc)) {
            r = new Report(6342);
            r.subject = entity.getId();
            r.indent(3);
            vDesc.addElement(r);
            return vDesc;
        }

        // This handles both water and vacuum breaches.
        if (entity.getLocationStatus(loc) > ILocationExposureStatus.NORMAL) {
            // Does the location have armor (check rear armor on Mek)
            // and is the check due to damage?
            int breachroll = 0;
            if (entity.getArmor(loc) > 0 &&
                (entity instanceof Mech ? (entity.getArmor(loc,true)>0) : true)
                && null == hex) {
                breachroll = Compute.d6(2);
                r = new Report(6345);
                r.subject = entity.getId();
                r.indent(3);
                r.add(entity.getLocationAbbr(loc));
                r.add(breachroll);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            // Breach by damage or lack of armor.
            if ( breachroll >= 10
                 || !(entity.getArmor(loc) > 0)
                 || !(entity instanceof Mech ? (entity.getArmor(loc,true)>0) :
                      true) ) {
                vDesc.addAll( breachLocation(entity, loc, hex, false));
            }
        }
        return vDesc;
    }

    /**
     * Marks all equipment in a location on an entity as useless.
     *
     * @param   entity the <code>Entity</code> that needs to be checked.
     * @param   loc the <code>int</code> location on the entity that needs
     *          to be checked for a breach.
     * @param   hex the <code>IHex</code> the enitity occupies when checking
     *          This value will be <code>null</code> if the check is the
     *          result of an attack, and non-null if it occurs during movement.
     * @param   harJel a <code>boolean</code> value indicating if the uselessness
     *          is the cause of a critically hit HarJel system
     */
    private Vector breachLocation(Entity entity, int loc, IHex hex, boolean harJel) {
        Vector vDesc = new Vector();
        Report r;

        if (entity.getInternal(loc) < 0 ||
            entity.getLocationStatus(loc) < ILocationExposureStatus.NORMAL) {
            //already destroyed or breached? don't bother
            return vDesc;
        }

        r = new Report(6350);
        if (harJel) r.messageId = 6351;
        r.subject = entity.getId();
        r.add(entity.getShortName());
        r.add(entity.getLocationAbbr(loc));
        r.newlines = 0;
        vDesc.addElement(r);

        if (entity instanceof Tank) {
            vDesc.addAll(
                destroyEntity(entity, "hull breach", true, true));
            return vDesc;
        }
        // equipment and crits will be marked in applyDamage?

        // equipment marked missing
        for (Enumeration i = entity.getEquipment(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getLocation() == loc) {
                mounted.setBreached(true);
            }
        }
        // all critical slots set as useless
        for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = entity.getCritical(loc, i);
            if (cs != null) {
                // for every undamaged actuator destroyed by breaching,
                // we make a PSR (see bug 1040858)
                if (entity.locationIsLeg(loc)) {
                    if (cs.isHittable()) {
                        switch(cs.getIndex()) {
                            case Mech.ACTUATOR_UPPER_LEG :
                            case Mech.ACTUATOR_LOWER_LEG :
                            case Mech.ACTUATOR_FOOT :
                                // leg/foot actuator piloting roll
                                game.addPSR( new PilotingRollData
                                    (entity.getId(), 1, "leg/foot actuator hit") );
                                break;
                            case Mech.ACTUATOR_HIP :
                                // hip piloting roll (at +0, because we get the +2
                                // anyway because the location is breached
                                // phase report will look a bit weird, but the roll
                                // is correct
                                game.addPSR( new PilotingRollData
                                    (entity.getId(), 0, "hip actuator hit") );
                                break;
                        }
                    }
                }
                cs.setBreached(true);
            }
        }

        //Check location for engine/cockpit breach and report accordingly
        if (loc == Mech.LOC_CT) {
            vDesc.addAll( destroyEntity(entity, "hull breach"));
            if (game.getOptions().booleanOption("auto_abandon_unit")) {
                vDesc.addAll(abandonEntity(entity));
            }
        }
        if (loc == Mech.LOC_HEAD) {
            entity.crew.setDoomed(true);
            vDesc.addAll( destroyEntity(entity, "hull breach"));
            if (entity.getLocationStatus(loc) == ILocationExposureStatus.WET) {
                r = new Report(6355);
                r.subject = entity.getId();
                r.addDesc(entity);
                vDesc.addElement(r);
            } else {
                r = new Report(6360);
                r.subject = entity.getId();
                r.addDesc(entity);
                vDesc.addElement(r);
            }
        }

        // Set the status of the location.
        // N.B. if we set the status before rolling water PSRs, we get a
        // "LEG DESTROYED" modifier; setting the status after gives a hip
        // actuator modifier.
        entity.setLocationStatus(loc, ILocationExposureStatus.BREACHED);

        // Did the hull breach destroy the engine?
        if (entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE,
                               Mech.LOC_LT) +
            entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE,
                               Mech.LOC_CT) +
            entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE,
                               Mech.LOC_RT)
            >= 3) {
            vDesc.addAll( destroyEntity(entity, "engine destruction"));
            if (game.getOptions().booleanOption("auto_abandon_unit")) {
                vDesc.addAll(abandonEntity(entity));
            }
        }

        return vDesc;
    }

    /**
     * Marks all equipment in a location on an entity as destroyed.
     */
    void destroyLocation(Entity en, int loc) {
        // if it's already marked as destroyed, don't bother
        if (en.getInternal(loc) < 0) {
            return;
        }
        // mark armor, internal as doomed
        en.setArmor(IArmorState.ARMOR_DOOMED, loc, false);
        en.setInternal(IArmorState.ARMOR_DOOMED, loc);
        if (en.hasRearArmor(loc)) {
            en.setArmor(IArmorState.ARMOR_DOOMED, loc, true);
        }
        // equipment marked missing
        for (Enumeration i = en.getEquipment(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getLocation() == loc) {
                mounted.setMissing(true);
            }
        }
        // all critical slots set as missing
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if (cs != null) {
                // count engine hits for maxtech engine explosions
                if (cs.getType() == CriticalSlot.TYPE_SYSTEM &&
                    cs.getIndex() == Mech.SYSTEM_ENGINE &&
                    !cs.isDamaged()) {
                        en.engineHitsThisRound++;
                }
                cs.setMissing(true);
            }
        }
        // if it's a leg, the entity falls
        if (en instanceof Mech && en.locationIsLeg(loc)) {
            game.addPSR(new PilotingRollData(en.getId(), PilotingRollData.AUTOMATIC_FAIL, 5, "leg destroyed"));
        }
        // dependent locations destroyed
        if (en.getDependentLocation(loc) != Entity.LOC_NONE) {
            destroyLocation(en, en.getDependentLocation(loc));
        }
    }

    /**
     * Mark the unit as destroyed!  Units transported in the destroyed unit
     * will get a chance to escape.
     *
     * @param   entity - the <code>Entity</code> that has been destroyed.
     * @param   reason - a <code>String</code> detailing why the entity
     *          was destroyed.
     * @return  a <code>Vector</code> of <code>Report</code> objects
     *          that can be sent to the output log.
     */
    private Vector destroyEntity(Entity entity, String reason) {
        return destroyEntity( entity, reason, true );
    }

    /**
     * Marks a unit as destroyed!  Units transported inside the destroyed
     * unit will get a chance to escape unless the destruction was not
     * survivable.
     *
     * @param   entity - the <code>Entity</code> that has been destroyed.
     * @param   reason - a <code>String</code> detailing why the entity
     *          was destroyed.
     * @param   survivable - a <code>boolean</code> that identifies the
     *          desctruction as unsurvivable for transported units.
     * @return  a <code>Vector</code> of <code>Report</code> objects
     *          that can be sent to the output log.
     */
    private Vector destroyEntity(Entity entity, String reason,
                                 boolean survivable) {
        // Generally, the entity can still be salvaged.
        return this.destroyEntity( entity, reason, survivable, true );
    }

    /**
     * Marks a unit as destroyed!  Units transported inside the destroyed
     * unit will get a chance to escape unless the destruction was not
     * survivable.
     *
     * @param   entity - the <code>Entity</code> that has been destroyed.
     * @param   reason - a <code>String</code> detailing why the entity
     *          was destroyed.
     * @param   survivable - a <code>boolean</code> that identifies the
     *          desctruction as unsurvivable for transported units.
     * @param   canSalvage - a <code>boolean</code> that indicates if
     *          the unit can be salvaged (or cannibalized for spare parts).
     *          If <code>true</code>, salvage operations are possible, if
     *          <code>false</code>, the unit is too badly damaged.
     * @return  a <code>Vector</code> of <code>Report</code> objects
     *          that can be sent to the output log.
     */
    private Vector destroyEntity(Entity entity, String reason,
                                 boolean survivable, boolean canSalvage) {
        Vector vDesc = new Vector();
        Report r;

        // The unit can suffer an ammo explosion after it has been destroyed.
        int condition = IEntityRemovalConditions.REMOVE_SALVAGEABLE;
        if ( !canSalvage ) {
            entity.setSalvage( canSalvage );
            condition = IEntityRemovalConditions.REMOVE_DEVASTATED;
        }

        // Destroy the entity, unless it's already destroyed.
        if (!entity.isDoomed() && !entity.isDestroyed()) {
            r = new Report(6365);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(reason);
            r.newlines=0;
            vDesc.addElement(r);

            entity.setDoomed(true);

            // Kill any picked up MechWarriors
            Enumeration iter = entity.getPickedUpMechWarriors().elements();
            while (iter.hasMoreElements() ) {
                Integer mechWarriorId = (Integer)iter.nextElement();
                Entity mw = game.getEntity(mechWarriorId.intValue());
                mw.setDestroyed(true);
                game.removeEntity( mw.getId(), condition );
                this.entityUpdate(mw.getId());
                send( createRemoveEntityPacket(mw.getId(), condition) );
                r = new Report(6370);
                r.subject = mw.getId();
                r.addDesc(mw);
                vDesc.addElement(r);
            }

            // Handle escape of transported units.
            iter = entity.getLoadedUnits().elements();
            if ( iter.hasMoreElements() ) {
                Entity other = null;
                Coords curPos = entity.getPosition();
                IHex entityHex = game.getBoard().getHex( curPos );
                int curFacing = entity.getFacing();
                while ( iter.hasMoreElements() ) {
                    other = (Entity) iter.nextElement();

                    // Can the other unit survive?
                    if ( !survivable ) {

                        // Nope.
                        other.setDestroyed(true);
                        game.moveToGraveyard( other.getId() );
                        this.entityUpdate(other.getId());
                        send( createRemoveEntityPacket(other.getId(),
                                                       condition) );
                        r = new Report(6370);
                        r.subject = other.getId();
                        r.addDesc(other);
                        vDesc.addElement(r);
                    }
                    // Can we unload the unit to the current hex?
                    // TODO : unloading into stacking violation is not
                    //        explicitly prohibited in the BMRr.
                    else if (null != Compute.stackingViolation(game, other.getId(), curPos)
                             || other.isHexProhibited(entityHex) ) {
                        // Nope.
                        other.setDestroyed(true);
                        game.moveToGraveyard( other.getId() );
                        this.entityUpdate(other.getId());
                        send( createRemoveEntityPacket(other.getId(),
                                                       condition) );
                        r = new Report(6375);
                        r.subject = other.getId();
                        r.addDesc(other);
                        vDesc.addElement(r);
                    } // End can-not-unload
                    else {
                        // The other unit survives.
                        this.unloadUnit( entity, other, curPos, curFacing, entity.getElevation() );
                    }

                } // Handle the next transported unit.

            } // End has-transported-unit

            // Handle transporting unit.
            if ( Entity.NONE != entity.getTransportId() ) {
                final Entity transport = game.getEntity
                    ( entity.getTransportId() );
                Coords curPos = transport.getPosition();
                int curFacing = transport.getFacing();
                this.unloadUnit( transport, entity, curPos, curFacing, transport.getElevation() );
                this.entityUpdate( transport.getId() );
            } // End unit-is-transported

            // Is this unit being swarmed?
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );

                // remove the swarmer from the move queue
                game.removeTurnFor(swarmer);
                send(createTurnVectorPacket());

                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                r = new Report(6380);
                r.subject = swarmerId;
                r.addDesc(swarmer);
                vDesc.addElement(r);
                this.entityUpdate( swarmerId );
            }

            // Is this unit swarming somebody?
            final int swarmedId = entity.getSwarmTargetId();
            if ( Entity.NONE != swarmedId ) {
                final Entity swarmed = game.getEntity( swarmedId );
                swarmed.setSwarmAttackerId( Entity.NONE );
                entity.setSwarmTargetId( Entity.NONE );
                r = new Report(6385);
                r.subject = swarmed.getId();
                r.addDesc(swarmed);
                vDesc.addElement(r);
                this.entityUpdate( swarmedId );
            }

        } // End entity-not-already-destroyed.
        
        // update our entity, so clients have correct data
        // needed for MekWars stuff
        this.entityUpdate(entity.getId());

        return vDesc;
    }
    

    /**
     * Makes a piece of equipment on a mech explode!  POW!  This expects either
     * ammo, or an explosive weapon.  Returns a vector of Report objects.
     */
    private Vector explodeEquipment(Entity en, int loc, int slot) {
        return explodeEquipment(en, loc, en.getEquipment(en.getCritical(loc, slot).getIndex()));
    }

    private Vector explodeEquipment(Entity en, int loc, Mounted mounted) {
        Vector vDesc = new Vector();
        // is this already destroyed?
        if (mounted.isDestroyed()) {
            System.err.println("server: explodeEquipment called on destroyed"
            + " equipment (" + mounted.getName() + ")");
            return vDesc;
        }

        // special-case.  RACs only explode when jammed
        if (mounted.getType() instanceof WeaponType &&
                ((WeaponType)mounted.getType()).getAmmoType() == AmmoType.T_AC_ROTARY) {
            if (!mounted.isJammed()) {
                return vDesc;
            }
        }

        // special case.  ACs only explode when firing incendiary ammo
        if (mounted.getType() instanceof WeaponType &&
                ((WeaponType)mounted.getType()).getAmmoType() == AmmoType.T_AC) {
            if (!mounted.isUsedThisRound()) {
                return vDesc;
            }
            Mounted ammo = mounted.getLinked();
            if(ammo == null || !(ammo.getType() instanceof AmmoType) || 
            ((AmmoType)ammo.getType()).getMunitionType() != AmmoType.M_INCENDIARY_AC ) {
                return vDesc;
            }
            
            WeaponType wtype = (WeaponType)mounted.getType();
            if ( ((wtype.getAmmoType() == AmmoType.T_LRM) ||                 
                    (wtype.getAmmoType() == AmmoType.T_LRM_STREAK) ||
                    (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO) ||
                    (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO_COMBO))){
                return vDesc;
            }
            
        }

        // Inferno ammo causes heat buildup as well as the damage
        if (mounted.getType() instanceof AmmoType
                && ((((AmmoType)mounted.getType()).getAmmoType() == AmmoType.T_SRM)
                || (((AmmoType)mounted.getType()).getAmmoType() == AmmoType.T_BA_INFERNO))
                && ((AmmoType)mounted.getType()).getMunitionType() == AmmoType.M_INFERNO
                && mounted.getShotsLeft() > 0) {
            en.heatBuildup += 30;
        }

        // determine and deal damage
        int damage = mounted.getExplosionDamage();

        if (damage <= 0) {
            return vDesc;
        }

        Report r = new Report(6390);
        r.subject = en.getId();
        r.add(mounted.getName());
        r.add(damage);
        r.indent(3);
        r.newlines = 0;
        vDesc.addElement(r);
        mounted.setShotsLeft(0);
        vDesc.addAll( damageEntity(en, new HitData(loc), damage, true));
        Report.addNewline(vDesc);

        
        int pilotDamage = 2;
        if (en.getCrew().getOptions().booleanOption("pain_resistance")) pilotDamage = 1;
        if (en.getCrew().getOptions().booleanOption("iron_man")) pilotDamage = 1;
        vDesc.addAll( damageCrew(en, pilotDamage));
        if ( en.crew.isDoomed() || en.crew.isDead() ) {
            vDesc.addAll( destroyEntity(en, "crew death", true) );
        } else {
            Report.addNewline(vDesc);
        }

        return vDesc;
    }

    /**
     * Makes one slot of ammo, determined by certain rules, explode on a mech.
     */
    private Vector explodeAmmoFromHeat(Entity entity) {
        int damage = 0;
        int rack = 0;
        int boomloc = -1;
        int boomslot = -1;
        Vector vDesc = new Vector();

        for (int j = 0; j < entity.locations(); j++) {
            for (int k = 0; k < entity.getNumberOfCriticals(j); k++) {
                CriticalSlot cs = entity.getCritical(j, k);
                if (cs == null || cs.isDestroyed() || cs.isHit() || cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                    continue;
                }
                Mounted mounted = entity.getEquipment(entity.getCritical(j, k).getIndex());
                if (!(mounted.getType() instanceof AmmoType)) {
                    continue;
                }
                AmmoType atype = (AmmoType)mounted.getType();
                if (!atype.isExplosive()) {
                    continue;
                }
                //ignore empty bins
                if (atype.getShots() == 0) {
                    continue;
                }
                // BMRr, pg. 48, compare one rack's
                // damage.  Ties go to most rounds.
                int newRack = atype.getDamagePerShot() * atype.getRackSize();
                int newDamage = mounted.getExplosionDamage();
                if ( !mounted.isHit() && ( rack < newRack ||
                       (rack == newRack && damage < newDamage) ) ) {
                    rack = newRack;
                    damage = newDamage;
                    boomloc = j;
                    boomslot = k;
                }
            }
        }
        if (boomloc != -1 && boomslot != -1) {
            CriticalSlot slot = entity.getCritical(boomloc, boomslot);
            slot.setHit(true);
            entity.getEquipment(slot.getIndex()).setHit(true);
            vDesc = explodeEquipment(entity, boomloc, boomslot);
        } else {
            //Luckily, there is no ammo to explode.
            Report r = new Report(5105);
            r.subject = entity.getId();
            r.indent();
            vDesc.addElement(r);
        }
        return vDesc;
    }

    /**
     * Makes a mech fall.
     */
    private void doEntityFall(Entity entity, Coords fallPos, int height, int facing, PilotingRollData roll) {
        Report r;

        IHex fallHex = game.getBoard().getHex(fallPos);

        // we don't need to deal damage yet, if the entity is doing DFA
        if (entity.isMakingDfa()) {
            r = new Report(2305);
            r.subject = entity.getId();
            addReport(r);
            entity.setProne(true);
            return;
        }
        // facing after fall
        String side;
        int table;
        switch(facing) {
            case 1:
            case 2:
                side = "right side";
                table = ToHitData.SIDE_RIGHT;
                break;
            case 3:
                side = "rear";
                table = ToHitData.SIDE_REAR;
                break;
            case 4:
            case 5:
                side = "left side";
                table = ToHitData.SIDE_LEFT;
                break;
            case 0:
            default:
                side = "front";
                table = ToHitData.SIDE_FRONT;
        }

        int waterDepth = fallHex.terrainLevel(Terrains.WATER);
        int damageHeight = height;


        // HACK: if the dest hex is water, assume that the fall height given is
        // to the floor of the hex, and modifiy it so that it's to the surface
        if (waterDepth > 0) {
            damageHeight = height - waterDepth;
        } else {
            waterDepth = 0; //because it will be used to set elevation
        }

        if(fallHex.containsTerrain(Terrains.ICE)) {
            waterDepth = 0;
        }
        
        //Falling into water instantly destroys most non-mechs
        if(waterDepth > 0
                && !(entity instanceof Mech)
                && !(entity instanceof Protomech)
                && (entity.getRunMP() > 0 && entity.getMovementMode() != IEntityMovementMode.HOVER)
                && entity.getMovementMode() != IEntityMovementMode.HYDROFOIL
                && entity.getMovementMode() != IEntityMovementMode.NAVAL
                && entity.getMovementMode() != IEntityMovementMode.SUBMARINE) {
            addReport(
                    destroyEntity(entity, "a watery grave", false));
            return;
        }

        // calculate damage for hitting the surface
        int damage = (int)Math.round(entity.getWeight() / 10.0) * (damageHeight + 1);
        // calculate damage for hitting the ground, but only if we actually fell
        // into water
        // if we fell onto the water surface, that damage is halved.
        int waterDamage = 0;
        if (waterDepth > 0) {
            damage /= 2;
            waterDamage = (int)Math.round(entity.getWeight() / 10.0) * (waterDepth + 1) /2;
        }

        // If the waterDepth is larger than the fall height,
        // we fell underwater
        if (waterDepth > height) {
            damage = 0;
            waterDamage = (int)Math.round(entity.getWeight() / 10.0) * (height + 1) /2;
        }
        // adjust damage for gravity
        damage = Math.round(damage * game.getOptions().floatOption("gravity"));
        waterDamage = Math.round(waterDamage * game.getOptions().floatOption("gravity"));

        // report falling
        if (waterDamage == 0) {
            r = new Report(2310);
            r.subject = entity.getId();
            r.indent();
            r.newlines = 0;
            r.addDesc(entity);
            r.add(side); //international issue
            r.add(damage);
        } else if (damage > 0) {
            r = new Report(2315);
            r.subject = entity.getId();
            r.indent();
            r.newlines = 0;
            r.addDesc(entity);
            r.add(side); //international issue
            r.add(damage);
            r.add(waterDamage);
        } else {
            r = new Report(2310);
            r.subject = entity.getId();
            r.indent();
            r.newlines = 0;
            r.addDesc(entity);
            r.add(side); //international issue
            r.add(waterDamage);
        }
        addReport(r);
        damage += waterDamage;

        // Any swarming infantry will be dislodged, but we don't want to
        // interrupt the fall's report.  We have to get the ID now because
        // the fall may kill the entity which will reset the attacker ID.
        final int swarmerId = entity.getSwarmAttackerId();

        // Positioning must be prior to damage for proper handling of breaches
        // Only Mechs can fall prone.
        if ( entity instanceof Mech ) {
            entity.setProne(true);
        }
        entity.setPosition(fallPos);
        entity.setFacing((entity.getFacing() + (facing - 1)) % 6);
        entity.setSecondaryFacing(entity.getFacing());
        entity.setElevation(-waterDepth);
        if (waterDepth > 0) {
            for (int loop=0; loop< entity.locations();loop++){
                entity.setLocationStatus(loop, ILocationExposureStatus.WET);
            }
        }

        // standard damage loop
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, table);
            hit.makeFallDamage(true);
            addReport( damageEntity(entity, hit, cluster));
            damage -= cluster;
        }

        //check for location exposure
        doSetLocationsExposure(entity, fallHex, false, -waterDepth);

        // we want to be able to avoid pilot damage even when it was
        // an automatic fall, only unconsciousness should cause auto-damage
        roll.removeAutos();

        if (height > 0) {
            roll.addModifier(height, "height of fall");
        }

        entity.addPilotingModifierForTerrain(roll, fallPos);

        if (roll.getValue() == PilotingRollData.IMPOSSIBLE) {
            r = new Report(2320);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(entity.crew.getName());
            r.indent();
            addReport(r);
            addReport( damageCrew(entity, 1));
            ((Report) vPhaseReport.elementAt(vPhaseReport.size() - 1)).newlines++;
        } else {
            int diceRoll = Compute.d6(2);
            r = new Report(2325);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(entity.crew.getName());
            r.add(roll.getValueAsString());
            r.add(diceRoll);
            if (diceRoll >= roll.getValue()) {
                r.choose(true);
                addReport(r);
            } else {
                r.choose(false);
                addReport(r);
                addReport( damageCrew(entity, 1));
                ((Report) vPhaseReport.elementAt(vPhaseReport.size() - 1)).newlines++;
            }
        }

        // Now dislodge any swarming infantry.
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            entity.setSwarmAttackerId( Entity.NONE );
            swarmer.setSwarmTargetId( Entity.NONE );
            // Did the infantry fall into water?
            if ( waterDepth > 0 ) {
                // Swarming infantry die.
                swarmer.setPosition( fallPos );
                r = new Report(2330);
                r.newlines = 0;
                r.subject = swarmer.getId();
                r.addDesc(swarmer);
                addReport(r);
                addReport( destroyEntity(swarmer, "a watery grave", false));
            } else {
                // Swarming infantry take an 11 point hit.
                // ASSUMPTION : damage should not be doubled.
                r = new Report(2335);
                r.newlines = 0;
                r.subject = swarmer.getId();
                r.addDesc(swarmer);
                addReport(r);
                addReport( damageEntity(swarmer, swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT), 11));
                ((Report) vPhaseReport.elementAt(vPhaseReport.size() - 1)).newlines++;
            }
            swarmer.setPosition( fallPos );
            entityUpdate( swarmerId );
        } // End dislodge-infantry

        // clear all PSRs after a fall -- the Mek has already failed ONE and fallen, it'd be cruel to make it fail some more!
        game.resetPSRs(entity);
    }

    /**
     * The mech falls into an unoccupied hex from the given height above
     */
    private void doEntityFall(Entity entity, Coords fallPos, int height, PilotingRollData roll) {
        doEntityFall(entity, fallPos, height, Compute.d6(1), roll);
    }

    /**
     * The mech falls down in place
     */
    private void doEntityFall(Entity entity, PilotingRollData roll) {
        doEntityFall(entity, entity.getPosition(), 0, roll);
    }

    /**
     * Report:
     * - Any ammo dumps beginning the following round.
     * - Any ammo dumps that have ended with the end of this round.
     */
    private void resolveAmmoDumps() {
        Report r;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            for (Enumeration j = entity.getAmmo(); j.hasMoreElements(); ) {
                Mounted m = (Mounted)j.nextElement();
                if (m.isPendingDump()) {
                    //report dumping next round
                    r = new Report(5110);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(m.getName());
                    addReport(r);
                    //update status
                    m.setPendingDump(false);
                    m.setDumping(true);
                }
                else if (m.isDumping()) {
                    //report finished dumping
                    r = new Report(5115);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(m.getName());
                    addReport(r);
                    //update status
                    m.setDumping(false);
                    m.setShotsLeft(0);
                }
            }
            entity.reloadEmptyWeapons();
        }
    }

    /**
     * Returns true if the hex is set on fire with the specified roll.  Of
     * course, also checks to see that fire is possible in the specified hex.
     *
     * @param   hex - the <code>IHex</code> to be lit.
     * @param   roll - the <code>int</code> target number for the ignition roll
     * @param   bAnyTerrain - <code>true</code> if the fire can be lit in any
     *          terrain.  If this value is <code>false</code> the hex will be
     *          lit only if it contains Woods,jungle or a Building.
     * @param   entityId - the entityId responsible for the ignite attempt.
     *          If the value is Entity.NONE, then the roll attempt will not
     *          be included in the report.
     */
   public boolean ignite( IHex hex, int roll, boolean bAnyTerrain,
                          int entityId ) {

        // The hex might be null due to spreadFire translation
        // goes outside of the board limit.
        if ( !game.getOptions().booleanOption("fire") || null == hex ) {
            return false;
        }

        // The hex may already be on fire.
        if ( hex.containsTerrain( Terrains.FIRE ) ) {
            return true;
        }

        if ( !bAnyTerrain &&
             !(hex.containsTerrain(Terrains.WOODS)) &&
             !(hex.containsTerrain(Terrains.JUNGLE)) &&
             !(hex.containsTerrain(Terrains.BUILDING)) ) {
            return false;
        }

        int fireRoll = Compute.d6(2);
        if (entityId != Entity.NONE) {
            Report r = new Report(3430);
            r.indent(3);
            r.subject = entityId;
            r.add(roll);
            r.add(fireRoll);
            addReport(r);
        }
        if (fireRoll >= roll) {
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the hex is set on fire with the specified roll.  Of
     * course, also checks to see that fire is possible in the specified hex.
     * This version of the method will not report the attempt roll.
     *
     * @param   hex - the <code>IHex</code> to be lit.
     * @param   roll - the <code>int</code> target number for the ignition roll
     * @param   bAnyTerrain - <code>true</code> if the fire can be lit in any
     *          terrain.  If this value is <code>false</code> the hex will be
     *          lit only if it contains Woods, jungle or a Building.
     */
    public boolean ignite(IHex hex, int roll, boolean bAnyTerrain)  {
       return ignite(hex, roll, bAnyTerrain, Entity.NONE);
    }

    public boolean ignite(IHex hex, int roll) {
        // default signature, assuming only woods can burn
        return ignite(hex, roll, false, Entity.NONE);
    }

    public void removeFire(int x, int y, IHex hex) {
        Coords fireCoords = new Coords(x, y);
        hex.removeTerrain(Terrains.FIRE);
        sendChangedHex(fireCoords);
        if (!game.getOptions().booleanOption("maxtech_fire")) {
            // only remove the 3 smoke hexes if under L2 rules!
            int windDir = game.getWindDirection();
            removeSmoke(x, y, windDir);
            removeSmoke(x, y, (windDir + 1) % 6);
            removeSmoke(x, y, (windDir + 5) % 6);
        }
        //fire goes out due to lack of fuel
        Report r = new Report(5170, Report.PUBLIC);
        r.add(fireCoords.getBoardNum());
        addReport(r);
    }

    /**
     * called when a fire is burning.  Adds smoke to hex in the direction
     * specified.  Called 3 times per fire hex
     * @param x       The <code>int</code> x-coordinate of the hex
     * @param y       The <code>int</code> y-coordinate of the hex
     * @param windDir The <code>int</code> specifying the winddirection
     */
    public void addSmoke(int x, int y, int windDir) {
        Coords smokeCoords = new Coords(Coords.xInDir(x, y, windDir), Coords.yInDir(x, y, windDir));
        IHex nextHex = game.getBoard().getHex(smokeCoords);
        if (nextHex != null && !(nextHex.containsTerrain(Terrains.SMOKE))) {
            nextHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 1));
            sendChangedHex(smokeCoords);
            Report r = new Report(5175, Report.PUBLIC);
            r.add(smokeCoords.getBoardNum());
            addReport(r);
        }
    }

    /**
     * Add lvl3 smoke to the hex specified by the parameters. Called once.
     * @param x       The <code>int</code> x-coordinate of the hex
     * @param y       The <code>int</code> y-coordinate of the hex
     */
    public void addL3Smoke(int x, int y) {
        IBoard board = game.getBoard();
        Coords smokeCoords = new Coords(x, y);
        IHex smokeHex = game.getBoard().getHex(smokeCoords);
        boolean infernoBurning = board.isInfernoBurning( smokeCoords );
        Report r;
        if (smokeHex == null) {
            return;
        }
        // Have to check if it's inferno smoke or from a heavy/hardened building - heavy smoke from those
        if (infernoBurning || Building.MEDIUM < smokeHex.terrainLevel(Terrains.BUILDING)) {
            if (smokeHex.terrainLevel(Terrains.SMOKE) == 2){
                //heavy smoke fills hex
                r = new Report(5180, Report.PUBLIC);
                r.add(smokeCoords.getBoardNum());
                addReport(r);
            } else {
                if (smokeHex.terrainLevel(Terrains.SMOKE) == 1){
                    //heavy smoke overrides light
                    smokeHex.removeTerrain(Terrains.SMOKE);
                }
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 2));
                sendChangedHex(smokeCoords);
                r = new Report(5185, Report.PUBLIC);
                r.add(smokeCoords.getBoardNum());
                addReport(r);
            }
        } else {
            if (smokeHex.terrainLevel(Terrains.SMOKE) == 2){
                //heavy smoke overpowers light
                r = new Report(5190, Report.PUBLIC);
                r.add(smokeCoords.getBoardNum());
                addReport(r);
            } else if (smokeHex.terrainLevel(Terrains.SMOKE) == 1){
                //light smoke continue to fill hex
                r = new Report(5195, Report.PUBLIC);
                r.add(smokeCoords.getBoardNum());
                addReport(r);
            } else {
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 1));
                sendChangedHex(smokeCoords);
                //light smoke fills hex
                r = new Report(5200, Report.PUBLIC);
                r.add(smokeCoords.getBoardNum());
                addReport(r);
            }
        }
    }

    public void removeSmoke(int x, int y, int windDir) { // L2 smoke removal
        Coords smokeCoords = new Coords(Coords.xInDir(x, y, windDir), Coords.yInDir(x, y, windDir));
        IHex nextHex = game.getBoard().getHex(smokeCoords);
        if (nextHex != null && nextHex.containsTerrain(Terrains.SMOKE)) {
            nextHex.removeTerrain(Terrains.SMOKE);
            sendChangedHex(smokeCoords);
            Report r = new Report(5205, Report.PUBLIC);
            r.add(smokeCoords.getBoardNum());
            addReport(r);
        }
    }


    /**
     * Scans the boards directory for map boards of the appropriate size
     * and returns them.
     */
    private Vector scanForBoardsInDir (File dir, String addPath, int w, int h) {
        String fileList[] = dir.list();
        Vector tempList = new Vector();
        Comparator sortComp = StringUtil.stringComparator();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].indexOf(".board") == -1) {
                continue;
            }
            if (Board.boardIsSize(addPath.concat("/").concat(fileList[i]), w, h)) {
                tempList.addElement(addPath.concat("/").concat(fileList[i].substring(0, fileList[i].lastIndexOf(".board"))));
            }
        }
        return tempList;
    }
    
    private Vector scanForBoards(int boardWidth, int boardHeight) {
        return scanForBoards (boardWidth, boardHeight, game.getOptions().booleanOption("maps_include_subdir"));
    }
    
    private Vector scanForBoards(int boardWidth, int boardHeight, boolean subdirs) {
        Vector boards = new Vector();

        File boardDir = new File("data/boards");
        boards.addElement(MapSettings.BOARD_GENERATED);
        // just a check...
        if (!boardDir.isDirectory()) {
            return boards;
        }

        // scan files
        String[] fileList = boardDir.list();
        Vector tempList = new Vector();
        Comparator sortComp = StringUtil.stringComparator();
        for (int i = 0; i < fileList.length; i++) {
            File x = new File (new String("data/boards/").concat(fileList[i]));
            if (x.isDirectory() && subdirs) {
                tempList.addAll(scanForBoardsInDir(x, fileList[i], boardWidth, boardHeight));
                continue;
            }
            if (fileList[i].indexOf(".board") == -1) {
                continue;
            }
            if (Board.boardIsSize(fileList[i], boardWidth, boardHeight)) {
                tempList.addElement(fileList[i].substring(0, fileList[i].lastIndexOf(".board")));
            }
        }

        // if there are any boards, add these:
        if (tempList.size() > 0) {
            boards.addElement( MapSettings.BOARD_RANDOM );
            boards.addElement( MapSettings.BOARD_SURPRISE );
            Collections.sort(tempList, sortComp);
            for ( int loop = 0; loop < tempList.size(); loop++ ) {
                boards.addElement( tempList.elementAt(loop) );
            }
        }

        //TODO: alphabetize files?

        return boards;
    }

    private boolean doBlind() {
        return (game.getOptions().booleanOption("double_blind") &&
        game.getPhase() >= IGame.PHASE_DEPLOYMENT);
    }

    /**
     * In a double-blind game, update only visible entities.  Otherwise,
     * update everyone
     */
    private void entityUpdate(int nEntityID) {
        entityUpdate(nEntityID, new Vector());
    }

    private void entityUpdate(int nEntityID, Vector movePath) {
        Entity eTarget = game.getEntity(nEntityID);
        if(eTarget == null) {
            if(game.getOutOfGameEntity(nEntityID) != null) {
                System.err.printf("S: attempted to send entity update for out of game entity, id was %d\n", nEntityID);
            } else {
                System.err.printf("S: attempted to send entity update for null entity, id was %d\n", nEntityID);
            }
            return; //do not send the update it will crash the client
        }
        if (doBlind()) {
            Vector vPlayers = game.getPlayersVector();
            Vector vCanSee = whoCanSee(eTarget);
            // send an entity update to everyone who can see
            Packet pack = createEntityPacket(nEntityID, movePath);
            for (int x = 0; x < vCanSee.size(); x++) {
                Player p = (Player)vCanSee.elementAt(x);
                send(p.getId(), pack);
            }
            // send an entity delete to everyone else
            pack = createRemoveEntityPacket( nEntityID,
                                             eTarget.getRemovalCondition() );
            for (int x = 0; x < vPlayers.size(); x++) {
                if (!vCanSee.contains(vPlayers.elementAt(x))) {
                    Player p = (Player)vPlayers.elementAt(x);
                    send(p.getId(), pack);
                }
            }
        }
        else {
            // everyone can see
            send(createEntityPacket(nEntityID, movePath));
        }
    }

    /**
     * Returns a vector of which players can see this entity.
     */
    private Vector whoCanSee(Entity entity) {
        
        //Some times Null entities are sent to this
        if ( entity == null )
            return new Vector();
        
        boolean bTeamVision = game.getOptions().booleanOption("team_vision");
        Vector vEntities = game.getEntitiesVector();

        Vector vCanSee = new Vector();
        vCanSee.addElement(entity.getOwner());
        if (bTeamVision) {
            addTeammates(vCanSee, entity.getOwner());
        }

        for (Enumeration p = game.getPlayers(); p.hasMoreElements();) {
            Player player = (Player)p.nextElement();

            if (player.canSeeAll() && !vCanSee.contains(p))
                vCanSee.addElement(player);
        }

        for (int i = 0; i < vEntities.size(); i++) {
            Entity e = (Entity)vEntities.elementAt(i);
            if (vCanSee.contains(e.getOwner()) || !e.isActive()) {
                continue;
            }
            if(e.isOffBoard()) {
                continue; //Off board units should not spot on board units
            }
            if (Compute.canSee(game, e, entity)) {
                vCanSee.addElement(e.getOwner());
                if (bTeamVision) {
                    addTeammates(vCanSee, e.getOwner());
                }
            }
        }
        return vCanSee;
    }

    private boolean canSee(Player p, Entity e) {
        if (e.getOwner().getId() == p.getId()) {
            //The owner of an entity should be able to see it, of course.
            return true;
        }
        Vector playersWhoCanSee = whoCanSee(e);
        for (int i = 0; i < playersWhoCanSee.size(); i++) {
            Player currentPlayer = (Player)playersWhoCanSee.elementAt(i);
            if (currentPlayer.equals(p)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds teammates of a player to the Vector.
     * Utility function for whoCanSee.
     */
    private void addTeammates(Vector vector, Player player) {
        Vector vPlayers = game.getPlayersVector();
        for (int j = 0; j < vPlayers.size(); j++) {
            Player p = (Player)vPlayers.elementAt(j);
            if (!player.isEnemyOf(p) && !vector.contains(p)) {
                vector.addElement(p);
            }
        }
    }

    /**
     * Send the complete list of entities to the players.
     * If double_blind is in effect, enforce it by filtering the entities
     */
    private void entityAllUpdate() {
        if (doBlind()) {
            Vector vPlayers = game.getPlayersVector();
            for (int x = 0; x < vPlayers.size(); x++) {
                Player p = (Player)vPlayers.elementAt(x);
                send(p.getId(), createFilteredEntitiesPacket(p));
            }
        }
        else {
            send(createEntitiesPacket());
        }
    }


    /**
     * Filters an entity vector according to LOS
     */
    private Vector filterEntities(Player pViewer, Vector vEntities) {
        Vector vCanSee = new Vector();
        Vector vAllEntities = game.getEntitiesVector();
        Vector vMyEntities = new Vector();
        boolean bTeamVision = game.getOptions().booleanOption("team_vision");

        // If they can see all, return the input list
        if (pViewer.canSeeAll()) {
            return vEntities;
        }

        for (int x = 0; x < vAllEntities.size(); x++) {
            Entity e = (Entity)vAllEntities.elementAt(x);
            if (e.getOwner() == pViewer || (bTeamVision && !e.getOwner().isEnemyOf(pViewer))) {
                vMyEntities.addElement(e);
            }
        }

        for (int x = 0; x < vEntities.size(); x++) {
            Entity e = (Entity)vEntities.elementAt(x);
            if (vMyEntities.contains(e)) {
                vCanSee.addElement(e);
                continue;
            }
            for (int y = 0; y < vMyEntities.size(); y++) {
                Entity e2 = (Entity)vMyEntities.elementAt(y);
                if(e2.isOffBoard()) {
                    continue;
                }
                if (Compute.canSee(game, e2, e)) {
                    vCanSee.addElement(e);
                    break;
                }
            }
        }
        return vCanSee;
    }

    //optimize and document me
    private Vector filterReportVector(Vector originalReportVector, Player p) {
        if (!doBlind()) {
            //don't bother filtering if double-blind rules aren't in effect
            return (Vector)originalReportVector.clone();
        }
        Vector filteredReportVector = new Vector();
        Report r;
        for (int i = 0; i < originalReportVector.size(); i++) {
            r = (Report)originalReportVector.elementAt(i);
            filteredReportVector.addElement(filterReport(r, p, false));
        }

        return filteredReportVector;
    }

    /**
     * Filter a single report so that the correct double-blind
     * obscuration takes place.
     *
     * @param r the Report to filter
     * @param p the Player that we are going to send the filtered report to
     * @param omitCheck boolean indicating that this report hapened in
     * the past, so we no longer have access to the Player
     * @return a new Report, which has possibly been obscured
     */
    private Report filterReport(Report r, Player p, boolean omitCheck) {
        if (r.subject == Entity.NONE && r.type != Report.PUBLIC) {
            //Reports that don't have a subject should be public.
            System.err.println("Error: Attempting to filter a Report object that is not public yet has no subject.\n\t\tmessageId: " + r.messageId);
            return r;
        }
        if (r.type == Report.PUBLIC || (p == null && !omitCheck)) {
            return r;
        }
        Entity entity = game.getEntity(r.subject);
        Player owner = null;
        if (entity != null)
            owner = entity.getOwner();
        if (!omitCheck && (entity == null || owner == null)) {
            System.err.println("Error: Attempting to filter a Report object that is not public but has a subject (" + entity + ") with owner (" + owner + ").\n\tmessageId: " + r.messageId);
            return r;
        }
        //off board (Artillery) units get treated as public messages
        if ( entity.isOffBoard() )
            return r;
        Report copy = new Report(r);
        for (int j = 0; j < copy.dataCount(); j++) {
            if (omitCheck || !canSee(p, entity)) {
                if (r.isValueObscured(j)) {
                    copy.hideData(j);
                    //Mark the original report to indicate which players
                    // received an obscured version of it.
                    if (p != null)
                        r.addObscuredRecipient(p.getName());
                }
                //simulate hiding the report for *true* double-blind play
                //***DEBUG*** TESTING ONLY
                //copy.markForTesting();
            }
        }
        return copy;
    }

    private Vector filterPastReports(Vector pastReports, Player p) {
        //This stuff really only needs to be printed for debug reasons. other wise the logs get
        //filled to the brim when ever someone connects. --Torren.
        System.err.println("filterPastReports() begin");
        System.err.println("  player is " + p.getName());
        if (doBlind()) {
            //System.err.println("  pastReports vector is\n" + pastReports);
            Vector filteredReports = new Vector();
            Vector filteredRoundReports = new Vector();
            Vector roundReports = new Vector();
            Report r;
            for (int i = 0; i < pastReports.size(); i++) {
                roundReports = (Vector)pastReports.elementAt(i);
                //System.err.println("  roundReports vector is\n" + roundReports);
                for (int j = 0; j < roundReports.size(); j++) {
                    r = (Report)roundReports.elementAt(j);
                    if (r.isObscuredRecipient(p.getName())) {
                        //System.err.println("  report is " + r + " -obscuring-");
                        filteredRoundReports.addElement(filterReport(r, null, true));
                    } else {
                        //System.err.println("  report is " + r);
                        filteredRoundReports.addElement(r);
                    }
                }
                //System.err.println("  filteredRoundReport is\n" + filteredRoundReports);
                filteredReports.addElement(filteredRoundReports.clone());
                filteredRoundReports.removeAllElements();
            }
            System.err.println("filterPastReports() end");
            return filteredReports;
        } else {
            //don't bother filtering if double-blind rules aren't in effect
            System.err.println("filterPastReports() end");
            return pastReports;
        }
    }

    /**
     * Updates entities graphical "visibility indications" which are used
     *  in double-blind games.
     */
    private void updateVisibilityIndicator() {
        Vector vAllEntities = game.getEntitiesVector();
        for (int x = 0; x < vAllEntities.size(); x++) {
            Entity e = (Entity)vAllEntities.elementAt(x);
            boolean previousVisibleValue = e.isVisibleToEnemy();
            boolean previousSeenValue = e.isSeenByEnemy();
            e.setVisibleToEnemy(false);
            Vector vCanSee = whoCanSee(e);
            for (int y = 0; y < vCanSee.size(); y++) {
                Player p = (Player)vCanSee.elementAt(y);
                if (e.getOwner().isEnemyOf(p) && !p.isObserver()) {
                    e.setVisibleToEnemy(true);
                    e.setSeenByEnemy(true);
                }
            }
            if (previousVisibleValue != e.isVisibleToEnemy()
                || previousSeenValue != e.isSeenByEnemy()) {
                sendVisibilityIndicator(e);
            }
        }
    }

    /**
     * Checks if an entity added by the client is valid and if so, adds it to the list
     */
    private void receiveEntityAdd(Packet c, int connIndex) {
        final Entity entity = (Entity)c.getObject(0);

        //Verify the entity's design
        if (Server.entityVerifier == null)
            Server.entityVerifier = new EntityVerifier(new File(VERIFIER_CONFIG_FILENAME));
        // we can only test meks and vehicles right now
        if (entity instanceof Mech || entity instanceof Tank) {
            TestEntity testEntity = null;
            entity.restore();
            if (entity instanceof Mech)
                testEntity = new TestMech((Mech)entity, Server.entityVerifier.mechOption, null);
            if (entity instanceof VTOL)
                testEntity = new TestTank((Tank)entity, Server.entityVerifier.tankOption, null);//not implemented yet.
            if (entity instanceof Tank)
                testEntity = new TestTank((Tank)entity, Server.entityVerifier.tankOption, null);
            StringBuffer sb = new StringBuffer();
            if (testEntity.correctEntity(sb, !game.getOptions().booleanOption("is_eq_limits"))) {
                entity.setDesignValid(true);
            } else {
                if (game.getOptions().booleanOption("allow_illegal_units")) {
                    entity.setDesignValid(false);
                } else {
                    Player cheater = game.getPlayer( connIndex );
                    sendServerChat("Player " + cheater.getName() + " attempted to add an illegal unit design (" + entity.getShortNameRaw() + "), the unit was rejected.");
                    System.err.println(sb);
                    return;
                }
            }
        }

        // If we're adding a Protomech, calculate it's unit number.
        if ( entity instanceof Protomech ) {

            // How many Protomechs does the player already have?
            int numPlayerProtos = game.getSelectedEntityCount
                ( new EntitySelector() {
                        private final int ownerId = entity.getOwnerId();
                        public boolean accept( Entity entity ) {
                            if ( entity instanceof Protomech &&
                                 ownerId == entity.getOwnerId() )
                                return true;
                            return false;
                        }
                    } );

            // According to page 54 of the BMRr, Protomechs must be
            // deployed in full Points of five, unless circumstances have
            // reduced the number to less that that.
            entity.setUnitNumber( (char) (numPlayerProtos / 5) );

        } // End added-Protomech

        // Only assign an entity ID when the client hasn't.
        if ( Entity.NONE == entity.getId() ) {
            entity.setId(getFreeEntityId());
        }

        game.addEntity(entity.getId(), entity);
        send(createAddEntityPacket(entity.getId()));
    }

    /**
     * Updates an entity with the info from the client.  Only valid to do this
     * during the lounge phase, except for heat sink changing.
     */
    private void receiveEntityUpdate(Packet c, int connIndex) {
        Entity entity = (Entity)c.getObject(0);
        Entity oldEntity = game.getEntity(entity.getId());
        if (oldEntity != null && oldEntity.getOwner() == getPlayer(connIndex)) {
            game.setEntity(entity.getId(), entity);
            entityUpdate(entity.getId());
            // In the chat lounge, notify players of customizing of unit
            if (game.getPhase() == IGame.PHASE_LOUNGE) {
                StringBuffer message = new StringBuffer();
                message.append( "Unit " );
                if(game.getOptions().booleanOption("blind_drop")
                    || game.getOptions().booleanOption("real_blind_drop")) {
                    if(Entity.NONE != entity.getExternalId()) {
                        message.append("[").append(entity.getExternalId()).append("] ");
                    }
                    message.append(entity.getId())
                        .append("(")
                        .append(entity.getOwner().getName())
                        .append(")");
                } else {
                    message.append( entity.getDisplayName() );
                }
                message.append( " has been customized." );
                sendServerChat( message.toString() );
            }
        } else {
            // hey!
        }
    }

    private void receiveEntityModeChange(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        int mode = c.getIntValue(2);
        Entity e = game.getEntity(entityId);
        if (e.getOwner() != getPlayer(connIndex)) {
            return;
        }
        Mounted m = e.getEquipment(equipId);

        // a mode change for ammo means dumping
        if (m.getType() instanceof AmmoType
            && !(m.getType().hasInstantModeSwitch())) {
            m.setPendingDump(mode == 1);
        }
        else {
            m.setMode(mode);
        }
    }

    private void receiveEntitySystemModeChange(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        int mode = c.getIntValue(2);
        Entity e = game.getEntity(entityId);
        if (e.getOwner() != getPlayer(connIndex)) {
            return;
        }
        if(e instanceof Mech && equipId == Mech.SYSTEM_COCKPIT) {
            ((Mech)e).setCockpitStatus(mode);
        }
    }

    private void receiveEntityAmmoChange(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int weaponId = c.getIntValue(1);
        int ammoId = c.getIntValue(2);
        Entity e = game.getEntity(entityId);

        // Did we receive a request for a valid Entity?
        if ( null == e ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: could not find entity #" );
            System.err.println( entityId );
            return;
        }
        Player player = getPlayer( connIndex );
        if ( null != player && e.getOwner() != player ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: player " );
            System.err.print( player.getName() );
            System.err.print( " does not own the entity " );
            System.err.println( e.getDisplayName() );
            return;
        }

        // Make sure that the entity has the given equipment.
        Mounted mWeap = e.getEquipment(weaponId);
        Mounted mAmmo = e.getEquipment(ammoId);
        if ( null == mAmmo ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " does not have ammo #" );
            System.err.println( ammoId );
            return;
        }
        if ( !(mAmmo.getType() instanceof AmmoType) ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( ammoId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mAmmo.getName() );
            System.err.println( " and not ammo." );
            return;
        }
        if ( null == mWeap ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " does not have weapon #" );
            System.err.println( weaponId );
            return;
        }
        if ( !(mWeap.getType() instanceof WeaponType) ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( weaponId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mWeap.getName() );
            System.err.println( " and not a weapon." );
            return;
        }
        if ( ((WeaponType) mWeap.getType()).getAmmoType() == AmmoType.T_NA ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( weaponId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mWeap.getName() );
            System.err.println( " and does not use ammo." );
            return;
        }
        if ( ((WeaponType) mWeap.getType()).hasFlag(WeaponType.F_ONESHOT)) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( weaponId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mWeap.getName() );
            System.err.println( " and cannot use external ammo." );
            return;
        }

        // Load the weapon.
        e.loadWeapon( mWeap, mAmmo );
    }

    /**
     * Deletes an entity owned by a certain player from the list
     */
    private void receiveEntityDelete(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        final Entity entity = game.getEntity(entityId);

        // Only allow players to delete their *own* entities.
        if ( entity != null && entity.getOwner() == getPlayer(connIndex) ) {

            // If we're deleting a Protomech, recalculate unit numbers.
            if ( entity instanceof Protomech ) {

                // How many Protomechs does the player have (include this one)?
                int numPlayerProtos = game.getSelectedEntityCount
                    ( new EntitySelector() {
                            private final int ownerId = entity.getOwnerId();
                            public boolean accept( Entity entity ) {
                                if ( entity instanceof Protomech &&
                                     ownerId == entity.getOwnerId() )
                                    return true;
                                return false;
                            }
                        } );

                // According to page 54 of the BMRr, Protomechs must be
                // deployed in full Points of five, unless "losses" have
                // reduced the number to less that that.
                final char oldMax =
                    (char)(Math.ceil( numPlayerProtos / 5.0 )-1);
                char newMax =
                    (char)(Math.ceil( (numPlayerProtos-1)/ 5.0 )-1);
                char deletedUnitNum = entity.getUnitNumber();

                // Do we have to update a Protomech from the last unit?
                if ( oldMax != deletedUnitNum && oldMax != newMax ) {

                    // Yup.  Find a Protomech from the last unit, and
                    // set it's unit number to the deleted entity.
                    Enumeration lastUnit = game.getSelectedEntities
                        ( new EntitySelector() {
                            private final int ownerId = entity.getOwnerId();
                            private final char lastUnitNum = oldMax;
                            public boolean accept( Entity entity ) {
                                if ( entity instanceof Protomech &&
                                     ownerId == entity.getOwnerId() &&
                                     lastUnitNum == entity.getUnitNumber() )
                                    return true;
                                return false;
                            }
                        } );
                    Entity lastUnitMember = (Entity) lastUnit.nextElement();
                    lastUnitMember.setUnitNumber( deletedUnitNum );
                    this.entityUpdate( lastUnitMember.getId() );

                } // End update-unit-numbetr

            } // End added-Protomech

            game.removeEntity(entityId, IEntityRemovalConditions.REMOVE_NEVER_JOINED);
            send(createRemoveEntityPacket(entityId, IEntityRemovalConditions.REMOVE_NEVER_JOINED));
        }
    }

    /**
     * Sets a player's ready status
     */
    private void receivePlayerDone(Packet pkt, int connIndex) {
        boolean ready = pkt.getBooleanValue(0);
        Player player = getPlayer(connIndex);
        if ( null != player ) {
            player.setDone(ready);
        }
    }

    private void receiveInitiativeRerollRequest(Packet pkt, int connIndex) {
        Player player = getPlayer(connIndex);
        if ( IGame.PHASE_INITIATIVE_REPORT != game.getPhase() ) {
            StringBuffer message = new StringBuffer();
            if ( null == player ) {
                message.append( "Player #" )
                    .append( connIndex );
            } else {
                message.append( player.getName() );
            }
            message.append( " is not allowed to ask for a reroll at this time." );
            System.err.println( message.toString() );
            sendServerChat( message.toString() );
            return;
        }
        if (game.hasTacticalGenius(player)) {
            game.addInitiativeRerollRequest(game.getTeamForPlayer(player));
        }
        if ( null != player ) {
            player.setDone(true);
        }
        checkReady();
    }

    /**
     * Sets game options, providing that the player has specified the password
     * correctly.
     *
     * @return true if any options have been successfully changed.
     */
    private boolean receiveGameOptions(Packet packet, int connId) {
        Player player = game.getPlayer( connId );
        // Check player
        if ( null == player ) {
            System.err.print
                ( "Server does not recognize player at connection " );
            System.err.println( connId );
            return false;
        }

        // check password
        if (password != null && password.length() > 0 && !password.equals(packet.getObject(0))) {
            sendServerChat(connId, "The password you specified to change game options is incorrect.");
            return false;
        }

        int changed = 0;

        for (Enumeration i = ((Vector)packet.getObject(1)).elements(); i.hasMoreElements();) {
            IBasicOption option = (IBasicOption)i.nextElement();
            IOption originalOption = game.getOptions().getOption(option.getName());

            if (originalOption == null) {
                continue;
            }

            StringBuffer message = new StringBuffer();
            message.append( "Player " )
                .append( player.getName() )
                .append( " changed option \"" )
                .append( originalOption.getDisplayableName() )
                .append( "\" to " )
                .append( option.getValue().toString() )
                .append( "." );
            sendServerChat( message.toString() );
            originalOption.setValue(option.getValue());
            changed++;            
        }

        // Set proper RNG
        Compute.setRNG(game.getOptions().intOption("rng_type"));

        return changed > 0;
    }

    /**
     * Performs the additional processing of the received options after the the 
     * <code>receiveGameOptions<code> done its job; should be called after
     * <code>receiveGameOptions<code> only if the <code>receiveGameOptions<code> 
     * returned <code>true</code>    
     * @param packet
     * @param connId
     */
    private void receiveGameOptionsAux(Packet packet, int connId) {

        for (Enumeration i = ((Vector)packet.getObject(1)).elements(); i.hasMoreElements();) {
            IBasicOption option = (IBasicOption)i.nextElement();
            IOption originalOption = game.getOptions().getOption(option.getName());
            if (originalOption != null) {
                if ("maps_include_subdir".equals(originalOption.getName())) {
                    mapSettings.setBoardsAvailableVector(scanForBoards(mapSettings.getBoardWidth(), mapSettings.getBoardHeight()));
                    mapSettings.removeUnavailable();
                    mapSettings.setNullBoards(DEFAULT_BOARD);
                    send(createMapSettingsPacket());                                    
                }
            }
        }
        
    }

    /**
     * Sends out all player info to the specified connection
     */
    private void transmitAllPlayerConnects(int connId) {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            send(connId, createPlayerConnectPacket(player.getId()));
        }
    }



    /**
     * Creates a packet informing that the player has connected
     */
    private Packet createPlayerConnectPacket(int playerId) {
        final Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = getPlayer(playerId);
        return new Packet(Packet.COMMAND_PLAYER_ADD, data);
    }

    /**
     * Creates a packet containing the player info, for update
     */
    private Packet createPlayerUpdatePacket(int playerId) {
        final Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = getPlayer(playerId);
        return new Packet(Packet.COMMAND_PLAYER_UPDATE, data);
    }

    /**
     * Sends out the player info updates for all players to all connections
     */
    private void transmitAllPlayerUpdates() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if ( null != player ) {
                send(createPlayerUpdatePacket(player.getId()));
            }
        }
    }

    /**
     * Sends out the player ready stats for all players to all connections
     */
    private void transmitAllPlayerDones() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            send(createPlayerDonePacket(player.getId()));
        }
    }

    /**
     * Creates a packet containing the player ready status
     */
    private Packet createPlayerDonePacket(int playerId) {
        Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = new Boolean(getPlayer(playerId).isDone());
        return new Packet(Packet.COMMAND_PLAYER_READY, data);
    }

    /** Creates a packet containing the current turn vector */
    private Packet createTurnVectorPacket() {
        return new Packet(Packet.COMMAND_SENDING_TURNS, game.getTurnVector());
    }

    /** Creates a packet containing the current turn index */
    private Packet createTurnIndexPacket() {
        return new Packet(Packet.COMMAND_TURN, new Integer(game.getTurnIndex()));
    }

    /**
     * Creates a packet containing the map settings
     */
    private Packet createMapSettingsPacket() {
        return new Packet(Packet.COMMAND_SENDING_MAP_SETTINGS, mapSettings);
    }

    /**
     * Creates a packet containing temporary map settings as a response to a
     * client query
     */
    private Packet createMapQueryPacket(MapSettings temp) {
        return new Packet(Packet.COMMAND_QUERY_MAP_SETTINGS, temp);
    }

    /**
     * Creates a packet containing the game settingss
     */
    private Packet createGameSettingsPacket() {
        return new Packet(Packet.COMMAND_SENDING_GAME_SETTINGS, game.getOptions());
    }

    /**
     * Creates a packet containing the game board
     */
    private Packet createBoardPacket() {
        return new Packet(Packet.COMMAND_SENDING_BOARD, game.getBoard());
    }

    /**
     * Creates a packet containing a single entity, for update
     */
    private Packet createEntityPacket(int entityId) {
        return createEntityPacket(entityId, new Vector());
    }
    private Packet createEntityPacket(int entityId, Vector movePath) {
        final Entity entity = game.getEntity(entityId);
        final Object[] data = new Object[3];
        data[0] = new Integer(entityId);
        data[1] = entity;
        data[2] = movePath;
        return new Packet(Packet.COMMAND_ENTITY_UPDATE, data);
    }

    /**
     * Creates a packet containing a Vector of Reports
     */
    private Packet createReportPacket(Player p) {
        //when the final report is created MM sends a null player to create the 
        //report so this will handel that issue.
        if ( p == null || !doBlind() )
            return new Packet(Packet.COMMAND_SENDING_REPORTS, filterReportVector(vPhaseReport, p));
        else
            return new Packet(Packet.COMMAND_SENDING_REPORTS, p.getTurnReport());   
    }

    /**
     * Creates a packet containing a Vector of special Reports which
     * needs to be sent during a phase that is not a report phase.
     */
    private Packet createSpecialReportPacket() {
        return new Packet(Packet.COMMAND_SENDING_REPORTS_SPECIAL, vPhaseReport.clone());
    }

    /**
     * Creates a packet containing a Vector of Reports that represent
     * a Tactical Genius re-roll request which needs to update a
     * current phase's report.
     */
    private Packet createTacticalGeniusReportPacket() {
        return new Packet(Packet.COMMAND_SENDING_REPORTS_TACTICAL_GENIUS, vPhaseReport.clone());
    }

    /**
     * Creates a packet containing all the round reports
     */
    private Packet createAllReportsPacket(Player p) {
        return new Packet(Packet.COMMAND_SENDING_REPORTS_ALL, filterPastReports(game.getAllReports(), p));
    }

    /**
     * Creates a packet containing all current entities
     */
    private Packet createEntitiesPacket() {
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, game.getEntitiesVector());
    }

    /**
     * Creates a packet containing all current and out-of-game entities
     */
    private Packet createFullEntitiesPacket() {
        final Object[] data = new Object[2];
        data[0] = game.getEntitiesVector();
        data[1] = game.getOutOfGameEntitiesVector();
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, data);
    }

    /**
     * Creates a packet containing all entities visible to the player in a blind game
     */
    private Packet createFilteredEntitiesPacket(Player p) {
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, filterEntities(p, game.getEntitiesVector()));
    }

    /**
     * Creates a packet containing all entities, including wrecks, visible to the player in a blind game
     */
    private Packet createFilteredFullEntitiesPacket(Player p) {
        final Object[] data = new Object[2];
        data[0] = filterEntities(p, game.getEntitiesVector());
        data[1] = game.getOutOfGameEntitiesVector();
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, data);
    }

    /**
     * Creates a packet detailing the addition of an entity
     */
    private Packet createAddEntityPacket(int entityId) {
        final Entity entity = game.getEntity(entityId);
        final Object[] data = new Object[2];
        data[0] = new Integer(entityId);
        data[1] = entity;
        return new Packet(Packet.COMMAND_ENTITY_ADD, data);
    }

    /**
     * Creates a packet detailing the removal of an entity.
     * Maintained for backwards compatability.
     *
     * @param   entityId - the <code>int</code> ID of the entity being removed.
     * @return  A <code>Packet</code> to be sent to clients.
     */
    private Packet createRemoveEntityPacket(int entityId) {
        return this.createRemoveEntityPacket(entityId, IEntityRemovalConditions.REMOVE_SALVAGEABLE);
    }

    /**
     * Creates a packet detailing the removal of an entity.
     *
     * @param   entityId - the <code>int</code> ID of the entity being removed.
     * @param   condition - the <code>int</code> condition the unit was in.
     *          This value must be one of <code>Game.UNIT_IN_RETREAT</code>,
     *          <code>Game.UNIT_PUSHED</code>, or
     *          <code>Game.UNIT_SALVAGEABLE</code>, or
     *          <code>Game.UNIT_EJECTED</code>, or
     *          <code>Game.UNIT_DEVASTATED</code> or an
     *          <code>IllegalArgumentException</code> will be thrown.
     * @return  A <code>Packet</code> to be sent to clients.
     */
    private Packet createRemoveEntityPacket(int entityId, int condition) {
        if ( condition != IEntityRemovalConditions.REMOVE_UNKNOWN &&
             condition != IEntityRemovalConditions.REMOVE_IN_RETREAT &&
             condition != IEntityRemovalConditions.REMOVE_PUSHED &&
             condition != IEntityRemovalConditions.REMOVE_SALVAGEABLE &&
             condition != IEntityRemovalConditions.REMOVE_EJECTED &&
             condition != IEntityRemovalConditions.REMOVE_CAPTURED &&
             condition != IEntityRemovalConditions.REMOVE_DEVASTATED &&
             condition != IEntityRemovalConditions.REMOVE_NEVER_JOINED ) {
            throw new IllegalArgumentException( "Unknown unit condition: " +
                                                condition );
        }
        Object[] array = new Object[2];
        array[0] = new Integer(entityId);
        array[1] = new Integer(condition);
        return new Packet(Packet.COMMAND_ENTITY_REMOVE, array);
    }

    /**
     * Creates a packet indicating end of game, including detailed unit status
     */
    private Packet createEndOfGamePacket() {
        Object[] array = new Object[3];
        array[0] = getDetailedVictoryReport();
        array[1] = new Integer(game.getVictoryPlayerId());
        array[2] = new Integer(game.getVictoryTeam());
        return new Packet(Packet.COMMAND_END_OF_GAME, array);
    }

    /**
     * Transmits a chat message to all players
     */
    private void sendChat(int connId, String origin, String message) {
        send(connId, new Packet(Packet.COMMAND_CHAT, origin + ": " + message));
    }

    /**
     * Transmits a chat message to all players
     */
    private void sendChat(String origin, String message) {
        String chat = origin + ": " + message;
        send(new Packet(Packet.COMMAND_CHAT, chat));
    }

    public void sendServerChat(int connId, String message) {
        sendChat(connId, "***Server", message);
    }

    public void sendServerChat(String message) {
        sendChat("***Server", message);
    }

    /**
     * Creates a packet containing a hex, and the coordinates it goes at.
     */
    private Packet createHexChangePacket(Coords coords, IHex hex) {
        final Object[] data = new Object[2];
        data[0] = coords;
        data[1] = hex;
        return new Packet(Packet.COMMAND_CHANGE_HEX, data);
    }

    /**
     * Sends notification to clients that the specified hex has changed.
     */
    public void sendChangedHex(Coords coords) {
        send(createHexChangePacket(coords, game.getBoard().getHex(coords)));
    }

    public void sendVisibilityIndicator(Entity e) {
        final Object[] data = new Object[3];
        data[0] = new Integer(e.getId());
        data[1] = new Boolean(e.isSeenByEnemy());
        data[2] = new Boolean(e.isVisibleToEnemy());
        send(new Packet(Packet.COMMAND_ENTITY_VISIBILITY_INDICATOR, data));
    }

    /**
     * Creates a packet for an attack
     */
    private Packet createAttackPacket(Vector vector, int charges) {
        final Object[] data = new Object[2];
        data[0] = vector;
        data[1] = new Integer(charges);
        return new Packet(Packet.COMMAND_ENTITY_ATTACK, data);
    }

    /**
     * Creates a packet for an attack
     */
    private Packet createAttackPacket(EntityAction ea, int charge) {
        Vector vector = new Vector(1);
        vector.addElement(ea);
        Object[] data = new Object[2];
        data[0] = vector;
        data[1] = new Integer(charge);
        return new Packet(Packet.COMMAND_ENTITY_ATTACK, data);
    }

    /**
     * Creates a packet containing offboard artillery attacks
     **/
    private Packet createArtilleryPacket(Player p) {
        
        if(p.getSeeAll()) {
            return new Packet(Packet.COMMAND_SENDING_ARTILLERYATTACKS, game.getArtilleryVector());
        }
        Vector v = new Vector();
        int team = p.getTeam();
        for(Enumeration i = game.getArtilleryAttacks();i.hasMoreElements();) {
            ArtilleryAttackAction aaa = (ArtilleryAttackAction)i.nextElement();
            if(aaa.getPlayerId() == p.getId() ||
                    (team != Player.TEAM_NONE && team == game.getPlayer(aaa.getPlayerId()).getTeam())) {
                v.addElement(aaa);
            }
        }
        return new Packet(Packet.COMMAND_SENDING_ARTILLERYATTACKS, v);
    }
    
    /**
     * Creates a packet containing flares
     **/
    private Packet createFlarePacket() {
        
        return new Packet(Packet.COMMAND_SENDING_FLARES, game.getFlares());
    }
    
    /**
     * Send a packet to all connected clients.
     */
    private void send(Packet packet) {
        if (connections == null) {
            return;
        }
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            conn.send(packet);
        }
    }

    private void sendReport() {
        sendReport(false);
    }

    /**
     * Send the round report to all connected clients.
     */
    private void sendReport(boolean tacticalGeniusReport) {
        if (connections == null) {
            return;
        }

        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            Player p = game.getPlayer(conn.getId());
            Packet packet;
            if (tacticalGeniusReport)
                packet = createTacticalGeniusReportPacket();
            else
                packet = createReportPacket(p);
            conn.send(packet);
        }
    }

    /**
     * Send a packet to a specific connection.
     */
    private void send(int connId, Packet packet) {
        if (getClient(connId) != null) {
            getClient(connId).send(packet);
        } else {
            //What should we do if we've lost this client?
            // For now, nothing.
        }
    }

    /**
     * Send a packet to a pending connection
     */
    private void sendToPending(int connId, Packet packet) {
        if (getPendingConnection(connId) != null) {
            getPendingConnection(connId).send(packet);
        } else {
            //What should we do if we've lost this client?
            // For now, nothing.
        }
    }

    /**
     * Process an in-game command
     */
    private void processCommand(int connId, String commandString) {
        String[] args;
        String commandName;
        // all tokens are read as strings; if they're numbers, string-ize 'em.
        StringTokenizer st = new StringTokenizer(commandString);
        args = new String[st.countTokens()];
        for (int i = 0; i < args.length; i++) {
            args[i] = st.nextToken();
        }

        // figure out which command this is
        commandName = args[0].substring(1);

        // process it
        ServerCommand command = getCommand(commandName);
        if (command != null) {
            command.run(connId, args);
        } else {
            sendServerChat(connId, "Command not recognized.  Type /help for a list of commands.");
        }
    }

    // Easter eggs.  Happy April Fool's Day!!
    private static final String DUNE_CALL = "They tried and failed?";
    private static final String DUNE_RESPONSE = "They tried and died!";
    private static final String STAR_WARS_CALL = "I'd just as soon kiss a Wookiee.";
    private static final String STAR_WARS_RESPONSE = "I can arrange that!";

    /**
     * Process a packet from a connection.
     *
     * @param   id - the <code>int</code> ID the connection that
     *          received the packet.
     * @param   packet - the <code>Packet</code> to be processed.
     */
    protected synchronized void handle(int connId, Packet packet) {
        Player player = game.getPlayer( connId );
        // Check player.  Please note, the connection may be pending.
        if ( null == player && null == getPendingConnection(connId) ) {
            System.err.print
                ( "Server does not recognize player at connection " );
            System.err.println( connId );
            return;
        }

        //System.out.println("s(" + cn + "): received command");
        if (packet == null) {
            System.out.println("server.connection.handle: got null packet");
            return;
        }
        // act on it
        switch(packet.getCommand()) {
            case Packet.COMMAND_CLOSE_CONNECTION :
                // We have a client going down!
                Connection c = getConnection(connId);
                if ( c!= null) {
                    c.close();
                }
                break;
            case Packet.COMMAND_CLIENT_NAME :
                receivePlayerName(packet, connId);
                break;
            case Packet.COMMAND_PLAYER_UPDATE :
                receivePlayerInfo(packet, connId);
                validatePlayerInfo(connId);
                send(createPlayerUpdatePacket(connId));
                break;
            case Packet.COMMAND_PLAYER_READY :
                receivePlayerDone(packet, connId);
                send(createPlayerDonePacket(connId));
                checkReady();
                break;
            case Packet.COMMAND_REROLL_INITIATIVE :
                receiveInitiativeRerollRequest(packet, connId);
                send(createPlayerDonePacket(connId));
                break;
            case Packet.COMMAND_CHAT :
                String chat = (String)packet.getObject(0);
                if (chat.startsWith("/")) {
                    processCommand(connId, chat);
                } else {
                    sendChat(player.getName(), chat);
                }
                // Easter eggs.  Happy April Fool's Day!!
                if ( DUNE_CALL.equals(chat) ) {
                    sendServerChat( DUNE_RESPONSE );
                }
                else if ( STAR_WARS_CALL.equals(chat) ) {
                    sendServerChat( STAR_WARS_RESPONSE );
                }
                break;
            case Packet.COMMAND_ENTITY_MOVE :
                receiveMovement(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_DEPLOY :
                receiveDeployment(packet, connId);
                break;
            case Packet.COMMAND_DEPLOY_MINEFIELDS :
                receiveDeployMinefields(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_ATTACK :
                receiveAttack(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_ADD :
                receiveEntityAdd(packet, connId);
                resetPlayersDone();
                transmitAllPlayerDones();
                break;
            case Packet.COMMAND_ENTITY_UPDATE :
                receiveEntityUpdate(packet, connId);
                resetPlayersDone();
                transmitAllPlayerDones();
                break;
            case Packet.COMMAND_ENTITY_MODECHANGE :
                receiveEntityModeChange(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_SYSTEMMODECHANGE :
                receiveEntitySystemModeChange(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_AMMOCHANGE :
                receiveEntityAmmoChange(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_REMOVE :
                receiveEntityDelete(packet, connId);
                resetPlayersDone();
                transmitAllPlayerDones();
                break;
            case Packet.COMMAND_SENDING_GAME_SETTINGS :
                if (receiveGameOptions(packet, connId)) {
                    resetPlayersDone();
                    transmitAllPlayerDones();
                    send(createGameSettingsPacket());
                    receiveGameOptionsAux(packet, connId);
                }
                break;
            case Packet.COMMAND_SENDING_MAP_SETTINGS :
                MapSettings newSettings=(MapSettings)packet.getObject(0);
                if (!mapSettings.equalMapGenParameters(newSettings)) {
                    sendServerChat("Player " + player.getName() +
                                   " changed mapsettings");
                }
                mapSettings = newSettings;
                newSettings = null;
                mapSettings.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
                resetPlayersDone();
                transmitAllPlayerDones();
                send(createMapSettingsPacket());
                break;
            case Packet.COMMAND_QUERY_MAP_SETTINGS :
                MapSettings temp = (MapSettings)packet.getObject(0);
                temp.setBoardsAvailableVector(scanForBoards(temp.getBoardWidth(), temp.getBoardHeight()));
                temp.removeUnavailable();
                temp.setNullBoards(DEFAULT_BOARD);
                temp.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
                temp.removeUnavailable();
                send(connId, createMapQueryPacket(temp));
                break;
            case Packet.COMMAND_UNLOAD_STRANDED :
                receiveUnloadStranded(packet, connId);
                break;
            case Packet.COMMAND_SET_ARTYAUTOHITHEXES :
                receiveArtyAutoHitHexes(packet, connId);
        }
    }


    /**
     * Listen for incoming clients.
     */
    public void run() {
        Thread currentThread = Thread.currentThread();
        System.out.println("s: listening for clients...");
        while (connector == currentThread) {
            try {
                Socket s = serverSocket.accept();

                int id = getFreeConnectionId();
                System.out.println("s: accepting player connection #" + id + " ...");

                Connection c =  ConnectionFactory.getInstance().createServerConnection(s, id);
                c.addConnectionListener(connectionListener);
                c.open();
                connectionsPending.addElement(c);

                greeting(id);
            } catch(IOException ex) {

            }
        }
    }

    /**
     * Makes one slot of inferno ammo, determined
     * by certain rules, explode on a mech.
     *
     * @param entity  The <code>Entity</code> that should suffer an
     *                inferno ammo explosion.
     */
    private Vector explodeInfernoAmmoFromHeat(Entity entity) {
        int damage = 0;
        int rack = 0;
        int boomloc = -1;
        int boomslot = -1;
        Vector vDesc = new Vector();
        Report r;

        // Find the most destructive Inferno ammo.
        for (int j = 0; j < entity.locations(); j++) {
            for (int k = 0; k < entity.getNumberOfCriticals(j); k++) {
                CriticalSlot cs = entity.getCritical(j, k);
                // Ignore empty, destroyed, hit, and structure slots.
                if ( cs == null || cs.isDestroyed() || cs.isHit() ||
                     cs.getType() != CriticalSlot.TYPE_EQUIPMENT ) {
                    continue;
                }
                // Ignore everything but weapons slots.
                Mounted mounted = entity.getEquipment
                    ( entity.getCritical(j, k).getIndex() );
                if (!(mounted.getType() instanceof AmmoType)) {
                    continue;
                }
                // Ignore everything but Inferno ammo.
                AmmoType atype = (AmmoType)mounted.getType();
                if ( !atype.isExplosive() ||
                     atype.getMunitionType() != AmmoType.M_INFERNO) {
                    continue;
                }
                // Find the most destructive undamaged ammo.
                // BMRr, pg. 48, compare one rack's
                // damage.  Ties go to most rounds.
                int newRack = atype.getDamagePerShot() * atype.getRackSize();
                int newDamage = mounted.getExplosionDamage();
                if ( !mounted.isHit() && ( rack < newRack ||
                       (rack == newRack && damage < newDamage) ) ) {
                    rack = newRack;
                    damage = newDamage;
                    boomloc = j;
                    boomslot = k;
                }
            }
        }
        // Did we find anything to explode?
        if (boomloc != -1 && boomslot != -1) {
            CriticalSlot slot = entity.getCritical(boomloc, boomslot);
            slot.setHit(true);
            entity.getEquipment(slot.getIndex()).setHit(true);
            // We've allocated heatBuildup to heat in resolveHeat(),
            // so need to add to the entity's heat instead.
            vDesc.addAll( explodeEquipment(entity, boomloc, boomslot));
            entity.heat += 30;
            r = new Report(5155);
            r.indent();
            r.subject = entity.getId();
            r.add(entity.heat);
            vDesc.addElement(r);
            entity.heatBuildup = 0;
        } else { //no ammo to explode
            r = new Report(5160);
            r.indent();
            r.subject = entity.getId();
            vDesc.addElement(r);
        }
        return vDesc;
    }

    /**
     * Determine the results of an entity moving through a wall of a building
     * after having moved a certain distance.  This gets called when a Mech
     * or a Tank enters a building, leaves a building, or travels from one
     * hex to another inside a multi-hex building.
     *
     * @param   entity - the <code>Entity</code> that passed through a wall.
     *          Don't pass <code>Infantry</code> units to this method.
     * @param   bldg - the <code>Building</code> the entity is passing through.
     * @param   lastPos - the <code>Coords</code> of the hex the entity is
     *          exiting.
     * @param   curPos - the <code>Coords</code> of the hex the entity is
     *          entering
     * @param   distance - the <code>int</code> number of hexes the entity
     *          has moved already this phase.
     * @param   why - the <code>String</code> explanatin for this action.
     * @return  <code>true</code> if the building collapses due to overloading.
     */
    private boolean passBuildingWall( Entity entity,
                                      Building bldg,
                                      Coords lastPos,
                                      Coords curPos,
                                      int distance,
                                      String why ) {

        Report r;

        // Need to roll based on building type.
        PilotingRollData psr = entity.rollMovementInBuilding(bldg, distance, why);

        // Did the entity make the roll?
        if ( !doSkillCheckWhileMoving( entity, lastPos,
                                       curPos, psr, false ) ) {

            // Divide the building's current CF by 10, round up.
            int damage = (int) Math.ceil( bldg.getCurrentCF() / 10.0 );

            // It is possible that the unit takes no damage.
            if ( damage == 0 ) {
                r = new Report(6440);
                r.add(entity.getDisplayName());
                r.subject = entity.getId();
                r.indent(2);
                addReport(r);
            } else {
                // BMRr, pg. 50: The attack direction for this damage is the front.
                HitData hit = entity.rollHitLocation( ToHitData.HIT_NORMAL,
                                                      ToHitData.SIDE_FRONT );
                addReport( damageEntity(entity, hit, damage));
            }
        }

        // Damage the building.  The CF can never drop below 0.
        int toBldg = (int) Math.ceil( entity.getWeight() / 10.0 );
        int curCF = bldg.getCurrentCF();
        curCF -= Math.min( curCF, toBldg );
        bldg.setCurrentCF( curCF );

        // Apply the correct amount of damage to infantry in the building.
        // ASSUMPTION: We inflict toBldg damage to infantry and
        //             not the amount to bring building to 0 CF.
        this.damageInfantryIn( bldg, toBldg );

        return checkBuildingCollapseWhileMoving(bldg, entity, curPos);
    }
    
    private boolean checkBuildingCollapseWhileMoving(Building bldg, Entity entity, Coords curPos) {
        // Get the position map of all entities in the game.
        Hashtable positionMap = game.getPositionMap();

        // Count the moving entity in its current position, not
        // its pre-move postition.  Be sure to handle nulls.
        Vector entities = null;
        if ( entity.getPosition() != null ) {
            entities = (Vector) positionMap.get( entity.getPosition() );
            entities.removeElement( entity );
        }
        entities = (Vector) positionMap.get( curPos );
        if ( entities == null ) {
            entities = new Vector();
            positionMap.put( curPos, entities );
        }
        entities.addElement( entity );

        // Check for collapse of this building due to overloading, and return.
        return this.checkForCollapse( bldg, positionMap );
    }

    /**
     * Apply the correct amount of damage that passes on to any infantry unit
     * in the given building, based upon the amount of damage the building
     * just sustained.  This amount is a percentage dictated by pg. 52 of BMRr.
     *
     * @param   bldg - the <code>Building</code> that sustained the damage.
     * @param   damage - the <code>int</code> amount of damage.
     */
    private void damageInfantryIn( Building bldg, int damage ) {
        // Calculate the amount of damage the infantry will sustain.
        float percent = 0.0f;
        Report r;
        switch( bldg.getType() ) {
        case Building.LIGHT: percent = 0.75f; break;
        case Building.MEDIUM: percent = 0.5f; break;
        case Building.HEAVY: percent = 0.25f; break;
        }

        // Round up at .5 points of damage.
        int toInf = Math.round( damage * percent );

        // Record if we find any infantry.
        boolean foundInfantry = false;

        // Walk through the entities in the game.
        Enumeration entities = game.getEntities();
        while ( entities.hasMoreElements() ) {
            Entity entity = (Entity) entities.nextElement();
            final Coords coords = entity.getPosition();

            // If the entity is infantry in one of the building's hexes?
            if ( entity instanceof Infantry &&
                 bldg.isIn( coords ) ) {

                // Is the entity is inside of the building
                // (instead of just on top of it)?
                if ( Compute.isInBuilding( game, entity, coords ) ) {
                    
                    // Report if the infantry receive no points of damage.
                    if ( toInf == 0 ) {
                        r = new Report(6445);
                        addReport(r);
                    } else {
                        // Yup.  Damage the entity.
                        // Battle Armor units use 5 point clusters.
                        r = new Report(6450);
                        r.indent(2);
                        r.subject = entity.getId();
                        r.add(entity.getDisplayName());
                        r.add(toInf);
                        addReport(r);
                        int remaining = toInf;
                        int cluster = toInf;
                        if ( entity instanceof BattleArmor ) {
                            cluster = 5;
                        }
                        while ( remaining > 0 ) {
                            int next = Math.min( cluster, remaining );
                            HitData hit = entity.rollHitLocation
                                ( ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT );
                            addReport( damageEntity(entity, hit, next) );
                            remaining -= next;
                        }
                        addReport(new Report(1210));
                    }

                } // End infantry-inside-building

            } // End entity-is-infantry-in-building-hex

        } // Handle the next entity

        // If we found any infantry, add a line to the phase report.
        if ( foundInfantry ) {
            addReport(new Report(1210));
        }

    } // End private void damageInfantryIn( Building, int )

    /**
     * Determine if the given building should collapse.  If so,
     * inflict the appropriate amount of damage on each entity in
     * the building and update the clients.  If the building does
     * not collapse, determine if any entities crash through its
     * floor into its basement.  Again, apply appropriate damage.
     *
     * @param   bldg - the <code>Building</code> being checked.
     *          This value should not be <code>null</code>.
     * @param   positionMap - a <code>Hashtable</code> that maps
     *          the <code>Coords</code> positions or each unit
     *          in the game to a <code>Vector</code> of
     *          <code>Entity</code>s at that position.
     *          This value should not be <code>null</code>.
     * @return  <code>true</code> if the building collapsed.
     */
    public boolean checkForCollapse( Building bldg, Hashtable positionMap ) {

        // If the input is meaningless, do nothing and throw no exception.
        if ( bldg == null ||
             positionMap == null || positionMap.isEmpty() ) {
            return false;
        }

        // Get the building's current CF.
        final int currentCF = bldg.getCurrentCF();

        // Track all units that fall into the building's basement by Coords.
        Hashtable basementMap = new Hashtable();

        // Walk through the hexes in the building, looking for a collapse.
        Enumeration bldgCoords = bldg.getCoords();
        boolean collapse = false;
        while ( !collapse && bldgCoords.hasMoreElements() ) {
            final Coords coords = (Coords) bldgCoords.nextElement();

            // Get the Vector of Entities at these coordinates.
            final Vector vector = (Vector) positionMap.get( coords );

            // Are there any Entities at these coords?
            if ( vector != null ) {

                // How many levels does this building have in this hex?
                final IHex curHex = game.getBoard().getHex( coords );
                final int hexElev = curHex.surface();
                final int numFloors = Math.max(0,curHex.terrainLevel( Terrains.BLDG_ELEV )); 
                final int bridgeEl = curHex.terrainLevel(Terrains.BRIDGE_ELEV);
                int numLoads = numFloors;
                if(bridgeEl != ITerrain.LEVEL_NONE) {
                    numLoads ++;
                }
                if(numLoads < 1) {
                    System.err.println("Check for collapse: hex "+coords.toString()+" has no bridge or building");
                    continue;
                }

                // Track the load of each floor (and of the roof) separately.
                // Track all units that fall into the basement in this hex.
                // N.B. don't track the ground floor, the first floor is at
                // index 0, the second is at index 1, etc., and the roof is
                // at index (numFloors-1).
                // if bridge is present, bridge will be numFloors
                int[] loads = new int[numLoads];
                Vector basement = new Vector();
                for ( int loop = 0; loop < numLoads; loop++ ) {
                    loads[loop] = 0;
                }

                // Walk through the entities in this position.
                Enumeration entities = vector.elements();
                while ( !collapse && entities.hasMoreElements() ) {
                    final Entity entity = (Entity) entities.nextElement();
                    final int entityElev = entity.getElevation();

                    if(entityElev != bridgeEl) {
                          // Ignore entities not *inside* the building
                          if ( entityElev > numFloors) {
                              continue;
                          }
                    }
                    
                    if(entity.getMovementMode() == IEntityMovementMode.HYDROFOIL
                            || entity.getMovementMode() == IEntityMovementMode.NAVAL
                            || entity.getMovementMode() == IEntityMovementMode.SUBMARINE) {
                        continue; //under the bridge even at same level
                    }

                    // Add the weight of a Mek or tank to the correct floor.
                    if ( entity instanceof Mech || entity instanceof Tank ) {
                        int load = (int) entity.getWeight();
                        int floor = entityElev;
                        if(floor == bridgeEl) {
                            floor = numLoads;
                        }

                        // Entities on the ground floor may fall into the
                        // basement, but they won't collapse the building.
                        if ( numFloors > 0 && floor == 0 && load > currentCF ) {
                            basement.addElement( entity );
                        } else if ( floor > 0 ) {

                            // If the load on any floor but the ground floor
                            // exceeds the building's current CF it collapses.
                            floor--;
                            loads[ floor ] += load;
                            if ( loads[ floor ] > currentCF ) {
                                collapse = true;
                            }

                        } // End not-ground-floor

                    } // End increase-load

                } // Handle the next entity.

                // Track all entities that fell into the basement.
                if ( !basement.isEmpty() ) {
                    basementMap.put( coords, basement );
                }

            } // End have-entities-here

        } // Check the next hex of the building.

        // Collapse the building if the flag is set.
        if ( collapse ) {
            Report r = new Report(2375);
            r.add(bldg.getName());
            addReport(r);
            this.collapseBuilding( bldg, positionMap );
        }

        // Otherwise, did any entities fall into the basement?
        else if ( !basementMap.isEmpty() ) {
            // TODO: implement basements
        }

        // Return true if the building collapsed.
        return collapse;

    } // End private boolean checkForCollapse( Building, Hashtable )

    /**
     * Collapse the building.  Inflict the appropriate amount of damage
     * on all entities in the building.  Update all clients.
     *
     * @param   bldg - the <code>Building</code> that has collapsed.
     * @param   positionMap - a <code>Hashtable</code> that maps
     *          the <code>Coords</code> positions or each unit
     *          in the game to a <code>Vector</code> of
     *          <code>Entity</code>s at that position.
     *          This value should not be <code>null</code>.
     */
    public void collapseBuilding( Building bldg, Hashtable positionMap ) {
        // Loop through the hexes in the building, and apply
        // damage to all entities inside or on top of the building.
        Report r;
        final int phaseCF = bldg.getPhaseCF();
        Enumeration bldgCoords = bldg.getCoords();
        while ( bldgCoords.hasMoreElements() ) {
            final Coords coords = (Coords) bldgCoords.nextElement();

            // Get the Vector of Entities at these coordinates.
            final Vector vector = (Vector) positionMap.get( coords );

            // Are there any Entities at these coords?
            if ( vector != null ) {

                // How many levels does this building have in this hex?
                final IHex curHex = game.getBoard().getHex( coords );
                final int hexElev = curHex.surface();
                final int bridgeEl = curHex.terrainLevel(Terrains.BRIDGE_ELEV);
                final int numFloors = Math.max(bridgeEl, curHex.terrainLevel( Terrains.BLDG_ELEV ));

                // Sort in elevation order
                Collections.sort(vector, new Comparator<Entity>() {
                    public int compare(Entity a, Entity b) {
                        if(a.getElevation() > b.getElevation())
                            return -1;
                        else if(a.getElevation() > b.getElevation())
                            return 1;
                        return 0;
                    }
                });
                // Walk through the entities in this position.
                Enumeration entities = vector.elements();
                while ( entities.hasMoreElements() ) {
                    final Entity entity = (Entity) entities.nextElement();
                    //final int entityElev = entity.elevationOccupied( curHex );
                    int floor = entity.getElevation();

                    // Ignore units above the building / bridge.
                    if ( floor > numFloors) {
                        continue;
                    }

                    // Treat units on the roof like
                    // they were in the top floor.
                    if ( floor == numFloors ) {
                        floor--;
                    }

                    // Calculate collapse damage for this entity.
                    int damage = (int) Math.ceil
                        ( phaseCF * (numFloors-floor) / 10.0 );

                    // Infantry suffer triple damage.
                    if ( entity instanceof Infantry ) {
                        damage *= 3;
                    }

                    // Apply collapse damage the entity.
                    // ASSUMPTION: use 5 point clusters.
                    r = new Report(6455);
                    r.indent();
                    r.subject = entity.getId();
                    r.add(entity.getDisplayName());
                    r.add(damage);
                    addReport(r);
                    int remaining = damage;
                    int cluster = damage;
                    if ( entity instanceof BattleArmor ||
                         entity instanceof Mech ||
                         entity instanceof Tank ) {
                        cluster = 5;
                    }
                    while ( remaining > 0 ) {
                        int next = Math.min( cluster, remaining );
                        // In www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf,
                        // pg. 18, Randall Bills says that all damage from a
                        // collapsing building is applied to the front.

                        HitData hit = entity.rollHitLocation
                            (ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT );
                        addReport( damageEntity(entity, hit, next) );
                        remaining -= next;
                    }
                    addReport(new Report(1210));
                    // TODO: Why are dead entities showing up on firing phase?

                    // Do we need to handle falling Meks?
                    // BMRr, pg. 53 only mentions falling BattleMechs;
                    // Tanks can't be above the floor and I guess that
                    // infantry don't suffer falling damage.
                    // TODO: implement basements, then fall into it.
                    // ASSUMPTION: we'll let the Mech fall twice: once
                    // during damageEntity() above and once here.
                    floor = entity.getElevation();
                    if ( floor > 0 || floor == bridgeEl) {
                        // ASSUMPTION: PSR to avoid pilot damage
                        // should use mods for entity damage and
                        // 20+ points of collapse damage (if any).
                        PilotingRollData psr = entity.getBasePilotingRoll();
                        entity.addPilotingModifierForTerrain(psr, coords);
                        if ( damage >= 20 ) {
                            psr.addModifier( 1, "20+ damage" );
                        }
                        this.doEntityFallsInto( entity, coords, coords, psr );
                    }

                    // Update this entity.
                    // ASSUMPTION: this is the correct thing to do.
                    this.entityUpdate( entity.getId() );

                } // Handle the next entity.

            } // End have-entities-here.

        } // Handle the next hex of the building.

        // Update the building.
        bldg.setCurrentCF( 0 );
        bldg.setPhaseCF( 0 );
        send( createCollapseBuildingPacket(bldg) );
        game.getBoard().collapseBuilding( bldg );

    } // End private void collapseBuilding( Building )

    /**
     * Tell the clients to replace the given building with rubble hexes.
     *
     * @param   bldg - the <code>Building</code> that has collapsed.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createCollapseBuildingPacket( Building bldg ) {
        Vector buildings = new Vector();
        buildings.addElement( bldg );
        return this.createCollapseBuildingPacket( buildings );
    }

    /**
     * Tell the clients to replace the given buildings with rubble hexes.
     *
     * @param   buildings - a <code>Vector</code> of <code>Building</code>s
     *          that has collapsed.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createCollapseBuildingPacket( Vector buildings ) {
        return new Packet( Packet.COMMAND_BLDG_COLLAPSE, buildings );
    }

    /**
     * Tell the clients to update the CFs of the given buildings.
     *
     * @param   buildings - a <code>Vector</code> of <code>Building</code>s
     *          that need to be updated.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createUpdateBuildingCFPacket( Vector buildings ) {
        return new Packet( Packet.COMMAND_BLDG_UPDATE_CF, buildings );
    }

    /**
     * Apply this phase's damage to all buildings.  Buildings may
     * collapse due to damage.
     */
    private void applyBuildingDamage() {

        // Walk through the buildings in the game.
        // Build the collapse and update vectors as you go.
        // N.B. never, NEVER, collapse buildings while you are walking through
        //      the Enumeration from megamek.common.Board#getBuildings.
        Vector collapse = new Vector();
        Vector update = new Vector();
        Enumeration buildings = game.getBoard().getBuildings();
        while ( buildings.hasMoreElements() ) {
            Building bldg = (Building) buildings.nextElement();

            // If the CF is zero, the building should fall.
            if ( bldg.getCurrentCF() == 0 ) {
                collapse.addElement( bldg );
            }

            // If the building took damage this round, update it.
            else if ( bldg.getPhaseCF() != bldg.getCurrentCF() ) {
                bldg.setPhaseCF( bldg.getCurrentCF() );
                update.addElement( bldg );
            }

        } // Handle the next building

        // If we have any buildings to collapse, collapse them now.
        if ( !collapse.isEmpty() ) {

            // Get the position map of all entities in the game.
            Hashtable positionMap = game.getPositionMap();

            // Walk through the buildings that have collapsed.
            buildings = collapse.elements();
            while ( buildings.hasMoreElements() ) {
                Building bldg = (Building) buildings.nextElement();
                Report r = new Report(6460,Report.PUBLIC);
                r.add(bldg.getName());
                addReport(r);
                this.collapseBuilding( bldg, positionMap );
            }

        }

        //check for buildings which should collapse due to being overloaded now CF is reduced
        if(!update.isEmpty()) {
            Hashtable positionMap = game.getPositionMap();
            for(Iterator i=update.iterator();i.hasNext();) {
                Building bldg = (Building) i.next();
                if(checkForCollapse(bldg, positionMap)) i.remove();
            }
        }

        // If we have any buildings to update, send the message.
        if ( !update.isEmpty() ) {
            sendChangedCFBuildings( update );
        }
    }

    /**
     * Apply the given amount of damage to the building.  Please note,
     * this method does <b>not</b> apply any damage to units inside the
     * building, update the clients, or check for the building's collapse.
     * <p/>
     * A default message will be used to describe why the building
     * took the damage.
     *
     * @param   bldg - the <code>Building</code> that has been damaged.
     *          This value should not be <code>null</code>, but no
     *          exception will occur.
     * @param   damage - the <code>int</code> amount of damage.
     * @return  a <code>Report</code> to be shown to the players.
     */
    private Report damageBuilding( Building bldg, int damage ) {
        final String defaultWhy = " absorbs ";
        return damageBuilding( bldg, damage, defaultWhy );
    }

    /**
     * Apply the given amount of damage to the building.  Please note,
     * this method does <b>not</b> apply any damage to units inside the
     * building, update the clients, or check for the building's collapse.
     *
     * @param   bldg - the <code>Building</code> that has been damaged.
     *          This value should not be <code>null</code>, but no
     *          exception will occur.
     * @param   damage - the <code>int</code> amount of damage.
     * @param   why - the <code>String</code> message that describes
     *          why the building took the damage.
     * @return  a <code>Report</code> to be shown to the players.
     */
    private Report damageBuilding( Building bldg, int damage, String why ) {
        Report r = new Report(1210);
        r.newlines = 0;

        // Do nothing if no building or no damage was passed.
        if ( bldg != null && damage > 0 ) {
            int curCF = bldg.getCurrentCF();
            final int startingCF = curCF;
            curCF -= Math.min( curCF, damage );
            bldg.setCurrentCF( curCF );
            r.messageId = 3435;
            r.add( bldg.getName() );
            r.add( why );
            r.add( damage );

            // If the CF is zero, the building should fall.
            if ( curCF == 0 && startingCF != 0 ) {
                r.messageId = 3440;
            }

        }
        return r;
    }

    public void sendChangedCFBuildings(Vector<Building> buildings) {
        send(createUpdateBuildingCFPacket(buildings));
    }
    
    /**
     * Receives an packet to unload entityis stranded on immobile transports,
     * and queue all valid requests for execution.  If all players that have
     * stranded entities have answered, executes the pending requests and end
     * the current turn.
     */
    private void receiveUnloadStranded( Packet packet, int connId ) {
        GameTurn.UnloadStrandedTurn turn = null;
        final Player player = game.getPlayer( connId );
        int[] entityIds = (int[]) packet.getObject(0);
        Vector declared = null;
        Player other = null;
        Enumeration pending = null;
        UnloadStrandedAction action = null;
        Entity entity = null;

        // Is this the right phase?
        if (game.getPhase() != IGame.PHASE_MOVEMENT) {
            System.err.println
                ("error: server got unload stranded packet in wrong phase");
            return;
        }

        // Are we in an "unload stranded entities" turn?
        if ( game.getTurn() instanceof GameTurn.UnloadStrandedTurn ) {
            turn = (GameTurn.UnloadStrandedTurn) game.getTurn();
        } else {
            System.err.println
                ("error: server got unload stranded packet out of sequence");
            StringBuffer message = new StringBuffer();
            message.append( player.getName() )
                .append( " should not be sending 'unload stranded entity' packets at this time." );
            sendServerChat( message.toString() );
            return;
        }

        // Can this player act right now?
        if (!turn.isValid(connId, game)) {
            System.err.println
                ("error: server got unload stranded packet from invalid player");
            StringBuffer message = new StringBuffer();
            message.append( player.getName() )
                .append( " should not be sending 'unload stranded entity' packets." );
            sendServerChat( message.toString() );
            return;
        }

        // Did the player already send an 'unload' request?
        // N.B. we're also building the list of players who
        //      have declared their "unload stranded" actions.
        declared = new Vector();
        pending = game.getActions();
        while ( pending.hasMoreElements() ) {
            action = (UnloadStrandedAction) pending.nextElement();
            if ( action.getPlayerId() == connId ) {
                System.err.println("error: server got multiple unload stranded packets from player");
                StringBuffer message = new StringBuffer();
                message.append( player.getName() )
                    .append( " should not send multiple 'unload stranded entity' packets." );
                sendServerChat( message.toString() );
                return;
            } else {
                // This player is not from the current connection.
                // Record this player to determine if this turn is done.
                other = game.getPlayer( action.getPlayerId() );
                if ( !declared.contains( other ) ) {
                    declared.addElement( other );
                }
            }
        } // Handle the next "unload stranded" action.

        // Make sure the player selected at least *one* valid entity ID.
        boolean foundValid = false;
        for ( int index = 0; null != entityIds && index < entityIds.length;
              index++ ) {
            entity = game.getEntity( entityIds[index] );
            if (!game.getTurn().isValid(connId, entity, game)) {
                System.err.println("error: server got unload stranded packet for invalid entity");
                StringBuffer message = new StringBuffer();
                message.append( player.getName() )
                    .append( " can not unload stranded entity " );
                if ( null == entity ) {
                    message.append( "#" )
                        .append( entityIds[index] );
                } else {
                    message.append( entity.getDisplayName() );
                }
                message.append( " at this time." );
                sendServerChat( message.toString() );
            } else {
                foundValid = true;
                game.addAction( new UnloadStrandedAction( connId,
                                                          entityIds[index] ) );
            }
        }

        // Did the player choose not to unload any valid stranded entity?
        if ( !foundValid ) {
            game.addAction( new UnloadStrandedAction( connId, Entity.NONE ) );
        }

        // Either way, the connection's player has now declared.
        declared.addElement( player );

        // Are all players who are unloading entities done? Walk
        // through the turn's stranded entities, and look to see
        // if their player has finished their turn.
        entityIds = turn.getEntityIds();
        for ( int index = 0; index < entityIds.length; index++ ) {
            entity = game.getEntity( entityIds[index] );
            other = entity.getOwner();
            if ( !declared.contains( other ) ) {
                // At least one player still needs to declare.
                return;
            }
        }

        // All players have declared whether they're unloading stranded units.
        // Walk the list of pending actions and unload the entities.
        pending = game.getActions();
        while ( pending.hasMoreElements() ) {
            action = (UnloadStrandedAction) pending.nextElement();

            // Some players don't want to unload any stranded units.
            if ( Entity.NONE != action.getEntityId() ) {
                entity = game.getEntity( action.getEntityId() );
                if ( null == entity ) {
                    // After all this, we couldn't find the entity!!!
                    System.err.print
                        ("error: server could not find stranded entity #");
                    System.err.print( action.getEntityId() );
                    System.err.println( " to unload!!!");
                } else {
                    // Unload the entity.  Get the unit's transporter.
                    Entity transporter =
                        game.getEntity( entity.getTransportId() );
                    this.unloadUnit( transporter, entity,
                                     transporter.getPosition(),
                                     transporter.getFacing(),
                                     transporter.getElevation());
                }
            }

        } // Handle the next pending unload action

        // Clear the list of pending units and move to the next turn.
        game.resetActions();
        changeToNextTurn();
    }
    /**
     * For all current artillery attacks in the air from this entity
     * with this weapon, clear the list of spotters.  Needed because
     * firing another round before first lands voids spotting.
     *
     * @param entityID int
     */
    private void clearArtillerySpotters(int entityID,int weaponID)  {
        for (Enumeration i = game.getArtilleryAttacks(); i.hasMoreElements();) {
            ArtilleryAttackAction aaa = (ArtilleryAttackAction) i.nextElement();
            if ( aaa.getWR().waa.getEntityId()==entityID &&
                 aaa.getWR().waa.getWeaponId()==weaponID ) {
                aaa.setSpotterIds(null);
            }

        }
    }

    /**
    * Find the tagged entity for this attack
    *
    * Each TAG will attract a number of shots up to its priority number (mode setting)
    * When all the TAGs are used up, the shots fired are reset.
    * So if you leave them all on 1-shot, then homing attacks will be evenly split, however many shots you fire.
    *
    * Priority setting is to allocate more homing attacks to a more important target as decided by player.
    * 
    * TAGs fired by the enemy aren't eligable, nor are TAGs fired at a target on a different map sheet.
    */
    private WeaponResult convertHomingShotToEntityTarget(ArtilleryAttackAction aaa, Entity ae) {
        WeaponResult wr = aaa.getWR();
        Targetable target = wr.waa.getTarget(game);
        
        final Coords tc = target.getPosition();
        Entity entityTarget = null;

        TagInfo info = null;
        Entity tagger = null;

        for(int pass=0;pass<2;pass++) {
            int bestDistance = Integer.MAX_VALUE;
            int bestIndex = -1;
            Vector v = game.getTagInfo();
            for(int i=0;i<v.size();i++) {
                info = (TagInfo)v.elementAt(i);
                tagger = game.getEntity(info.attackerId);
                if(info.shots < info.priority && !ae.isEnemyOf(tagger)) {
                    System.err.println("Checking TAG "+i+" with priority "+info.priority);
                    entityTarget = game.getEntity(info.targetId);
                    if(entityTarget != null && entityTarget.isOnSameSheet(tc)) {
                        if(tc.distance(entityTarget.getPosition()) < bestDistance) {
                            bestIndex = i;
                            bestDistance = tc.distance(entityTarget.getPosition());
                            if(!game.getOptions().booleanOption("a4homing_target_area")) {
                                break; //first will do if mapsheets can't overlap
                            }
                        }
                    }
                }
            }
            if(bestIndex != -1) {
                info = (TagInfo)v.elementAt(bestIndex);
                entityTarget = game.getEntity(info.targetId);
                tagger = game.getEntity(info.attackerId);
                System.err.println("attacker: " + ae.getDisplayName());
                System.err.println("   " + tagger.getDisplayName() + " selected to TAG");
                System.err.println("   " + entityTarget.getDisplayName() + " selected as target");
                info.shots++;
                game.updateTagInfo(info,bestIndex);
                break; //got a target, stop searching
            } else {
                entityTarget = null;
            }
            //nothing found on 1st pass, so clear shots fired to 0
            System.err.println("nothing on 1st pass");
            game.clearTagInfoShots(ae, tc);
        }

        if(entityTarget == null || info == null) {
            wr.toHit = new ToHitData(ToHitData.IMPOSSIBLE, "no targets tagged on map sheet");
        } 
        else if(info.missed) {
            wr.waa.setTargetId(entityTarget.getId());
            wr.waa.setTargetType(Targetable.TYPE_ENTITY);
            wr.toHit = new ToHitData(ToHitData.IMPOSSIBLE, "tag missed the target");
        } else {
            //update for hit table resolution
            wr.artyAttackerCoords = tagger.getPosition();
            wr.waa.setTargetId(entityTarget.getId());
            wr.waa.setTargetType(Targetable.TYPE_ENTITY);
        }

        return wr;
    }

    /**
     * resolve Indirect Artillery Attacks for this turn
     */
    private void resolveIndirectArtilleryAttacks()  {
        Vector results = new Vector(game.getArtillerySize());
        Vector attacks = new Vector(game.getArtillerySize());

        // loop thru received attack actions, getting weapon results
        for (Enumeration i = game.getArtilleryAttacks(); i.hasMoreElements();) {
            ArtilleryAttackAction aaa =
                (ArtilleryAttackAction) i.nextElement();

            // Does the attack land this turn?
            if (aaa.turnsTilHit <= 0) {
                WeaponResult wr = aaa.getWR();
                //HACK, for correct hit table resolution.
                wr.artyAttackerCoords=aaa.getCoords();
                final Vector spottersBefore=aaa.getSpotterIds();
                final Targetable target = wr.waa.getTarget(game);
                final Coords targetPos = target.getPosition();
                final int playerId = aaa.getPlayerId();
                Entity bestSpotter=null;

                Entity ae = game.getEntity(wr.waa.getEntityId());
                if (ae == null) {
                  ae = game.getOutOfGameEntity( wr.waa.getEntityId() );
                }
                Mounted ammo = ae.getEquipment(wr.waa.getAmmoId());
                final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();
                if(atype != null && atype.getMunitionType() == AmmoType.M_HOMING) {
                    wr = convertHomingShotToEntityTarget(aaa, ae);
                } else {
                    // Are there any valid spotters?
                    if ( null != spottersBefore ) {

                        //fetch possible spotters now
                        Enumeration spottersAfter=
                            game.getSelectedEntities( new EntitySelector() {
                                    public int player = playerId;
                                    public Targetable targ = target;
                                    public boolean accept(Entity entity) {
                                        Integer id = new Integer( entity.getId() );
                                        if ( player == entity.getOwnerId() &&
                                             spottersBefore.contains(id) &&
                                             !( LosEffects.calculateLos
                                                (game, entity.getId(), targ)
                                                ).isBlocked() &&
                                             entity.isActive() &&
                                             !entity.isINarcedWith(INarcPod.HAYWIRE)) {
                                            return true;
                                        }
                                        return false;
                                    }
                                } );

                        // Out of any valid spotters, pick the best.
                        while ( spottersAfter.hasMoreElements() ) {
                            Entity ent = (Entity) spottersAfter.nextElement();
                            if ( bestSpotter == null || ent.crew.getGunnery() <
                                 bestSpotter.crew.getGunnery() ){
                                bestSpotter = ent;
                            }
                        }

                    } // End have-valid-spotters

                    //If at least one valid spotter, then get the benefits thereof.
                    if (null != bestSpotter) {
                        int mod = (bestSpotter.crew.getGunnery() - 4) / 2;
                        wr.toHit.addModifier(mod, "Spotting modifier");
                    }

                    // Is the attacker still alive?
                    Entity artyAttacker = wr.waa.getEntity( game );
                    if (null != artyAttacker) {

                        // Get the arty weapon.
                        Mounted weapon = artyAttacker.getEquipment
                            ( wr.waa.getWeaponId() );

                        // If the shot hit the target hex, then all subsequent
                        // fire will hit the hex automatically.
                        if(wr.roll >= wr.toHit.getValue()) {
                            artyAttacker.aTracker.setModifier
                                ( weapon,
                                  ToHitData.AUTOMATIC_SUCCESS,
                                  targetPos );
                        }
                        // If the shot missed, but was adjusted by a
                        // spotter, future shots are more likely to hit.
                        else if (null != bestSpotter) {
                            artyAttacker.aTracker.setModifier
                                ( weapon,
                                  artyAttacker.aTracker.getModifier
                                  ( weapon, targetPos ) - 1,
                                  targetPos );
                        }

                    } // End artyAttacker-alive
                }

                // Schedule this attack to be resolved.
                results.addElement(wr);
                attacks.addElement( aaa );

            } // End attack-hits-this-turn

            // This attack is one round closer to hitting.
            aaa.turnsTilHit--;

        } // Handle the next attack

        // loop through weapon results and resolve
        int lastEntityId = Entity.NONE;
        for (Enumeration i = results.elements();i.hasMoreElements();) {
            WeaponResult wr = (WeaponResult) i.nextElement();
            resolveWeaponAttack(wr, lastEntityId);
            lastEntityId = wr.waa.getEntityId();
        }

        // Clear out all resolved attacks.
        for (Enumeration i = attacks.elements(); i.hasMoreElements();) {
            game.removeArtilleryAttack
                ( (ArtilleryAttackAction) i.nextElement() );
        }
        for(Enumeration i = game.getPlayers();i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            int connId = player.getId();
            send(connId, createArtilleryPacket(player));
        }
    }

    /**
     * enqueues any indirect artillery attacks made this turn
     */
    private void enqueueIndirectArtilleryAttacks() {
        resolveAllButWeaponAttacks();
        ArtilleryAttackAction aaa;
        for (Enumeration i = game.getActions();i.hasMoreElements();) {
            EntityAction ea = (EntityAction) i.nextElement();
            final Entity firingEntity = game.getEntity(ea.getEntityId());
            if (ea instanceof WeaponAttackAction) {
                final WeaponAttackAction waa = (WeaponAttackAction) ea;
                WeaponResult wr = preTreatWeaponAttack(waa);
                boolean firingAtNewHex = false;
                for (Enumeration j = game.getArtilleryAttacks();
                     !firingAtNewHex && j.hasMoreElements();) {
                    ArtilleryAttackAction oaaa = (ArtilleryAttackAction) j.nextElement();
                    if ( oaaa.getWR().waa.getEntityId() == wr.waa.getEntityId() &&
                         !oaaa.getWR().waa.getTarget(game).getPosition().equals(wr.waa.getTarget(game).getPosition())) {
                        firingAtNewHex = true;
                    }
                }
                if (firingAtNewHex) {
                    clearArtillerySpotters( firingEntity.getId(),
                                            waa.getWeaponId() );
                }
                Enumeration spotters = game.getSelectedEntities(new EntitySelector() {
                    public int player = firingEntity.getOwnerId();
                    public Targetable target = waa.getTarget(game);
                    public boolean accept(Entity entity) {
                        if ( (player == entity.getOwnerId()) &&
                            !((LosEffects.calculateLos(game, entity.getId(), target)).isBlocked()) && entity.isActive()) {
                            return true;
                        } else {
                            return false;
                        }

                    }
                } );

                Vector spotterIds = new Vector();
                while ( spotters.hasMoreElements() ) {
                    Integer id = new Integer
                        ( ((Entity) spotters.nextElement() ).getId() );
                    spotterIds.addElement( id );
                }
                aaa = new ArtilleryAttackAction( wr, game,
                                                 firingEntity.getOwnerId(),
                                                 spotterIds, firingEntity.getPosition());
                game.addArtilleryAttack(aaa);
            }
        }
        game.resetActions();
        for(Enumeration i = game.getPlayers();i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            int connId = player.getId();
            send(connId, createArtilleryPacket(player));
        }
    }

    /**
     * Credits a Kill for an entity, if the target got killed.
     *
     * @param target   The <code>Entity</code> that got killed.
     * @param attacker The <code>Entity</code> that did the killing.
     */
    private void creditKill(Entity target, Entity attacker) {
        if (target.isDoomed() && !target.getGaveKillCredit()) {
            attacker.addKill(target);
        }
    }

    /**
     * pre-treats a physical attack
     *
     * @param aaa The <code>AbstractAttackAction</code> of the physical attack
     *            to pre-treat
     *
     * @return    The <code>PhysicalResult</code> of that action, including
     *            possible damage.
     */
    private PhysicalResult preTreatPhysicalAttack(AbstractAttackAction aaa) {
        final Entity ae = game.getEntity(aaa.getEntityId());
        int damage = 0;
        PhysicalResult pr = new PhysicalResult();
        ToHitData toHit = new ToHitData();
        pr.roll = Compute.d6(2);
        pr.aaa = aaa;
        if (aaa instanceof BrushOffAttackAction) {
            BrushOffAttackAction baa = (BrushOffAttackAction)aaa;
            int arm = baa.getArm();
            baa.setArm(BrushOffAttackAction.LEFT);
            toHit = BrushOffAttackAction.toHit(game, aaa.getEntityId(), aaa.getTarget(game), BrushOffAttackAction.LEFT);
            baa.setArm(BrushOffAttackAction.RIGHT);
            pr.toHitRight = BrushOffAttackAction.toHit(game, aaa.getEntityId(), aaa.getTarget(game), BrushOffAttackAction.RIGHT);
            damage = BrushOffAttackAction.getDamageFor(ae, BrushOffAttackAction.LEFT);
            pr.damageRight = BrushOffAttackAction.getDamageFor(ae, BrushOffAttackAction.RIGHT);
            baa.setArm(arm);
            pr.rollRight = Compute.d6(2);
        } else if (aaa instanceof ChargeAttackAction) {
            ChargeAttackAction caa = (ChargeAttackAction)aaa;
            toHit = caa.toHit(game);
            damage = ChargeAttackAction.getDamageFor(ae);
        } else if (aaa instanceof ClubAttackAction) {
            ClubAttackAction caa = (ClubAttackAction)aaa;
            toHit = caa.toHit(game);
            damage = ClubAttackAction.getDamageFor(ae, caa.getClub());
        } else if (aaa instanceof DfaAttackAction) {
            DfaAttackAction daa = (DfaAttackAction)aaa;
            toHit = daa.toHit(game);
            damage = DfaAttackAction.getDamageFor(ae);
        } else if (aaa instanceof KickAttackAction) {
            KickAttackAction kaa = (KickAttackAction)aaa;
            toHit = kaa.toHit(game);
            damage = KickAttackAction.getDamageFor(ae, kaa.getLeg());
        } else if (aaa instanceof ProtomechPhysicalAttackAction) {
            ProtomechPhysicalAttackAction paa = (ProtomechPhysicalAttackAction)aaa;
            toHit = paa.toHit(game);
            damage = ProtomechPhysicalAttackAction.getDamageFor(ae);
        } else if (aaa instanceof PunchAttackAction) {
            PunchAttackAction paa = (PunchAttackAction)aaa;
            int arm = paa.getArm();
            int damageRight = 0;
            paa.setArm(PunchAttackAction.LEFT);
            toHit = paa.toHit(game);
            paa.setArm(PunchAttackAction.RIGHT);
            ToHitData toHitRight = paa.toHit(game);
            damage = PunchAttackAction.getDamageFor(ae, PunchAttackAction.LEFT);
            damageRight = PunchAttackAction.getDamageFor(ae, PunchAttackAction.RIGHT);
            paa.setArm(arm);
            // If we're punching while prone (at a Tank,
            // duh), then we can only use one arm.
            if ( ae.isProne() ) {
                double oddsLeft = Compute.oddsAbove(toHit.getValue());
                double oddsRight = Compute.oddsAbove(toHitRight.getValue());
                // Use the best attack.
                if (  oddsLeft*damage > oddsRight*damageRight ) {
                    paa.setArm(PunchAttackAction.LEFT);
                } else paa.setArm(PunchAttackAction.RIGHT);
            }
            pr.damageRight = damageRight;
            pr.toHitRight = toHitRight;
            pr.rollRight = Compute.d6(2);
        } else if (aaa instanceof PushAttackAction) {
            PushAttackAction paa = (PushAttackAction)aaa;
            toHit = paa.toHit(game);
        } else if (aaa instanceof LayExplosivesAttackAction) {
            LayExplosivesAttackAction leaa = (LayExplosivesAttackAction)aaa;
            toHit = leaa.toHit(game);
            damage = LayExplosivesAttackAction.getDamageFor(ae);
        } else if (aaa instanceof ThrashAttackAction) {
            ThrashAttackAction taa = (ThrashAttackAction)aaa;
            toHit = taa.toHit(game);
            damage = ThrashAttackAction.getDamageFor(ae);
        }
        pr.toHit = toHit;
        pr.damage = damage;
        return pr;
    }

    /**
     * Resolve a Physical Attack
     *
     * @param pr  The <code>PhysicalResult</code> of the physical attack
     * @param cen The <code>int</code> Entity Id of the entit's whose
     *            physical attack was last resolved
     */
    private void resolvePhysicalAttack(PhysicalResult pr, int cen) {
        AbstractAttackAction aaa = pr.aaa;
        if (aaa instanceof PunchAttackAction) {
            PunchAttackAction paa = (PunchAttackAction)aaa;
            if (paa.getArm() == PunchAttackAction.BOTH) {
                paa.setArm(PunchAttackAction.LEFT);
                pr.aaa = paa;
                resolvePunchAttack(pr, cen);
                cen = paa.getEntityId();
                paa.setArm(PunchAttackAction.RIGHT);
                pr.aaa = paa;
                resolvePunchAttack(pr, cen);
            } else {
                resolvePunchAttack(pr, cen);
                cen = paa.getEntityId();
            }
        } else if (aaa instanceof KickAttackAction) {
            resolveKickAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof BrushOffAttackAction) {
            BrushOffAttackAction baa = (BrushOffAttackAction)aaa;
            if (baa.getArm() == BrushOffAttackAction.BOTH) {
                baa.setArm(BrushOffAttackAction.LEFT);
                pr.aaa = baa;
                resolveBrushOffAttack(pr, cen);
                cen = baa.getEntityId();
                baa.setArm(BrushOffAttackAction.RIGHT);
                pr.aaa = baa;
                resolveBrushOffAttack(pr, cen);
            } else {
                resolveBrushOffAttack(pr, cen);
                cen = baa.getEntityId();
            }
        } else if (aaa instanceof ThrashAttackAction) {
            resolveThrashAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof ProtomechPhysicalAttackAction) {
            resolveProtoAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof ClubAttackAction) {
            resolveClubAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof PushAttackAction) {
            resolvePushAttack(pr, cen);
            cen = aaa.getEntityId();
        }  else if (aaa instanceof ChargeAttackAction) {
            resolveChargeAttack(pr, cen);
            cen = aaa.getEntityId();
        }  else if (aaa instanceof DfaAttackAction) {
            resolveDfaAttack(pr, cen);
            cen = aaa.getEntityId();
        }  else if (aaa instanceof LayExplosivesAttackAction) {
            resolveLayExplosivesAttack(pr, cen);
            cen = aaa.getEntityId();
        } else {
            // hmm, error.
        }
        // Not all targets are Entities.
        Targetable target = game.getTarget( aaa.getTargetType(),
                                        aaa.getTargetId() );
        if ( target instanceof Entity ) {
            creditKill( (Entity) target, game.getEntity(cen) );
        }
    }

    /**
     * Add any extreme gravity PSRs the entity gets due to its movement
     *
     * @param entity The <code>Entity</code> to check.
     * @param step   The last <code>MoveStep</code> of this entity
     * @param curPos The current <code>Coords</code> of this entity
     * @param cachedMaxMPExpenditure Server checks run/jump MP at start of move, as appropriate, caches to avoid mid-move change in MP causing erroneous grav check
     */
    private void checkExtremeGravityMovement(Entity entity, MoveStep step, Coords curPos, int cachedMaxMPExpenditure) {
        PilotingRollData rollTarget;
        if (game.getOptions().floatOption("gravity") != 1) {
            if (entity instanceof Mech) {
                if (step.getMovementType() == IEntityMovementType.MOVE_WALK
                        || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK)
                        || step.getMovementType() == IEntityMovementType.MOVE_RUN
                        || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN)) {
                    if (step.getMpUsed() > cachedMaxMPExpenditure) {
                        // We moved too fast, let's make PSR to see if we get damage
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    }
                } else if (step.getMovementType() == IEntityMovementType.MOVE_JUMP) {
                    if (step.getMpUsed() > cachedMaxMPExpenditure) {
                        // We jumped too far, let's make PSR to see if we get damage
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    } else if (game.getOptions().floatOption("gravity") > 1) {
                        // jumping in high g is bad for your legs
                        rollTarget = entity.getBasePilotingRoll();
                        entity.addPilotingModifierForTerrain(rollTarget, step);
                        rollTarget.append(new PilotingRollData(entity.getId(), 0, "jumped in high gravity"));
                        game.addExtremeGravityPSR(rollTarget);
                    }
                }
            } else if (entity instanceof Tank) {
                if (step.getMovementType() == IEntityMovementType.MOVE_WALK
                        || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK)
                        || step.getMovementType() == IEntityMovementType.MOVE_RUN
                        || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN)) {
                    // For Tanks, we need to check if the tank had
                    // more MPs because it was moving along a road.
                    if ((step.getMpUsed() > cachedMaxMPExpenditure) && !step.isOnlyPavement()) {
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    }
                    else if (step.getMpUsed() > cachedMaxMPExpenditure + 1) {
                        // If the tank was moving on a road, he got a +1 bonus.
                        // N.B. The Ask Precentor Martial forum said that a 4/6
                        //      tank on a road can move 5/7, **not** 5/8.
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    } // End tank-has-road-bonus
                }
            }
        }
    }

    /**
     * Damage the inner structure of a mech's leg / a tank's front.
     * This only happens when the Entity fails an extreme Gravity PSR.
     * @param entity The <code>Entity</code> to damage.
     * @param damage The <code>int</code> amount of damage.
     */
    private void doExtremeGravityDamage(Entity entity, int damage) {
        HitData hit;
        if (entity instanceof BipedMech) {
            for (int i = 6; i<=7; i++) {
                hit = new HitData (i);
                addReport( damageEntity(entity, hit, damage, false, 0, true));
            }
        } if (entity instanceof QuadMech) {
            for (int i = 4; i<=7; i++) {
                hit = new HitData (i);
                addReport( damageEntity(entity, hit, damage, false, 0, true));
            }
        } else if (entity instanceof Tank) {
            hit = new HitData (Tank.LOC_FRONT);
            addReport( damageEntity(entity, hit, damage, false, 0, true));
        }
    }

    /**
     * Eject an Entity.
     * @param entity    The <code>Entity</code> to eject.
     * @param autoEject The <code>boolean</code> state of the entity's auto-
     *                  ejection system
     * @return a <code>Vector</code> of report objects for the gamelog.
     */
    public Vector ejectEntity(Entity entity, boolean autoEject) {
        Vector vDesc = new Vector();
        Report r;

        // An entity can only eject it's crew once.
        if (entity.getCrew().isEjected())
            return vDesc;
        
        // If the crew are already dead, don't bother
        if(entity.getCrew().isDead() || entity.getCrew().isDoomed())
            return vDesc;

        // Mek pilots may get hurt during ejection,
        // and run around the board afterwards.
        if (entity instanceof Mech) {
            PilotingRollData rollTarget = new PilotingRollData(entity.getId(), entity.getCrew().getPiloting(), "ejecting");
            if (entity.isProne()) {
                rollTarget.addModifier(5, "Mech is prone");
            }
            if (entity.getCrew().isUnconscious()) {
                rollTarget.addModifier(3, "pilot unconscious");
            }
            if (autoEject) {
                rollTarget.addModifier(1, "automatic ejection");
            }
            if (entity.getInternal(Mech.LOC_HEAD) < 3) {
                rollTarget.addModifier(Math.min(3 - entity.getInternal(Mech.LOC_HEAD),2), "Head Internal Structure Damage");
            }
            int facing = entity.getFacing();
            Coords targetCoords = entity.getPosition().translated((facing + 3)%6);
            IHex targetHex = game.getBoard().getHex(targetCoords);
            if (targetHex != null) {
                if (targetHex.terrainLevel(Terrains.WATER) > 0 && !(targetHex.containsTerrain(Terrains.ICE))) {
                    rollTarget.addModifier(-1, "landing in water");
                } else if (targetHex.containsTerrain(Terrains.ROUGH)) {
                    rollTarget.addModifier(0, "landing in rough");
                } else if (targetHex.containsTerrain(Terrains.RUBBLE)) {
                    rollTarget.addModifier(0, "landing in rubble");
                } else if (targetHex.terrainLevel(Terrains.WOODS) == 1) {
                    rollTarget.addModifier(2, "landing in light woods");
                } else if (targetHex.terrainLevel(Terrains.WOODS) == 2) {
                    rollTarget.addModifier(3, "landing in heavy woods");
                } else if (targetHex.terrainLevel(Terrains.WOODS) == 3) {
                    rollTarget.addModifier(4, "landing in ultra heavy woods");
                } else if (targetHex.terrainLevel(Terrains.JUNGLE) == 1) {
                    rollTarget.addModifier(3, "landing in light jungle");
                } else if (targetHex.terrainLevel(Terrains.JUNGLE) == 2) {
                    rollTarget.addModifier(5, "landing in heavy jungle");
                } else if (targetHex.terrainLevel(Terrains.JUNGLE) == 3) {
                    rollTarget.addModifier(7, "landing in ultra heavy jungle");
                } else if (targetHex.terrainLevel(Terrains.BLDG_ELEV) > 0) {
                    rollTarget.addModifier(targetHex.terrainLevel(Terrains.BLDG_ELEV), "landing in a building");
                } else rollTarget.addModifier(-2, "landing in clear terrain");
            } else {
                rollTarget.addModifier(-2, "landing off the board");
            }
            if (autoEject) {
                r = new Report(6395);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.indent(2);
                r.newlines = 0;
                vDesc.addElement(r);
            }
            // okay, print the info
            r = new Report(2180);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(rollTarget.getLastPlainDesc(), true);
            r.indent(3);
            vDesc.addElement(r);
            // roll
            final int diceRoll = Compute.d6(2);
            r = new Report(2190);
            r.subject = entity.getId();
            r.add(rollTarget.getValueAsString());
            r.add(rollTarget.getDesc());
            r.add(diceRoll);
            r.indent(4);
            r.newlines = 0;
            // create the MechWarrior in any case, for campaign tracking
            MechWarrior pilot = new MechWarrior(entity);
            pilot.setDeployed(true);
            pilot.setId(getFreeEntityId());
            game.addEntity(pilot.getId(), pilot);
            send(createAddEntityPacket(pilot.getId()));
            // make him not get a move this turn
            pilot.setDone(true);
            if (diceRoll < rollTarget.getValue()) {
                r.choose(false);
                vDesc.addElement(r);
                Report.addNewline(vDesc);
                Vector v = damageCrew(pilot, 1);
                if(v.size() > 0) {
                    r = (Report)v.firstElement();
                    r.indent(3);
                    vDesc.addElement(r);
                }
            } else {
                r.choose(true);
                vDesc.addElement(r);
            }
            if (entity.getCrew().isDoomed()) {
                vDesc.addAll(
                        destroyEntity(pilot, "deadly ejection", false, false));
            }
            else {
                // Add the pilot as an infantry unit on the battlefield.
                if (game.getBoard().contains(targetCoords)) {
                    pilot.setPosition(targetCoords);
/* Can pilots eject into water???
   ASSUMPTION : They can (because they get a -1 mod to the PSR.
                    // Did the pilot land in water?
                    if ( game.getBoard().getHex( targetCoords).levelOf
                         ( Terrain.WATER ) > 0 ) {
                         //report missing
                        desc.append("and the pilot ejects, but lands in water!!!\n");
                        //report missing
                        desc.append(destroyEntity( pilot, "a watery grave", false ));
                    } else {
                        //report missing
                        desc.append("and the pilot ejects safely!\n");
                    }
*/
                    //report safe ejection
                    r = new Report(6400);
                    r.subject = entity.getId();
                    r.indent(5);
                    vDesc.addElement(r);
                    if (game.getOptions().booleanOption("vacuum")) {
                        //ended up in a vacuum
                        r = new Report(6405);
                        r.subject = entity.getId();
                        r.indent(3);
                        vDesc.addElement(r);
                        vDesc.addAll(
                            destroyEntity(pilot, "explosive decompression", false, false));
                    }
                    // Update the entity
                    this.entityUpdate(pilot.getId());
                    // check if the pilot lands in a minefield
                    doEntityDisplacementMinefieldCheck( pilot,
                            entity.getPosition(),
                            targetCoords );
                } else {
                    //ejects safely
                    r = new Report(6410);
                    r.subject = entity.getId();
                    r.indent(3);
                    vDesc.addElement(r);
                    if (game.getOptions().booleanOption("vacuum")) {
                        //landed in vacuum
                        r = new Report(6405);
                        r.subject = entity.getId();
                        r.indent(3);
                        vDesc.addElement(r);
                        vDesc.addAll(
                            destroyEntity(pilot, "explosive decompression", false, false));
                    } else {
                        game.removeEntity( pilot.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT );
                        send(createRemoveEntityPacket(pilot.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT) );
                    }
                }
                if (game.getOptions().booleanOption("ejected_pilots_flee")) {
                    game.removeEntity(pilot.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT);
                    send(createRemoveEntityPacket(pilot.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT));
                }
            } // Pilot safely ejects.

        } // End entity-is-Mek

        // Mark the entity's crew as "ejected".
        entity.getCrew().setEjected( true );
        vDesc.addAll(
            destroyEntity(entity, "ejection", true, true));

        // only remove the unit that ejected manually
        if (!autoEject) {
            game.removeEntity( entity.getId(), IEntityRemovalConditions.REMOVE_EJECTED );
            send(createRemoveEntityPacket(entity.getId(), IEntityRemovalConditions.REMOVE_EJECTED));
        }
        return vDesc;
    }

    /**
     * Abandon an Entity.
     * @param entity    The <code>Entity</code> to abandon.
     *
     * @return a <code>Vector</code> of report objects for the gamelog.
     */
    public Vector abandonEntity(Entity entity) {
        Vector vDesc = new Vector();
        Report r;

        // An entity can only eject it's crew once.
        if (entity.getCrew().isEjected())
            return vDesc;

        if (entity.getCrew().isDoomed()) 
            return vDesc;
        
        //Don't make them abandon into vacuum
        if (game.getOptions().booleanOption("vacuum")) 
            return vDesc;
        
        Coords targetCoords = entity.getPosition();
        
        if (entity instanceof Mech) {
            // okay, print the info
            r = new Report(2027);
            r.subject = entity.getId();
            r.add(entity.getCrew().getName());
            r.addDesc(entity);
            r.indent(3);
            vDesc.addElement(r);

            // create the MechWarrior in any case, for campaign tracking
            MechWarrior pilot = new MechWarrior(entity);
            pilot.getCrew().setUnconscious(entity.getCrew().isUnconscious());
            pilot.setDeployed(true);
            pilot.setId(getFreeEntityId());
            game.addEntity(pilot.getId(), pilot);
            send(createAddEntityPacket(pilot.getId()));
            // make him not get a move this turn
            pilot.setDone(true);
            // Add the pilot as an infantry unit on the battlefield.
            if (game.getBoard().contains(targetCoords))
                    pilot.setPosition(targetCoords);
            // Update the entity
            this.entityUpdate(pilot.getId());
            // check if the pilot lands in a minefield
            doEntityDisplacementMinefieldCheck( pilot,
                    entity.getPosition(),
                    targetCoords );
            if (game.getOptions().booleanOption("ejected_pilots_flee")) {
                game.removeEntity(pilot.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT);
                send(createRemoveEntityPacket(pilot.getId(), IEntityRemovalConditions.REMOVE_IN_RETREAT));
            }
        } // End entity-is-Mek

        // Mark the entity's crew as "ejected".
        entity.getCrew().setEjected( true );

        return vDesc;
    }

    /**
     * Checks if ejected Mechwarriors are eligible to be picked up,
     * and if so, captures them or picks them up
     */
    private void resolveMechWarriorPickUp() {
        Report r;

        // fetch all mechWarriors that are not picked up
        Enumeration mechWarriors =
            game.getSelectedEntities( new EntitySelector() {
                public boolean accept(Entity entity) {
                    if (entity instanceof MechWarrior) {
                        MechWarrior mw = (MechWarrior)entity;
                        if (mw.getPickedUpById() == Entity.NONE &&
                            !mw.isDoomed() && (mw.getTransportId() == Entity.NONE) ) {
                            return true;
                        }
                    }
                    return false;
                }
            } );
        // loop through them, check if they are in a hex occupied by another
        // unit
        while ( mechWarriors.hasMoreElements() ) {
            boolean pickedUp = false;
            MechWarrior e = (MechWarrior) mechWarriors.nextElement();
            Enumeration pickupEntities = game.getEntities(e.getPosition());
            while (pickupEntities.hasMoreElements() ) {
                Entity pe = (Entity) pickupEntities.nextElement();
                if (pe.isDoomed() || pe.isShutDown() || pe.getCrew().isUnconscious()) {
                    continue;
                }
                if (!pickedUp && pe.getOwnerId() == e.getOwnerId() && pe.getId() != e.getId()) {
                    if (pe instanceof MechWarrior) {
                        //picked up by friendlies
                        r = new Report(6415, Report.PUBLIC); //no subject we can use...
                        r.add(pe.getDisplayName());
                        addReport(r);
                        continue;
                    }
                    // Pick up the unit.
                    pe.pickUp(e);
                    // The picked unit is being carried by the loader.
                    e.setPickedUpById(pe.getId());
                    e.setPickedUpByExternalId(pe.getExternalId());
                    pickedUp = true;
                    r = new Report(6420, Report.PUBLIC);
                    r.add(e.getDisplayName());
                    r.addDesc(pe);
                    addReport(r);
                }
            }
            if (!pickedUp) {
                Enumeration pickupEnemyEntities = game.getEnemyEntities(e.getPosition(), e);
                while (pickupEnemyEntities.hasMoreElements() ) {
                    Entity pe = (Entity) pickupEnemyEntities.nextElement();
                    if (pe.isDoomed() || pe.isShutDown() ||
                        pe.getCrew().isUnconscious()) {
                        continue;
                    }
                    if (pe instanceof MechWarrior) {
                        //picked up by friendlies
                        r = new Report(6415, Report.PUBLIC); //no subject we can use...
                        r.add(pe.getDisplayName());
                        addReport(r);
                        continue;
                    }
                    // Capture the unit.
                    pe.pickUp(e);
                    // The captured unit is being carried by the loader.
                    e.setCaptured( true );
                    e.setPickedUpById(pe.getId());
                    e.setPickedUpByExternalId(pe.getExternalId());
                    pickedUp = true;
                    r = new Report(6420, Report.PUBLIC);
                    r.add(e.getDisplayName());
                    r.addDesc(pe);
                    addReport(r);
                }
            }
            if (pickedUp) {
                // Remove the picked-up unit from the screen.
                e.setPosition( null );
                // Update the loaded unit.
                this.entityUpdate( e.getId() );
            }
        }
    }

    /**
     * destroy all wheeled and tracked Tanks that got displaced into water
     */
    private void resolveSinkVees() {
        Enumeration sinkableTanks =
            game.getSelectedEntities( new EntitySelector() {
            public boolean accept(Entity entity) {
                if (entity.isOffBoard()) {
                    return false;
                }
                
                if (entity instanceof Tank
                        && (entity.getPosition() != null)
                        && (entity.getMovementMode() == IEntityMovementMode.TRACKED
                        || entity.getMovementMode() == IEntityMovementMode.WHEELED )
                        && game.getBoard().getHex(entity.getPosition()).terrainLevel(Terrains.WATER) > 0
                        && entity.getElevation() < 0) {
                        return true;
                }
                return false;
            }
        });
        while (sinkableTanks.hasMoreElements()) {
            Entity e = (Entity)sinkableTanks.nextElement();
            addReport(
                                  destroyEntity(e, "a watery grave", false));
        }
    }

    /**
     * let all Entities make their "break-free-of-swamp-stickyness" PSR
     */
    private void doTryUnstuck() {
        if (game.getPhase() != IGame.PHASE_MOVEMENT)
            return;

        Report r;

        Enumeration stuckEntities =
            game.getSelectedEntities( new EntitySelector() {
            public boolean accept(Entity entity) {
                if (entity.isStuck()) {
                    return true;
                }
                return false;
            }
        });
        PilotingRollData rollTarget;
        while (stuckEntities.hasMoreElements()) {
            Entity entity = (Entity)stuckEntities.nextElement();
            rollTarget = entity.getBasePilotingRoll();
            entity.addPilotingModifierForTerrain(rollTarget);
            // apart from swamp & liquid magma, -1 modifier
            IHex hex = game.getBoard().getHex(entity.getPosition());
            if(!(hex.containsTerrain(Terrains.SWAMP))
                && !(hex.terrainLevel(Terrains.MAGMA) == 2)) {
                rollTarget.addModifier(-1, "bogged down");
            }
            // okay, print the info
            r = new Report(2340);
            r.addDesc(entity);
            addReport(r);

            // roll
            final int diceRoll = Compute.d6(2);
            r = new Report(2190);
            r.add(rollTarget.getValueAsString());
            r.add(rollTarget.getDesc());
            r.add(diceRoll);
            if (diceRoll < rollTarget.getValue()) {
                r.choose(false);
            } else {
                r.choose(true);
                entity.setStuck(false);
                entity.setCanUnstickByJumping(false);
            }
            addReport(r);
        }
    }
    
    /**
     * Remove all iNarc pods from all vehicles that did not
     * move and shoot this round
     * NOTE: this is not quite what the rules say, the player
     * should be able to choose whether or not to remove all iNarc Pods
     * that are attached.
     */
    private void resolveVeeINarcPodRemoval() {
        Enumeration vees =
            game.getSelectedEntities( new EntitySelector() {
            public boolean accept(Entity entity) {
                if (entity instanceof Tank &&
                    entity.mpUsed == 0) {
                    return true;
                }
                return false;
            }
        });
        boolean canSwipePods;
        while (vees.hasMoreElements()) {
            canSwipePods = true;
            Entity entity = (Entity)vees.nextElement();
            for (int i=0;i<=5;i++) {
                if ( entity.weaponFiredFrom(i) ) {
                    canSwipePods = false;
                }
            }
            if (canSwipePods && entity.hasINarcPodsAttached() &&
                entity.getCrew().isActive()) {
                entity.removeAllINarcPods();
                Report r = new Report(2345);
                r.addDesc(entity);
                addReport(r);
            }
        }
    }
    
    private void deadEntitiesCleanup() {
        //See note above where knownDeadEntities variable is declared
        /*
        Entity en = null;
        for(Enumeration k = game.getGraveyardEntities(); k.hasMoreElements(); en = (Entity) k.nextElement()) {
            if (en != null) {
                if (!knownDeadEntities.contains(en)) {
                    knownDeadEntities.add(en);
                }
            }           
        }
        */
    }
    
    private void resolveIceBroken(Coords c) {
        game.getBoard().getHex(c).removeTerrain(Terrains.ICE);
        sendChangedHex(c);
        //drop entities on the surface into the water
        for(Enumeration entities = game.getEntities(c);entities.hasMoreElements();) {
            Entity e = (Entity)entities.nextElement();
            if(e.getElevation() == 0) {
                doEntityFall(e, new PilotingRollData(TargetRoll.AUTOMATIC_FAIL));
            }
        }
    }
        
    private void checkForVehicleFire(Tank tank, boolean inferno) {
        int boomroll = Compute.d6(2);
        int penalty = 0;
        switch(tank.getMovementMode()) {
        case IEntityMovementMode.HOVER:
            penalty = 4;
            break;
        case IEntityMovementMode.VTOL:
        case IEntityMovementMode.WHEELED:
            penalty = 2;
            break;
        }
        if(inferno) {
            boomroll = 12;
        }
        Report r = new Report(5250);
        r.subject = tank.getId();
        r.addDesc(tank);
        r.add(8-penalty);
        r.add(boomroll);
        if (boomroll + penalty < 8) {
            //phew!
            r.choose(true);
            addReport(r);
        } else {
            //eek
            if(!inferno) {
                r.choose(false);
                addReport(r);
            }
            if(boomroll + penalty < 10) {
                vehicleMotiveDamage(tank, penalty - 1);
            } else {
                resolveVehicleFire(tank, false);
                if(boomroll + penalty >= 12) {
                    r = new Report(5255);
                    r.subject = tank.getId();
                    r.indent(3);
                    addReport(r);
                    tank.setOnFire();
                }
            }
        }
    }
    
    private void resolveVehicleFire(Tank tank, boolean existingStatus) {
        if(existingStatus && !tank.isOnFire())
            return;
        for(int i=0;i<tank.locations();i++) {
            if(i==Tank.LOC_BODY || (tank instanceof VTOL && i==VTOL.LOC_ROTOR))
                continue;
            if(existingStatus && !(tank.isLocationBurning(i)))
                continue;
            HitData hit = new HitData(i);
            int damage =Compute.d6(1);
            addReport(damageEntity(tank, hit, damage));
            if(damage == 1 && existingStatus) {
                tank.extinguishLocation(i);
            }
        }        
       addNewLines();
    }
    
    private void vehicleMotiveDamage(Tank te, int modifier) {
        Vector vDesc = new Vector();
        Report r;
        int roll = Compute.d6(2) + modifier;
        r = new Report(6305);
        r.subject = te.getId();
        r.add("movement system");
        r.newlines = 0;
        r.indent(3);
        vDesc.addElement(r);
        r = new Report(6310);
        r.subject = te.getId();
        r.add(roll);
        r.newlines = 0;
        vDesc.addElement(r);
        if(roll <= 7) {
            //no effect
            r = new Report(6005);
            r.subject = te.getId();
            r.add(roll);
            vDesc.addElement(r);
        } else if (roll <=9) {
            //minor damage
            r = new Report(6470);
            r.subject = te.getId();
            r.add(roll);
            vDesc.addElement(r);
            te.addMovementDamage(1);
        } else if (roll <12) {
            //major damage
            r = new Report(6135);
            r.subject = te.getId();
            vDesc.addElement(r);
            te.addMovementDamage(2);
            int nMP = te.getOriginalWalkMP();
            if (nMP > 0) {
                te.setOriginalWalkMP(nMP - 1);

                if (te.getOriginalWalkMP()==0) {
                    // Hovercraft reduced to 0MP over water sink
                    if ( te.getMovementMode() == IEntityMovementMode.HOVER &&
                         game.getBoard().getHex( te.getPosition() ).terrainLevel(Terrains.WATER) > 0 &&
                         !(game.getBoard().getHex( te.getPosition() ).containsTerrain(Terrains.ICE))) {
                        vDesc.addAll( destroyEntity(te, "a watery grave", false) );
                    }
                }
            }
        } else  {
            r = new Report(6140);
            r.subject = te.getId();
            vDesc.addElement(r);
            te.immobilize();
            // Does the hovercraft sink?
            IHex te_hex = game.getBoard().getHex( te.getPosition() );
            if ( te.getMovementMode() == IEntityMovementMode.HOVER &&
                 te_hex.terrainLevel(Terrains.WATER) > 0 &&
                 !(te_hex.containsTerrain(Terrains.ICE))) {
                vDesc.addAll( destroyEntity(te, "a watery grave", false) );
            }
            if(te instanceof VTOL) {
                Report.addNewline(vDesc);
                //report problem: add tab
                vDesc.addAll( crashVTOL((VTOL)te));
            }
        }
        addReport(vDesc);
    }

    
    /**
     * Add a whole lotta Reports to the players report queues
     * as well as the Master report queue vPhaseReport.
     */
    private  void addReport(Vector reports){
        //Only bother with player reports if doing double blind.
        if ( doBlind())
            for (Enumeration i = connections.elements(); i.hasMoreElements();) {
                final Connection conn = (Connection)i.nextElement();
                Player p = game.getPlayer(conn.getId());
    
                p.getTurnReport().addAll(filterReportVector(reports,p));
            }
        vPhaseReport.addAll(reports);
    }
    
    /**
     * Add a single report to the report queue of all players and the master
     * vPhaseReport queue
     */
    private void addReport(Report report){
        //Only bother with player reports if doing double blind.
        if ( doBlind())
            for (Enumeration i = connections.elements(); i.hasMoreElements();) {
                final Connection conn = (Connection)i.nextElement();
                Player p = game.getPlayer(conn.getId());
    
                p.getTurnReport().addElement(filterReport(report,p,false));
            }
        vPhaseReport.addElement(report);
    }

    /**
     * New Round has started clear everyones report queue
     */
    private void clearReports(){
        //Only bother with player reports if doing double blind.
        if ( doBlind())
            for (Enumeration i = connections.elements(); i.hasMoreElements();) {
                final Connection conn = (Connection)i.nextElement();
                Player p = game.getPlayer(conn.getId());
    
                p.getTurnReport().removeAllElements();
            }
        vPhaseReport.removeAllElements();        
    }
    
    /**
     * make sure all teh new lines that where added to the old vPhaseReport
     * get added to all of the players filters
     */
    private void addNewLines(){
        //Only bother with player reports if doing double blind.
        if ( doBlind())
            for (Enumeration i = connections.elements(); i.hasMoreElements();) {
                final Connection conn = (Connection)i.nextElement();
                Player p = game.getPlayer(conn.getId());
    
                Report.addNewline(p.getTurnReport());
            }
        Report.addNewline(vPhaseReport);
    }
    
    public void doAssaultDrop(Entity entity) {
        PilotingRollData psr;
        if(entity instanceof Mech) {
            psr=entity.getBasePilotingRoll();
        } else {
            psr=new PilotingRollData(entity.getId(),4,"landing assault drop");
        }
        int roll = Compute.d6(2);
        //check for a safe landing
        Report r = new Report(2380);
        r.subject = entity.getId();
        r.add(entity.getDisplayName(), true);
        r.add(psr.getValueAsString());
        r.add(roll);
        r.choose(roll >= psr.getValue());
        addReport(r);
        if(roll < psr.getValue()) {
            int fallHeight = psr.getValue() - roll;
            //determine where we really land
            int distance = Compute.d6(fallHeight);
            Coords c = Compute.scatter(entity.getPosition(), distance);
            r = new Report(2385);
            r.subject = entity.getId();
            r.add(distance);
            r.indent(3);
            r.newlines = 0;
            addReport(r);
            if(fallHeight >=5 || !game.getBoard().contains(c)) {
                r = new Report(2386);
                addReport(r);
                game.removeEntity(entity.getId(),IEntityRemovalConditions.REMOVE_NEVER_JOINED);
                return;
            }
            entity.setPosition(c);
            
            //do fall damage
            if(entity instanceof Mech || entity instanceof Protomech) {
                entity.setElevation(fallHeight);
                doEntityFallsInto(entity, c, c, psr, true);
            }
            else if(entity instanceof BattleArmor) {
                for(int i=1;i<entity.locations();i++) {
                    HitData h = new HitData(i);
                    addReport(damageEntity(entity,h,Compute.d6(fallHeight)));
                    addNewLines();
                }
            }
            else if(entity instanceof Infantry) {
                HitData h = new HitData(Infantry.LOC_INFANTRY);
                addReport(damageEntity(entity,h, 1));
                addNewLines();
            }
        }
        //set entity to expected elevation
        IHex hex = game.getBoard().getHex(entity.getPosition());
        entity.setElevation(entity.elevationOccupied(hex) - hex.floor());
        //finally, check for any stacking violations
        Entity violated = Compute.stackingViolation(game, entity, entity.getPosition(), null);
        if(violated != null) {
            //handle this as accidental fall from above
            entity.setElevation(violated.getElevation() + 2);
            r = new Report(2390);
            r.subject = entity.getId();
            r.add(entity.getDisplayName(), true);
            r.add(violated.getDisplayName(), true);
            addReport(r);
            doEntityFallsInto(entity, entity.getPosition(), entity.getPosition(), psr);
        }
        return;
    }
    
    void doAllAssaultDrops() {
        for(Enumeration i = game.getEntities();i.hasMoreElements();) {
            Entity e = (Entity)(i.nextElement());
            if(e.isAssaultDropInProgress()) {
                doAssaultDrop(e);
                e.setLandedAssaultDrop();
            }
        }
    }
    
    void doMagmaDamage(Entity en, boolean eruption) {
        if((en.getMovementMode() == IEntityMovementMode.VTOL ||
           (en.getMovementMode() == IEntityMovementMode.HOVER && en.getOriginalWalkMP() > 0 && !eruption))
           && !(en.isImmobile())) {
           return;
        }
        Report r;
        boolean isMech = en instanceof Mech;
        if(isMech)
            r = new Report(2405);
        else
            r = new Report(2400);
        r.addDesc(en);
        r.subject = en.getId();
        addReport(r);
        if(isMech) {
            HitData h;
            for(int i=0;i<en.locations();i++) {
                if(eruption || en.locationIsLeg(i) || en.isProne()) {
                    h = new HitData(i);
                    addReport(damageEntity(en, h, Compute.d6(2)));
                }
            }
        } else {
            addReport(destroyEntity(en, "fell into magma", false, false));
        }
        addNewLines();
    }
    
    /**
     * deal area saturation damage to an individual hex
     * @param coords         The hex being hit
     * @param attackSource   The location the attack came from.  For hit table resolution
     * @param damage         Amount of damage to deal to each entity
     * @param ammo           The ammo type being used
     * @param subjectId      Subject for reports
     * @param killer         Who should be credited with kills
     * @param exclude        Entity that should take no damage (used for homing splash)
     * @param flak           Flak, hits flying units only, instead of flyers being immune
     * @param altitude       Absolute altitude for flak attack
     */
    void artilleryDamageHex(Coords coords, Coords attackSource, int damage, AmmoType ammo, int subjectId, Entity killer, Entity exclude, boolean flak, int altitude) {

        IHex hex = game.getBoard().getHex(coords);
        if(hex == null) return; //not on board.

        int flakElevation = altitude - hex.surface();

        Report r;

        Building bldg = game.getBoard().getBuildingAt(coords);
        int bldgAbsorbs = 0;
        if(bldg != null
           && !(flak && flakElevation > hex.terrainLevel(Terrains.BLDG_ELEV))) {
            bldgAbsorbs = bldg.getPhaseCF() / 10;
            if(!(ammo != null && ammo.getMunitionType() == AmmoType.M_FLECHETTE)) {
                //damage the building
                r = damageBuilding( bldg, damage );
                r.subject = subjectId;
                addReport(r);
                addNewLines();
            }
        }

        if(flak
           && (flakElevation <= 0
               || flakElevation <= hex.terrainLevel(Terrains.BLDG_ELEV)
               || flakElevation == hex.terrainLevel(Terrains.BRIDGE_ELEV))) {
            //Flak in this hex would only hit landed units
            return;
        }

        // get units in hex
        for (Enumeration impactHexHits = game.getEntities(coords);impactHexHits.hasMoreElements();) {
            Entity entity = (Entity)impactHexHits.nextElement();
            int hits = damage;
            ToHitData toHit = new ToHitData();
            int cluster = 5;
            
            //Check: is entity excluded?
            if(entity == exclude)
                continue;

            //Check: is entity inside building?
            if(bldg != null &&
               bldgAbsorbs > 0 &&
               entity.getElevation() < hex.terrainLevel(Terrains.BLDG_ELEV)) {
                cluster -= bldgAbsorbs;
                if(entity instanceof Infantry) {
                    continue; //took its damage already from building damage
                }
                else if(cluster <= 0) {
                    //entity takes no damage
                    r = new Report(6426);
                    r.subject = subjectId;
                    r.addDesc(entity);
                    addReport(r);
                    continue;
                } else {
                    r = new Report(6425);
                    r.subject = subjectId;
                    r.add(bldgAbsorbs);
                    addReport(r);
                }
            }

            if(flak) {
                //Check: is entity not a VTOL in flight
                if (!(entity instanceof VTOL ||
                    entity.getMovementMode()==IEntityMovementMode.VTOL)) {
                    continue;
                }
                //Check: is entity at correct elevation?
                if(entity.getElevation() != flakElevation)
                    continue;
            } else {
                //Check: is entity a VTOL in flight?
                if (entity instanceof VTOL ||
                    entity.getMovementMode()==IEntityMovementMode.VTOL) {
                    // VTOLs take no damage from normal artillery unless landed
                    if (entity.getElevation()!=0
                        && entity.getElevation()!=hex.terrainLevel(Terrains.BLDG_ELEV)
                        && entity.getElevation()!=hex.terrainLevel(Terrains.BRIDGE_ELEV)) {
                        continue;
                    }
                }
            }

            //Work out hit table to use
            if (attackSource!=null) {
                toHit.setSideTable(entity.sideTable(attackSource));
                if(ammo != null 
                   && entity instanceof Mech
                   && ammo.getMunitionType() == AmmoType.M_CLUSTER
                   && attackSource.equals(coords)) {
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                }
            }

            //Entity/ammo specific damage modifiers
            if(ammo != null) {
                if(ammo.getMunitionType() == AmmoType.M_CLUSTER) {
                    if(hex.containsTerrain(Terrains.FORTIFIED) 
                            && entity instanceof Infantry
                            && !(entity instanceof BattleArmor)) {
                        hits *= 2;
                    }
                    if(hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.JUNGLE)) {
                        hits = (hits + 1) / 2;
                    }
                }
                else if(ammo.getMunitionType() == AmmoType.M_FLECHETTE) {
                    //wheeled and hover tanks take movement critical
                    if(entity instanceof Tank &&
                       (entity.getMovementMode() == IEntityMovementMode.WHEELED ||
                        entity.getMovementMode() == IEntityMovementMode.HOVER)) {
                        r = new Report(6480);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        r.add(toHit.getTableDesc());
                        r.add(0);
                        addReport(r);
                        vehicleMotiveDamage((Tank)entity, 0);
                        continue;
                    }
                    //non infantry are immune
                    if(entity instanceof BattleArmor ||
                       !(entity instanceof Infantry)) {
                        continue;
                    }
                    if(hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.JUNGLE)) {
                        hits = (hits + 1) / 2;
                    }
                }
            }

            //Do the damage
            addNewLines();
            r = new Report(6480);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(toHit.getTableDesc());
            r.add(hits);
            addReport(r);
            while(hits>0) {
                HitData hit = entity.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());

                addReport( damageEntity(entity, hit, Math.min(cluster, hits), false, 0, false, true, false));
                hits -= Math.min(5,hits);
            }
            if(killer != null) {
                creditKill(entity, killer);
            }
            addNewLines();
        }
    }
    
    /**
     * deal area saturation damage to the map, used for artillery
     * @param centre         The hex on which damage is centred
     * @param attackSource   The position the attack came from
     * @param ammo           The ammo type doing the damage
     * @param subjectId      Subject for reports
     * @param killer         Who should be credited with kills
     * @param flak           Flak, hits flying units only, instead of flyers being immune
     * @param altitude       Absolute altitude for flak attack
     */
    void artilleryDamageArea(Coords centre, Coords attackSource, AmmoType ammo, int subjectId, Entity killer, boolean flak, int altitude) {
        int damage; 
        int falloff=5;
        if(ammo.getMunitionType() == AmmoType.M_FLECHETTE) {
            damage = ammo.getRackSize() + 10;
        } 
        else if(ammo.getMunitionType() == AmmoType.M_CLUSTER) {
            if(ammo.getAmmoType() == AmmoType.T_SNIPER)
                damage = 15;
            else
                damage = ammo.getRackSize();
            attackSource = centre;
        }
        else if(game.getOptions().booleanOption("maxtech_artillery")) {
            //"standard" mutates into high explosive
            if(ammo.getAmmoType() == AmmoType.T_LONG_TOM)
                damage = 25;
            else
                damage = ammo.getRackSize() + 10;
            falloff = 10;
        }
        else {
            //level 2 ammo
            damage = ammo.getRackSize();
            falloff = (damage + 1) / 2;
        }
        artilleryDamageArea(centre, attackSource, ammo, subjectId, killer, damage, falloff, flak, altitude);
    }
    
    /**
     * Deals area-saturation damage to an area of the board.
     * Used for artillery, bombs, or anything else with linear decreas in damage
     * @param centre         The hex on which damage is centred
     * @param attackSource   The position the attack came from
     * @param ammo           The ammo type doing the damage
     * @param subjectId      Subject for reports
     * @param killer         Who should be credited with kills
     * @param damage         Damage at ground zero
     * @param falloff        Reduction in damage for each hex of distance
     * @param flak           Flak, hits flying units only, instead of flyers being immune
     * @param altitude       Absolute altitude for flak attack
     */
    void artilleryDamageArea(Coords centre, Coords attackSource, AmmoType ammo, int subjectId, Entity killer, int damage, int falloff, boolean flak, int altitude) {
        for(int ring=0;damage > 0;ring++,damage-=falloff) {
            ArrayList<Coords> hexes = Compute.coordsAtRange(centre, ring);
            for(Coords c : hexes) {
                artilleryDamageHex(c, attackSource, damage, ammo, subjectId, killer, null, flak, altitude);
            }
            attackSource = centre; // all splash comes from ground zero
        }
    }
}

