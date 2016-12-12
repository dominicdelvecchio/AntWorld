package antworld.client;

import antworld.common.AntData;
import antworld.common.CommData;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by Dominic on 12/9/2016.
 */
public class AttackBrain extends AntBrain
{
  public AttackBrain(AntData ant, CommData commData, WorldMap map, LinkedList<Tiles> exploreDeque,
                     HashSet<Tiles> blockedMoves, HashSet<Tiles> plannedMoves, int centerX, int centerY,
                     Tiles waterPosition)
  {
    
    super(ant,commData, map, exploreDeque,blockedMoves,plannedMoves,centerX,centerY,waterPosition);
  }
}
