/*
 * Map2DInput.java
 *
 * Created on 17 de agosto de 2005, 11:30
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */


package drillsim.visualization2d;

import drillsim.forms.*;
import drillsim.layers.*;
import drillsim.database.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.util.*;
import drillsim.parser.*;
import drillsim.concepts.*;
import drillsim.concepts.Avatar;
import drillsim.concepts.Hazard;
import drillsim.layers.GridLayer;
import drillsim.layers.Layer;
import drillsim.layers.LineLayer;
import drillsim.layers.PointLayer;
import drillsim.layers.PolygonLayer;
import drillsim.layers.Property;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.PrintWriter;
import javax.vecmath.Point2f;


/**
 *
 * @author Luis
 */
public class Map2DInput extends JPanel implements Runnable,
                                KeyListener, MouseListener, MouseMotionListener, Display {

	private GeoInterface iface = new GeoInterface("tchen3", "69Weiaid");

    State st = new State(this);    
    Parser parser = new Parser(st);
	String layerName = "";
   
    // OUTPUT
    PrintWriter out_to_server;

    // AFFINE TRANSFORM
    AffineTransform transform_user = new AffineTransform();
    
    // Form list of properties
    private final PropertyEditor property_editor = new PropertyEditor(null);
	private final Resource resource = new Resource(null);
	private final Entry entry = new Entry(null);
	private final Crisis crisis = new Crisis(null);
	private final Obstacles obstacle = new Obstacles(null);
	private final People people = new People(null);
	private final Personnel personnel = new Personnel(null);

	// Currently selected
	private final int PEOPLE = 0;
	private final int CRISIS = 1;
	private final int ENTRY = 2;
	private final int OBSTACLE = 3;
	private final int PERSONNEL = 4;
	private final int RESOURCE = 5;
    
    /** Creates a new instance of Map2D */
    public Map2DInput(PrintWriter output) {
      super(true);
      out_to_server = output;
      createPopupMenus();
      addMouseListener(this);
      addMouseMotionListener(this);
    }
    
    
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        g2.setBackground(st.back_color);
        g2.clearRect(0, 0, 1024, 768);

        g2.transform(transform_user);
        g2.setBackground(Color.LIGHT_GRAY);
        g2.clearRect(0, 0, 1024, 768);
        BufferedImage bg = st.getBackground();
        if(bg != null)
        g2.drawImage(bg, null, 0, 0);

        g2.transform(st.transform);
        
        // Drawing avatars
        for(Iterator<Avatar> it = st.getAvatarIterator(); it.hasNext(); )
            it.next().draw(g2);
        
        // Drawing hazards
        g2.setColor(new Color(255, 0, 0, 100));
        
        for(Iterator<Hazard> it = st.getHazardIterator(); it.hasNext(); )
            it.next().draw(g2);
        
        for(Iterator<Layer> it = layers.values().iterator(); it.hasNext(); )
            it.next().draw(g2);
    }
        
    public void executeCommand(String command)
    {
        Command c = parser.parse(command);        
        if(c != null) c.execute();
    }
        
    Point2D sToW(Point2D p)
    {
        try
        {
            AffineTransform t = new AffineTransform(st.transform);            
            t.preConcatenate(transform_user);
            t.inverseTransform(p, p);
        }
        catch(Exception e)
        {
            p = new Point.Float(0, 0);
        }        
        return p;
    }
    
    Point2D wToS(Point2D p)
    {
        try
        {
            AffineTransform t = new AffineTransform(st.transform);            
            t.preConcatenate(transform_user);
            t.transform(p, p);
        }
        catch(Exception e)
        {
            p = new Point.Float(0, 0);
        }        
        return p;
    }
    
    // THREAD
    public void run()
    {
    }
    
    // TIME MANAGEMENT
    private long time = 0;
    private long inc = 100;
    
    public synchronized long getTime()
    {
        return time;
    }
    
    public synchronized void incTime()
    {
        time += inc;
    }
    
    public synchronized void setTime(long t)
    {
        time = t;
    }
        
    public Parser getParser()
    {
        return parser;
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyPressed(KeyEvent e)
    {
        switch(e.getKeyCode())
        {
            case KeyEvent.VK_UP:
                transform_user.translate(0, 2);
                repaint();
                break;
                
            case KeyEvent.VK_DOWN:
                transform_user.translate(0, -2);
                repaint();
                break;
                
            case KeyEvent.VK_RIGHT:
                transform_user.translate(-2, 0);
                repaint();
                break;
                
            case KeyEvent.VK_LEFT:
                transform_user.translate(2, 0);
                repaint();
                break;
                
            case KeyEvent.VK_PAGE_UP:
                //transform.translate(st.w / 2, st.h / 2);
                //transform.scale(1.1, 1.1);
                //transform.translate(-st.w / 2, -st.h / 2);
                repaint();
                break;
                
            case KeyEvent.VK_PAGE_DOWN:
                //transform.translate(st.w / 2, st.h / 2);
                //transform.scale(0.9, 0.9);
                //transform.translate(-st.w / 2, -st.h / 2);
                repaint();
                break;
        }
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    // RIGHT CLICK ACTION
	
    public void mousePressed(MouseEvent e)
    {
		
        if(e.getButton() == e.BUTTON3)
        {
            if(active_layer != null)
            {
                Pickable picked = null;
                Point2D p = sToW(new Point2D.Float(e.getX(), e.getY()));
                
                if((picked = active_layer.click((float)p.getX(), (float)p.getY())) != null)
                {
                    Vector<Property> pro = active_layer.getProperties(picked);
                    repaint();
                    
                    if(pro != null)
                    {
						
						if (active_layer.getInputType() == RESOURCE)
						{
							resource.setProperties(pro);
							resource.setVisible(true);
						}
						else if (active_layer.getInputType() == ENTRY)
						{
							entry.setProperties(pro);
							entry.setVisible(true);
						}	
						else if (active_layer.getInputType() == CRISIS)
						{
							crisis.setProperties(pro);
							crisis.setVisible(true);
						}
						else if (active_layer.getInputType() == OBSTACLE)
						{
							obstacle.setProperties(pro);
							obstacle.setVisible(true);
						}
						else if (active_layer.getInputType() == PEOPLE)
						{
							people.setProperties(pro);
							people.setVisible(true);
						}
						else if (active_layer.getInputType() == PERSONNEL)
						{
							personnel.setProperties(pro);
							personnel.setVisible(true);
						}
						
						// original
						// property_editor.setProperties(pro);
						// property_editor.setVisible(true);
                        return;
                    }
                }
            }
            popup.show(this, e.getX(), e.getY());
			
        }
        else if(e.getButton() == e.BUTTON1 && active_layer != null)
        {
            Point2D p = sToW(new Point2D.Float(e.getX(), e.getY()));
            active_layer.addPoint(new Point2f((float)p.getX(), (float)p.getY()));
            repaint();
        }
		
    }
	
    
    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseMoved(MouseEvent e)
    {
    }

    public void mouseDragged(MouseEvent e)
    {
        Point2D p = sToW(new Point2D.Float(e.getX(), e.getY()));
        active_layer.addPoint(new Point2f((float)p.getX(), (float)p.getY()));
        repaint();
    }
    
    // Layers
    Hashtable<String, Layer> layers = new Hashtable<String, Layer>();
    Enumeration layers_c = null;
    Layer active_layer = null;
    
    // POPUP MENU
    JPopupMenu popup;    
	JMenuItem menu_primitive;
    JMenu menu_new_people;
		//JMenuItem menu_primitive_people;
        JMenuItem menu_points_people;        
        JMenuItem menu_lines_people;
        JMenuItem menu_polygons_people;
        JMenuItem menu_grid_people;
	JMenu menu_new_crisis;
		//JMenuItem menu_primitive_crisis;
        JMenuItem menu_points_crisis;        
        JMenuItem menu_lines_crisis;
        JMenuItem menu_polygons_crisis;
        JMenuItem menu_grid_crisis;
	JMenu menu_new_personnel;
		//JMenuItem menu_primitive_personnel;
        JMenuItem menu_points_personnel;        
        JMenuItem menu_lines_personnel;
        JMenuItem menu_polygons_personnel;
        JMenuItem menu_grid_personnel;
	JMenu menu_new_obstacles;
		//JMenuItem menu_primitive_obstacles;
        JMenuItem menu_points_obstacles;        
        JMenuItem menu_lines_obstacles;
        JMenuItem menu_polygons_obstacles;
        JMenuItem menu_grid_obstacles;
	JMenu menu_new_entry;
		//JMenuItem menu_primitive_entry;
        JMenuItem menu_points_entry;        
        JMenuItem menu_lines_entry;
        JMenuItem menu_polygons_entry;
        JMenuItem menu_grid_entry;
	JMenu menu_new_resource;
		//JMenuItem menu_primitive_resource;
        JMenuItem menu_points_resource;        
        JMenuItem menu_lines_resource;
        JMenuItem menu_polygons_resource;
        JMenuItem menu_grid_resource;
	
		
    JMenu menu_active;
        JRadioButtonMenuItem menu_none;
        // JRadioButtonMenuItems active
    ButtonGroup b_group;
    JMenu  menu_draw;
        // JCheckBoxMenuItems to draw
    JMenuItem menu_color;
    JMenu menu_delete;
        // JMenuItems with names of layers
    JMenuItem menu_add_property_layer;
	// Write to database
	JMenuItem commit;
    
    
    private void createPopupMenus()
    {
        popup = new JPopupMenu();
        
        // PRIMITIVE
        menu_primitive = new JMenuItem(new AbstractAction("New Primitive")
        {
            public void actionPerformed(ActionEvent evt)
            {
                if(active_layer != null)
                    active_layer.newPrimitive();
                else
                    JOptionPane.showMessageDialog(null, "No layer selected.");
            }
        });

        // NEW
		// people
        menu_new_people = new JMenu("New people");        
        menu_points_people = new JMenuItem(new AddLayerAction("People Points"));
        menu_new_people.add(menu_points_people);
        menu_lines_people = new JMenuItem(new AddLayerAction("People Lines"));
        menu_new_people.add(menu_lines_people);
        menu_polygons_people = new JMenuItem(new AddLayerAction("People Polygons"));
        menu_new_people.add(menu_polygons_people);
        menu_grid_people = new JMenuItem(new AddLayerAction("People Grid"));
        menu_new_people.add(menu_grid_people);

		// crisis
		menu_new_crisis = new JMenu("New crisis");        
        menu_points_crisis = new JMenuItem(new AddLayerAction("Crisis Points"));
        menu_new_crisis.add(menu_points_crisis);
        menu_lines_crisis = new JMenuItem(new AddLayerAction("Crisis Lines"));
        menu_new_crisis.add(menu_lines_crisis);
        menu_polygons_crisis = new JMenuItem(new AddLayerAction("Crisis Polygons"));
        menu_new_crisis.add(menu_polygons_crisis);
        menu_grid_crisis = new JMenuItem(new AddLayerAction("Crisis Grid"));
        menu_new_crisis.add(menu_grid_crisis);

		// personnel
		menu_new_personnel = new JMenu("New personnel");        
        menu_points_personnel = new JMenuItem(new AddLayerAction("Personnel Points"));
        menu_new_personnel.add(menu_points_personnel);
        menu_lines_personnel = new JMenuItem(new AddLayerAction("Personnel Lines"));
        menu_new_personnel.add(menu_lines_personnel);
        menu_polygons_personnel = new JMenuItem(new AddLayerAction("Personnel Polygons"));
        menu_new_personnel.add(menu_polygons_personnel);
        menu_grid_personnel = new JMenuItem(new AddLayerAction("Personnel Grid"));
        menu_new_personnel.add(menu_grid_personnel);

		// obstacle
		menu_new_obstacles = new JMenu("New obstacle");        
        menu_points_obstacles = new JMenuItem(new AddLayerAction("Obstacles Points"));
        menu_new_obstacles.add(menu_points_obstacles);
        menu_lines_obstacles = new JMenuItem(new AddLayerAction("Obstacles Lines"));
        menu_new_obstacles.add(menu_lines_obstacles);
        menu_polygons_obstacles = new JMenuItem(new AddLayerAction("Obstacles Polygons"));
        menu_new_obstacles.add(menu_polygons_obstacles);
        menu_grid_obstacles = new JMenuItem(new AddLayerAction("Obstacles Grid"));
        menu_new_obstacles.add(menu_grid_obstacles);

		// entry
		menu_new_entry = new JMenu("New entry");        
        menu_points_entry = new JMenuItem(new AddLayerAction("Entry Points"));
        menu_new_entry.add(menu_points_entry);
        menu_lines_entry = new JMenuItem(new AddLayerAction("Entry Lines"));
        menu_new_entry.add(menu_lines_entry);
        menu_polygons_entry = new JMenuItem(new AddLayerAction("Entry Polygons"));
        menu_new_entry.add(menu_polygons_entry);
        menu_grid_entry = new JMenuItem(new AddLayerAction("Entry Grid"));
        menu_new_entry.add(menu_grid_entry);

		// resource
		menu_new_resource = new JMenu("New resource");        
        menu_points_resource = new JMenuItem(new AddLayerAction("Resource Points"));
        menu_new_resource.add(menu_points_resource);
        menu_lines_resource = new JMenuItem(new AddLayerAction("Resource Lines"));
        menu_new_resource.add(menu_lines_resource);
        menu_polygons_resource = new JMenuItem(new AddLayerAction("Resource Polygons"));
        menu_new_resource.add(menu_polygons_resource);
        menu_grid_resource = new JMenuItem(new AddLayerAction("Resource Grid"));
        menu_new_resource.add(menu_grid_resource);
              
        // ACTIVE
        menu_active = new JMenu("Active layer");
        b_group = new ButtonGroup();
        menu_none = new JRadioButtonMenuItem(new SetLayerAction("None"));
        b_group.add(menu_none);     
        menu_none.setSelected(true);
        menu_active.add(menu_none);
        
        // DRAW
        menu_draw = new JMenu("Draw layer");
        
        // COLOR
        menu_color = new JMenuItem(new AbstractAction("Set Color")
        {
            {
                putValue(Action.SHORT_DESCRIPTION, "Select the color for the active layer.");
            } 
            
            public void actionPerformed(ActionEvent evt)
            {
                if(active_layer == null)
                {
                    JOptionPane.showMessageDialog(null, "There is no active layer.");
                    return;
                }
                
                Color c = JColorChooser.showDialog(null, "Color for layer " + active_layer.getName(), 
                        Color.black);
                
                if(c != null) 
                {
                    active_layer.setColor(c);                
                    menu_color.setForeground(c);
                    repaint();
                }
            }
        });
        
        // DELETE
        menu_delete = new JMenu("Delete layer");
        
        
		/* -- not using since you can't specify which polygon you want to add attributes to --

		// PropertyLayer
        menu_add_property_layer = new JMenuItem(new AbstractAction("Add property to layer")
        {
            {
                putValue(Action.SHORT_DESCRIPTION, "Add a property to the active layer.");
            } 
            
            public void actionPerformed(ActionEvent evt)
            {
                if(active_layer == null)
                {
                    JOptionPane.showMessageDialog(null, "There is no active layer.");
                    return;
                }

                if (active_layer.getInputType() == RESOURCE)
				{
					resource.setProperties(active_layer.getProperties());
					resource.setVisible(true);
				}
				else if (active_layer.getInputType() == ENTRY)
				{
					entry.setProperties(active_layer.getProperties());
					entry.setVisible(true);
				}
				else if (active_layer.getInputType() == CRISIS)
				{
					crisis.setProperties(active_layer.getProperties());
					crisis.setVisible(true);
				}
				else if (active_layer.getInputType() == OBSTACLE)
				{
					obstacle.setProperties(active_layer.getProperties());
					obstacle.setVisible(true);
				}
				else if (active_layer.getInputType() == PEOPLE)
				{
					people.setProperties(active_layer.getProperties());
					people.setVisible(true);
				}
				else if (active_layer.getInputType() == PERSONNEL)
				{
					personnel.setProperties(active_layer.getProperties());
					personnel.setVisible(true);
				}

				// original
				// property_editor.setProperties(active_layer.getProperties());
				// property_editor.setVisible(true);
            }
        });
		*/

		commit = new JMenuItem(new AbstractAction("Commit")
		{
			public void actionPerformed(ActionEvent evt)
			{
				commitToDB();
			}
		});

        popup.add(menu_primitive);
        popup.add(menu_new_people);
		popup.add(menu_new_crisis);
		popup.add(menu_new_personnel);
		popup.add(menu_new_obstacles);
		popup.add(menu_new_entry);
		popup.add(menu_new_resource);
        popup.add(menu_active);
        popup.add(menu_draw);
        popup.add(menu_color);
        popup.add(menu_delete);
        //popup.add(menu_add_property_layer);
		popup.add(commit);
    }    

	private void commitToDB()
	{
		Layer commitingLayer = null;
		Vector<Property> commitingProperties = null;
		layers_c = layers.keys();
		String saveStr;
		int saveInt;

		if(!layers_c.hasMoreElements())
		{
			JOptionPane.showMessageDialog(null, "Nothing to commit");
			return;
		}

		saveStr = JOptionPane.showInputDialog(null, "Enter save ID:");

		try
		{
			saveInt = new Integer(saveStr);
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(this, "You have to provide an integer as value.");
            return;
		}

		for(Enumeration e = layers_c; e.hasMoreElements();)
		{
			String name = (String)e.nextElement();
			
			commitingLayer = layers.get(name);
			
			if(commitingLayer.getInputType() == PEOPLE)
			{
				if(commitingLayer instanceof PolygonLayer)
				{
					int numProp = ((PolygonLayer)commitingLayer).size();
					int[] num = new int[numProp];
					double[] x = new double[numProp];
					double[] y = new double[numProp];
					double[] width = new double[numProp];
					double[] height = new double[numProp];

					for(int it = 0; it < numProp; it++)
					{
						Property p = ((PolygonLayer)commitingLayer).indexGetProperties(it);
						num[it] = ((PeopleProperty)p).getNum();
						x[it] = ((PolygonLayer)commitingLayer).getX(it);
						y[it] = ((PolygonLayer)commitingLayer).getY(it);
						width[it] = ((PolygonLayer)commitingLayer).getWidth(it);
						height[it] = ((PolygonLayer)commitingLayer).getHeight(it);

						JOptionPane.showMessageDialog(null, "commiting to people property: " + num[it] + "\n" + x[it] + "\n" + y[it] + "\n" + width[it] + "\n" + height[it]);
						
						iface.people(saveInt, num[it], x[it], y[it], width[it], height[it]);	// use interface to write to DB
					}
				}
			}
			else if(commitingLayer.getInputType() == ENTRY)
			{
				if(commitingLayer instanceof PolygonLayer)
				{
					int numProp = ((PolygonLayer)commitingLayer).size();
					double[] dist = new double[numProp];
					double[] x = new double[numProp];
					double[] y = new double[numProp];
					double[] width = new double[numProp];
					double[] height = new double[numProp];

					for(int it = 0; it < numProp; it++)
					{
						Property p = ((PolygonLayer)commitingLayer).indexGetProperties(it);
						dist[it] = ((EntryProperty)p).getDist();
						x[it] = ((PolygonLayer)commitingLayer).getX(it);
						y[it] = ((PolygonLayer)commitingLayer).getY(it);
						width[it] = ((PolygonLayer)commitingLayer).getWidth(it);
						height[it] = ((PolygonLayer)commitingLayer).getHeight(it);

						JOptionPane.showMessageDialog(null, "commiting to entry property: " + dist[it] + "\n" + x[it] + "\n" + y[it] + "\n" + width[it] + "\n" + height[it]);
						
						iface.entry(saveInt, dist[it], x[it], y[it], width[it], height[it]);	// use interface to write to DB
					}
				}
			}
			else if(commitingLayer.getInputType() == PERSONNEL)
			{				
				if(commitingLayer instanceof PolygonLayer)
				{
					int numProp = ((PolygonLayer)commitingLayer).size();
					String[] persType = new String[numProp];
					int[] time = new int[numProp];
					int[] num = new int[numProp];
					double[] x = new double[numProp];
					double[] y = new double[numProp];
					double[] width = new double[numProp];
					double[] height = new double[numProp];

					for(int it = 0; it < numProp; it++)
					{
						Property p = ((PolygonLayer)commitingLayer).indexGetProperties(it);
						persType[it] = ((PersonnelProperty)p).getPersType();
						time[it] = ((PersonnelProperty)p).getTime();
						num[it] = ((PersonnelProperty)p).getNum();
						x[it] = ((PolygonLayer)commitingLayer).getX(it);
						y[it] = ((PolygonLayer)commitingLayer).getY(it);
						width[it] = ((PolygonLayer)commitingLayer).getWidth(it);
						height[it] = ((PolygonLayer)commitingLayer).getHeight(it);

						JOptionPane.showMessageDialog(null, "commiting to resource property: " + 
							persType[it] +"\n" + time[it] + "\n" + num[it] + "\n" + x[it] + "\n" + y[it] + "\n" + width[it] + "\n" + height[it]);
						
						iface.personnel(saveInt, persType[it], time[it], num[it], x[it], y[it], width[it], height[it]); 
					}
				}
			}
			else if(commitingLayer.getInputType() == OBSTACLE)
			{				
				if(commitingLayer instanceof PolygonLayer)
				{
					int numProp = ((PolygonLayer)commitingLayer).size();
					String[] obType = new String[numProp];
					double[] obHeight = new double[numProp];
					double[] x = new double[numProp];
					double[] y = new double[numProp];
					double[] width = new double[numProp];
					double[] height = new double[numProp];

					for(int it = 0; it < numProp; it++)
					{
						Property p = ((PolygonLayer)commitingLayer).indexGetProperties(it);
						obType[it] = ((ObstacleProperty)p).getObType();
						obHeight[it] = ((ObstacleProperty)p).getHeight();
						x[it] = ((PolygonLayer)commitingLayer).getX(it);
						y[it] = ((PolygonLayer)commitingLayer).getY(it);
						width[it] = ((PolygonLayer)commitingLayer).getWidth(it);
						height[it] = ((PolygonLayer)commitingLayer).getHeight(it);

						JOptionPane.showMessageDialog(null, "commiting to resource property: " + 
							 obHeight[it] +"\n" + obType[it] + "\n" + x[it] + "\n" + y[it] + "\n" + width[it] + "\n" + height[it]);
						
						iface.obstacle(saveInt, obType[it], obHeight[it], x[it], y[it], width[it], height[it]);
					}
				}
			}
			else if(commitingLayer.getInputType() == RESOURCE)
			{	
				if(commitingLayer instanceof PolygonLayer)
				{
					int numProp = ((PolygonLayer)commitingLayer).size();
					String[] resType = new String[numProp];
					int[] capacity = new int[numProp];
					boolean[] isStatic = new boolean[numProp];
					double[] x = new double[numProp];
					double[] y = new double[numProp];
					double[] width = new double[numProp];
					double[] height = new double[numProp];

					for(int it = 0; it < numProp; it++)
					{
						Property p = ((PolygonLayer)commitingLayer).indexGetProperties(it);
						resType[it] = ((ResourceProperty)p).getResType();
						capacity[it] = ((ResourceProperty)p).getCapacity();
						isStatic[it] = ((ResourceProperty)p).getIsStatic();
						x[it] = ((PolygonLayer)commitingLayer).getX(it);
						y[it] = ((PolygonLayer)commitingLayer).getY(it);
						width[it] = ((PolygonLayer)commitingLayer).getWidth(it);
						height[it] = ((PolygonLayer)commitingLayer).getHeight(it);

						JOptionPane.showMessageDialog(null, "commiting to resource property: " + 
							 capacity[it] +"\n" + isStatic[it] + "\n" + resType[it] + "\n" + x[it] + "\n" + y[it] + "\n" + width[it] + "\n" + height[it]);
						
						iface.resource(saveInt, isStatic[it], capacity[it], resType[it], x[it], y[it], width[it], height[it]);
					}
				}
			}
			else if(commitingLayer.getInputType() == CRISIS)
			{
				if(commitingLayer instanceof PolygonLayer)
				{
					int numProp = ((PolygonLayer)commitingLayer).size();
					String[] crisType = new String[numProp];
					int[] time = new int[numProp];
					double[] x = new double[numProp];
					double[] y = new double[numProp];
					double[] width = new double[numProp];
					double[] height = new double[numProp];

					for(int it = 0; it < numProp; it++)
					{
						Property p = ((PolygonLayer)commitingLayer).indexGetProperties(it);
						crisType[it] = ((CrisisProperty)p).getCrisType();
						time[it] = ((CrisisProperty)p).getTime();
						x[it] = ((PolygonLayer)commitingLayer).getX(it);
						y[it] = ((PolygonLayer)commitingLayer).getY(it);
						width[it] = ((PolygonLayer)commitingLayer).getWidth(it);
						height[it] = ((PolygonLayer)commitingLayer).getHeight(it);

						JOptionPane.showMessageDialog(null, "commiting to crisis property: " + 
							crisType[it] +"\n" + time[it] + "\n" + x[it] + "\n" + y[it] + "\n" + width[it] + "\n" + height[it]);
						
						iface.crisis(saveInt, time[it], crisType[it], x[it], y[it], width[it], height[it]);
					}
				}
			}
			else
			{
				JOptionPane.showMessageDialog(null, "shouldn't get here!");
				return;
			}
		}
	}
    
    private class AddLayerAction extends AbstractAction
    {
        String type;
        
        public AddLayerAction(String type)
        {
            super(type);
            this.type = type;           
        }
        
        public void actionPerformed(ActionEvent evt)
        {
            String name = JOptionPane.showInputDialog(null, "Name of the Layer:");
            
            if(layers.containsKey(name))
            {
                JOptionPane.showMessageDialog(null, name + " is already a layer.");
                return;
            }
            
            if(type.compareTo("Resource Points") == 0 || type.compareTo("Entry Points") == 0 || 
			   type.compareTo("Crisis Points") == 0 || type.compareTo("Obstacles Points") == 0 || 
			   type.compareTo("Personnel Points") == 0 || type.compareTo("People Points") == 0)
			{
				active_layer = new PointLayer(name);	

				if (type.compareTo("Resource Points") == 0)
					active_layer.setInputType(RESOURCE);
				else if (type.compareTo("Entry Points") == 0)
					active_layer.setInputType(ENTRY);
				else if (type.compareTo("Crisis Points") == 0)
					active_layer.setInputType(CRISIS);
				else if (type.compareTo("Obstacles Points") == 0)
					active_layer.setInputType(OBSTACLE);
				else if (type.compareTo("Personnel Points") == 0)
					active_layer.setInputType(PERSONNEL);
				else if (type.compareTo("People Points") == 0)
					active_layer.setInputType(PEOPLE);
			}
            else if(type.compareTo("Resource Lines") == 0 || type.compareTo("Entry Lines") == 0 || 
					type.compareTo("Crisis Lines") == 0 || type.compareTo("Obstacles Lines") == 0 || 
					type.compareTo("Personnel Lines") == 0 || type.compareTo("People Lines") == 0)
			{
				active_layer = new LineLayer(name);
                
				if (type.compareTo("Resource Lines") == 0)
					active_layer.setInputType(RESOURCE);
				else if (type.compareTo("Entry Lines") == 0)
					active_layer.setInputType(ENTRY);
				else if (type.compareTo("Crisis Lines") == 0)
					active_layer.setInputType(CRISIS);
				else if (type.compareTo("Obstacles Lines") == 0)
					active_layer.setInputType(OBSTACLE);
				else if (type.compareTo("Personnel Lines") == 0)
					active_layer.setInputType(PERSONNEL);
				else if (type.compareTo("People Lines") == 0)
					active_layer.setInputType(PEOPLE);
			}
            else if(type.compareTo("Resource Polygons") == 0 || type.compareTo("Entry Polygons") == 0 || 
					type.compareTo("Crisis Polygons") == 0 || type.compareTo("Obstacles Polygons") == 0 || 
					type.compareTo("Personnel Polygons") == 0 || type.compareTo("People Polygons") == 0)
			{
				active_layer = new PolygonLayer(name);
				
				if (type.compareTo("Resource Polygons") == 0)
					active_layer.setInputType(RESOURCE);
				else if (type.compareTo("Entry Polygons") == 0)
					active_layer.setInputType(ENTRY);
				else if (type.compareTo("Crisis Polygons") == 0)
					active_layer.setInputType(CRISIS);
				else if (type.compareTo("Obstacles Polygons") == 0)
					active_layer.setInputType(OBSTACLE);
				else if (type.compareTo("Personnel Polygons") == 0)
					active_layer.setInputType(PERSONNEL);
				else if (type.compareTo("People Polygons") == 0)
					active_layer.setInputType(PEOPLE);
			}
            else if(type.compareTo("Resource Grid") == 0 || type.compareTo("Entry Grid") == 0 || 
					type.compareTo("Crisis Grid") == 0 || type.compareTo("Obstacles Grid") == 0 || 
					type.compareTo("Personnel Grid") == 0 || type.compareTo("People Grid") == 0)
            {
                int grid_x = 0, grid_y = 0;
                do
                {
                    String grid = JOptionPane.showInputDialog(null, "Subdivision in x:");
                    try
                    {
                        grid_x = new Integer(grid).intValue();
                    }
                    catch(NumberFormatException e)
                    {
                        grid_x = 0;
                    }
                }
                while(grid_x == 0);
                
                do
                {
                    String grid = JOptionPane.showInputDialog(null, "Subdivision in y:");
                    try
                    {
                        grid_y = new Integer(grid).intValue();
                    }
                    catch(NumberFormatException e)
                    {
                        grid_y = 0;
                    }
                }
                while(grid_y == 0);
                
                active_layer = new GridLayer(name, st.xmin, st.ymin, st.wreal, st.hreal, grid_x, grid_y);
                repaint();

				if (type.compareTo("Resource Grid") == 0)
					active_layer.setInputType(RESOURCE);
				else if (type.compareTo("Entry Grid") == 0)
					active_layer.setInputType(ENTRY);
				else if (type.compareTo("Crisis Grid") == 0)
					active_layer.setInputType(CRISIS);
				else if (type.compareTo("Obstacles Grid") == 0)
					active_layer.setInputType(OBSTACLE);
				else if (type.compareTo("Personnel Grid") == 0)
					active_layer.setInputType(PERSONNEL);
				else if (type.compareTo("People Grid") == 0)
					active_layer.setInputType(PEOPLE);
            }
            else
			{
                System.err.println("It is not suppose to come here!!");
			}
            
            layers.put(name, active_layer);
            
            JRadioButtonMenuItem nmenu = new JRadioButtonMenuItem(new SetLayerAction(name));
            b_group.add(nmenu);        
            nmenu.setSelected(true);
            menu_active.add(nmenu);
            
            JCheckBoxMenuItem dmenu = new JCheckBoxMenuItem(new DrawLayerAction(name));
            dmenu.setSelected(true);            
            menu_draw.add(dmenu);
            
            JMenuItem delmenu = new JMenuItem(new DeleteLayerAction(name));
            menu_delete.add(delmenu);
            
            menu_color.setForeground(active_layer.getColor());
        }
    }
    
    private class SetLayerAction extends AbstractAction
    {
        String name = null;
        
        public SetLayerAction(String name)
        {
            super(name);
            this.name = name;
        }
        
        public void actionPerformed(ActionEvent evt)
        {
            active_layer = layers.get(name);
            if(active_layer != null)
                menu_color.setForeground(active_layer.getColor());
            else
                menu_color.setForeground(Color.black);
        }
    }

    private class DrawLayerAction extends AbstractAction
    {
        String name = null;
        
        public DrawLayerAction(String name)
        {
            super(name);
            this.name = name;
        }
        
        public void actionPerformed(ActionEvent evt)
        {
            layers.get(name).setVisible(!layers.get(name).getVisible());
            repaint();
        }
    }

    private class DeleteLayerAction extends AbstractAction
    {
        String name;
        
        public DeleteLayerAction(String name)
        {
            super(name);
            this.name = name;           
        }
        
        public void actionPerformed(ActionEvent evt)
        {
            int opt = JOptionPane.showConfirmDialog(null, "Delete " + name + "?", "Delete layer", JOptionPane.YES_NO_OPTION);
            
            if(opt == JOptionPane.YES_OPTION)
            {
                Layer l = layers.remove(name);
                
                if(l == active_layer)
                {
                    menu_color.setForeground(Color.black);
                    menu_none.setSelected(true);
                    active_layer = null;
                }
                
                for(int i = 0; i < menu_active.getItemCount(); i++)
                    if(menu_active.getItem(i).getText().compareTo(name) == 0)
                        menu_active.remove(i);
                for(int i = 0; i < menu_draw.getItemCount(); i++)
                    if(menu_draw.getItem(i).getText().compareTo(name) == 0)
                        menu_draw.remove(i);
                for(int i = 0; i < menu_delete.getItemCount(); i++)
                    if(menu_delete.getItem(i).getText().compareTo(name) == 0)
                        menu_delete.remove(i);
                
                repaint();
            }
        }
    }        
}

