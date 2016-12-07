package antworld.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 * @author Ederin Igharoro
 * Created by Ultimate Ediri on 11/29/2016.
 */
public class AstarAlgorithm
{

  private WorldMap map;

  public AstarAlgorithm(WorldMap antWorldMap)
  {
    this.map = antWorldMap;
  }

  public LinkedList<Tiles> pathFinder(Tiles startTile, Tiles endTile)
  {
    HashSet<Tiles> open =  new HashSet<>();
    HashSet<Tiles> closed = new HashSet<>();
    HashMap<Tiles, Tiles> cameFrom = new HashMap<>();
    HashMap<Tiles, Integer> fScore = new HashMap<>();
    HashMap<Tiles, Integer> gScore = new HashMap<>();

    gScore.put(startTile, 0);
    fScore.put(startTile, gScore.get(startTile) + getCost(startTile, endTile));

    open.add(startTile);

    while(!open.isEmpty())
    {
      Tiles currentTile = null;
      int currentScore = 0;

      for(Tiles tile : open)
      {
        if(currentTile == null)
        {
          currentTile = tile;
          currentScore = fScore.get(currentTile);
        }

        else if(fScore.get(tile) < currentScore)
        {
          currentTile = tile;
          currentScore = fScore.get(currentTile);
        }
      }


      if(currentTile.equals(endTile)){return remakePathFinder(cameFrom, currentTile);}

      else if(currentTile.isBlocked())
      {
        closed.add(currentTile);
        continue;
      }

      closed.add(currentTile);
      open.remove(currentTile);

      for(Tiles neighborTiles : map.getNeighbors(currentTile.xPos, currentTile.yPos))
      {
        if(neighborTiles.isBlocked())
        {
          closed.add(neighborTiles);
        }

        if(closed.contains(neighborTiles))
        {
          continue;
        }

        int gScoreGuess = gScore.get(currentTile) + neighborTiles.greenValue;

        if((!open.contains(neighborTiles)) || gScoreGuess < gScore.get(neighborTiles))
        {
          cameFrom.put(neighborTiles, currentTile);
          gScore.put(neighborTiles, gScoreGuess);
          fScore.put(neighborTiles, gScoreGuess + getCost(neighborTiles, endTile));
          open.add(neighborTiles);
        }
      }

    }

    return null;
  }

  private Integer getCost(Tiles t1, Tiles t2)
  {
    return t1.distance(t2);
  }

  private LinkedList<Tiles> remakePathFinder(HashMap<Tiles, Tiles> cameFrom, Tiles currentTile)
  {
    LinkedList<Tiles> path = new LinkedList<>();

    while(cameFrom.containsKey(currentTile))
    {
      Tiles lastTile = currentTile;
      currentTile = cameFrom.get(lastTile);
      path.push(lastTile);
    }

    path.push(currentTile);
    return path;
  }

}
