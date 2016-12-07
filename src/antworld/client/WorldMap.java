package antworld.client;

import antworld.common.Util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author Ederin Igharoro
 * Created by Ultimate Ediri on 11/26/2016.
 */
public class WorldMap
{
  private BufferedImage worldMapImage = null;
  public Tiles [][] mapTiles = null;

  public final int WIDTH;
  public final int HEIGHT;
  private final static int EXPLORE_SIZE = 20;

  public AstarAlgorithm discoverPath;
  public Set<Tiles>exploreTiles;
  public HashMap<Tiles, Set<Tiles> > exploreNeighbors;


  public WorldMap() throws IOException
  {
    worldMapImage = Util.loadImage("AntWorld.png", null);
    WIDTH = worldMapImage.getWidth();
    HEIGHT = worldMapImage.getHeight();
    mapTiles = new Tiles[WIDTH][HEIGHT];

    for(int i = 0; i < mapTiles.length; i++)
    {
      for(int j = 0; j < mapTiles[i].length; j++)
      {
        mapTiles[i][j] = generateWorldMapTiles(i, j, worldMapImage);
      }
    }

    discoverPath = new AstarAlgorithm(this);
    generateExploreSetAndMap();

  }

  private Tiles generateWorldMapTiles(int xPos, int yPos, BufferedImage worldMapImage)
  {
    return new Tiles(worldMapImage.getRGB(xPos, yPos), xPos, yPos);
  }

  public Tiles getTile(int xPos, int yPos)
  {
    return mapTiles[xPos][yPos];
  }

  LinkedList<Tiles> getNeighbors(int xPos, int yPos)
  {
    LinkedList<Tiles> neighborTiles = new LinkedList<>();

    for(int i = -1; i < 2; i++)
    {
      for(int j = -1; j < 2; j++)
      {
        if(i == 0 && j == 0){continue;}

        neighborTiles.add(getTile(xPos + i, yPos+j));
      }
    }

    return neighborTiles;
  }

  LinkedList<Tiles> generatePath(Tiles startTile, Tiles endTile)
  {
    return discoverPath.pathFinder(startTile, endTile);
  }

  public void generateExploreSetAndMap()
  {
    exploreTiles = new HashSet<>();

    for(int xPos = EXPLORE_SIZE / 2; xPos < WIDTH; xPos += EXPLORE_SIZE)
    {
      for(int yPos = EXPLORE_SIZE / 2; yPos < HEIGHT; yPos += EXPLORE_SIZE)
      {
        Tiles currentTile = getTile(xPos, yPos);

        if(currentTile.land){exploreTiles.add(currentTile);}

        else
          {
            currentTile = locateLandNearTile(currentTile);

            if(currentTile != null)
            {
              exploreTiles.add(currentTile);
            }
          }
      }
    }

    exploreNeighbors = new HashMap<>();

    for(Tiles tile: exploreTiles)
    {
      HashSet<Tiles> neighborTiles = new HashSet<>();

      for(int xPos = tile.xPos - EXPLORE_SIZE; xPos < tile.xPos + EXPLORE_SIZE; xPos += EXPLORE_SIZE)
      {
        for(int yPos = tile.yPos - EXPLORE_SIZE; yPos < tile.yPos + EXPLORE_SIZE; yPos += EXPLORE_SIZE)
        {
          if(xPos >= 0 && xPos < WIDTH && yPos >= 0 && yPos < HEIGHT && (xPos != tile.xPos && yPos != tile.yPos))
          {
            neighborTiles.add(getTile(xPos, yPos));
          }
        }
      }

      exploreNeighbors.put(tile, neighborTiles);
    }
  }

  private Tiles locateLandNearTile(Tiles center)
  {
    Tiles min = null;

    int minDistance = EXPLORE_SIZE + 1;
    int halfSize = EXPLORE_SIZE / 2;

    for(int xPos = center.xPos - halfSize; xPos < center.xPos + halfSize; xPos++)
    {
      for(int yPos = center.yPos - halfSize; yPos < center.yPos + halfSize; yPos++)
      {
        Tiles currentTile = getTile(xPos, yPos);

        if(min == null && currentTile.land)
        {
          min = currentTile;
          minDistance = currentTile.distance(center);
        }

        else if(currentTile.land && currentTile.distance(center) < minDistance)
        {
          min = currentTile;
          minDistance = currentTile.distance(center);
        }
      }
    }


    return min;
  }
}
