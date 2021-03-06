package com.dominicswaine.seg_agile_project.Logic;

/* Used for JSON processing */
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/* Functionality and exception-handling*/
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Arrays;

/**
 * Parser class mediates between files and backend functionality.
 * The custom games and even
 * @author Horia Pavel
 * @version 07-12-2018
 */
public class Parser {

    /* Parent JSON object for writing data to .json files */
    private JSONObject obj;
    /* Current state of the board is needed to update any needed changes */
    private Board board;

    /**
     * Constructor for the Parser class.
     * @param board Board -- the current state of the board
     */
    public Parser(Board board) {
        obj = new JSONObject();
        this.board = board;
    }

    /**
     * addContent method updates the state of the parent object
     * according to the new state of the board.
     *
     * If the state of the board has changed -- then the new .json file to be
     * written would be instantiated from the new, updated parent object.
     */
    @SuppressWarnings("unchecked")
    public void addContent() {
        Hole[] holes = board.getHoles();
        Kazan[] kazans = board.getKazans();

        JSONObject obj1 = new JSONObject();
        JSONObject obj2 = new JSONObject();
        JSONArray players = new JSONArray();

        // Tuzes
        int whiteTuzIndex = board.getPlayerTuz(Side.WHITE);
        int blackTuzIndex = board.getPlayerTuz(Side.BLACK);

        // Configuration for player '1'

        obj1.put("name","white");

        JSONObject whiteTuz = new JSONObject();
        whiteTuz.put("tuz", whiteTuzIndex);

        JSONArray p1 = new JSONArray();
        JSONObject player1Kazan = new JSONObject();
        player1Kazan.put("kazan",kazans[0].getNumberOfKoorgools());
        p1.add(player1Kazan);
        p1.add(whiteTuz);
        for(int i = 0 ; i < 9 ; ++i) {
            JSONObject player1Hole = new JSONObject();
            player1Hole.put("hole:" + i, holes[i].getNumberOfKoorgools() );
            p1.add(player1Hole);
        }

        obj1.put("config",p1);

        // Configuration for player '2'

        obj2.put("name","black");


        JSONObject blackTuz = new JSONObject();
        blackTuz.put("tuz", blackTuzIndex);

        JSONArray p2 = new JSONArray();
        JSONObject player2Kazan = new JSONObject();
        player2Kazan.put("kazan",kazans[0].getNumberOfKoorgools());
        p2.add(player2Kazan);
        p2.add(blackTuz);
        for(int i = 9 ; i < 18 ; ++i) {
            JSONObject player2Hole = new JSONObject();
            player2Hole.put("hole:" + (i - 9), holes[i].getNumberOfKoorgools() );
            p2.add(player2Hole);
        }

        obj2.put("config",p2);

        // Put everything in the object to be returned `obj`
        players.add(obj1);
        players.add(obj2);

        obj.put("players",players);
    }

    /**
     * This method takes a file path as argument and writes the content of the
     * parent json object to the a new file.
     *
     * If the name of the file already exists, its contents will be updated by calling
     * this method.
     *
     * @param filePath String -- the file path | usually resides in the resources/game_files folder
     */
    public void writeToFile(String filePath) {
        try(FileWriter file = new FileWriter(filePath)) {
            file.write(obj.toJSONString());
            file.flush();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method takes a file path as a argument and reads 'json' data from it.
     * The read data is then organised in a map with two entries: one for the board configuration
     * of each player.
     *
     * With the Map returned, the game board can be updated according to the data retrieved.
     *
     * @param filePath String -- the file path | where the data is read from
     * @return Map -- a map with content of a board to be created
     */
    public Map readFromFile(String filePath) {
        JSONParser parser = new JSONParser();
        Map<String,Integer> whiteSideMap = new HashMap<>();
        Map<String,Integer> blackSideMap = new HashMap<>();
        Map<String,Map<String,Integer>> doubleMap = new HashMap<>();
        try {
            // Disentangle the parent JSON object
            Object obj = parser.parse(new FileReader(filePath));
            JSONObject jsonObj = (JSONObject) obj;
            JSONArray listOfPlayers = (JSONArray) jsonObj.get("players");

            // Getting the data from the 'players' JSONArray
            JSONObject whiteSideData = (JSONObject) listOfPlayers.get(0);
            JSONObject blackSideData = (JSONObject) listOfPlayers.get(1);

            // Parsing white side data:
            JSONArray whiteSideHoles = (JSONArray) whiteSideData.get("config");

            JSONObject kazanObj = (JSONObject) whiteSideHoles.get(0);
            whiteSideMap.put("kazan", Math.toIntExact((Long) kazanObj.get("kazan")));

            JSONObject tuzObj = (JSONObject) whiteSideHoles.get(1);
            whiteSideMap.put("tuz", Math.toIntExact((Long) tuzObj.get("tuz")));

            for(int i = 2 ; i < whiteSideHoles.size() ; ++i) {
                JSONObject holeI = (JSONObject) whiteSideHoles.get(i);
                whiteSideMap.put("hole:" + (i-2), Math.toIntExact((Long) holeI.get("hole:"+(i-2))));
            }

            // Parsing black side data:
            JSONArray blackSideHoles = (JSONArray) blackSideData.get("config");

            kazanObj = (JSONObject) blackSideHoles.get(0);
            tuzObj = (JSONObject) blackSideHoles.get(1);

            blackSideMap.put("kazan",Math.toIntExact((Long) kazanObj.get("kazan")));
            blackSideMap.put("tuz",Math.toIntExact((Long) tuzObj.get("tuz")));

            for(int i = 2 ; i < blackSideHoles.size() ; ++i) {
                JSONObject holeI = (JSONObject) whiteSideHoles.get(i);
                blackSideMap.put("hole:" + (i-2), Math.toIntExact((Long) holeI.get("hole:"+(i-2))));
            }

            // Put everything in the double map
            doubleMap.put("white",whiteSideMap);
            doubleMap.put("black",blackSideMap);

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (ParseException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }

        return doubleMap;

    }

    /**
     * Save custom game method is used to save a custom game configuration into a file
     * from the Custom Game Panel. Once the player has constructed the game, pressing the
     * 'save' button will call this method and save the file into save_games directory.
     * @param filePath String -- the name of the file saved in the save_games directory
     * @param playerTuz String -- the playerTuz value
     * @param opponentTuz String -- the opponentTuz value
     * @param playerHoles int[] -- the players hole values (i.e. how many korgools are in the players holes)
     * @param opponentHoles int[] -- the opponent hole values (i.e. how many korgools are in the opponent holes)
     */
    public static void saveCustomGame(String filePath ,String playerTuz, String opponentTuz,
                                      int[] playerHoles, int[] opponentHoles) {

        StringBuilder sb = new StringBuilder();
        sb.append(playerTuz);
        sb.append("|");
        sb.append(opponentTuz);
        sb.append("|");
        sb.append(Arrays.toString(playerHoles));
        sb.append("|");
        sb.append(Arrays.toString(opponentHoles));

        try (FileWriter file = new FileWriter(filePath + ".sav")) {
            file.write(sb.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The load custom game method loads a previously selected file from the 'saved_games'
     * directory and starts a game according to the configuration just read.
     * @param filePath String -- the path of the saved game file.
     */
    public static void loadCustomGame(String filePath) {
        try {
            String[] data = (new String(Files.readAllBytes(Paths.get(filePath)))).split("\\|");

            // Handle playerHoles
            String[] playerHolesString = data[2].replaceAll("\\[","")
                                                .replaceAll("\\]","")
                                                .replaceAll("\\s","")
                                                .split(",");
            int[] playerHolesInt = new int[playerHolesString.length];
            for (int i = 0 ; i < playerHolesInt.length ; ++i) {
                playerHolesInt[i] = Integer.parseInt(playerHolesString[i]);
            }

            // Handle opponentHoles
            String[] opponentHolesString = data[3].replaceAll("\\[","")
                                                .replaceAll("\\]","")
                                                .replaceAll("\\s","")
                                                .split(",");
            int[] opponentHolesInt = new int[opponentHolesString.length];
            for(int i = 0 ; i < opponentHolesString.length ; ++i) {
                opponentHolesInt[i] = Integer.parseInt(opponentHolesString[i]);
            }

            // Launch game with the retrieved data
            new Game(data[0],data[1],playerHolesInt,opponentHolesInt);

        }catch (IOException e){
            e.printStackTrace();
        }

    }

}
