package antworld.client;

/**
 * Created by Dominic on 12/9/2016.
 */

import java.awt.*;
import antworld.common.Util;
import antworld.common.AntAction;
import antworld.common.AntAction.AntActionType;
import antworld.common.AntData;
import antworld.common.CommData;
import antworld.common.Direction;
import antworld.common.FoodData;
import antworld.common.FoodType;
import java.awt.Point;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Ant wrapper class to wrap the AntData given by the server and perform operations and commands
 */
public class WorkerBrain extends AntBrain
{
  
  
   
  Command command = Command.EMPTY;
  
  
  
  // Debug flags
  public static final boolean DEBUG_HOME = false;
  public static final boolean DEBUG_FOOD = false;
  public static final boolean DEBUG_MOVE = false;
  public static final boolean DEBUG_BLOCKED = false;
  
  
  
   
  enum Command
  {
    EXPLORE,
    WATER,
    DEFEND,
    GATHER,
    ATTACK,
    HOME,
    ESCAPE,
    EMPTY,
    HEAL,
  }
  
  /**
   * Create a new Ant
   *
   * @param ant ant data for this ant
   * @param commData current comm data
   * @param map map representation
   * @param exploreDeque explore tiles
   * @param blockedMoves current blocked moves set
   * @param plannedMoves current planned moves set
   * @param centerX nest x
   * @param centerY nest y
   * @param waterPosition best known water position
   */
  public WorkerBrain(AntData ant, CommData commData, WorldMap map, LinkedList<Tiles> exploreDeque,
             HashSet<Tiles> blockedMoves, HashSet<Tiles> plannedMoves, int centerX, int centerY,
             Tiles waterPosition)
  {
    
    super(ant,commData, map, exploreDeque,blockedMoves,plannedMoves,centerX,centerY,waterPosition);
  }
  
  /**
   * This is the main logic of the ant
   *
   * Uses a switch based on command setting (which is an enum) for what action the ant should take
   * Works in conjunction with the client to make a strategy
   */
  void continueCommand()
  {
    if (DEBUG_BLOCKED)
    {
      //System.out.println("Blocked size = " + blockedMoves.size() + " plannedSize = " + plannedMoves.size());
    }
    //System.out.println("Command = " + command + " ant = " + data.id);
    switch (command)
    {
      case GATHER:
        if (commData.foodSet == null || (!commData.foodSet.contains(closeFood)))
        {
          target = null;
          command = Command.EXPLORE;
          continueCommand();
          return;
        }
    
        if (target == null)
        {
          target = map.getTile(closeFood.gridX, closeFood.gridY);
        }
        if (nextTo(target.xPos, target.yPos))
        {
          pickup(moveToDirection(target.xPos - data.gridX, target.yPos - data.gridY));
          target = null;
          command = Command.HOME;
          return;
        }
        moveTowardsTarget();
        break;
      
      case ATTACK:
        if (enemy == null && !nextToEnemy())
        {
          command = Command.EXPLORE;
          continueCommand();
        }
        else
        {
          command = Command.ESCAPE;
        }
        break;
      case EXPLORE:
        if (target == null || (target.xPos == data.gridX && target.yPos == data.gridY))
        {
          target = exploreDeque.pop();
          exploreDeque.addLast(target);
        }
        moveTowardsTarget();
        break;
      
      case HOME:
        if (tile.nest && tile.distance(centerX, centerY) < 20)
        {
          if (!underground())
          {
            enterNest();
            return;
          }
          
          if (data.carryUnits > 0)
          {
            if (DEBUG_FOOD)
            {
              System.out.println("DROPPING FOOD");
            }
            drop();
            target = null;
            command = Command.HEAL;
            return;
          }
          
          command = Command.HEAL;
          target = null;
          continueCommand();
          break;
        }
        target = map.getTile(centerX, centerY);
        
        moveTowardsTarget();
        break;
      case HEAL:
        if (data.health < data.antType.getMaxHealth())
        {
          if (commData.foodStockPile[FoodType.WATER.ordinal()] > 0)
          {
            heal();
          }
          else
          {
            stasis();
          }
        }
        else
        {
          command = Command.EMPTY;
          target = null;
        }
        break;
      case WATER:
        if (target == null)
        {
          target = waterPosition;
        }
        
        if (nextToWater())
        {
          pickup(moveToDirection(target.xPos - data.gridX, target.yPos - data.gridY));
          target = null;
          command = Command.HOME;
          return;
        }
        moveTowardsTarget();
        break;
      
      case EMPTY:
      default:
        command = Command.EXPLORE;
        continueCommand();
        break;
    }
  }
  
  
 
  
}
