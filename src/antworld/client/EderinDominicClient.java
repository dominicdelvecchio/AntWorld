package antworld.client;

/**
 * Created by Dominic on 12/9/2016.
 */

import antworld.common.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Dominic on 12/9/2016.
 */


import antworld.common.Util;
import antworld.common.AntAction;
import antworld.common.AntAction.AntActionType;
import antworld.common.AntData;
import antworld.common.AntType;
import antworld.common.CommData;
import antworld.common.Constants;
import antworld.common.FoodType;
import antworld.common.NestNameEnum;
import antworld.common.TeamNameEnum;
import antworld.client.DisplayMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class EderinDominicClient
{
  
  private boolean DEBUG = false;
  private boolean GUI = false;
  private static final TeamNameEnum myTeam = TeamNameEnum.Ederin_Dominic;
  private static final long password = 962740848319L;//Each team has been assigned a random password.
  private ObjectInputStream inputStream = null;
  private ObjectOutputStream outputStream = null;
  private boolean isConnected = false;
  private NestNameEnum myNestName = null;
  private int centerX, centerY;
  
  private final static int MAX_EXPLORE_DIST = 1200;
  
  private LinkedList<Tiles> exploreDeque;
  
  private HashMap<Integer, AntBrain> antTable = new HashMap<>();
  private final WorldMap map;
  private HashSet<Tiles> plannedMoves;
  private HashSet<Tiles> blockedMoves;
  private Tiles waterPosition = null;
  private AntBrain basicAnt = null;
  
  private DisplayMap mapView;
  
  private Socket clientSocket;
  
  private static Random random = Constants.random;
  private static final int MIN_ANT_ALIVE = 40;
  
  public EderinDominicClient(String host, int portNumber, int debugFlag) throws IOException
  {
    plannedMoves = new HashSet<>();
    blockedMoves = new HashSet<>();
    this.exploreDeque = new LinkedList<>();
    switch (debugFlag)
    {
      case 1:
        DEBUG = true;
        break;
      case 2:
        DEBUG = true;
        GUI = true;
        break;
      case 3:
        DEBUG = false;
        GUI = true;
      default:
        break;
    }
    
    this.map = new WorldMap();
    
    System.out.println("Starting EdernDominicClient: " + System.currentTimeMillis());
    isConnected = false;
    while (!isConnected)
    {
      isConnected = openConnection(host, portNumber);
      if (!isConnected)
      {
        try
        {
          Thread.sleep(1000);
        }
        catch (InterruptedException e1)
        {
        }
      }
    }
    
    if (GUI)
    {
      mapView = new DisplayMap(exploreDeque);
    }
    
    CommData data = obtainNest();
    mainGameLoop(data);
    closeAll();
  }
  
  private boolean openConnection(String host, int portNumber)
  {
    
    try
    {
      clientSocket = new Socket(host, portNumber);
    }
    catch (UnknownHostException e)
    {
      System.err.println("EderinDominicClient Error: Unknown Host " + host);
      e.printStackTrace();
      return false;
    }
    catch (IOException e)
    {
      System.err.println("EderinDominicClient: Could not open connection to " + host + " on port " + portNumber);
      e.printStackTrace();
      return false;
    }
    
    try
    {
      outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
      inputStream = new ObjectInputStream(clientSocket.getInputStream());
      
    }
    catch (IOException e)
    {
      System.err.println("EderinDominicClient: Could not open i/o streams");
      e.printStackTrace();
      return false;
    }
    
    return true;
    
  }
  
  public void closeAll()
  {
    System.out.println("EderinDominicClient.closeAll()");
    {
      try
      {
        if (outputStream != null)
        {
          outputStream.close();
        }
        if (inputStream != null)
        {
          inputStream.close();
        }
        clientSocket.close();
      }
      catch (IOException e)
      {
        System.err.println("EderinDominicClient Error: Could not close");
        e.printStackTrace();
      }
    }
  }
  
  public CommData obtainNest()
  {
    CommData data = new CommData(myTeam);
    data.password = password;
    
    if( sendCommData(data) )
    {
      try
      {
        if (DEBUG) System.out.println("EderinDominicClient: listening to socket....");
        data = (CommData) inputStream.readObject();
        if (DEBUG) System.out.println("EderinDominicClient: received <<<<<<<<<"+inputStream.available()+"<...\n" + data);
        
        if (data.errorMsg != null)
        {
          System.err.println("EderinDominicClient***ERROR***: " + data.errorMsg);
          System.exit(0);
        }
      }
      catch (IOException e)
      {
        System.err.println("EderinDominicClient***ERROR***: client read failed");
        e.printStackTrace();
        System.exit(0);
      }
      catch (ClassNotFoundException e)
      {
        System.err.println("EderinDominicClient***ERROR***: client sent incorrect common format");
      }
    }
    if (data.myTeam != myTeam)
    {
      System.err.println("EderinDominicClient***ERROR***: Server returned wrong team name: "+data.myTeam);
      System.exit(0);
    }
    if (data.myNest == null)
    {
      System.err.println("EderinDominicClient***ERROR***: Server returned NULL nest");
      System.exit(0);
    }
    
    myNestName = data.myNest;
    centerX = data.nestData[myNestName.ordinal()].centerX;
    centerY = data.nestData[myNestName.ordinal()].centerY;
    System.out.println("EderinDominicClient: ==== Nest Assigned ===>: " + myNestName);
    return data;
  }
  
  
  
  public void mainGameLoop(CommData data)
  {
    int foodCount = 0;
    
    setExploreTiles();
    setRightMostWater();
    
    //waterPositionMethed();
    Collections.sort(data.myAntList, new Comparator<AntData>()
    {
      
      @Override
      public int compare(AntData o1, AntData o2)
      {
        int ant1Dist = Util.manhattanDistance(o1.gridX, o1.gridY, centerX, centerX);
        int ant2Dist = Util.manhattanDistance(o2.gridX, o2.gridY, centerX, centerX);
        
        return Integer.compare(ant1Dist, ant2Dist);
      }
      
    });
    
    while (true)
    {
      try
      {
        
        if (DEBUG)
        {
          foodCount = countFood(data);
          
          System.out.println("Food count = " + foodCount);
          System.out.println("Water count = " + data.foodStockPile[FoodType.WATER.ordinal()]);
          System.out.println("Ant count = " + data.myAntList.size());
        }
        
        if (DEBUG)
        {
          System.out.println("EderinDominicClient: chooseActions: " + myNestName);
        }
        
        chooseActionsOfAllAnts(data);
        
        CommData sendData = data.packageForSendToServer();
        
        //System.out.println("SmartClient: Sending>>>>>>>: " + sendData);
        outputStream.writeObject(sendData);
        outputStream.flush();
        outputStream.reset();
        
        if (DEBUG)
        {
          System.out.println("EderinDominicClient: listening to socket....");
        }
        CommData recivedData = (CommData) inputStream.readObject();
        if (DEBUG)
        {
          System.out.println("EderinDominicClient: received <<<<<<<<<" + inputStream.available() + "<...\n" + recivedData);
        }
        data = recivedData;
        
        if (DEBUG)
        {
          System.out.println("Nest at (" + centerX + "," + centerY + ")");
        }
        
        if ((myNestName == null) || (data.myTeam != myTeam))
        {
          System.err.println("EderinDominicClient: !!!!ERROR!!!! " + myNestName);
        }
      }
      catch (IOException e)
      {
        System.err.println("EderinDominicClient***ERROR***: client read failed");
        e.printStackTrace();
        try
        {
          Thread.sleep(1000);
        }
        catch (InterruptedException e1)
        {
        }
        
      }
      catch (ClassNotFoundException e)
      {
        System.err.println("ServerToClientConnection***ERROR***: client sent incorect data format");
        e.printStackTrace();
        try
        {
          Thread.sleep(1000);
        }
        catch (InterruptedException e1)
        {
        }
      }
      
    }
  }
  
  private int countFood(CommData data)
  {
    int count = 0;
    for (int i = 0; i < data.foodStockPile.length; i++)
    {
      if (i == FoodType.WATER.ordinal())
      {
        continue;
      }
      count += data.foodStockPile[i];
    }
    return count;
  }
  
  private boolean sendCommData(CommData data)
  {
    
    CommData sendData = data.packageForSendToServer();
    try
    {
      if (DEBUG)
      {
        System.out.println("EderinDominicClient.sendCommData(" + sendData + ")");
      }
      outputStream.writeObject(sendData);
      outputStream.flush();
      outputStream.reset();
    }
    catch (IOException e)
    {
      System.err.println("EderinDominicClient***ERROR***: client read failed");
      e.printStackTrace();
      try
      {
        Thread.sleep(1000);
      }
      catch (InterruptedException e1)
      {
      }
      return false;
    }
    
    return true;
    
  }
  
  private void antGroup()
  {
    
  }
  
  private AntBrain getBrain (AntData ant, CommData commData, WorldMap map, LinkedList<Tiles> exploreDeque,
  HashSet<Tiles> blockedMoves, HashSet<Tiles> plannedMoves, int centerX, int centerY,
  Tiles waterPosition)
  {
    AntBrain curr;
    if(ant.antType.equals(AntType.ATTACK))
    {
      curr =  new AttackBrain(ant, commData, map,exploreDeque, blockedMoves,
              plannedMoves, centerX, centerY, waterPosition);
      return curr;
    }
  
    else if(ant.antType.equals(AntType.DEFENCE))
    {
      curr =  new DefenceBrain(ant, commData, map,exploreDeque, blockedMoves,
              plannedMoves, centerX, centerY, waterPosition);
      return curr;
    }
  
    else if(ant.antType.equals(AntType.SPEED))
    {
      curr =  new SpeedBrain(ant, commData, map,exploreDeque, blockedMoves,
              plannedMoves, centerX, centerY, waterPosition);
      return curr;
    }
  
    else if(ant.antType.equals(AntType.MEDIC))
    {
      curr =  new MedicBrain(ant, commData, map,exploreDeque, blockedMoves,
              plannedMoves, centerX, centerY, waterPosition);
      return curr;
    }
  
    else if(ant.antType.equals(AntType.VISION))
    {
      curr =  new VisionBrain(ant, commData, map,exploreDeque, blockedMoves,
              plannedMoves, centerX, centerY, waterPosition);
      return curr;
    }
    else
      curr = new WorkerBrain(ant, commData, map,exploreDeque, blockedMoves,
              plannedMoves, centerX, centerY, waterPosition);
      return curr;
  }
  
  private void chooseActionsOfAllAnts(CommData commData)
  {
    if (GUI)
    {
      mapView.setData(commData);
    }
    
    blockedMoves.clear();
    plannedMoves.clear();
    
    if (DEBUG)
    {
      System.out.println("Choosing all ant actions.");
    }
    
    // Sort ants by distance from nest (want ants closest to nest to move first)
    Collections.sort(commData.myAntList, new Comparator<AntData>()
    {
      
      @Override
      public int compare(AntData ant1, AntData ant2)
      {
        int ant1DistHome = Util.manhattanDistance(ant1.gridX, ant1.gridY, centerX, centerY);
        int ant2DistHome = Util.manhattanDistance(ant2.gridX, ant2.gridY, centerX, centerY);
        
        return Integer.compare(ant1DistHome, ant2DistHome);
      }
    });
    
    for (AntData ant : commData.myAntList)
    {
      AntBrain curr;
      
      if (ant.myAction.type == AntAction.AntActionType.DIED)
      {
        if (antTable.containsKey(ant.id))
        {
          antTable.remove(ant.id);
        }
        return;
      }
      else if (antTable.containsKey(ant.id))
      {
        curr = antTable.get(ant.id);
        curr.updateData(ant, commData);
      }
      else
      {
        curr = getBrain(ant,commData, map, exploreDeque, blockedMoves, plannedMoves, centerX, centerY, waterPosition);
        antTable.put(ant.id, curr);
      }
      
      if (basicAnt == null)
      {
        basicAnt = curr;
      }
      
      blockedMoves.add(curr.getTile());
      chooseAction(curr, commData);
    }
    if (DEBUG)
    {
      System.out.println("All ant actions chosen.");
    }
    
    if (commData.myAntList.size() < MIN_ANT_ALIVE)
    {
      //birthNewAnt(commData);
    }
    
  }
  
  private void chooseAction(AntBrain curr, CommData commData)
  {
    if (curr.canMove())
    {
      
      if (curr.equals(basicAnt))
      {
        switch (basicAnt.command)
        {
          case HOME:
            break;
          case HEAL:
            basicAnt = null;
            break;
          default:
            basicAnt.command = AntBrain.Command.WATER;
            break;
        }
        
        if (curr.data.myAction.type == AntAction.AntActionType.DIED)
        {
          basicAnt = null;
        }
      }
      
      if (curr.nextToEnemy())
      {
        curr.setCommand(AntBrain.Command.ATTACK);
      }
      else if (curr.data.carryUnits > 0)
      {
        curr.setCommand(AntBrain.Command.HOME);
      }
      else if (curr.underground() && curr.command != AntBrain.Command.HOME && curr.command != AntBrain.Command.HEAL)
      {
        int x = centerX - Constants.NEST_RADIUS + random.nextInt(2 * Constants.NEST_RADIUS);
        int y = centerY - Constants.NEST_RADIUS + random.nextInt(2 * Constants.NEST_RADIUS);
        curr.exitNest(x, y);
        return;
      }
      else if (curr.lowHealth())
      {
        curr.target = null;
        curr.setCommand(AntBrain.Command.HOME);
      }
      else if (curr.command != AntBrain.Command.GATHER && curr.nearFood() && (!curr.equals(basicAnt)))
      {
        curr.target = null;
        curr.setCommand(AntBrain.Command.GATHER);
      }
      else if (curr.target != null && curr.command == AntBrain.Command.EXPLORE && curr.tile.distance(centerX, centerY) > MAX_EXPLORE_DIST)
      {
        exploreDeque.remove(curr.target);
        curr.target = null;
      }
      
      curr.continueCommand();
    }
    else
    {
      curr.stasis();
    }
    
  }
  
  /**
   * Set the tiles for ants to choose from when they want to explore explore tiles are limited by
   * Manhattan distance
   *
   * If ants find that they are moving to far away
   */
  private void setExploreTiles()
  {
    for (Tiles tile : map.exploreTiles)
    {
      if (tile.distance(centerX, centerY) < MAX_EXPLORE_DIST && tile.distance(centerX, centerY) > 30)
      {
        exploreDeque.push(tile);
      }
    }
    
    // Need better way to set explore tiles
    Collections.sort(exploreDeque, new Comparator<Tiles>()
    {
      
      @Override
      public int compare(Tiles t1, Tiles t2)
      {
        return Integer.compare(t1.distance(centerX, centerY), t2.distance(centerX, centerY));
      }
    });
    
    System.out.println("Distance first = " + exploreDeque.get(0).distance(centerX, centerY));
    System.out.println("Distance second = " + exploreDeque.get(1).distance(centerX, centerY));
    System.out.println("Distance last = " + exploreDeque.peekLast().distance(centerX, centerY));
    
  }
  
  /**
   * Find water closest to the right of nest
   */
  private void setRightMostWater()
  {
    for (int x = centerX; x < map.WIDTH; x++)
    {
      if (map.getTile(x, centerY).water)
      {
        waterPosition = map.getTile(x, centerY);
        return;
      }
    }
  }
  
  /**
   * Birth a new ant of preferred type need to test called on line 449
   *
   * @param commData current comm data to update
   */
  /*private void birthNewAnt(CommData commData)
  {
    List<AntType> preferences = new ArrayList<AntType>();
    preferences.add(AntType.DEFENCE);
    preferences.add(AntType.WORKER);
    preferences.add(AntType.ATTACK);
    preferences.add(AntType.SPEED);
    preferences.add(AntType.VISION);
    preferences.add(AntType.MEDIC);
    
    // Try preferred birth type
    for (AntType type : preferences)
    {
      int i = 0;
      while(type.getBirthFood().si)
      if (commData.foodStockPile[type.getBirthFood().ordinal()] >= type.getFoodUnitsToSpawn())
      {
        AntData newAnt = new AntData(Constants.UNKNOWN_ANT_ID, type, commData.myNest, commData.myTeam);
        newAnt.myAction = new AntAction(AntAction.AntActionType.BIRTH);
        commData.myAntList.add(newAnt);
        if (DEBUG)
        {
          System.out.println("Birthing new ant of type " + type);
        }
        return;
      }
    }
    
    // Otherwise search through all types
    for (AntType type : AntType.values())
    {
      if (commData.foodStockPile[type.getBirthFood().ordinal()] >= type.getFoodUnitsToSpawn())
      {
        AntData newAnt = new AntData(Constants.UNKNOWN_ANT_ID, type, commData.myNest, commData.myTeam);
        newAnt.myAction = new AntAction(AntAction.AntActionType.BIRTH);
        commData.myAntList.add(newAnt);
        if (DEBUG)
        {
          System.out.println("Birthing new ant of type " + type);
        }
        return;
      }
    }
  }*/
  
  /**
   * Main runs the client and connects to the server
   *
   * @param args command line args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException
  {
    String serverHost = "localhost";
    //System.out.println(args.length);
    if (args.length > 0)
    {
      serverHost = args[0];
    }
    
    if (args.length > 1)
    {
      new EderinDominicClient(serverHost, Constants.PORT, Integer.parseInt(args[1]));
    }
    
    new EderinDominicClient(serverHost, Constants.PORT, 0);
  }
}
