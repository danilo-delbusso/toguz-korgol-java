package com.dominicswaine.seg_agile_project.Logic;

import com.dominicswaine.seg_agile_project.Board.HoleUI;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * The Game Board
 * @author Ayberk Demirkol, Dominic Swaine
 * @version 13/12/2018
 */
public class Board {
    private Hole[] holes = new Hole[18];
    private Kazan[] kazans = new Kazan[2];
    private Side nextToPlay;

    /**
     * Constructor for board object. Creates 18 holes and 2 kazans. Sets next turn to white side.
     */
    public Board(){
        for(int holeIndex = 0; holeIndex<holes.length; holeIndex++)
            holes[holeIndex] = new Hole(holeIndex);
        for(int kazanIndex = 0; kazanIndex<kazans.length; kazanIndex++)
            kazans[kazanIndex] = new Kazan(kazanIndex);
        nextToPlay = Side.WHITE;
    }

    /**
     * Return array of holes in game board
     * @return holes array
     */
    public Hole[] getHoles(){
        return holes;
    }

    /**
     * Return array of kazans in game board
     * @return kazans array
     */
    protected Kazan[] getKazans(){
        return kazans;
    }

    /**
     * Returns Kazan with given index
     * @param index index of Kazan to be returned
     * @return Kazan with the given index
     */
    protected Kazan getKazanByIndex(int index){
        return getKazans()[index];
    }

    /**
     * Returns Hole with given index
     * @param index index of Hole to be returned
     * @return Hole with given index
     */
    protected Hole getHoleByIndex(int index){
        return getHoles()[index];
    }

    /**
     * Returns side to play next
     * @return Side to play next
     */
    protected Side getNextToPlay(){
        return nextToPlay;
    }

    /**
     * Distribute each korgool for the selected hole.
     * @param holeIndex
     */
    public void redistribute(int holeIndex){

        if(holes[holeIndex].getOwner() == nextToPlay) {
            ArrayList<Korgool> korgoolsToMove = holes[holeIndex].getKoorgools();
            Hole holeChosen = holes[holeIndex];
            Hole lastHole;
            //@Check if there are no korgools in the hole.
            if(korgoolsToMove.size() == 0){
                return;
            }
            //@Check if there are 1 korgool in the hole.
            else if(korgoolsToMove.size() == 1){
                lastHole = holes[(holeIndex + 1) % 18];
                lastHole.addKorgool(holeChosen.getKoorgools().get(0));
                holeChosen.emptyHole();
            }
            else{
                lastHole = holes[(holeIndex + korgoolsToMove.size() - 1) % 18];
                //Distributes each korgools
                for(int distributeIndex = 1; distributeIndex < korgoolsToMove.size(); distributeIndex++){
                    holes[(holeIndex + distributeIndex) % 18].addKorgool(korgoolsToMove.get(distributeIndex));
                }
                Korgool first = korgoolsToMove.get(0);
                holeChosen.emptyHole();
                holeChosen.addKorgool(first);
            }
            //@Check if we add to kazan or make tuz.

            if(lastHole.getOwner() != nextToPlay) {
                Side checkTuzSide = (nextToPlay == Side.WHITE) ? Side.BLACK : Side.WHITE;
                int otherTuzIndex = getPlayerTuz(checkTuzSide);
                int playersKazanIndex = (nextToPlay == Side.WHITE) ? 0 : 1;
                ArrayList<Korgool> lastHoleKorgools = lastHole.getKoorgools();
                    if ((((otherTuzIndex - 9) != lastHole.getHoleIndex() && (otherTuzIndex + 9) != lastHole.getHoleIndex()) || otherTuzIndex == -1) && (lastHole.getHoleIndex() != 8 && lastHole.getHoleIndex() != 17) && lastHoleKorgools.size() == 3 && !lastHole.isTuz() && !nextToPlay.hasTuz()) {
                        lastHole.markAsTuz();

                        nextToPlay.makeTuz();
                        if (nextToPlay == Side.BLACK) {
                            MouseListener mouseListener = lastHole.getGui().getMouseListeners()[0];
                            lastHole.getGui().removeMouseListener(mouseListener);
                        }
                        for (int i = 0; i < lastHoleKorgools.size(); i++) {
                            kazans[playersKazanIndex].addKorgool(new Korgool());
                        }
                        kazans[playersKazanIndex].addKorgools(lastHole.getKoorgools());
                        lastHole.emptyHole();
                    }
                else if(lastHoleKorgools.size() % 2 == 0){
                    for(int i = 0; i < lastHoleKorgools.size(); i++) {
                        kazans[playersKazanIndex].addKorgool(new Korgool());
                    }
                    lastHole.emptyHole();
                }
            }
            nextToPlay = nextToPlay==Side.WHITE ? Side.BLACK : Side.WHITE;
        }
    }

    /**
     * Returns legal moves for the given Side
     * @param turnSide Side that holes are linked to
     * @return ArrayList of Holes that can be redistributed and owned by given Side
     */
    public ArrayList<Hole> availableMoves(Side turnSide){
        ArrayList<Hole> holesOwned = new ArrayList<>();
        for(Hole h : holes){
            if(h.getOwner() == turnSide && h.getNumberOfKoorgools() != 0){
                holesOwned.add(h);
            }
        }
        return holesOwned;
    }

    /**
     * Picks a random hole belonging side to play next and redistributes that hole
     */
    public void randomMove(){
        ArrayList<Hole> availableHoles = availableMoves(nextToPlay);
        int holeIndex = (int)(Math.random() * (((availableHoles.size()-1) - 0) + 1)) + 0;
        ArrayList<Korgool> korgools = availableHoles.get(holeIndex).getKoorgools();
        while(korgools.size() == 0){
            holeIndex = (int)(Math.random() * (((availableHoles.size()-1) - 0) + 1)) + 0;
            korgools = availableHoles.get(holeIndex).getKoorgools();
        }
        redistribute(availableHoles.get(holeIndex).getHoleIndex());
    }

    /**
     * Picks the hole that will be most beneficial to player
     * Prioritizes making Tuz
     * Next priority is getting maximum number of korgools to kazan
     * When neither is found, makes a random move
     */
    public void challengeMove(){
        int maxOutcome = -1;
        int returnIndex = -1;
        Hole lastHole;
        Hole selectedHole;
        ArrayList<Hole> availableHoles = availableMoves(nextToPlay);
        for(int i = 0; i < availableHoles.size(); i++){
            selectedHole = availableHoles.get(i);
            lastHole = holes[(selectedHole.getHoleIndex() + selectedHole.getNumberOfKoorgools() - 1) % 18];
            if(lastHole.getOwner() != nextToPlay){
                int numOfKorgools = lastHole.getNumberOfKoorgools() +1;
                if(numOfKorgools == 3 && !nextToPlay.hasTuz()){
                    int otherTuzIndex = getPlayerTuz(Side.WHITE);
                    if(otherTuzIndex == -1 || ((otherTuzIndex + 9) != lastHole.getHoleIndex())) {
                        redistribute(selectedHole.getHoleIndex());
                        return;
                    }
                    redistribute(selectedHole.getHoleIndex());
                    return;
                }
                if(numOfKorgools % 2 == 0 && numOfKorgools > maxOutcome){
                    maxOutcome = numOfKorgools;
                    returnIndex = selectedHole.getHoleIndex();
                }
            }
        }
        if(returnIndex <= -1){
            randomMove();
            return;
        }
        redistribute(returnIndex);
    }

    /**
     * Returns the index of the hole that is tuz for given side
     * @param owner Side that picked tuz belongs to
     * @return index of the hole that is tuz for given side
     */
    public int getPlayerTuz(Side owner) {
        for(int i = 0 ; i <= 17 ; ++i) {
            if(holes[i].isTuz() && holes[i].getOwner() == owner) {
                return i;
            }
        }
        return -1;
    }
}
