package antworld.client;

import antworld.common.Util;
import java.awt.*;

/**
 *
 * @author Ederin Igharoro
 * Created by Ultimate Ediri on 11/26/2016.
 */
public class Tiles
{
  public boolean land = false;
  public boolean water = false;
  public boolean nest = false;

  public int greenValue = 0;
  public int  xPos, yPos;


  /**
   *
   * @param colorRGB rgb int values
   * @param x x position
   * @param y y position
   */
  public Tiles(int colorRGB, int x, int y)
  {
    Color color = new Color(colorRGB);

    this.xPos = x;
    this.yPos = y;

  }


  /**
   * Set the tile values based on the color in the image map
   * @param tileColor - Color assigned to each tile
   */
  private void setTile(Color tileColor)
  {
    if(tileColor.getBlue() >= 200){water = true;}
    else if(tileColor.getRed() >= 200 && tileColor.getGreen()>=200){nest = true;}

    else
      {
        greenValue = tileColor.getGreen();
        land = true;
      }
  }

  /**
   * checks if the position is blocked for the ant
   * @return true if water is true, false if otherwise
   */
  public boolean isBlocked(){return water;}

  /**
   * Distance from current tile to another tile
   *
   * @param otherTile other tile to compare distance
   * @return Manhattan distance
   */
  public int distance(Tiles otherTile)
  {
    return Util.manhattanDistance(xPos, yPos, otherTile.xPos, otherTile.yPos);
  }

  /**
   * distance from an x and y postion
   *
   * @param gridX x pos
   * @param gridY y pos
   * @return Manhattan distance
   */
  public int distance(int gridX, int gridY)
  {
    return Util.manhattanDistance(xPos, yPos, gridX, gridY);
  }


  /**
   * Hash is x position + y position
   *
   * @return hash
   */
  @Override
  public int hashCode()
  {
    return xPos + yPos;
  }


  /**
   * Compare map tile to other objects
   *
   * @param obj
   * @return true if position is the same
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
    final Tiles other = (Tiles) obj;
    if (this.xPos != other.xPos)
    {
      return false;
    }
    if (this.yPos != other.yPos)
    {
      return false;
    }
    return true;
  }

}
