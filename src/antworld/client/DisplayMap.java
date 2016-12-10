package antworld.client;

/**
 * Created by Dominic on 12/9/2016.
 */

        
        import antworld.client.Tiles;
        import antworld.common.AntData;
        import antworld.common.CommData;
        import antworld.common.FoodData;
        import java.awt.Color;
        import java.awt.Dimension;
        import java.awt.Graphics;
        import java.awt.Graphics2D;
        import java.awt.geom.AffineTransform;
        import java.awt.image.BufferedImage;
        import java.io.IOException;
        import java.net.URL;
        import java.util.*;
        import java.util.logging.Level;
        import java.util.logging.Logger;
        import javax.imageio.ImageIO;
        import javax.swing.*;


 //Debug utility class used for viewing the map.
 
public class DisplayMap extends JFrame
{
  
  CommData data;
  ImagePanel map;
  LinkedList<Tiles> exploreDeque = null;
  final int DRAW_SIZE = 5;
  
  public DisplayMap() throws IOException
  {
    map = new ImagePanel(getClass().getClassLoader().getResource("antworld/resources/AntWorld.png"));
    JScrollPane pane = new JScrollPane(map);
    getContentPane().add(pane);
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setPreferredSize(new Dimension(1280, 720));
    pack();
    setVisible(true);
  }
  
  public DisplayMap(LinkedList<Tiles> exploreDeque) throws IOException
  {
    this();
    this.exploreDeque = new LinkedList<Tiles>(exploreDeque);
  }
  
  public void setData(CommData data)
  {
    this.data = data;
    map.repaint();
  }
  
  public static void main(String[] args)
  {
    try
    {
      new DisplayMap();
    }
    catch (IOException ex)
    {
      Logger.getLogger(DisplayMap.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public class ImagePanel extends JPanel
  {
    
    BufferedImage image;
    double zoomScale = 1.0;
    double zoomSize = 0.5;
    final int WIDTH;
    final int HEIGHT;
    
    private ImagePanel(URL imageURL) throws IOException
    {
      image = ImageIO.read(imageURL);
      WIDTH = image.getWidth();
      HEIGHT = image.getHeight();
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      Graphics2D g2D = (Graphics2D) g;
      AffineTransform trans = new AffineTransform();
      trans.scale(zoomScale, zoomScale);
      g2D.drawRenderedImage(image, trans);
      g2D.setColor(Color.PINK);
      if (exploreDeque != null)
      {
        for (Tiles tile : exploreDeque)
        {
          g2D.fillRect(tile.xPos, tile.yPos, 5, 5);
        }
      }
      
      if (data != null)
      {
        g2D.setColor(Color.YELLOW);
        if (data.myAntList != null)
        {
          for (AntData ant : data.myAntList)
          {
            if (ant.carryUnits > 0)
            {
              g2D.setColor(Color.ORANGE);
              
            }
            else
            {
              g2D.setColor(Color.YELLOW);
              
            }
            g2D.fillRect(ant.gridX, ant.gridY, DRAW_SIZE, DRAW_SIZE);
          }
        }
        
        g2D.setColor(Color.CYAN);
        if (data.foodSet != null)
        {
          for (FoodData food : data.foodSet)
          {
            g2D.fillRect(food.gridX, food.gridY, DRAW_SIZE, DRAW_SIZE);
          }
        }
        
        g2D.setColor(Color.RED);
        
        if (data.enemyAntSet != null)
        {
          for (AntData ant : data.enemyAntSet)
          {
            g2D.fillRect(ant.gridX, ant.gridY, DRAW_SIZE, DRAW_SIZE);
          }
        }
      }
      
      int width = (int) (WIDTH * zoomScale);
      int height = (int) (HEIGHT * zoomScale);
      setPreferredSize(new Dimension(width, height));
    }
    
  }
  
}