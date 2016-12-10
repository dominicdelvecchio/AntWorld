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
  /**
   * Nest Position
   */
  int centerX, centerY;
  
  /**
   * data linked to this ant
   */
  AntData data;
  
  /**
   * current commData
   */
  CommData commData;
  
  /**
   * Food to gather
   */
  FoodData closeFood;
  
  /**
   * map representation
   */
  WorldMap map;
  
  /**
   * Current command
   */
  Command command = Command.EMPTY;
  
  /**
   * Known closest water
   */
  Tiles waterPosition;
  
  /**
   * Explore targets to choose from
   */
  LinkedList<Tiles> exploreDeque;
  
  /**
   * Current Target (where the ant is going)
   */
  Tiles target = null;
  
  /**
   * Current position
   */
  Tiles tile;
  
  /**
   * Enemy ant is next to
   */
  AntData enemy;
  
  // Debug flags
  public static final boolean DEBUG_HOME = false;
  public static final boolean DEBUG_FOOD = false;
  public static final boolean DEBUG_MOVE = false;
  public static final boolean DEBUG_BLOCKED = false;
  
  // Moves to avoid
  
  
  /**
   * Distance to count near food
   */
  
  
  /**
   * Commands to control the ant with, some unimplemented
   */
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
    HEAL
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
      case ATTACK:
        if (enemy == null && !nextToEnemy())
        {
          command = Command.EXPLORE;
          continueCommand();
        }
        else
        {
          attack(moveToDirection(enemy.gridX - tile.xPos, enemy.gridY - tile.yPos));
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
  
  /**
   * Current tile
   *
   * @return position tile
   */
  Tiles getTile()
  {
    return tile;
  }
  
  /**
   * Calculate the tile that is direction away
   *
   * @param dir direction
   * @return tile at direction
   */
  public Tiles getTileOffset(Direction dir)
  {
    return map.getTile(tile.xPos + dir.deltaX(), tile.yPos + dir.deltaY());
  }
  
  /**
   * ant data
   *
   * @return data
   */
  @Override
  public AntData getData()
  {
    return data;
  }
  
  /**
   * If ant is in stasis
   *
   * @return true if can move false otherwise
   */
  public boolean canMove()
  {
    return data.ticksUntilNextAction == 0;
  }
  
  /**
   * Set ant action to stasis
   */
  public void stasis()
  {
    data.myAction = new AntAction(AntActionType.STASIS);
  }
  
  /**
   * Unsupported, meant to flee from a direction
   *
   * @param dir direction to go away from
   */
  @Override
  public void flee(Direction dir)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  /**
   * Set action to attack
   *
   * @param dir direction to attack
   */
  @Override
  public void attack(Direction dir)
  {
    data.myAction = new AntAction(AntActionType.ATTACK, dir);
  }
  
  /**
   * Pickup maximum amount
   *
   * @param dir direction to pickup from
   */
  @Override
  public void pickup(Direction dir)
  {
    data.myAction = new AntAction(AntActionType.PICKUP, dir, data.antType.getCarryCapacity());
  }
  
  /**
   * Pickup
   *
   * @param dir direction to pickup from
   * @param quantity amount to pickup
   */
  @Override
  public void pickup(Direction dir, int quantity)
  {
    data.myAction = new AntAction(AntActionType.PICKUP, dir, quantity);
  }
  
  /**
   * Set action to drop Drop logic, tries to find the best direction to drop in
   */
  
  
  /**
   * Set action to heal for underground ants
   */
  public void heal()
  {
    data.myAction = new AntAction(AntActionType.HEAL);
  }
  
  /**
   * Move in direction
   *
   * @param dir which direction to move
   */
  
  
  /**
   * Normalize number to magnitude of 1 (or 0 if x is 0)
   *
   * @param x number to normalize
   * @return normalized number
   */
  public int normalize(int x)
  {
    if (x != 0)
    {
      return x / Math.abs(x);
    }
    return 0;
  }
  
  /**
   * Basic logic to move towards a target, does not account for path
   *
   * Tries to find greedy best move first and then moves near that if that is blocked
   */
  
  
  /**
   * Expects delta of magnitude 1, will find correct direction Enum value to return based on deltas
   *
   * @param deltaX x delta of move
   * @param deltaY y delta of move
   * @return direction of to move corresponding delta
   */
  private Direction moveToDirection(int deltaX, int deltaY)
  {
    for (Direction dir : Direction.values())
    {
      if (dir.deltaX() == deltaX && dir.deltaY() == deltaY)
      {
        return dir;
      }
    }
    System.out.println("Bad move in moveToDirection " + data.id + " x = " + deltaX + " y = " + deltaY);
    return Direction.getRandomDir();
  }
  
  /**
   * Check if ant is next to a position
   *
   * @param x x position
   * @param y y position
   * @return true if next to otherwise false
   */
  @Override
  public boolean nextTo(int x, int y)
  {
    return Math.abs(data.gridX - x) == 1 && Math.abs(data.gridY - y) == 1;
  }
  
  /**
   * Check to see if ant is next to enemy
   *
   * @return true if enemy is one square away
   */
  boolean nextToEnemy()
  {
    this.enemy = null;
    
    if (commData.enemyAntSet == null || commData.enemyAntSet.isEmpty())
    {
      return false;
    }
    
    for (AntData enemy : commData.enemyAntSet)
    {
      if (nextTo(enemy.gridX, enemy.gridY))
      {
        this.enemy = enemy;
        return true;
      }
    }
    return false;
  }
  
  /**
   * If ant is near food set closeFood and return true
   *
   * @return false if not near food in foodset true if near food
   */
  @Override
  public boolean nearFood()
  {
    if (closeFood != null)
    {
      if (commData.foodSet.contains(closeFood))
      {
        return true;
      }
    }
    for (FoodData food : commData.foodSet)
    {
      if (nearPoint(food.gridX, food.gridY))
      {
        closeFood = food;
        return true;
      }
    }
    closeFood = null;
    return false;
  }
  
  private boolean nextToWater()
  {
    for (Tiles neighbor : map.getNeighbors(tile.xPos, tile.yPos))
    {
      if (neighbor.water)
      {
        waterPosition = neighbor;
        target = neighbor;
        return true;
      }
    }
    return false;
  }
  
  /**
   * Determines if ant is near point using the NEAR_DISTANCE constant
   *
   * @param x position
   * @param y position
   * @return true if Manhattan distance is < NEAR_DISTANCE false otherwise
   */
 
  
  /**
   * Set action to move to a random direction
   */
  @Override
  public void moveRandom()
  {
    data.myAction = new AntAction(AntActionType.MOVE, Direction.getRandomDir());
  }
  
  /**
   * Set action to enter the nest
   */
  @Override
  public void enterNest()
  {
    data.myAction = new AntAction(AntActionType.ENTER_NEST);
  }
  
  /**
   * Set action to exit the nest
   *
   * @param p point to exit at
   */
  @Override
  public void exitNest(Point p)
  {
    data.myAction = new AntAction(AntActionType.EXIT_NEST, p.x, p.y);
  }
  
  /**
   * Set action to exit nest
   *
   * @param x x position to exit at
   * @param y y position to exit at
   */
  public void exitNest(int x, int y)
  {
    data.myAction = new AntAction(AntActionType.EXIT_NEST, x, y);
  }
  
  /**
   * Boolean check to see if ant is at home nest
   *
   * @return true if at home nest false otherwise
   */
  public boolean atNest()
  {
    if (Util.manhattanDistance(data.gridX, data.gridY, centerX, centerX) < 20
            && map.getTile(data.gridX, data.gridY).nest)
    {
      return true;
    }
    return false;
  }
  
  /**
   * If ant is 1 move away from food (so it can pickup food)
   *
   * @return true if next to any food, false otherwise
   */
  private boolean nextToFood()
  {
    for (FoodData food : commData.foodSet)
    {
      if (nextTo(food.gridX, food.gridY))
      {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Set the ant data
   *
   * @param ant data to set to
   */
  public void setData(AntData ant)
  {
    data = ant;
  }
  
  /**
   * Check if ant is underground
   *
   * @return true if underground false otherwise
   */
  public boolean underground()
  {
    return data.underground;
  }
  
  /**
   * Update needed ant data on communication update
   *
   * @param ant new ant data
   * @param commData new comm data
   */
  public void updateData(AntData ant, CommData commData)
  {
    data = ant;
    tile = map.getTile(data.gridX, data.gridY);
    this.commData = commData;
  }
  
  /**
   * Compare ant to another ant by id's
   *
   * @param ant other ant
   * @return true if other ant has same id
   */
  public boolean equals(AntData ant)
  {
    return data.id == ant.id;
  }
  
  /**
   * Hashcode for Hash structures, uses ant id
   *
   * @return int for hashcode which is just the ant id
   */
  @Override
  public int hashCode()
  {
    return data.id;
  }
  
  /**
   * Compare ant to another object
   *
   * @param obj object to compare to
   * @return true if other object is an ant and has the same id
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    final WorkerBrain other = (WorkerBrain) obj;
    if (this.data.id != other.data.id)
    {
      return false;
    }
    return true;
  }
  
  /**
   * Check if health is low
   *
   * @return true if health is less than or equal to half false otherwise
   */
  public boolean lowHealth()
  {
    return data.health <= data.antType.getMaxHealth() / 2;
  }
  
  /**
   * Set the current ant command
   *
   * @param command command to be set to
   */
  public void setCommand(Command command)
  {
    this.command = command;
  }
  
  /**
   * Set the current ant target
   *
   * @param target new target for ant
   */
  public void setTarget(Tiles target)
  {
    this.target = target;
  }
  
  /**
   * Set target based on coordinates
   *
   * @param x x coord
   * @param y y coord
   */
  public void setTarget(int x, int y)
  {
    target = map.getTile(x, y);
  }
  
  /**
   * Use AStar to find a path, very slow
   *
   * @param goal path to this target from current position
   * @return list of maptiles representing the path
   */
  @Override
  public java.util.List<Tiles> findPath(Tiles goal)
  {
    return map.generatePath(map.getTile(data.gridX, data.gridY), goal);
  }
  
}
