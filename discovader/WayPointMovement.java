package discovader;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.awt.Point;
import discovader.BattleField;
import discovader.Enemy;
import discovader.ContinuousList;
import discovader.RepeatingList;
import discovader.OscillatingList;

/**
 * WayPointMovement2
 */
public class WayPointMovement
{
	public Point next=null;
	Point last=null;
	ContinuousList<Point> wayPoints=null;
	double stuckThreshold=10.0;
	double wallBuffer=10.0;
	private boolean oscillate=false; // versus repeat
	BattleField bf=null;
	public double closeEnough=40.0;
	private boolean reverse=false;
	
	public WayPointMovement(BattleField b)
	{
		bf=b;
	}
	public void setOscillate(boolean isOscillate)
	{
		if (isOscillate!=oscillate)
		{
			if (wayPoints!=null)
			{
				ArrayList<Point> tmp=new ArrayList<Point>();
				tmp.addAll(wayPoints);
				wayPoints.clear();
				if (isOscillate)
				{
					wayPoints=new OscillatingList<Point>();
				}
				else
				{
					wayPoints=new RepeatingList<Point>();
				}
				wayPoints.addAll(tmp);
			}
			oscillate=isOscillate;
		}
	}
	public void setWayPoints(List<Point> w)
	{
		if (oscillate)
			wayPoints=new OscillatingList<Point>();
		else
			wayPoints=new RepeatingList<Point>();
		wayPoints.addAll(w);
	}
	
	public boolean reverse()
	{
		return reverse=!reverse;
	}
	public Point skip(int count)
	{
		for (int i=0; i<count; i++)
			getNext(last, true);
		return next;
	}
	public void setNext(Point point)
	{
		try
		{
			wayPoints.setNext(point);
			next=wayPoints.getCurrent();
		}
		catch (Exception e)
		{
			next=point;
		}
	}
	public Point getNext(Point here)
	{
		return getNext(here, false);
	}
	public Point getNext(Point here, boolean force)
	{
		// if we didn't have a destination before, default to closest
		if (next==null)
		{
			setNext(getNearestPoint(here));
			return next;
		}
		// if we're not close enough to the current destination
		// stick with current destination
		if (!force)
		{
			if (here.distance(next)>closeEnough) return next;
		}
		
		// we're ready for a new destination
		last=next;
		if (reverse)
			next=bf.wallBufferPoint(wayPoints.getPrevious(), wallBuffer);
		else
			next=bf.wallBufferPoint(wayPoints.getNext(), wallBuffer);
		return next;
    }
	public Point getNearestPoint(Point here)
	{
		return getNearestPoint(wayPoints, here);
	}
	public Point getNearestPoint(List<Point> source, Point here)
	{
		if (source==null || source.size()==0) return null;
		Point p=null;
		int r=0;;
		double minDistance=bf.width*bf.height;
		Iterator<Point> it=source.iterator();
		while (it.hasNext())
		{
			p=it.next();
			if (here.distance(p)<minDistance)
			{
				minDistance=here.distance(p);
				r=source.indexOf(p);
			}
		}
		//System.out.println(source.toString());
		//System.out.println("r="+r);
		return source.get(r);
	}
	public List<Point> getNearPoints(List<Point> source, Point here
		, double range)
	{
		List<Point> results=new ArrayList<Point>();
		Point p=null;
		Iterator<Point> it=source.iterator();
		while (it.hasNext())
		{
			p=it.next();
			if (here.distance(p)<=range)
			{
				results.add(p);
			}
		}
		if (results.size()<1)
			results.add(getNearestPoint(source, here));
		return results;
	}
	public Point getRemotestPoint(Collection<Enemy> obstacles)
	{
		Point p=null; // waypoint point iteration
		Point o=null; // tmp obstacle point (enemy)
		Point t=null; // target point (with max d)
		double max=-1;
		double d=0;
		Iterator<Point> it=wayPoints.iterator();
		while (it.hasNext())
		{
			d=0;
			p=it.next();
			// go through each obstacle and sum distances
			Iterator<Enemy> ob=obstacles.iterator();
			while (ob.hasNext())
			{
				o=ob.next();
				d+=p.distance(o);
			}
			if (d>max)
			{
				max=d;
				t=p;
			}
		}
		return t;
	}
	public Point getWeightedRemotestPoint(Collection<Enemy> obstacles
		, Point here)
	{
		return getWeightedRemotestPoint(wayPoints, obstacles, here);
	}
	public Point getWeightedRemotestPoint(List<Point> source
		, Collection<Enemy> obstacles, Point here)
	{
		Point p=null; // waypoint iteration
		Enemy o=null; // tmp obstacle point (enemy)
		Point t=null; // target point (with max d)
		double max=999999999;
		double strength=0;
		Iterator<Point> it=source.iterator();
		while (it.hasNext())
		{
			strength=0;
			p=it.next();
			// go through each obstacle and sum weighted strength
			Iterator<Enemy> ob=obstacles.iterator();
			while (ob.hasNext())
			{
				o=ob.next();
				strength+=o.strength/Math.pow(p.distance(o)+1, 2)+1;
			}
			if (strength<max)
			{
				max=strength;
				t=p;
			}
		}
		return t;
	}
}
