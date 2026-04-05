package discovader;
import robocode.*;
import discovader.RobocodeUtils;
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Color;
import static robocode.util.Utils.normalRelativeAngle;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.util.HashMap;
import java.util.Iterator;

/**
 * DiscoRobot - abstract advanced robot that does stuff
 */
abstract class DiscoRobot extends AdvancedRobot
{
	Enemy target;
	HashMap<String,Enemy> targets;
	final double PI = Math.PI;
	double firePower;
	private int radarDirection=1;
	
	Point dest=null; // waypoint we are currently headed towards
	double threshold=80; // number of pixels we must get within before moving to next
	
	int repeatedHits=0;
	int lastHitTurn=0;
	double wallBuffer=50;
	RobocodeUtils ru=null;
	BattleField bf=null;
	double defaultStrength=100;
	
	public void onPaint(Graphics2D g)
	{
		if (dest==null) return;
		g.setColor(new Color(193, 205, 193));
		g.fillOval((int)dest.getX(), (int)dest.getY(), 1, 1);
		g.drawOval((int)(dest.getX()-(threshold/2))
			, (int)(dest.getY()-(threshold/2))
			, (int)threshold, (int)threshold);
	}

	public void onRobotDeath(RobotDeathEvent e) 
	{
		Enemy en = targets.get(e.getName());
		if (en!=null)
			en.live = false;		
	}
	
	void doFirePower() 
	{
		//firePower=3;
		//return;
		
		if (target==null) return;
		//selects a bullet power based on our distance away from the target
		firePower = 400/target.distance;
		if (firePower > 3) 
		{
			firePower = 3;
		}
		
	}
	
	void doScannedEventsQueue()
	{
		for (ScannedRobotEvent e : getScannedRobotEvents())
		{
			catalogScan(e); 
		}
	}
	
	/**keep the scanner turning**/
	void doScanner() 
	{
		setTurnRadarLeftRadians(2*PI);
	}

	/**
	 * predicts the time of the intersection between the  bullet and the target based on 
	 * a simple iteration.  It then moves the gun to the correct angle to fire on the target.
	 */
	void doGun() 
	{
		if (target==null) return;
    	long time;
    	long nextTime;
    	Point p=new Point();
    	p.setLocation(target.x, target.y);
    	for (int i = 0; i < 10; i++)
		{
        	nextTime = 
    			(int)Math.round((getRange(getX(),getY(),p.x,p.y)/(20-(3*firePower))));
        	time = getTime() + nextTime;
        	p = target.guessPosition(time, getTime());
    	}
		p=bf.wallBufferPoint(p, 0);
		Graphics2D g=getGraphics();
		g.setColor(Color.cyan);
		g.drawOval((int)p.getX()-5, (int)p.getY()-5, 10, 10);
		java.awt.geom.Line2D.Double line=new java.awt.geom.Line2D.Double();
		line.setLine(p, target);
		g.draw(line);
    	/**Turn the gun to the correct angle**/
    	double gunOffset = getGunHeadingRadians() 
			- (Math.PI/2 - Math.atan2(p.y - getY(), p.x - getX()));
    	setTurnGunLeftRadians(normaliseBearing(gunOffset));
	}

	void doGun2()
	{
		if (target==null) return;
		Intercept intercept = new Intercept();
		intercept.calculate
		(
			getRobotLocation().x,
			getRobotLocation().y,
			target.x,
			target.y,
			target.heading,
			target.speed,
			firePower,
			0 // Angular velocity
		);
		
		// Helper function that converts any angle into  
		// an angle between +180 and -180 degrees.
		double turnAngle = normalRelativeAngleDegrees(
			intercept.bulletHeading_deg - getGunHeading());
		
		// Move gun to target angle
		setTurnGunRight(turnAngle);
		
		Graphics2D g=getGraphics();
		g.setColor(Color.cyan);
		g.drawOval((int)intercept.impactPoint.getX()-5
			, (int)intercept.impactPoint.getY()-5, 10, 10);
		
		if (Math.abs(turnAngle) <= intercept.angleThreshold) 
		{
			// Ensure that the gun is pointing at the correct angle
			if (bf.contains(intercept.impactPoint))
			{
				// Ensure that the predicted impact point is within 
				// the battlefield
				fire(firePower);
			}
		}
	}

	void catalogScan(ScannedRobotEvent e)
	{
		Enemy en;
		if (targets.containsKey(e.getName())) 
		{
			en = (Enemy)targets.get(e.getName());
		} else 
		{
			en = new Enemy();
			targets.put(e.getName(),en);
		}
		//the next line gets the absolute bearing to the point where the bot is
		double absbearing_rad = (getHeadingRadians()+e.getBearingRadians())%(2*PI);
		//this section sets all the information about our target
		en.name = e.getName();
		double h = normaliseBearing(e.getHeadingRadians() - en.heading);
		h = h/(getTime() - en.ctime);
		en.changehead = h;
		//works out the x coordinate of where the target is
		//en.x = getX()+Math.sin(absbearing_rad)*e.getDistance();
		//works out the y coordinate of where the target is
		//en.y = getY()+Math.cos(absbearing_rad)*e.getDistance();
		en.setLocation(getX()+Math.sin(absbearing_rad)*e.getDistance()
			, getY()+Math.cos(absbearing_rad)*e.getDistance());
		en.bearing = e.getBearingRadians();
		en.heading = e.getHeadingRadians();
		en.ctime = getTime();				//game time at which this scan was produced
		en.speed = e.getVelocity();
		en.distance = e.getDistance();	
		en.live = true;
		en.strength=defaultStrength;
		if ((en.distance < target.distance)||(target.live == false)) 
		{
			target = en;
		}
	}
	
	public void aimGunAtCenter()
	{
		setTurnGunRightRadians(RobocodeUtils.normalRelativeAngleRadians(
			RobocodeUtils.absoluteBearingRadians(getRobotLocation()
			, bf.getCenter()) - getGunHeadingRadians()));
	}
	
    public void handleOnHitRobot(HitRobotEvent e)
    {
		// keep track of how often we are hitting to detect possible suicide by collision
		// in that case, change tactic
		if (getRoundNum()-lastHitTurn<5)
		{
			repeatedHits++;
		}
		lastHitTurn=getRoundNum();
		
		switch (repeatedHits%8)
		{
			case 0:
			case 1:
				//out.println("too many collisions, headed to center");
				//RobocodeUtils.goQuickTo(this, RobocodeUtils.getCenter(this));
				//break;
			case 2:
			case 3:
				out.println("too many collisions, headed to nearest corner");
				goQuickTo(bf.wallBufferPoint(
					bf.getNearestCorner(getRobotLocation()), wallBuffer));
				break;
			case 4:
			case 5:
				out.println("too many collisions, reversing direction");
				hitRobotStrategy(e);
				break;
			default:
				out.println("too many collisions, headed to farthest corner");
				goQuickTo(bf.wallBufferPoint(
					bf.getFarthestCorner(getRobotLocation()), wallBuffer));
				break;
		}
	}
	public void hitRobotStrategy(HitRobotEvent e)
	{
	}
	/**
     * Fire with scaled energy based on distance to target assuming more energy
     * is worthwhile for closer targets as the chances of hitting the target
     * increase with the target being closer.
     *
     * TODO: factor in robot's energy level and possibly other factors.
     */
    public void smartFire(double robotDistance) 
	{
        if (robotDistance > 300 || getEnergy() < 15) 
		{
            fire(1);
        } else if (robotDistance > 100) 
		{
            fire(2);
        } else 
		{
            fire(3);
        }
    }
	public Point getLocation()
	{
		return getRobotLocation();
	}
    public Point getRobotLocation() 
    {
        Point p=new Point();
        p.setLocation(getX(), getY());
        return p;
    }
	public void goTo(Point point) 
	{
		goTo(point, true);
	}
    public void goTo(Point point, boolean goForward) 
    {
        double offset=0;
        if (!goForward) offset=java.lang.Math.PI;
        setTurnRightRadians(
            RobocodeUtils.normalRelativeAngleRadians(
                RobocodeUtils.absoluteBearingRadians(
                getRobotLocation(), point
                ) - getHeadingRadians() + offset
            )
         );
        if (goForward)
        {
            setAhead(getRobotLocation().distance(point));
        } else
        {
            setAhead(getRobotLocation().distance(point)*-1);
        }
    }
    /**
     * Sets motion that will take robot forward or backward to the specified
     * Point depending on which is more efficient.
     */
    public Point goQuickTo(Point point) 
    {
        double offset=java.lang.Math.PI;
        boolean goForward=true;
        double angle=RobocodeUtils.normalRelativeAngleRadians(
            RobocodeUtils.absoluteBearingRadians(getRobotLocation(), point) 
            - getHeadingRadians());
        if (Math.abs(angle) > offset/2) 
        {
            if (angle > 0.0) { angle-=offset; }
            else { angle+=offset; }
            goForward=false;
        }
        setTurnRightRadians(angle);
        if (goForward)
        {
            setAhead(getRobotLocation().distance(point));
        } else
        {
            setAhead(getRobotLocation().distance(point)*-1);
        }
		return point;
    }
    /**
     * Same as goQuickTo but does not necessarily work.
     */
    public void efficientlyGoTo(Point point) 
    {
        Point location=new Point();
        location.setLocation(getX(), getY());
        double distance = location.distance(point);
        double angle = normalRelativeAngle(
            RobocodeUtils.absoluteBearing(location, point) - getHeading());
        if (Math.abs(angle) > 90.0) {
            distance *= -1.0;
            if (angle > 0.0) {
                angle -= 180.0;
            }
            else {
                angle += 180.0;
            }
        }
        setTurnRight(angle);
        setAhead(distance);
    }
    /**
     * Sets motion to turn the robot to face the specified Point.
     */
    public void facePoint(Point point)
    {
        setTurnRightRadians(RobocodeUtils.normalRelativeAngleRadians(
			RobocodeUtils.absoluteBearingRadians(getRobotLocation(), point) 
			- getHeadingRadians()));
        waitFor(new TurnCompleteCondition(this));
    }

	public void onCustomEvent(CustomEvent e)
	{
		if (e.getCondition() instanceof RadarTurnCompleteCondition) sweep();
	}

	//if a bearing is not within the -pi to pi range, alters it to provide the shortest angle
	double normaliseBearing(double ang) 
	{
		if (ang > PI)
			ang -= 2*PI;
		if (ang < -PI)
			ang += 2*PI;
		return ang;
	}
	
	//if a heading is not within the 0 to 2pi range, alters it to provide the shortest angle
	double normaliseHeading(double ang) 
	{
		if (ang > 2*PI)
			ang -= 2*PI;
		if (ang < 0)
			ang += 2*PI;
		return ang;
	}

	//returns the distance between two x,y coordinates
	public double getRange( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = Math.sqrt( xo*xo + yo*yo );
		return h;	
	}
	
	protected void sweep()
	{
		double maxBearingAbs=0, maxBearing=0;
		int scannedBots=0;
		Iterator iterator = targets.values().iterator();
		
		while(iterator.hasNext())
		{
			Enemy tmp = (Enemy)iterator.next();
			if (tmp!=null && tmp.isUpdated(getTime()))
			{
				double bearing=normalRelativeAngle(
					getHeading() + tmp.bearing
					- getRadarHeading());
				if (Math.abs(bearing)>maxBearingAbs)
				{
					maxBearingAbs=Math.abs(bearing); 
					maxBearing=bearing; 
				}
				scannedBots++;
			}
		}

		double radarTurn=180*radarDirection;
		if (scannedBots==getOthers()) 		
			radarTurn=maxBearing+sign(maxBearing)*22.5; 

		setTurnRadarRight(radarTurn);
		radarDirection=(int)sign(radarTurn);
	}
	
	private double sign(double number)
	{
		if (number<0) return -1;
		else return 1;
	}
}